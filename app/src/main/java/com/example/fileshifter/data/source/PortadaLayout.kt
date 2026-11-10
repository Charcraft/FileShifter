package com.example.fileshifter.data.source

import com.example.fileshifter.data.model.DatosEntrega
import com.example.fileshifter.data.model.PlantillaEntrega
import com.example.fileshifter.data.model.PosicionLogo
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlin.math.max
import kotlin.math.min

/** Color en componentes 0..1 para poder dibujarlo igual en PDF y en la vista previa. */
data class ColorPortada(val r: Float, val g: Float, val b: Float)

/** Rectángulo decorativo. La coordenada Y se mide desde la base de la página (como en PDF). */
data class RectPortada(
    val x: Float,
    val y: Float,
    val ancho: Float,
    val alto: Float,
    val color: ColorPortada
)

/** Una línea de texto ya posicionada. Y es el borde superior de la caja de línea. */
data class TextoPortada(
    val texto: String,
    val x: Float,
    val yArriba: Float,
    val tamano: Float,
    val negrita: Boolean,
    val color: ColorPortada,
    val centrado: Boolean = true,
    val espaciado: Float = 0f,
    val anchoTexto: Float = 0f
)

data class LogoPortada(
    val uri: String,
    val x: Float,
    val y: Float,
    val ancho: Float,
    val alto: Float,
    val opacidad: Float = 1f
)

/**
 * Resultado de maquetar una portada. Se calcula una sola vez y lo consumen tanto el
 * generador de PDF como la vista previa de Compose, de modo que ambos son idénticos.
 */
data class PortadaLayout(
    val ancho: Float,
    val alto: Float,
    val fondos: List<RectPortada>,
    val textos: List<TextoPortada>,
    val logo: LogoPortada?
) {
    val proporcion: Float get() = ancho / alto
}

object PortadaLayoutBuilder {
    const val ANCHO_A4 = 595.28f
    const val ALTO_A4 = 841.89f

    private val NAVY = ColorPortada(0.047f, 0.231f, 0.369f)
    private val ACENTO = ColorPortada(0.055f, 0.647f, 0.914f)
    private val TEXTO = ColorPortada(0.043f, 0.145f, 0.251f)
    private val SECUNDARIO = ColorPortada(0.392f, 0.467f, 0.549f)
    private val MUTED = ColorPortada(0.455f, 0.529f, 0.608f)
    private val LINEA = ColorPortada(0.878f, 0.918f, 0.953f)
    private val BLANCO = ColorPortada(1f, 1f, 1f)

    private val ESCALAS = floatArrayOf(
        1f, 0.96f, 0.92f, 0.88f, 0.84f, 0.80f, 0.76f,
        0.72f, 0.68f, 0.64f, 0.60f, 0.56f
    )

    /**
     * Maqueta la portada. Si el contenido es muy largo reduce los tamaños de forma
     * progresiva (hasta 56 %) para que todo siga entrando y se vea equilibrado.
     */
    fun componer(
        plantilla: PlantillaEntrega,
        datos: DatosEntrega,
        aspectoLogo: Float? = null
    ): PortadaLayout {
        var ultimo: PortadaLayout? = null
        ESCALAS.forEach { escala ->
            val intento = construir(plantilla, datos, aspectoLogo, escala)
            ultimo = intento.layout
            if (intento.cabe) return intento.layout
        }
        return ultimo ?: construir(plantilla, datos, aspectoLogo, 1f).layout
    }

    private data class Intento(val layout: PortadaLayout, val cabe: Boolean)

