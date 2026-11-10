package com.example.fileshifter

import com.example.fileshifter.data.source.Cuadrilatero
import com.example.fileshifter.data.source.DetectorDocumento
import com.example.fileshifter.data.source.Punto
import com.example.fileshifter.data.source.ScanEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class DetectorDocumentoTest {

    private val ancho = 420
    private val alto = 320

    private fun pintarDocumento(
        esquinas: List<Punto>,
        fondo: Float = 28f,
        hoja: Float = 225f
    ): FloatArray {
        val datos = FloatArray(ancho * alto)
        for (y in 0 until alto) {
            for (x in 0 until ancho) {
                val dentro = dentroDelPoligono(x.toFloat(), y.toFloat(), esquinas)
                // textura suave para que no sea una imagen ideal
                val ruido = (((x * 73856093) xor (y * 19349663)) % 15).toFloat() - 7f
                datos[y * ancho + x] = (if (dentro) hoja else fondo) + ruido
            }
        }
        return datos
    }

    private fun dentroDelPoligono(x: Float, y: Float, puntos: List<Punto>): Boolean {
        var signo = 0
        for (i in 0 until 4) {
            val a = puntos[i]
            val b = puntos[(i + 1) % 4]
            val cruz = (b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x)
            val actual = if (cruz >= 0) 1 else -1
            if (signo == 0) signo = actual else if (signo != actual) return false
        }
        return true
    }

    private fun errorMaximo(esperado: List<Punto>, detectado: Cuadrilatero): Float {
        val puntos = detectado.puntos
        return esperado.indices.maxOf { i ->
            hypot(
                (puntos[i].x * ancho) - esperado[i].x,
                (puntos[i].y * alto) - esperado[i].y
            )
        }
    }

    @Test
    fun detectaHojaClaraSobreFondoOscuro() {
        val esperado = listOf(
            Punto(62f, 48f),
            Punto(352f, 40f),
            Punto(334f, 262f),
            Punto(74f, 268f)
        )
        val datos = pintarDocumento(esperado)

        val detectado = DetectorDocumento.detectarGris(datos, ancho, alto)

        assertNotNull("Debería detectar la hoja", detectado)
        val error = errorMaximo(esperado, detectado!!)
        assertTrue("Error de esquinas demasiado grande: $error px", error < 18f)
    }

    @Test
    fun detectaHojaOscuraSobreFondoClaro() {
        val esperado = listOf(
            Punto(70f, 60f),
            Punto(345f, 55f),
            Punto(330f, 255f),
            Punto(80f, 262f)
        )
        val datos = pintarDocumento(esperado, fondo = 235f, hoja = 40f)

        val detectado = DetectorDocumento.detectarGris(datos, ancho, alto)

        assertNotNull("Debería detectar la hoja oscura", detectado)
        val error = errorMaximo(esperado, detectado!!)
        assertTrue("Error de esquinas demasiado grande: $error px", error < 20f)
    }

    @Test
    fun documentoQueCasiLlenaElMarcoDevuelveCasiTodaLaImagen() {
        val esperado = listOf(
            Punto(7f, 7f),
            Punto(412f, 6f),
            Punto(413f, 312f),
            Punto(6f, 313f)
        )
        val datos = pintarDocumento(esperado, fondo = 22f, hoja = 228f)
        val detectado = DetectorDocumento.detectarGris(datos, ancho, alto)
        assertNotNull(detectado)
        val area = detectado!!.areaAproximada()
        assertTrue("El área detectada debería cubrir casi la imagen: $area", area > 0.85f)
    }

    @Test
    fun girar90MantieneElOrdenDeLasEsquinas() {
        val cuadrilatero = Cuadrilatero(
            Punto(0.1f, 0.1f),
            Punto(0.9f, 0.1f),
            Punto(0.9f, 0.9f),
            Punto(0.1f, 0.9f)
        )
        val girado = cuadrilatero.girar90()
        assertEquals(0.1f, girado.supIzq.x, 0.001f)
        assertEquals(0.1f, girado.supIzq.y, 0.001f)
        assertEquals(0.9f, girado.supDer.x, 0.001f)
        assertEquals(0.1f, girado.supDer.y, 0.001f)
        assertEquals(0.9f, girado.infDer.x, 0.001f)
        assertEquals(0.9f, girado.infDer.y, 0.001f)

        val cuatroGiros = cuadrilatero.girar(360)
        assertEquals(cuadrilatero.supIzq.x, cuatroGiros.supIzq.x, 0.001f)
        assertEquals(cuadrilatero.infDer.y, cuatroGiros.infDer.y, 0.001f)
    }

    @Test
    fun homografiaLlevaElRectanguloALasEsquinasDelDocumento() {
        val destino = listOf(
            Punto(0f, 0f),
            Punto(400f, 0f),
            Punto(400f, 300f),
            Punto(0f, 300f)
        )
        val origen = listOf(
            Punto(60f, 40f),
            Punto(350f, 55f),
            Punto(330f, 270f),
            Punto(70f, 255f)
        )
        val h = ScanEngine.calcularHomografia(destino, origen)
        assertNotNull(h)

        for (i in 0 until 4) {
            val x = destino[i].x.toDouble()
            val y = destino[i].y.toDouble()
            val denominador = h!![6] * x + h[7] * y + 1.0
            val sx = (h[0] * x + h[1] * y + h[2]) / denominador
            val sy = (h[3] * x + h[4] * y + h[5]) / denominador
            assertTrue("x[$i] no coincide", abs(sx - origen[i].x) < 0.5)
            assertTrue("y[$i] no coincide", abs(sy - origen[i].y) < 0.5)
        }
    }
}
