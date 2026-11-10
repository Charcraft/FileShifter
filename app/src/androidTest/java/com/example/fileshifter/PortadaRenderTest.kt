package com.example.fileshifter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fileshifter.data.model.DatosEntrega
import com.example.fileshifter.data.model.PlantillaEntrega
import com.example.fileshifter.data.model.PosicionLogo
import com.example.fileshifter.data.source.PdfEngine
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PortadaRenderTest {

    private val datos = DatosEntrega(
        institucion = "Universidad Nacional Autónoma",
        carrera = "Ingeniería en Sistemas Computacionales",
        materia = "Programación Avanzada",
        titulo = "Sistema de gestión documental con arquitectura limpia",
        nombre = "Ana María López Hernández",
        matricula = "2024-0123",
        docente = "Mtra. Laura Ruiz",
        fecha = "Noviembre 2026"
    )

    @Test
    fun generaYRenderizaPortadas() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val carpeta = File(context.getExternalFilesDir(null), "portadas").apply { mkdirs() }
        val logo = crearLogo(context)

        listOf(
            "escolar" to PlantillaEntrega.ESCOLAR,
            "oficio" to PlantillaEntrega.OFICIO,
            "minimal" to PlantillaEntrega.MINIMAL
        ).forEach { (nombre, plantilla) ->
            val pdf = File(carpeta, "$nombre.pdf")
            PdfEngine.portadaEntrega(context, plantilla.name, datos, pdf)
            renderizar(pdf, File(carpeta, "$nombre.png"))
        }

        val conLogo = datos.copy(
            logoUri = Uri.fromFile(logo).toString(),
            posicionLogo = PosicionLogo.SUPERIOR_IZQUIERDA
        )
        val pdfLogo = File(carpeta, "escolar_logo.pdf")
        PdfEngine.portadaEntrega(context, PlantillaEntrega.ESCOLAR.name, conLogo, pdfLogo)
        renderizar(pdfLogo, File(carpeta, "escolar_logo.png"))

        val fondo = datos.copy(
            logoUri = Uri.fromFile(logo).toString(),
            posicionLogo = PosicionLogo.FONDO_CENTRO
        )
        val pdfFondo = File(carpeta, "oficio_fondo.pdf")
        PdfEngine.portadaEntrega(context, PlantillaEntrega.OFICIO.name, fondo, pdfFondo)
        renderizar(pdfFondo, File(carpeta, "oficio_fondo.png"))

        val largo = datos.copy(
            textoExtra = "Este documento presenta el análisis, diseño e implementación de un sistema " +
                "de gestión documental desarrollado como proyecto final del curso. Incluye la " +
                "justificación, los objetivos, el marco teórico y los resultados obtenidos.",
            indice = (1..18).joinToString("\n") { "${it.toString().padStart(2, '0')}. Punto del índice con un título algo largo" }
        )
        val pdfLargo = File(carpeta, "escolar_largo.pdf")
        PdfEngine.portadaEntrega(context, PlantillaEntrega.ESCOLAR.name, largo, pdfLargo)
        renderizar(pdfLargo, File(carpeta, "escolar_largo.png"))

        assertTrue(File(carpeta, "escolar.png").length() > 0)
    }

    private fun crearLogo(context: Context): File {
        val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        val pincel = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(12, 59, 94)
        }
        canvas.drawCircle(160f, 160f, 150f, pincel)
        pincel.color = Color.WHITE
        pincel.textSize = 92f
        pincel.textAlign = Paint.Align.CENTER
        canvas.drawText("UN", 160f, 195f, pincel)
        val archivo = File(context.cacheDir, "logo_prueba.png")
        archivo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return archivo
    }

    private fun renderizar(pdf: File, png: File) {
        val descriptor = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)
        PdfRenderer(descriptor).use { renderer ->
            renderer.openPage(0).use { pagina ->
                val ancho = pagina.width * 2
                val alto = pagina.height * 2
                val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).drawColor(Color.WHITE)
                pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                png.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
    }
}
