package com.example.fileshifter.domain

/**
 * Corrector ortografico offline para espanol.
 *
 * No es IA: usa reglas deterministas y un diccionario de acentuacion.
 * - palabras repetidas ("la la")
 * - palabras sin tilde de uso comun (aqui -> aquí, tambien -> también)
 * - terminaciones -cion / -sion sin tilde
 * - errores frecuentes (havia -> había, haiga -> haya)
 * - mayuscula al inicio de oracion
 *
 * Es lógica pura: se prueba con JUnit, sin Android.
 */
data class Sugerencia(
    val inicio: Int,
    val fin: Int,
    val original: String,
    val correccion: String,
    val motivo: String
)

object CorrectorOrtografico {

    /** Palabras que siempre llevan tilde y suelen escribirse sin ella. */
    private val acentos = mapOf(
        "aqui" to "aquí",
        "asi" to "así",
        "alli" to "allí",
        "ahi" to "ahí",
        "tambien" to "también",
        "despues" to "después",
        "ademas" to "además",
        "quizas" to "quizás",
        "jamas" to "jamás",
        "detras" to "detrás",
        "atraves" to "a través",
        "facil" to "fácil",
        "dificil" to "difícil",
        "util" to "útil",
        "movil" to "móvil",
        "estandar" to "estándar",
        "caracter" to "carácter",
        "examenes" to "exámenes",
        "imagenes" to "imágenes",
        "paginas" to "páginas",
        "matricula" to "matrícula",
        "practica" to "práctica",
        "tecnica" to "técnica",
        "logica" to "lógica",
        "publico" to "público",
        "numero" to "número",
        "proximo" to "próximo",
        "ultimo" to "último",
        "ingles" to "inglés",
        "espanol" to "español",
        "diseno" to "diseño",
        "informacion" to "información",
        "aplicacion" to "aplicación"
    )

    /** Errores frecuentes que no siguen una regla. */
    private val errores = mapOf(
        "havia" to "había",
        "haigan" to "hayan",
        "haiga" to "haya",
        "hicistes" to "hiciste",
        "dijistes" to "dijiste",
        "fuistes" to "fuiste",
        "sabistes" to "sabiste",
        "andastes" to "anduviste",
        "esplicar" to "explicar",
        "esplicacion" to "explicación",
        "escoger" to "escoger",
        "dijieron" to "dijeron",
        "trajieron" to "trajeron",
        "conducido" to "conducido",
        "preveyó" to "previó",
        "renocido" to "reconocido"
    )

    /** Palabras que, respetando la regla, no se corrigen (evita falsos positivos). */
    private val excepciones = setOf(
        "decision", "vision", "mision", "television", "revision", "division", "sesion", "version"
    )

    fun revisar(texto: String): List<Sugerencia> {
        if (texto.isBlank()) return emptyList()
        val sugerencias = mutableListOf<Sugerencia>()

        // 1. Palabras repetidas: "la la", "de de"
        Regex("\\b(\\p{L}+)(\\s+)(\\1)\\b", RegexOption.IGNORE_CASE).findAll(texto).forEach { match ->
            val segunda = match.groups[3]!!
            sugerencias += Sugerencia(
                inicio = segunda.range.first,
                fin = segunda.range.last + 1,
                original = segunda.value,
                correccion = "",
                motivo = "Palabra repetida"
            )
        }

        // 2. Palabras token por token
        Regex("\\p{L}+").findAll(texto).forEach { match ->
            val palabra = match.value
            val minuscula = palabra.lowercase()
            if (palabra.length <= 2) return@forEach

            val correccion = errores[minuscula] ?: acentos[minuscula] ?: reglaTilde(minuscula)
            if (correccion != null && minuscula != correccion.lowercase()) {
                val ajustada = adaptarMayusculas(palabra, correccion)
                if (ajustada != palabra) {
                    sugerencias += Sugerencia(
                        inicio = match.range.first,
                        fin = match.range.last + 1,
                        original = palabra,
                        correccion = ajustada,
                        motivo = if (errores.containsKey(minuscula)) {
                            "Error frecuente"
                        } else {
                            "Falta una tilde"
                        }
                    )
                }
            }

            // 3. Mayuscula al inicio de oracion
            val anterior = texto.take(match.range.first).trimEnd()
            val inicioOracion = anterior.isEmpty() ||
                anterior.endsWith('.') || anterior.endsWith('?') || anterior.endsWith('!')
            if (inicioOracion && palabra.first().isLowerCase()) {
                sugerencias += Sugerencia(
                    inicio = match.range.first,
                    fin = match.range.last + 1,
                    original = palabra,
                    correccion = palabra.replaceFirstChar { it.uppercase() },
                    motivo = "Inicio de oracion"
                )
            }
        }

        return sugerencias
            .distinctBy { it.inicio }
            .sortedBy { it.inicio }
    }

    /** -cion / -sion sin tilde (informacion -> información). */
    private fun reglaTilde(palabra: String): String? {
        if (palabra in excepciones) return null
        return when {
            palabra.endsWith("cion") && palabra.length > 5 -> palabra.dropLast(4) + "ción"
            palabra.endsWith("sion") && palabra.length > 5 -> palabra.dropLast(4) + "sión"
            palabra.endsWith("ciones") && palabra.length > 7 -> palabra.dropLast(6) + "ciones"
            else -> null
        }
    }

    private fun adaptarMayusculas(original: String, correccion: String): String = when {
        original.all { it.isUpperCase() } -> correccion.uppercase()
        original.first().isUpperCase() -> correccion.replaceFirstChar { it.uppercase() }
        else -> correccion
    }

    /** Aplica las sugerencias de una sola vez (de atras hacia adelante). */
    fun aplicar(texto: String, sugerencias: List<Sugerencia>): String {
        val ordenadas = sugerencias.sortedByDescending { it.inicio }
        var resultado = texto
        ordenadas.forEach { sugerencia ->
            if (sugerencia.inicio >= 0 && sugerencia.fin <= resultado.length) {
                resultado = resultado.replaceRange(sugerencia.inicio, sugerencia.fin, sugerencia.correccion)
            }
        }
        return resultado
    }
}
