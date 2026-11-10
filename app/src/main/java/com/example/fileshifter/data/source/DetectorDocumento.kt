package com.example.fileshifter.data.source

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Detector de la hoja de papel dentro de una foto.
 *
 * Estrategia en dos pasadas:
 *  1. Transformada de Hough guiada por la orientación del gradiente (Sobel). Busca las
 *     rectas de los cuatro bordes, cruza las candidatas y elige el cuadrilátero que
 *     mejor se apoya sobre bordes reales de la imagen.
 *  2. Si no hay una propuesta válida, segmenta la zona más brillante (u oscura) de la
 *     imagen y toma sus puntos extremos como esquinas.
 *
 * Todos los puntos que devuelve están normalizados (0..1) respecto a la imagen.
 */
object DetectorDocumento {

    private const val LADO_ANALISIS = 720
    private const val BUCKETS_ANGULO = 180          // resolución de 1 grado
    private const val CANDIDATAS_POR_LADO = 5
    private const val MUESTRAS_LADO = 48

    /** Detecta las esquinas de la hoja a partir de los píxeles ARGB. */
    fun detectar(ancho: Int, alto: Int, pixeles: IntArray): Cuadrilatero? {
        if (ancho < 16 || alto < 16) return null

        val escala = min(1f, LADO_ANALISIS.toFloat() / max(ancho, alto))
        val w = max(16, (ancho * escala).roundToInt())
        val h = max(16, (alto * escala).roundToInt())

        val gris = FloatArray(w * h)
        for (y in 0 until h) {
            val origenY = min(alto - 1, (y / escala).toInt())
            val filaOrigen = origenY * ancho
            val filaDestino = y * w
            for (x in 0 until w) {
                val origenX = min(ancho - 1, (x / escala).toInt())
                val color = pixeles[filaOrigen + origenX]
                gris[filaDestino + x] = 0.299f * ((color shr 16) and 0xFF) +
                    0.587f * ((color shr 8) and 0xFF) +
                    0.114f * (color and 0xFF)
            }
        }
        return detectarGris(gris, w, h)
    }

    /** Núcleo del detector. Devuelve esquinas normalizadas o null si no hay documento. */
    fun detectarGris(entrada: FloatArray, ancho: Int, alto: Int): Cuadrilatero? {
        if (entrada.size < ancho * alto || ancho < 16 || alto < 16) return null
        val suave = desenfoque(entrada, ancho, alto)
        val gradienteX = FloatArray(ancho * alto)
        val gradienteY = FloatArray(ancho * alto)
        sobel(suave, ancho, alto, gradienteX, gradienteY)

        val magnitud = FloatArray(ancho * alto)
        var maximoMagnitud = 1f
        for (i in magnitud.indices) {
            val gx = gradienteX[i]
            val gy = gradienteY[i]
            val m = hypot(gx, gy)
            magnitud[i] = m
            if (m > maximoMagnitud) maximoMagnitud = m
        }
        val umbral = max(10f, otsu(magnitud, maximoMagnitud) * 0.9f)

        val porHough = porHough(
            gradienteX, gradienteY, magnitud, maximoMagnitud, umbral, ancho, alto
        )
        if (porHough != null) return porHough

        return porMascara(suave, ancho, alto)
    }

    // ------------------------------------------------------------------ Hough

    private data class Linea(
        val nx: Float,
        val ny: Float,
        val rho: Float,
        val puntaje: Int
    )

