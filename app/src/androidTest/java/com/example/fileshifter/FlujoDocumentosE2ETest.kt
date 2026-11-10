package com.example.fileshifter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fileshifter.data.local.AppDatabase
import com.example.fileshifter.data.model.ArchivoSeleccionado
import com.example.fileshifter.data.model.tipoPorNombre
import com.example.fileshifter.data.repository.DocumentoRepositoryImpl
import com.example.fileshifter.data.repository.HistorialRepository
import com.example.fileshifter.data.source.PdfEngine
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FlujoDocumentosE2ETest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var historial: HistorialRepository
    private lateinit var repo: DocumentoRepositoryImpl

    @Before
    fun preparar() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        historial = HistorialRepository(db.historialDao())
        repo = DocumentoRepositoryImpl(context, historial)
    }

    @After
    fun cerrar() = runBlocking {
        db.close()
    }

    private fun describir(archivo: File): ArchivoSeleccionado = ArchivoSeleccionado(
        uri = Uri.fromFile(archivo).toString(),
        nombre = archivo.name,
        tamanoBytes = archivo.length(),
        tipo = tipoPorNombre(archivo.name)
    )

    private fun crearImagen(nombre: String): File {
        val mapa = Bitmap.createBitmap(1240, 1754, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(mapa)
        lienzo.drawColor(Color.WHITE)
        val pincel = Paint().apply {
            color = Color.BLACK
            textSize = 64f
            isAntiAlias = true
        }
        lienzo.drawText("Documento de prueba", 90f, 240f, pincel)
        lienzo.drawText("FileShifter 1.0.0", 90f, 340f, pincel)
        val archivo = File(context.cacheDir, nombre)
        archivo.outputStream().use { mapa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        mapa.recycle()
        return archivo
    }

    @Test
    fun imagenAPdf_generaUnaPaginaTamanoCarta() = runBlocking {
        val imagen = crearImagen("captura.png")
        val resultado = repo.imagenesAPdf(listOf(Uri.fromFile(imagen).toString()), unir = true, calidad = 1)

        assertTrue(resultado.mensaje, resultado.exito)
        val pdf = File(resultado.archivos.first())
        assertTrue(pdf.exists())
        val info = PdfEngine.info(context, Uri.fromFile(pdf))
        assertEquals(1, info.paginas)

        val anchoA4 = info.ancho in 580f..610f || info.alto in 580f..610f
        assertTrue("El PDF debe ser tamano carta/A4 (${info.ancho} x ${info.alto})", anchoA4)
    }

    @Test
    fun elPdfGeneradoApareceEnLaCarpetaDocumentos() = runBlocking {
        val imagen = crearImagen("publicar.png")
        val resultado = repo.imagenesAPdf(
            listOf(Uri.fromFile(imagen).toString()),
            unir = true,
            calidad = 1
        )
        assertTrue(resultado.exito)
        val nombre = File(resultado.archivos.first()).name

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val proyeccion = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH
            )
            val uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            context.contentResolver.query(
                uri,
                proyeccion,
                "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                arrayOf(nombre),
                null
            )?.use { cursor ->
                assertTrue("El archivo $nombre debe existir en MediaStore", cursor.moveToFirst())
                val carpeta = cursor.getString(1).orEmpty()
                assertTrue(
                    "Debe estar en Documentos/FileShifter y esta en $carpeta",
                    carpeta.contains("Documents/FileShifter")
                )
                val tamano = cursor.getLong(0)
                assertTrue(tamano >= 0)
            } ?: error("No se pudo consultar MediaStore")
        } else {
            @Suppress("DEPRECATION")
            val carpeta = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "FileShifter"
            )
            assertTrue(File(carpeta, nombre).exists())
        }
    }

    @Test
    fun elHistorialRegistraLoGenerado() = runBlocking {
        val imagen = crearImagen("historial.png")
        val resultado = repo.imagenesAPdf(
            listOf(Uri.fromFile(imagen).toString()),
            unir = true,
            calidad = 1
        )
        assertTrue(resultado.exito)

        val registros = runBlocking { historial.historial.first() }
        assertTrue(registros.any { it.nombre == File(resultado.archivos.first()).name })
    }

    @Test
    fun validador_calculaElMismoHashQueElSistema() = runBlocking {
        val imagen = crearImagen("validar.png")
        val pdf = repo.imagenesAPdf(listOf(Uri.fromFile(imagen).toString()), true, 1)
        val archivo = File(pdf.archivos.first())

        val reporte = repo.validar(describir(archivo))
        val digest = MessageDigest.getInstance("SHA-256")
        val esperado = digest.digest(archivo.readBytes()).joinToString("") { "%02x".format(it) }

        assertEquals(esperado, reporte.hash)
        assertEquals(1, reporte.paginas)
        assertTrue(reporte.chequeos.isNotEmpty())
        assertNotNull(reporte.semaforo)
    }

    @Test
    fun qrGenerado_sePuedeLeerConMlKit() = runBlocking {
        val url = "https://github.com/Charcraft/FileShifter"
        val imagen = repo.generarCodigo(url, esQr = true)

        assertTrue(imagen.exists())
        val mapa = BitmapFactory.decodeFile(imagen.absolutePath)
        assertNotNull(mapa)

        val lector = BarcodeScanning.getClient()
        val codigos = Tasks.await(
            lector.process(InputImage.fromBitmap(mapa, 0)),
            20,
            TimeUnit.SECONDS
        )
        lector.close()

        assertTrue("Debe detectar al menos un codigo", codigos.isNotEmpty())
        assertEquals(url, codigos.first().rawValue)
    }

    @Test
    fun codigoDeBarras_generaCode128Legible() = runBlocking {
        val contenido = "253432"
        val imagen = repo.generarCodigo(contenido, esQr = false)
        val mapa = BitmapFactory.decodeFile(imagen.absolutePath)

        val lector = BarcodeScanning.getClient()
        val codigos = Tasks.await(lector.process(InputImage.fromBitmap(mapa, 0)), 20, TimeUnit.SECONDS)
        lector.close()

        assertTrue(codigos.isNotEmpty())
        assertEquals(contenido, codigos.first().rawValue)
    }
}