    private fun construir(
        plantilla: PlantillaEntrega,
        datos: DatosEntrega,
        aspectoLogo: Float?,
        escala: Float
    ): Intento {
        val ancho = ANCHO_A4
        val alto = ALTO_A4
        val margenLados = if (plantilla == PlantillaEntrega.OFICIO) 66f else 68f
        val anchoContenido = ancho - margenLados * 2f
        val textos = mutableListOf<TextoPortada>()
        val fondos = decoraciones(plantilla, ancho, alto).toMutableList()
        val logo = calcularLogo(plantilla, datos, aspectoLogo, ancho, alto, margenLados)

        val anchoCabecera = if (logo != null &&
            datos.posicionLogo != PosicionLogo.FONDO_CENTRO &&
            plantilla != PlantillaEntrega.OFICIO
        ) {
            max(220f, anchoContenido - logo.ancho - 28f)
        } else {
            anchoContenido
        }

        val cabecera = Flujo(anchoCabecera, escala)
        val inicioCabecera = alto - margenCabecera(plantilla)
        when (plantilla) {
            PlantillaEntrega.ESCOLAR -> {
                if (datos.institucion.isNotBlank()) {
                    cabecera.linea(datos.institucion.uppercase(), 13f, true, NAVY, 1.1f, maxLineas = 2)
                    if (datos.carrera.isNotBlank()) {
                        cabecera.salto(5f)
                        cabecera.linea(datos.carrera, 10f, false, SECUNDARIO, 0.2f, maxLineas = 2)
                    }
                    cabecera.salto(13f)
                    cabecera.regla(170f, 1.2f, ACENTO, 0f, 0f)
                } else if (datos.carrera.isNotBlank()) {
                    cabecera.linea(datos.carrera, 10f, false, SECUNDARIO, 0.2f, maxLineas = 2)
                    cabecera.salto(13f)
                    cabecera.regla(170f, 1.2f, ACENTO, 0f, 0f)
                }
            }

            PlantillaEntrega.OFICIO -> {
                if (datos.institucion.isNotBlank()) {
                    cabecera.linea(datos.institucion.uppercase(), 13f, true, BLANCO, 1.3f, maxLineas = 2)
                    if (datos.carrera.isNotBlank()) {
                        cabecera.salto(5f)
                        cabecera.linea(datos.carrera, 9.5f, false, ColorPortada(0.82f, 0.89f, 0.95f), 0.2f, maxLineas = 2)
                    }
                } else if (datos.carrera.isNotBlank()) {
                    cabecera.linea(datos.carrera, 9.5f, false, ColorPortada(0.82f, 0.89f, 0.95f), 0.2f, maxLineas = 2)
                } else {
                    cabecera.linea("ENTREGA ACADÉMICA", 10f, true, ColorPortada(0.82f, 0.89f, 0.95f), 2f)
                }
            }

            PlantillaEntrega.MINIMAL -> {
                if (datos.institucion.isNotBlank()) {
                    cabecera.linea(datos.institucion.uppercase(), 9.5f, true, SECUNDARIO, 1.8f, maxLineas = 2)
                    if (datos.carrera.isNotBlank()) {
                        cabecera.salto(6f)
                        cabecera.linea(datos.carrera, 9f, false, MUTED, 0.2f, maxLineas = 2)
                    }
                } else if (datos.carrera.isNotBlank()) {
                    cabecera.linea(datos.carrera, 9f, false, MUTED, 0.2f, maxLineas = 2)
                }
            }
        }

        val cabeceraAlto = cabecera.altoTotal()
        var mainTop = when (plantilla) {
            PlantillaEntrega.OFICIO -> alto - 84f - 34f
            else -> inicioCabecera - cabeceraAlto - 30f
        }
        if (logo != null && datos.posicionLogo != PosicionLogo.FONDO_CENTRO) {
            mainTop = min(mainTop, logo.y - 24f)
        }

        val main = Flujo(anchoContenido, escala)
        val kickerColor = if (plantilla == PlantillaEntrega.MINIMAL) SECUNDARIO else ACENTO
        val tituloColor = if (plantilla == PlantillaEntrega.MINIMAL) TEXTO else NAVY
        val tituloTam = when (plantilla) {
            PlantillaEntrega.ESCOLAR -> 29f
            PlantillaEntrega.OFICIO -> 26f
            PlantillaEntrega.MINIMAL -> 33f
        }
        val nombreTam = if (plantilla == PlantillaEntrega.OFICIO) 17f else 18f

        if (datos.materia.isNotBlank()) {
            main.linea(
                datos.materia.uppercase(),
                if (plantilla == PlantillaEntrega.MINIMAL) 10f else 10.5f,
                true, kickerColor, 1.7f, maxLineas = 2
            )
            main.salto(10f)
        }
        if (datos.titulo.isNotBlank()) {
            main.parrafo(datos.titulo, tituloTam, true, tituloColor, 1.2f)
        }

        when (plantilla) {
            PlantillaEntrega.ESCOLAR -> {
                main.salto(16f)
                main.regla(44f, 3f, ACENTO, 0f, 0f)
                main.salto(24f)
            }

            PlantillaEntrega.OFICIO -> {
                main.salto(18f)
                main.regla(anchoContenido * 0.6f, 0.9f, LINEA, 0f, 0f)
                main.salto(22f)
            }

            PlantillaEntrega.MINIMAL -> main.salto(28f)
        }

        if (plantilla != PlantillaEntrega.MINIMAL) {
            main.linea("PRESENTA", 8.5f, true, MUTED, 1.6f)
            main.salto(7f)
        }
        if (datos.nombre.isNotBlank()) {
            main.linea(datos.nombre, nombreTam, true, NAVY, 0.2f)
        }
        if (datos.matricula.isNotBlank()) {
            main.salto(6f)
            main.linea("Matrícula: ${datos.matricula}", 10.5f, false, SECUNDARIO)
        }
        if (datos.docente.isNotBlank()) {
            main.salto(3f)
            main.linea("Docente: ${datos.docente}", 10.5f, false, SECUNDARIO)
        }
        if (datos.fecha.isNotBlank()) {
            main.salto(3f)
            main.linea(datos.fecha, 10.5f, false, SECUNDARIO)
        }
        if (datos.textoExtra.isNotBlank()) {
            main.salto(20f)
            main.parrafo(datos.textoExtra, 10.5f, false, SECUNDARIO, 1.35f)
        }
        if (datos.indice.isNotBlank()) {
            main.salto(24f)
            if (plantilla != PlantillaEntrega.MINIMAL) {
                main.regla(anchoContenido * 0.34f, 0.8f, LINEA, 0f, 10f)
            }
            main.linea("ÍNDICE", 10.5f, true, kickerColor, 1.7f)
            main.salto(8f)
            datos.indice.split("\n").forEach { crudo ->
                val punto = crudo.trim()
                if (punto.isNotEmpty()) {
                    main.parrafo(punto, 9.8f, false, SECUNDARIO, 1.32f)
                }
            }
        }

        val mainBottom = when (plantilla) {
            PlantillaEntrega.ESCOLAR -> 46f
            PlantillaEntrega.OFICIO -> 74f
            PlantillaEntrega.MINIMAL -> 46f
        }
        val altoMain = main.altoTotal()
        val libre = (mainTop - mainBottom) - altoMain
        val cabe = libre >= -0.5f
        val yMain = if (cabe) mainTop - libre * 0.42f else mainTop

        asignar(cabecera, inicioCabecera, ancho, textos, fondos)
        asignar(main, yMain, ancho, textos, fondos, limiteInferior = mainBottom)

        if (plantilla == PlantillaEntrega.OFICIO) {
            fondos += RectPortada(margenLados, 58f, anchoContenido, 0.7f, LINEA)
        }
        val footer = "Generado con FileShifter · sin conexión"
        val footerTam = 7.8f
        textos += TextoPortada(
            texto = footer,
            x = 0f,
            yArriba = when (plantilla) {
                PlantillaEntrega.ESCOLAR -> 22f
                PlantillaEntrega.OFICIO -> 40f
                PlantillaEntrega.MINIMAL -> 30f
            },
            tamano = footerTam,
            negrita = false,
            color = MUTED,
            anchoTexto = medir(footer, false, footerTam, 0f)
        )

        return Intento(
            layout = PortadaLayout(
                ancho = ancho,
                alto = alto,
                fondos = fondos,
                textos = textos,
                logo = logo
            ),
            cabe = cabe
        )
    }