    private fun porHough(
        gx: FloatArray,
        gy: FloatArray,
        magnitud: FloatArray,
        maximoMagnitud: Float,
        umbral: Float,
        ancho: Int,
        alto: Int
    ): Cuadrilatero? {
        val cosenos = FloatArray(BUCKETS_ANGULO)
        val senos = FloatArray(BUCKETS_ANGULO)
        for (b in 0 until BUCKETS_ANGULO) {
            val rad = Math.toRadians(b.toDouble())
            cosenos[b] = cos(rad).toFloat()
            senos[b] = sin(rad).toFloat()
        }

        val diagonal = hypot(ancho.toFloat(), alto.toFloat())
        val desplazamiento = diagonal
        val tamRho = (2 * diagonal).roundToInt() + 3
        val acumulador = Array(BUCKETS_ANGULO) { IntArray(tamRho) }

        var maxGlobal = 0
        var puntosBorde = 0
        for (y in 0 until alto) {
            var i = y * ancho
            for (x in 0 until ancho) {
                if (magnitud[i] > umbral) {
                    var angulo = Math.toDegrees(
                        atan2(gy[i].toDouble(), gx[i].toDouble())
                    )
                    angulo %= 180.0
                    if (angulo < 0) angulo += 180.0
                    val bucket = angulo.roundToInt() % BUCKETS_ANGULO
                    val rho = x * cosenos[bucket] + y * senos[bucket]
                    val indice = (rho + desplazamiento).roundToInt()
                    val fila = acumulador[bucket]
                    if (indice in fila.indices) {
                        val valor = ++fila[indice]
                        if (valor > maxGlobal) maxGlobal = valor
                        puntosBorde++
                    }
                }
                i++
            }
        }
        if (puntosBorde < 150) return null

        // Perfil suavizado: una recta inclinada reparte sus votos entre varias celdas
        // de rho, así que se suman ventanas de 7 celdas antes de buscar picos.
        var maxSuave = 0
        for (b in 0 until BUCKETS_ANGULO) {
            val fila = acumulador[b]
            var suma = 0
            for (j in 0..6) suma += fila[j]
            var i = 3
            while (i < fila.size - 4) {
                if (suma > maxSuave) maxSuave = suma
                suma += fila[i + 4] - fila[i - 3]
                i++
            }
        }
        if (maxSuave <= 0) return null

        val lineas = ArrayList<Linea>()
        val minimo = max(14, (maxSuave * 0.10f).toInt())
        val perfil = IntArray(tamRho)
        for (b in 0 until BUCKETS_ANGULO) {
            val fila = acumulador[b]
            var suma = 0
            for (j in 0..6) suma += fila[j]
            var i = 3
            while (i < fila.size - 4) {
                perfil[i] = suma
                suma += fila[i + 4] - fila[i - 3]
                i++
            }

            var indice = 5
            while (indice < fila.size - 5) {
                val ventana = perfil[indice]
                if (ventana >= minimo &&
                    ventana >= perfil[indice - 2] &&
                    ventana >= perfil[indice + 2]
                ) {
                    var sumaPeso = 0
                    var sumaPos = 0
                    for (j in indice - 5..indice + 5) {
                        val peso = fila[j]
                        sumaPeso += peso
                        sumaPos += peso * j
                    }
                    if (sumaPeso > 0) {
                        val rho = (sumaPos.toFloat() / sumaPeso) - desplazamiento
                        val nx = cosenos[b]
                        val ny = senos[b]
                        val duplicada = lineas.any { existente ->
                            abs(existente.nx * nx + existente.ny * ny) > 0.998f &&
                                abs(existente.rho - rho) < 6f
                        }
                        if (!duplicada) {
                            lineas += Linea(nx, ny, rho, ventana)
                        }
                    }
                    indice += 4
                } else {
                    indice++
                }
            }
            // se limpia solo la zona usada para no reescribir todo el vector
            java.util.Arrays.fill(perfil, 0)
        }
        if (lineas.isEmpty()) return null

        val centroX = (ancho - 1) / 2f
        val centroY = (alto - 1) / 2f
        val superiores = ArrayList<Linea>()
        val inferiores = ArrayList<Linea>()
        val izquierdas = ArrayList<Linea>()
        val derechas = ArrayList<Linea>()

        lineas.forEach { linea ->
            val puntoX = linea.nx * linea.rho
            val puntoY = linea.ny * linea.rho
            if (abs(linea.ny) >= abs(linea.nx)) {
                if (puntoY < centroY) superiores += linea else inferiores += linea
            } else {
                if (puntoX < centroX) izquierdas += linea else derechas += linea
            }
        }

        val borde = Linea(0f, 1f, 0f, 0)
        val bordeInf = Linea(0f, 1f, (alto - 1).toFloat(), 0)
        val bordeIzq = Linea(1f, 0f, 0f, 0)
        val bordeDer = Linea(1f, 0f, (ancho - 1).toFloat(), 0)
        superiores += borde
        inferiores += bordeInf
        izquierdas += bordeIzq
        derechas += bordeDer

        val candidatos = listOf(superiores, inferiores, izquierdas, derechas)
            .map { lista -> lista.sortedByDescending { it.puntaje }.take(CANDIDATAS_POR_LADO) }

        val areaImagen = ancho.toFloat() * alto
        val diagonalMinima = diagonal * 0.14f
        val maxLinea = max(1, maxSuave).toFloat()

        var mejor: Cuadrilatero? = null
        var mejorPuntaje = -1f
        var mejorSoporte = 0f

        candidatos[0].forEach { arriba ->
            candidatos[1].forEach { abajo ->
                candidatos[2].forEach { izquierda ->
                    candidatos[3].forEach { derecha ->
                        val supIzq = interseccion(arriba, izquierda) ?: return@forEach
                        val supDer = interseccion(arriba, derecha) ?: return@forEach
                        val infDer = interseccion(abajo, derecha) ?: return@forEach
                        val infIzq = interseccion(abajo, izquierda) ?: return@forEach

                        val area = areaCuadrilatero(supIzq, supDer, infDer, infIzq)
                        if (area <= 0) return@forEach
                        if (area < areaImagen * 0.10f || area > areaImagen * 1.08f) return@forEach
                        if (minimoLado(supIzq, supDer, infDer, infIzq) < diagonalMinima) return@forEach
                        if (fueraDeMarco(supIzq, supDer, infDer, infIzq, ancho, alto)) return@forEach

                        val soporte = soporteCuadrilatero(
                            supIzq, supDer, infDer, infIzq,
                            magnitud, ancho, alto, maximoMagnitud
                        )
                        if (soporte < 0.24f) return@forEach

                        val puntajeLineas = (arriba.puntaje + abajo.puntaje +
                            izquierda.puntaje + derecha.puntaje).toFloat() / (4f * maxLinea)
                        val puntaje = 0.55f * soporte +
                            0.30f * puntajeLineas +
                            0.15f * (area / areaImagen)

                        if (puntaje > mejorPuntaje) {
                            mejorPuntaje = puntaje
                            mejorSoporte = soporte
                            mejor = Cuadrilatero(supIzq, supDer, infDer, infIzq)
                        }
                    }
                }
            }
        }

        if (mejor == null || mejorSoporte < 0.28f) return null
        return mejor.normalizado(ancho, alto)
    }




