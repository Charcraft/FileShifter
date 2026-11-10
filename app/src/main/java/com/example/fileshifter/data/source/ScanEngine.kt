package com.example.fileshifter.data.source

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Procesamiento de imágenes del escáner: detección de la hoja, corrección de la
 * perspectiva y los filtros que dan el "aspecto de escáner".
 */
object ScanEngine {

    private const val LADO_VISTA = 1400
    private const val LADO_DECODIFICADO = 2600
    private const val LADO_PROCESO = 2000
    private const val CALIDAD_JPEG = 92

    // ------------------------------------------------------------- entradas

    /** Foto reducida para el editor (ligera para la vista y para detectar bordes). */
    fun cargarVista(archivo: File): Bitmap? =
        cargarBitmap(archivo, LADO_VISTA)

    /** Foto a resolución completa para generar la página final. */
    fun cargarProceso(archivo: File): Bitmap? =
        cargarBitmap(archivo, LADO_DECODIFICADO)

    fun cargarBitmap(archivo: File, ladoMaximo: Int): Bitmap? {
        if (!archivo.exists()) return null
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.absolutePath, limites)
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        var muestra = 1
        var mayor = max(limites.outWidth, limites.outHeight)
        while (mayor / (muestra * 2) >= ladoMaximo) muestra *= 2

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = muestra
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var bitmap = BitmapFactory.decodeFile(archivo.absolutePath, opciones) ?: return null
        bitmap = aplicarOrientacionExif(archivo, bitmap)
        val mayorActual = max(bitmap.width, bitmap.height)
        if (mayorActual > ladoMaximo) {
            val factor = ladoMaximo.toFloat() / mayorActual
            val escalado = Bitmap.createScaledBitmap(
                bitmap,
                max(1, (bitmap.width * factor).roundToInt()),
                max(1, (bitmap.height * factor).roundToInt()),
                true
            )
            if (escalado !== bitmap) bitmap.recycle()
            bitmap = escalado
        }
        return bitmap
    }

    private fun aplicarOrientacionExif(archivo: File, bitmap: Bitmap): Bitmap {
        return try {
            val orientacion = ExifInterface(archivo.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matriz = Matrix()
            when (orientacion) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matriz.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matriz.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matriz.postRotate(90f)
                    matriz.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matriz.postRotate(270f)
                    matriz.postScale(-1f, 1f)
                }
                else -> return bitmap
            }
            val girado = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
            if (girado !== bitmap) bitmap.recycle()
            girado
        } catch (_: Exception) {
            bitmap
        }
    }

    fun rotar(bitmap: Bitmap, grados: Int): Bitmap {
        if (grados % 360 == 0) return bitmap
        val matriz = Matrix().apply { postRotate(grados.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }

    fun miniatura(bitmap: Bitmap, lado: Int = 220): Bitmap {
        val mayor = max(bitmap.width, bitmap.height)
        if (mayor <= lado) return bitmap
        val factor = lado.toFloat() / mayor
        return Bitmap.createScaledBitmap(
            bitmap,
            max(1, (bitmap.width * factor).roundToInt()),
            max(1, (bitmap.height * factor).roundToInt()),
            true
        )
    }

    fun guardarJpeg(bitmap: Bitmap, destino: File, calidad: Int = CALIDAD_JPEG): File {
        destino.parentFile?.mkdirs()
        FileOutputStream(destino).use { salida ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
        }
        return destino
    }

    // ----------------------------------------------------------- deteccion

    /** Busca las esquinas del documento. Devuelve null si no encuentra nada fiable. */
    fun detectar(bitmap: Bitmap): Cuadrilatero? {
        val ancho = bitmap.width
        val alto = bitmap.height
        val pixeles = IntArray(ancho * alto)
        bitmap.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto)
        return DetectorDocumento.detectar(ancho, alto, pixeles)
    }

    /** Detecta y valida; si falla usa el marco completo de la foto. */
    fun detectarOSuponer(bitmap: Bitmap): Cuadrilatero =
        detectar(bitmap)?.takeIf { it.areaAproximada() > 0.06f } ?: Cuadrilatero.COMPLETO

    // ------------------------------------------------------------ perspectiva

    /**
     * Endereza la hoja: aplica la homografía que lleva las cuatro esquinas
     * detectadas a un rectángulo perfecto.
     */
    fun corregirPerspectiva(origen: Bitmap, cuadrilatero: Cuadrilatero): Bitmap {
        if (cuadrilatero.pareceMarcoCompleto()) return limitar(origen)

        val anchoOrigen = origen.width
        val altoOrigen = origen.height
        val esquinas = cuadrilatero.puntos.map {
            Punto(
                it.x.coerceIn(-0.25f, 1.25f) * anchoOrigen,
                it.y.coerceIn(-0.25f, 1.25f) * altoOrigen
            )
        }

        fun lado(a: Punto, b: Punto) = hypot(a.x - b.x, a.y - b.y)
        val anchoArriba = lado(esquinas[0], esquinas[1])
        val anchoAbajo = lado(esquinas[3], esquinas[2])
        val altoIzquierda = lado(esquinas[0], esquinas[3])
        val altoDerecha = lado(esquinas[1], esquinas[2])

        var anchoSalida = max(anchoArriba, anchoAbajo).roundToInt().coerceAtLeast(64)
        var altoSalida = max(altoIzquierda, altoDerecha).roundToInt().coerceAtLeast(64)
        val factor = min(1f, LADO_PROCESO.toFloat() / max(anchoSalida, altoSalida))
        anchoSalida = max(64, (anchoSalida * factor).roundToInt())
        altoSalida = max(64, (altoSalida * factor).roundToInt())

        val destino = listOf(
            Punto(0f, 0f),
            Punto(anchoSalida.toFloat(), 0f),
            Punto(anchoSalida.toFloat(), altoSalida.toFloat()),
            Punto(0f, altoSalida.toFloat())
        )
        val homografia = calcularHomografia(destino, esquinas)
            ?: return recorteSimple(origen, esquinas)

        return aplicarHomografia(origen, homografia, anchoSalida, altoSalida)
    }

    private fun limitar(origen: Bitmap): Bitmap {
        val mayor = max(origen.width, origen.height)
        if (mayor <= LADO_PROCESO) {
            return origen.copy(Bitmap.Config.ARGB_8888, false) ?: origen
        }
        val factor = LADO_PROCESO.toFloat() / mayor
        return Bitmap.createScaledBitmap(
            origen,
            max(1, (origen.width * factor).roundToInt()),
            max(1, (origen.height * factor).roundToInt()),
            true
        )
    }

    private fun recorteSimple(origen: Bitmap, esquinas: List<Punto>): Bitmap {
        val minX = esquinas.minOf { it.x }.coerceIn(0f, origen.width - 2f).toInt()
        val maxX = esquinas.maxOf { it.x }.coerceIn(minX + 2f, origen.width.toFloat()).toInt()
        val minY = esquinas.minOf { it.y }.coerceIn(0f, origen.height - 2f).toInt()
        val maxY = esquinas.maxOf { it.y }.coerceIn(minY + 2f, origen.height.toFloat()).toInt()
        return Bitmap.createBitmap(origen, minX, minY, maxX - minX, maxY - minY)
    }

    /**
     * Resuelve la homografía que transforma los puntos [destino] en los puntos
     * [origen] (8 incógnitas, sistema lineal 8x8 por eliminación de Gauss).
     */
    fun calcularHomografia(destino: List<Punto>, origen: List<Punto>): DoubleArray? {
        if (destino.size != 4 || origen.size != 4) return null
        val matriz = Array(8) { DoubleArray(9) }
        for (i in 0 until 4) {
            val x = destino[i].x.toDouble()
            val y = destino[i].y.toDouble()
            val u = origen[i].x.toDouble()
            val v = origen[i].y.toDouble()
            matriz[i * 2] = doubleArrayOf(x, y, 1.0, 0.0, 0.0, 0.0, -u * x, -u * y, u)
            matriz[i * 2 + 1] = doubleArrayOf(0.0, 0.0, 0.0, x, y, 1.0, -v * x, -v * y, v)
        }

        for (columna in 0 until 8) {
            var pivote = columna
            for (fila in columna + 1 until 8) {
                if (abs(matriz[fila][columna]) > abs(matriz[pivote][columna])) pivote = fila
            }
            if (abs(matriz[pivote][columna]) < 1e-9) return null
            val temporal = matriz[columna]
            matriz[columna] = matriz[pivote]
            matriz[pivote] = temporal

            val divisor = matriz[columna][columna]
            for (j in columna..8) matriz[columna][j] /= divisor
            for (fila in 0 until 8) {
                if (fila == columna) continue
                val factor = matriz[fila][columna]
                if (factor == 0.0) continue
                for (j in columna..8) matriz[fila][j] -= factor * matriz[columna][j]
            }
        }
        return DoubleArray(8) { matriz[it][8] }
    }

    private fun aplicarHomografia(
        origen: Bitmap,
        h: DoubleArray,
        ancho: Int,
        alto: Int
    ): Bitmap {
        val anchoOrigen = origen.width
        val altoOrigen = origen.height
        val fuente = IntArray(anchoOrigen * altoOrigen)
        origen.getPixels(fuente, 0, anchoOrigen, 0, 0, anchoOrigen, altoOrigen)

        val salida = IntArray(ancho * alto)
        val h0 = h[0]
        val h1 = h[1]
        val h2 = h[2]
        val h3 = h[3]
        val h4 = h[4]
        val h5 = h[5]
        val h6 = h[6]
        val h7 = h[7]

        for (y in 0 until alto) {
            val yf = y + 0.5
            val base = y * ancho
            for (x in 0 until ancho) {
                val xf = x + 0.5
                val denominador = h6 * xf + h7 * yf + 1.0
                if (abs(denominador) < 1e-9) {
                    salida[base + x] = BLANCO
                    continue
                }
                val sx = (h0 * xf + h1 * yf + h2) / denominador
                val sy = (h3 * xf + h4 * yf + h5) / denominador
                salida[base + x] = muestrear(fuente, anchoOrigen, altoOrigen, sx, sy)
            }
        }
        return Bitmap.createBitmap(salida, ancho, alto, Bitmap.Config.ARGB_8888)
    }

    private const val BLANCO = -1 // 0xFFFFFFFF

    private fun muestrear(fuente: IntArray, ancho: Int, alto: Int, x: Double, y: Double): Int {
        if (x < -1 || y < -1 || x > ancho || y > alto) return BLANCO
        val x0 = kotlin.math.floor(x).toInt().coerceIn(0, ancho - 1)
        val y0 = kotlin.math.floor(y).toInt().coerceIn(0, alto - 1)
        val x1 = min(ancho - 1, x0 + 1)
        val y1 = min(alto - 1, y0 + 1)
        val fx = (x - x0).coerceIn(0.0, 1.0).toFloat()
        val fy = (y - y0).coerceIn(0.0, 1.0).toFloat()

        val c00 = fuente[y0 * ancho + x0]
        val c10 = fuente[y0 * ancho + x1]
        val c01 = fuente[y1 * ancho + x0]
        val c11 = fuente[y1 * ancho + x1]

        val r = interpolar((c00 shr 16) and 0xFF, (c10 shr 16) and 0xFF,
            (c01 shr 16) and 0xFF, (c11 shr 16) and 0xFF, fx, fy)
        val g = interpolar((c00 shr 8) and 0xFF, (c10 shr 8) and 0xFF,
            (c01 shr 8) and 0xFF, (c11 shr 8) and 0xFF, fx, fy)
        val b = interpolar(c00 and 0xFF, c10 and 0xFF, c01 and 0xFF, c11 and 0xFF, fx, fy)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun interpolar(
        c00: Int, c10: Int, c01: Int, c11: Int,
        fx: Float, fy: Float
    ): Int {
        val arriba = c00 + (c10 - c00) * fx
        val abajo = c01 + (c11 - c01) * fx
        return (arriba + (abajo - arriba) * fy).roundToInt().coerceIn(0, 255)
    }

    // ---------------------------------------------------------------- filtros

    /** Aplica el aspecto de escáner sobre una imagen ya enderezada. */
    fun aplicarFiltro(origen: Bitmap, filtro: FiltroEscaneo): Bitmap {
        if (filtro == FiltroEscaneo.ORIGINAL) return origen
        val ancho = origen.width
        val alto = origen.height
        if (ancho < 8 || alto < 8) return origen

        val total = ancho * alto
        val pixeles = IntArray(total)
        origen.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto)

        val gris = FloatArray(total)
        for (i in 0 until total) {
            val color = pixeles[i]
            gris[i] = 0.299f * ((color shr 16) and 0xFF) +
                0.587f * ((color shr 8) and 0xFF) +
                0.114f * (color and 0xFF)
        }

        val fondo = fondoLocal(gris, ancho, alto)
        val ganancia = FloatArray(256) { indice ->
            (245f / max(indice.toFloat(), 6f)).coerceIn(0.55f, 4f)
        }
        for (i in 0 until total) {
            val indice = fondo[i].toInt().coerceIn(0, 255)
            gris[i] = (gris[i] * ganancia[indice]).coerceIn(0f, 255f)
        }

        val (rangoBajo, rangoAlto) = percentiles(gris)
        val estiramiento = FloatArray(256) { valor ->
            ((valor - rangoBajo) * 255f / max(rangoAlto - rangoBajo, 1f)).coerceIn(0f, 255f)
        }
        val mapa = IntArray(256) { estiramiento[it].roundToInt().coerceIn(0, 255) }

        when (filtro) {
            FiltroEscaneo.GRIS -> {
                for (i in 0 until total) {
                    val v = mapa[gris[i].toInt().coerceIn(0, 255)]
                    pixeles[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
                }
            }
            FiltroEscaneo.BLANCO_NEGRO -> {
                val umbralBase = otsu(gris).coerceIn(70f, 205f)
                val umbral = mapa[umbralBase.toInt().coerceIn(0, 255)]
                for (i in 0 until total) {
                    val v = if (mapa[gris[i].toInt().coerceIn(0, 255)] > umbral) 255 else 0
                    pixeles[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
                }
            }
            FiltroEscaneo.MEJORADO -> {
                for (i in 0 until total) {
                    val color = pixeles[i]
                    val g = ganancia[fondo[i].toInt().coerceIn(0, 255)]
                    var r = mapa[(((color shr 16) and 0xFF) * g).toInt().coerceIn(0, 255)]
                    var v = mapa[(((color shr 8) and 0xFF) * g).toInt().coerceIn(0, 255)]
                    var b = mapa[((color and 0xFF) * g).toInt().coerceIn(0, 255)]
                    // ligero refuerzo de color, como el "color mágico" de las apps de escaneo
                    val luminancia = 0.299f * r + 0.587f * v + 0.114f * b
                    r = (luminancia + (r - luminancia) * 1.12f).roundToInt().coerceIn(0, 255)
                    v = (luminancia + (v - luminancia) * 1.12f).roundToInt().coerceIn(0, 255)
                    b = (luminancia + (b - luminancia) * 1.12f).roundToInt().coerceIn(0, 255)
                    pixeles[i] = (0xFF shl 24) or (r shl 16) or (v shl 8) or b
                }
            }
            FiltroEscaneo.ORIGINAL -> Unit
        }

        return Bitmap.createBitmap(pixeles, ancho, alto, Bitmap.Config.ARGB_8888)
    }

    /** Endereza y filtra en un solo paso, a tamaño reducido (vista previa del editor). */
    fun vistaPrevia(
        origen: Bitmap,
        cuadrilatero: Cuadrilatero,
        filtro: FiltroEscaneo,
        ladoMaximo: Int = 520
    ): Bitmap {
        val enderezado = corregirPerspectivaConLimite(origen, cuadrilatero, ladoMaximo)
        val resultado = aplicarFiltro(enderezado, filtro)
        if (resultado !== enderezado) enderezado.recycle()
        return resultado
    }

    private fun corregirPerspectivaConLimite(
        origen: Bitmap,
        cuadrilatero: Cuadrilatero,
        ladoMaximo: Int
    ): Bitmap {
        val anchoOrigen = origen.width
        val altoOrigen = origen.height
        val esquinas = cuadrilatero.puntos.map {
            Punto(
                it.x.coerceIn(-0.25f, 1.25f) * anchoOrigen,
                it.y.coerceIn(-0.25f, 1.25f) * altoOrigen
            )
        }
        fun lado(a: Punto, b: Punto) = hypot(a.x - b.x, a.y - b.y)
        var anchoSalida = max(lado(esquinas[0], esquinas[1]), lado(esquinas[3], esquinas[2]))
            .roundToInt().coerceAtLeast(32)
        var altoSalida = max(lado(esquinas[0], esquinas[3]), lado(esquinas[1], esquinas[2]))
            .roundToInt().coerceAtLeast(32)
        val factor = min(1f, ladoMaximo.toFloat() / max(anchoSalida, altoSalida))
        anchoSalida = max(32, (anchoSalida * factor).roundToInt())
        altoSalida = max(32, (altoSalida * factor).roundToInt())

        val destino = listOf(
            Punto(0f, 0f),
            Punto(anchoSalida.toFloat(), 0f),
            Punto(anchoSalida.toFloat(), altoSalida.toFloat()),
            Punto(0f, altoSalida.toFloat())
        )
        val homografia = calcularHomografia(destino, esquinas)
            ?: return recorteSimple(origen, esquinas)
        return aplicarHomografia(origen, homografia, anchoSalida, altoSalida)
    }

    /**
     * Estimación del brillo del papel: se promedia la imagen en celdas, se suaviza la
     * rejilla y se interpola. Dividir por este mapa elimina sombras y manchas de luz.
     */
    private fun fondoLocal(gris: FloatArray, ancho: Int, alto: Int): FloatArray {
        val celda = max(8, min(ancho, alto) / 24)
        val celdasX = max(1, ancho / celda)
        val celdasY = max(1, alto / celda)
        val suma = FloatArray(celdasX * celdasY)
        val cuenta = IntArray(celdasX * celdasY)

        for (y in 0 until alto) {
            val cy = min(celdasY - 1, y * celdasY / alto)
            val fila = y * ancho
            for (x in 0 until ancho) {
                val cx = min(celdasX - 1, x * celdasX / ancho)
                val indice = cy * celdasX + cx
                suma[indice] += gris[fila + x]
                cuenta[indice]++
            }
        }

        val rejilla = FloatArray(celdasX * celdasY)
        for (i in rejilla.indices) rejilla[i] = suma[i] / max(1, cuenta[i])

        var actual = rejilla
        repeat(2) {
            val suavizada = FloatArray(actual.size)
            for (y in 0 until celdasY) {
                for (x in 0 until celdasX) {
                    var acumulado = 0f
                    var n = 0
                    for (dy in -1..1) {
                        val yy = (y + dy).coerceIn(0, celdasY - 1)
                        for (dx in -1..1) {
                            val xx = (x + dx).coerceIn(0, celdasX - 1)
                            acumulado += actual[yy * celdasX + xx]
                            n++
                        }
                    }
                    suavizada[y * celdasX + x] = acumulado / n
                }
            }
            actual = suavizada
        }

        val salida = FloatArray(ancho * alto)
        for (y in 0 until alto) {
            val fy = ((y + 0.5f) * celdasY / alto - 0.5f).coerceIn(0f, celdasY - 1f)
            val y0 = fy.toInt().coerceIn(0, celdasY - 1)
            val y1 = min(celdasY - 1, y0 + 1)
            val ty = fy - y0
            val fila = y * ancho
            for (x in 0 until ancho) {
                val fx = ((x + 0.5f) * celdasX / ancho - 0.5f).coerceIn(0f, celdasX - 1f)
                val x0 = fx.toInt().coerceIn(0, celdasX - 1)
                val x1 = min(celdasX - 1, x0 + 1)
                val tx = fx - x0
                val arriba = actual[y0 * celdasX + x0] +
                    (actual[y0 * celdasX + x1] - actual[y0 * celdasX + x0]) * tx
                val abajo = actual[y1 * celdasX + x0] +
                    (actual[y1 * celdasX + x1] - actual[y1 * celdasX + x0]) * tx
                salida[fila + x] = arriba + (abajo - arriba) * ty
            }
        }
        return salida
    }

    private fun percentiles(datos: FloatArray): Pair<Float, Float> {
        val histograma = IntArray(256)
        for (valor in datos) {
            histograma[valor.toInt().coerceIn(0, 255)]++
        }
        val total = datos.size
        val objetivoBajo = (total * 0.02f).toInt()
        val objetivoAlto = (total * 0.98f).toInt()

        var acumulado = 0
        var bajo = 0f
        for (i in 0 until 256) {
            acumulado += histograma[i]
            if (acumulado >= objetivoBajo) {
                bajo = i.toFloat()
                break
            }
        }
        acumulado = 0
        var alto = 255f
        for (i in 0 until 256) {
            acumulado += histograma[i]
            if (acumulado >= objetivoAlto) {
                alto = i.toFloat()
                break
            }
        }
        if (alto - bajo < 30f) return 0f to 255f
        return bajo to alto
    }

    private fun otsu(datos: FloatArray): Float {
        val histograma = IntArray(256)
        for (valor in datos) histograma[valor.toInt().coerceIn(0, 255)]++
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
        return mejorUmbral.toFloat()
    }
}