    private fun margenCabecera(plantilla: PlantillaEntrega): Float = when (plantilla) {
        PlantillaEntrega.ESCOLAR -> 46f
        PlantillaEntrega.OFICIO -> 30f
        PlantillaEntrega.MINIMAL -> 66f
    }

    private fun decoraciones(
        plantilla: PlantillaEntrega,
        ancho: Float,
        alto: Float
    ): List<RectPortada> = when (plantilla) {
        PlantillaEntrega.ESCOLAR -> listOf(
            RectPortada(0f, alto - 6f, ancho, 6f, NAVY),
            RectPortada(0f, alto - 9f, ancho, 3f, ACENTO),
            RectPortada(0f, 0f, ancho, 9f, NAVY)
        )

        PlantillaEntrega.OFICIO -> listOf(
            RectPortada(0f, alto - 84f, ancho, 84f, NAVY),
            RectPortada(0f, alto - 88f, ancho, 4f, ACENTO)
        )

        PlantillaEntrega.MINIMAL -> listOf(
            RectPortada((ancho - 56f) / 2f, alto - 42f, 56f, 2.5f, ACENTO)
        )
    }

    private fun calcularLogo(
        plantilla: PlantillaEntrega,
        datos: DatosEntrega,
        aspecto: Float?,
        ancho: Float,
        alto: Float,
        margenLados: Float
    ): LogoPortada? {
        val uri = datos.logoUri
        if (uri.isNullOrBlank() || datos.posicionLogo == PosicionLogo.NINGUNA) return null
        val proporcion = (aspecto ?: 1f).coerceIn(0.08f, 12f)

        if (datos.posicionLogo == PosicionLogo.FONDO_CENTRO) {
            var w = ancho * 0.60f
            var h = w / proporcion
            val maxAlto = alto * 0.46f
            if (h > maxAlto) {
                h = maxAlto
                w = h * proporcion
            }
            return LogoPortada(uri, (ancho - w) / 2f, (alto - h) / 2f, w, h, opacidad = 0.08f)
        }

        val maxAncho = 92f
        val maxAlto = 54f
        var w = maxAncho
        var h = w / proporcion
        if (h > maxAlto) {
            h = maxAlto
            w = h * proporcion
        }
        val y = if (plantilla == PlantillaEntrega.OFICIO) {
            alto - 84f - 16f - h
        } else {
            alto - 44f - h
        }
        val x = if (datos.posicionLogo == PosicionLogo.SUPERIOR_IZQUIERDA) {
            margenLados
        } else {
            ancho - margenLados - w
        }
        return LogoPortada(uri, x, y, w, h)
    }