    /** Fracción media del borde del cuadrilátero que cae sobre un gradiente fuerte. */
    private fun soporteCuadrilatero(
        a: Punto,
        b: Punto,
        c: Punto,
        d: Punto,
        magnitud: FloatArray,
        ancho: Int,
        alto: Int,
        maximoMagnitud: Float
    ): Float {
        val referencia = max(40f, maximoMagnitud)
        return (
            soporteLado(a, b, magnitud, ancho, alto, referencia) +
                soporteLado(b, c, magnitud, ancho, alto, referencia) +
                soporteLado(c, d, magnitud, ancho, alto, referencia) +
                soporteLado(d, a, magnitud, ancho, alto, referencia)
            ) / 4f
    }

    private fun soporteLado(
        a: Punto,
        b: Punto,
        magnitud: FloatArray,
        ancho: Int,
        alto: Int,
        referencia: Float
    ): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val largo = hypot(dx, dy)
        if (largo < 6f) return 0f
        val perpendicularX = -dy / largo
        val perpendicularY = dx / largo
        var suma = 0f
        for (m in 0 until MUESTRAS_LADO) {
            val t = (m + 0.5f) / MUESTRAS_LADO
            val x = a.x + dx * t
            val y = a.y + dy * t
            var mejor = 0f
            for (desvio in -2..2) {
                val xi = (x + perpendicularX * desvio).roundToInt().coerceIn(0, ancho - 1)
                val yi = (y + perpendicularY * desvio).roundToInt().coerceIn(0, alto - 1)
                val valor = magnitud[yi * ancho + xi]
                if (valor > mejor) mejor = valor
            }
            suma += mejor
        }
        return (suma / (MUESTRAS_LADO * referencia)).coerceIn(0f, 1f)
    }

    private fun interseccion(a: Linea, b: Linea): Punto? {
        val determinante = a.nx * b.ny - a.ny * b.nx
        if (abs(determinante) < 1e-4f) return null
        val x = (a.rho * b.ny - b.rho * a.ny) / determinante
        val y = (a.nx * b.rho - b.nx * a.rho) / determinante
        if (!x.isFinite() || !y.isFinite()) return null
        return Punto(x, y)
    }

    private fun areaCuadrilatero(a: Punto, b: Punto, c: Punto, d: Punto): Float {
        val suma = a.x * b.y - b.x * a.y +
            b.x * c.y - c.x * b.y +
            c.x * d.y - d.x * c.y +
            d.x * a.y - a.x * d.y
        return suma / 2f
    }

    private fun minimoLado(a: Punto, b: Punto, c: Punto, d: Punto): Float {
        val lados = floatArrayOf(
            distancia(a, b),
            distancia(b, c),
            distancia(c, d),
            distancia(d, a)
        )
        return lados.min()
    }

    private fun fueraDeMarco(
        a: Punto, b: Punto, c: Punto, d: Punto,
        ancho: Int, alto: Int
    ): Boolean {
        val margenX = ancho * 0.18f
        val margenY = alto * 0.18f
        return listOf(a, b, c, d).any {
            it.x < -margenX || it.x > ancho + margenX ||
                it.y < -margenY || it.y > alto + margenY
        }
    }

    // ------------------------------------------------------------- Por máscara

    private fun porMascara(gris: FloatArray, ancho: Int, alto: Int): Cuadrilatero? {
        val borde = ArrayList<Float>(2 * (ancho + alto))
        for (x in 0 until ancho) {
            borde += gris[x]
            borde += gris[(alto - 1) * ancho + x]
        }
        for (y in 0 until alto) {
            borde += gris[y * ancho]
            borde += gris[y * ancho + ancho - 1]
        }
        borde.sort()
        val fondo = borde[borde.size / 2]
        val areaTotal = ancho.toFloat() * alto

        val intentos = listOf(
            fondo + 16f, fondo + 30f, fondo + 48f,
            fondo - 16f, fondo - 30f, fondo - 48f
        )

        intentos.forEach { umbral ->
            val mascara = BooleanArray(ancho * alto)
            for (i in mascara.indices) {
                mascara[i] = if (umbral >= fondo) gris[i] > umbral else gris[i] < umbral
            }
            val componente = componenteMasGrande(mascara, ancho, alto) ?: return@forEach
            if (componente.first < areaTotal * 0.10f) return@forEach
            val cuadrilatero = componente.second.normalizado(ancho, alto)
            val area = areaCuadrilatero(
                cuadrilatero.supIzq, cuadrilatero.supDer,
                cuadrilatero.infDer, cuadrilatero.infIzq
            ) / (ancho * alto)
            if (area > 0.09f && area <= 1.08f) return cuadrilatero
        }
        return null
    }

    private fun componenteMasGrande(
        mascara: BooleanArray,
        ancho: Int,
        alto: Int
    ): Pair<Float, Cuadrilatero>? {
        val visitado = BooleanArray(mascara.size)
        val pila = IntArray(mascara.size)
        var mejorArea = 0
        var mejorCuadrilatero: Cuadrilatero? = null

        for (inicio in mascara.indices) {
            if (!mascara[inicio] || visitado[inicio]) continue
            var tope = 0
            pila[tope++] = inicio
            visitado[inicio] = true
            var area = 0
            var minSuma = Float.MAX_VALUE
            var maxSuma = -Float.MAX_VALUE
            var minDif = Float.MAX_VALUE
            var maxDif = -Float.MAX_VALUE
            var supIzq = Punto(0f, 0f)
            var infDer = Punto(0f, 0f)
            var supDer = Punto(0f, 0f)
            var infIzq = Punto(0f, 0f)

            while (tope > 0) {
                val indice = pila[--tope]
                val x = indice % ancho
                val y = indice / ancho
                area++
                val suma = (x + y).toFloat()
                val diferencia = (x - y).toFloat()
                if (suma < minSuma) {
                    minSuma = suma
                    supIzq = Punto(x.toFloat(), y.toFloat())
                }
                if (suma > maxSuma) {
                    maxSuma = suma
                    infDer = Punto(x.toFloat(), y.toFloat())
                }
                if (diferencia > maxDif) {
                    maxDif = diferencia
                    supDer = Punto(x.toFloat(), y.toFloat())
                }
                if (diferencia < minDif) {
                    minDif = diferencia
                    infIzq = Punto(x.toFloat(), y.toFloat())
                }
                if (x > 0 && mascara[indice - 1] && !visitado[indice - 1]) {
                    visitado[indice - 1] = true
                    pila[tope++] = indice - 1
                }
                if (x < ancho - 1 && mascara[indice + 1] && !visitado[indice + 1]) {
                    visitado[indice + 1] = true
                    pila[tope++] = indice + 1
                }
                if (y > 0 && mascara[indice - ancho] && !visitado[indice - ancho]) {
                    visitado[indice - ancho] = true
                    pila[tope++] = indice - ancho
                }
                if (y < alto - 1 && mascara[indice + ancho] && !visitado[indice + ancho]) {
                    visitado[indice + ancho] = true
                    pila[tope++] = indice + ancho
                }
            }

            if (area > mejorArea) {
                val cuadrilatero = Cuadrilatero(supIzq, supDer, infDer, infIzq)
                val areaCuadrilatero = areaCuadrilatero(supIzq, supDer, infDer, infIzq)
                if (areaCuadrilatero > 0) {
                    mejorArea = area
                    mejorCuadrilatero = cuadrilatero
                }
            }
        }
        return mejorCuadrilatero?.let { mejorArea.toFloat() to it }
    }

    // ---------------------------------------------------------------- soporte

    private fun desenfoque(datos: FloatArray, ancho: Int, alto: Int): FloatArray {
        val horizontal = FloatArray(datos.size)
        val salida = FloatArray(datos.size)
        for (y in 0 until alto) {
            val fila = y * ancho
            for (x in 0 until ancho) {
                val x0 = max(0, x - 1)
                val x1 = min(ancho - 1, x + 1)
                horizontal[fila + x] =
                    (datos[fila + x0] + datos[fila + x] + datos[fila + x1]) / 3f
            }
        }
        for (y in 0 until alto) {
            val y0 = max(0, y - 1)
            val y1 = min(alto - 1, y + 1)
            for (x in 0 until ancho) {
                salida[y * ancho + x] = (
                    horizontal[y0 * ancho + x] +
                        horizontal[y * ancho + x] +
                        horizontal[y1 * ancho + x]
                    ) / 3f
            }
        }
        return salida
    }

    private fun sobel(
        datos: FloatArray,
        ancho: Int,
        alto: Int,
        gx: FloatArray,
        gy: FloatArray
    ) {
        for (y in 0 until alto) {
            val arriba = max(0, y - 1) * ancho
            val medio = y * ancho
            val abajo = min(alto - 1, y + 1) * ancho
            for (x in 0 until ancho) {
                val x0 = max(0, x - 1)
                val x1 = min(ancho - 1, x + 1)
                val a = datos[arriba + x0]
                val b = datos[arriba + x]
                val c = datos[arriba + x1]
                val d = datos[medio + x0]
                val e = datos[medio + x1]
                val f = datos[abajo + x0]
                val g = datos[abajo + x]
                val h = datos[abajo + x1]
                gx[medio + x] = (c + 2f * e + h) - (a + 2f * d + f)
                gy[medio + x] = (f + 2f * g + h) - (a + 2f * b + c)
            }
        }
    }

    private fun otsu(datos: FloatArray, maximo: Float): Float {
        val histograma = IntArray(256)
        val escala = 255f / max(1f, maximo)
        for (valor in datos) {
            val bin = (valor * escala).toInt().coerceIn(0, 255)
            histograma[bin]++
        }
        val total = datos.size
        var sumaTotal = 0.0
        for (i in 0 until 256) sumaTotal += i.toDouble() * histograma[i]

        var sumaFondo = 0.0
        var pesoFondo = 0
        var mejorVarianza = -1.0
        var mejorUmbral = 0
        for (t in 0 until 256) {
            pesoFondo += histograma[t]
            if (pesoFondo == 0) continue
            val pesoFrente = total - pesoFondo
            if (pesoFrente == 0) break
            sumaFondo += t.toDouble() * histograma[t]
            val mediaFondo = sumaFondo / pesoFondo
            val mediaFrente = (sumaTotal - sumaFondo) / pesoFrente
            val varianza = pesoFondo.toDouble() * pesoFrente *
                (mediaFondo - mediaFrente) * (mediaFondo - mediaFrente)
            if (varianza > mejorVarianza) {
                mejorVarianza = varianza
                mejorUmbral = t
            }
        }
        return (mejorUmbral + 1) / escala
    }

    private fun distancia(a: Punto, b: Punto): Float =
        hypot(a.x - b.x, a.y - b.y)
}
