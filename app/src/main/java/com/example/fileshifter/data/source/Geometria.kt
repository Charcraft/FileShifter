package com.example.fileshifter.data.source

/** Punto en coordenadas de imagen (pÃ­xeles o normalizado 0..1). */
data class Punto(val x: Float, val y: Float)

/**
 * Las cuatro esquinas de una hoja, en orden: superior izquierda, superior derecha,
 * inferior derecha e inferior izquierda. Las coordenadas estÃ¡n normalizadas (0..1)
 * respecto al ancho y alto de la imagen.
 */
data class Cuadrilatero(
    val supIzq: Punto,
    val supDer: Punto,
    val infDer: Punto,
    val infIzq: Punto
) {
    val puntos: List<Punto> get() = listOf(supIzq, supDer, infDer, infIzq)

    fun normalizado(ancho: Int, alto: Int): Cuadrilatero = Cuadrilatero(
        Punto(supIzq.x / ancho, supIzq.y / alto),
        Punto(supDer.x / ancho, supDer.y / alto),
        Punto(infDer.x / ancho, infDer.y / alto),
        Punto(infIzq.x / ancho, infIzq.y / alto)
    )

    fun escalado(ancho: Int, alto: Int): Cuadrilatero = Cuadrilatero(
        Punto(supIzq.x * ancho, supIzq.y * alto),
        Punto(supDer.x * ancho, supDer.y * alto),
        Punto(infDer.x * ancho, infDer.y * alto),
        Punto(infIzq.x * ancho, infIzq.y * alto)
    )

    /** Gira el marco 90Â° en sentido horario (el mismo giro que sufre la foto). */
    fun girar90(): Cuadrilatero {
        val p = puntos.map { Punto(1f - it.y, it.x) }
        return Cuadrilatero(p[3], p[0], p[1], p[2])
    }

    fun girar(grados: Int): Cuadrilatero {
        val pasos = (((grados % 360) + 360) % 360) / 90
        var actual = this
        repeat(pasos) { actual = actual.girar90() }
        return actual
    }

    fun areaAproximada(): Float {
        val p = puntos
        var suma = 0f
        for (i in 0 until 4) {
            val a = p[i]
            val b = p[(i + 1) % 4]
            suma += a.x * b.y - b.x * a.y
        }
        return kotlin.math.abs(suma) / 2f
    }

    fun pareceMarcoCompleto(): Boolean = puntos.all {
        it.x in -0.01f..1.01f && it.y in -0.01f..1.01f
    } && areaAproximada() > 0.97f

    companion object {
        val COMPLETO = Cuadrilatero(
            Punto(0f, 0f),
            Punto(1f, 0f),
            Punto(1f, 1f),
            Punto(0f, 1f)
        )

        fun centrado(margen: Float = 0.08f): Cuadrilatero = Cuadrilatero(
            Punto(margen, margen * 1.6f),
            Punto(1f - margen, margen * 1.6f),
            Punto(1f - margen, 1f - margen * 1.6f),
            Punto(margen, 1f - margen * 1.6f)
        )
    }
}

/** Estilos de escaneo disponibles en el editor. */
enum class FiltroEscaneo(val etiqueta: String, val descripcion: String) {
    ORIGINAL("Original", "Solo recorta y endereza"),
    MEJORADO("Mejorado", "Blanquea el papel y resalta el texto"),
    GRIS("Grises", "Escala de grises con papel limpio"),
    BLANCO_NEGRO("B/N", "Texto negro, fondo blanco");

    companion object {
        fun desdeNombre(nombre: String?): FiltroEscaneo =
            entries.firstOrNull { it.name == nombre } ?: MEJORADO
    }
}
