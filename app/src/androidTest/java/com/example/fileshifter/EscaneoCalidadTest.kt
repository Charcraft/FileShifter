package com.example.fileshifter

import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fileshifter.data.source.FiltroEscaneo
import com.example.fileshifter.data.source.ScanEngine
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Prueba de calidad del escáner con una foto real. Se salta si no existe
 * files/prueba.jpg en el almacenamiento de la app.
 */
@RunWith(AndroidJUnit4::class)
class EscaneoCalidadTest {

    private fun log(mensaje: String) = Log.i("CALIDAD", mensaje)

    @Test
    fun procesaFotoRealYReportaResolucion() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val carpeta = contexto.getExternalFilesDir(null) ?: return
        val entrada = File(carpeta, "prueba.jpg")
        if (!entrada.exists()) {
            log("se omite, no existe ${entrada.absolutePath}")
            return
        }

        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(entrada.absolutePath, limites)
        log(
            "memoria maxima ${Runtime.getRuntime().maxMemory() / (1024 * 1024)} MB, " +
                "foto ${limites.outWidth}x${limites.outHeight}"
        )

        val vista = ScanEngine.cargarVista(entrada) ?: error("No se pudo cargar la vista")
        val cuadrilatero = ScanEngine.detectar(vista)
        log("deteccion=${cuadrilatero?.puntos} area=${cuadrilatero?.areaAproximada()}")
        check(cuadrilatero != null) { "No se detectó la hoja" }
        vista.recycle()

        val inicio = System.currentTimeMillis()
        val original = ScanEngine.cargarProceso(entrada) ?: error("No se pudo cargar la foto")
        log("decodificada ${original.width}x${original.height} en ${System.currentTimeMillis() - inicio} ms")

        val enderezado = ScanEngine.corregirPerspectiva(original, cuadrilatero)
        original.recycle()
        val filtrado = ScanEngine.aplicarFiltro(enderezado, FiltroEscaneo.MEJORADO)
        if (filtrado !== enderezado) enderezado.recycle()

        log("pagina ${filtrado.width}x${filtrado.height} en ${System.currentTimeMillis() - inicio} ms total")
        check(filtrado.width >= 1300) { "La página quedó muy pequeña: ${filtrado.width}px" }

        val salida = File(carpeta, "resultado_calidad.jpg")
        ScanEngine.guardarJpeg(filtrado, salida)
        log("guardada ${salida.absolutePath} (${salida.length() / 1024} KB)")
        filtrado.recycle()
    }
}