    private fun asignar(
        flujo: Flujo,
        yInicio: Float,
        anchoPagina: Float,
        textos: MutableList<TextoPortada>,
        fondos: MutableList<RectPortada>,
        limiteInferior: Float = 0f
    ) {
        var y = yInicio
        flujo.items.forEach { item ->
            when (item) {
                is ItemFlujo.Espacio -> y -= item.alto

                is ItemFlujo.Regla -> {
                    y -= item.antes
                    fondos += RectPortada(
                        x = (anchoPagina - item.ancho) / 2f,
                        y = y - item.grosor,
                        ancho = item.ancho,
                        alto = item.grosor,
                        color = item.color
                    )
                    y -= item.grosor + item.despues
                }

                is ItemFlujo.Linea -> {
                    if (y - item.alto >= limiteInferior) {
                        textos += TextoPortada(
                            texto = item.texto,
                            x = (anchoPagina - item.ancho) / 2f,
                            yArriba = y,
                            tamano = item.tamano,
                            negrita = item.negrita,
                            color = item.color,
                            centrado = true,
                            espaciado = item.espaciado,
                            anchoTexto = item.ancho
                        )
                    }
                    y -= item.alto
                }
            }
        }
    }

    internal fun medir(texto: String, negrita: Boolean, tamano: Float, espaciado: Float): Float {
        val fuente = if (negrita) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA
        val base = fuente.getStringWidth(texto) / 1000f * tamano
        return base + espaciado * (texto.length - 1).coerceAtLeast(0)
    }

    private fun envolver(
        texto: String,
        negrita: Boolean,
        tamano: Float,
        maxAncho: Float,
        espaciado: Float
    ): List<String> {
        if (medir(texto, negrita, tamano, espaciado) <= maxAncho) return listOf(texto)

        val salida = mutableListOf<String>()
        var actual = ""

        fun cerrar() {
            if (actual.isNotEmpty()) {
                salida += actual
                actual = ""
            }
        }

        texto.split(" ").filter { it.isNotEmpty() }.forEach { palabra ->
            var resto = palabra
            while (resto.isNotEmpty()) {
                val prueba = if (actual.isEmpty()) resto else "$actual $resto"
                if (medir(prueba, negrita, tamano, espaciado) <= maxAncho) {
                    actual = prueba
                    resto = ""
                } else if (actual.isNotEmpty()) {
                    cerrar()
                } else {
                    var corte = 1
                    while (corte < resto.length &&
                        medir(resto.substring(0, corte + 1), negrita, tamano, espaciado) <= maxAncho
                    ) {
                        corte++
                    }
                    if (corte >= resto.length) {
                        actual = resto
                        resto = ""
                    } else {
                        salida += resto.substring(0, corte)
                        resto = resto.substring(corte)
                    }
                }
            }
        }
        cerrar()
        return salida.ifEmpty { listOf(texto) }
    }

