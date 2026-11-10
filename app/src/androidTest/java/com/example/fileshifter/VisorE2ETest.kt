package com.example.fileshifter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.room.Room
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fileshifter.data.local.AppDatabase
import com.example.fileshifter.data.model.FormatoSalida
import com.example.fileshifter.data.model.tipoPorNombre
import com.example.fileshifter.data.repository.DocumentoRepositoryImpl
import com.example.fileshifter.data.repository.HistorialRepository
import com.example.fileshifter.data.source.CacheVisor
import com.example.fileshifter.presentation.visor.VisorActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Pruebas del visor y del cache: comprueba que se genera la miniatura de un PDF,
 * que se reutiliza, que el cache se puede vaciar y que el historial guarda la
 * copia publica del archivo (la que sobrevive a la desinstalacion).
 */
@RunWith(AndroidJUnit4::class)
class VisorE2ETest {

    @get:Rule
    val regla = createEmptyComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var historial: HistorialRepository
    private lateinit var repo: DocumentoRepositoryImpl

    @Before
    fun preparar() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        historial = HistorialRepository(db.historialDao())
        repo = DocumentoRepositoryImpl(context, historial)
    }

    @After
    fun cerrar() = runBlocking {
        CacheVisor.carpeta(context).listFiles()?.forEach { it.delete() }
        db.close()
    }

    private suspend fun crearPdf(): File {
        val origen = File(context.cacheDir, "visor.txt").apply {
            writeText("Documento de prueba para el visor interno de FileShifter.")
        }
        val resultado = repo.convertir(
            listOf(
                com.example.fileshifter.data.model.ArchivoSeleccionado(
                    uri = Uri.fromFile(origen).toString(),
                    nombre = origen.name,
                    tamanoBytes = origen.length(),
                    tipo = tipoPorNombre(origen.name)
                )
            ),
            FormatoSalida.PDF
        )
        return File(resultado.archivos.first())
    }

    private fun crearImagen(): File {
        val mapa = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
        Canvas(mapa).apply {
            drawColor(Color.WHITE)
            drawText("Hotel", 60f, 120f, Paint().apply { color = Color.BLACK; textSize = 48f })
        }
        val archivo = File(context.cacheDir, "visor_foto.png")
        archivo.outputStream().use { mapa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        mapa.recycle()
        return archivo
    }

    @Test
    fun generaMiniaturaDeLaPrimeraPaginaDeUnPdf() = runBlocking {
        val pdf = crearPdf()
        val miniatura = CacheVisor.miniatura(context, pdf)

        assertNotNull("Debe existir una miniatura del PDF", miniatura)
        assertTrue(miniatura!!.exists())
        assertTrue(miniatura.length() > 0)
    }

    @Test
    fun reutilizaLaMiniaturaYaGenerada() = runBlocking {
        val pdf = crearPdf()
        val primera = CacheVisor.miniatura(context, pdf)
        val segunda = CacheVisor.miniatura(context, pdf)

        assertNotNull(primera)
        assertEquals(primera!!.absolutePath, segunda!!.absolutePath)
        assertEquals(primera.lastModified(), segunda.lastModified())
    }

    @Test
    fun generaMiniaturaDeUnaImagen() = runBlocking {
        val imagen = crearImagen()
        val miniatura = CacheVisor.miniatura(context, imagen)

        assertNotNull(miniatura)
        assertTrue(miniatura!!.length() > 0)
    }

    @Test
    fun limpiarElCacheBorraLasMiniaturasViejas() = runBlocking {
        val pdf = crearPdf()
        val miniatura = CacheVisor.miniatura(context, pdf)!!
        assertTrue(miniatura.setLastModified(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))

        CacheVisor.limpiar(context)

        assertTrue("La miniatura vieja debe borrarse", !miniatura.exists())
    }

    @Test
    fun elHistorialGuardaLaCopiaPublicaDelArchivo() = runBlocking {
        crearPdf()
        val registros = historial.historial.first()

        assertTrue(registros.isNotEmpty())
        val registro = registros.first()
        assertNotNull("Debe tener la Uri publica de MediaStore", registro.publica)
        assertTrue(
            "La copia publica debe apuntar a Documentos: ${registro.publica}",
            registro.publica!!.startsWith("content://") || registro.publica!!.contains("Documents")
        )
    }

    @Test
    fun elCacheNoSeMezclaConLosDocumentos() {
        val carpetaCache = CacheVisor.carpeta(context)
        val carpetaDocumentos = File(context.getExternalFilesDir(null), "FileShifter")

        assertTrue(carpetaCache.absolutePath.contains("cache"))
        assertTrue(
            "El cache no debe estar dentro de la carpeta de documentos",
            !carpetaCache.absolutePath.startsWith(carpetaDocumentos.absolutePath)
        )
    }

    @Test
    fun elVisorDibujaLaPrimeraPaginaDeUnPdf() = runBlocking {
        val pdf = crearPdf()
        val intencion = Intent(context, VisorActivity::class.java)
            .putExtra(VisorActivity.EXTRA_URI, Uri.fromFile(pdf).toString())
            .putExtra(VisorActivity.EXTRA_NOMBRE, pdf.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        ActivityScenario.launch<VisorActivity>(intencion).use {
            regla.waitUntil(timeoutMillis = 15_000) {
                regla.onAllNodesWithText("Página 1 de 1").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}
