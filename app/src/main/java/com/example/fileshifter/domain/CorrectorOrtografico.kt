package com.example.fileshifter.domain

data class Sugerencia(
    val inicio: Int,
    val fin: Int,
    val original: String,
    val correccion: String,
    val motivo: String
)

object CorrectorOrtografico {
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

    private val excepciones = setOf(
        "decision", "vision", "mision", "television", "revision", "division", "sesion", "version"
    )

    fun revisar(texto: String): List<Sugerencia> {
        if (texto.isBlank()) return emptyList()
        val sugerencias = mutableListOf<Sugerencia>()

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

            val anterior = texto.take(match.range.first).trimEnd()
            val inicioOracion = anterior.isEmpty() ||
                anterior.endsWith('.') || anterior.endsWith('?') || anterior.endsWith('!')
            if (inicioOracion && palabra.first().isLowerCase()) {
                sugerencias += Sugerencia(
                    inicio = match.range.first,
                    fin = match.range.last + 1,
                    original = palabra,
                    correccion = palabra.replaceFirstChar { it.uppercase() },
                    motivo = "Inicio de oración"
                )
            }
        }

        return sugerencias
            .distinctBy { it.inicio }
            .sortedBy { it.inicio }
    }

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