    private fun recortarConPuntos(
        texto: String,
        negrita: Boolean,
        tamano: Float,
        maxAncho: Float,
        espaciado: Float
    ): String {
        var base = texto
        while (base.isNotEmpty() && medir("$base...", negrita, tamano, espaciado) > maxAncho) {
            base = base.dropLast(1)
        }
        return if (base.isEmpty()) texto else "$base..."
    }

    private sealed interface ItemFlujo {
        val alto: Float

        data class Linea(
            val texto: String,
            val tamano: Float,
            val negrita: Boolean,
            val color: ColorPortada,
            val espaciado: Float,
            override val alto: Float,
            val ancho: Float
        ) : ItemFlujo

        data class Espacio(override val alto: Float) : ItemFlujo

        data class Regla(
            val ancho: Float,
            val grosor: Float,
            val color: ColorPortada,
            val antes: Float,
            val despues: Float
        ) : ItemFlujo {
            override val alto: Float get() = antes + grosor + despues
        }
    }

    private class Flujo(private val anchoMax: Float, private val escala: Float) {
        val items = mutableListOf<ItemFlujo>()

        private fun e(valor: Float) = valor * escala

        fun salto(espacio: Float) {
            if (espacio > 0f) items += ItemFlujo.Espacio(e(espacio))
        }

        fun linea(
            texto: String,
            tamano: Float,
            negrita: Boolean,
            color: ColorPortada,
            espaciado: Float = 0f,
            maxLineas: Int? = null
        ) {
            val limpio = normalizar(texto).trim()
            if (limpio.isEmpty()) return
            val tam = e(tamano)
            val esp = e(espaciado)
            var lineas = envolver(limpio, negrita, tam, anchoMax, esp)
            if (maxLineas != null && lineas.size > maxLineas) {
                lineas = lineas.take(maxLineas).toMutableList().also { recortadas ->
                    recortadas[recortadas.lastIndex] = recortarConPuntos(
                        recortadas.last(), negrita, tam, anchoMax, esp
                    )
                }
            }
            lineas.forEach { linea ->
                items += ItemFlujo.Linea(
                    texto = linea,
                    tamano = tam,
                    negrita = negrita,
                    color = color,
                    espaciado = esp,
                    alto = tam * 1.22f,
                    ancho = medir(linea, negrita, tam, esp)
                )
            }
        }

        fun parrafo(
            texto: String,
            tamano: Float,
            negrita: Boolean,
            color: ColorPortada,
            interlineado: Float = 1.35f
        ) {
            val tam = e(tamano)
            normalizar(texto).split("\n").forEach { parrafo ->
                val limpio = parrafo.trim()
                if (limpio.isEmpty()) return@forEach
                envolver(limpio, negrita, tam, anchoMax, 0f).forEach { linea ->
                    items += ItemFlujo.Linea(
                        texto = linea,
                        tamano = tam,
                        negrita = negrita,
                        color = color,
                        espaciado = 0f,
                        alto = tam * interlineado,
                        ancho = medir(linea, negrita, tam, 0f)
                    )
                }
            }
        }

        fun regla(
            ancho: Float,
            grosor: Float,
            color: ColorPortada,
            antes: Float,
            despues: Float
        ) {
            items += ItemFlujo.Regla(ancho, e(grosor), color, e(antes), e(despues))
        }

        fun altoTotal(): Float = items.sumOf { it.alto.toDouble() }.toFloat()
    }

    /** Deja el texto dentro de lo que puede escribir la fuente Helvetica (WinAnsi). */
    private fun normalizar(texto: String): String {
        val salida = StringBuilder(texto.length)
        texto.forEach { caracter ->
            when (caracter) {
                '\u2018', '\u2019', '\u201A', '\u2032' -> salida.append('\'')
                '\u201C', '\u201D', '\u201E', '\u2033' -> salida.append('"')
                '\u2013', '\u2014', '\u2212' -> salida.append('-')
                '\u2026' -> salida.append("...")
                '\u00A0' -> salida.append(' ')
                '\u2022' -> salida.append('-')
                '\t' -> salida.append(' ')
                else -> when {
                    caracter == '\n' || caracter == '\r' -> salida.append(' ')
                    caracter.code in 32..255 -> salida.append(caracter)
                    else -> salida.append(' ')
                }
            }
        }
        return salida.toString()
    }
}
