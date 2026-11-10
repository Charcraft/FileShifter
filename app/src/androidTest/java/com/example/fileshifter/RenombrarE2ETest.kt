package com.example.fileshifter

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fileshifter.data.local.AppDatabase
import com.example.fileshifter.data.model.ArchivoSeleccionado
import com.example.fileshifter.data.model.FormatoSalida
import com.example.fileshifter.data.model.tipoPorNombre
import com.example.fileshifter.data.repository.DocumentoRepositoryImpl
import com.example.fileshifter.data.repository.HistorialRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Pruebas de "cambiar nombre" desde el historial: se renombra el archivo privado,
 * la copia de Documentos y el registro de Room, conservando la extension.
 */
@RunWith(AndroidJUnit4::class)
class RenombrarE2ETest {

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

        // Solo se limpian los archivos que generan estas pruebas, no los del usuario.
        val carpeta = File(context.getExternalFilesDir(null), "FileShifter")
        val prefijos = listOf(
            "original", "para_renombrar", "invalido", "uno", "dos", "vacio",
            "Acta", "Constancia"
        )
        carpeta.listFiles()?.forEach { archivo ->
            if (prefijos.any { archivo.name.startsWith(it) }) archivo.delete()
        }
    }

    @After
    fun cerrar() {
        // Quita de Documentos las copias creadas por estas pruebas.
        val prefijos = listOf("original", "para_renombrar", "invalido", "uno", "dos", "vacio", "Acta", "Constancia")
        runCatching {
            val coleccion = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            prefijos.forEach { prefijo ->
                context.contentResolver.delete(
                    coleccion,
                    "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
                    arrayOf("$prefijo%")
                )
            }
        }
        db.close()
    }

    private suspend fun generarPdf(nombre: String = "original.txt"): File {
        val origen = File(context.cacheDir, nombre).apply {
            writeText("Documento de prueba para renombrar en el historial.")
        }
        val resultado = repo.convertir(
            listOf(
                ArchivoSeleccionado(
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

    @Test
    fun renombraElArchivoYElRegistro() = runBlocking {
        val pdf = generarPdf()
        val registro = historial.historial.first().first { it.nombre == pdf.name }

        val resultado = repo.renombrar(registro, "Acta de la reunion")

        assertTrue(resultado.mensaje, resultado.exito)
        val actualizado = historial.historial.first().first { it.id == registro.id }
        assertEquals("Acta de la reunion.pdf", actualizado.nombre)
        assertTrue(File(actualizado.ruta).exists())
        assertFalse("El archivo anterior ya no debe existir", File(registro.ruta).exists())
        assertEquals("El nombre debe conservar la extension", "pdf", File(actualizado.ruta).extension)
    }

    @Test
    fun renombraTambienLaCopiaDeDocumentos() = runBlocking {
        val pdf = generarPdf("para_renombrar.txt")
        val registro = historial.historial.first().first { it.nombre == pdf.name }
        assertNotNull("El registro debe tener copia publica", registro.publica)

        repo.renombrar(registro, "Constancia")

        val uri = Uri.parse(
            historial.historial.first().first { it.id == registro.id }.publica
        )
        val nombrePublico = context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

        assertTrue(
            "La copia pública debe llamarse Constancia: $nombrePublico",
            nombrePublico?.startsWith("Constancia") == true && nombrePublico.endsWith(".pdf")
        )
    }

    @Test
    fun quitaCaracteresNoPermitidos() = runBlocking {
        val pdf = generarPdf("invalido.txt")
        val registro = historial.historial.first().first { it.nombre == pdf.name }

        repo.renombrar(registro, "Acta/Final:2026?")

        val actualizado = historial.historial.first().first { it.id == registro.id }
        assertEquals("ActaFinal2026.pdf", actualizado.nombre)
    }

    @Test
    fun siElNombreYaExisteAgregaUnSufijo() = runBlocking {
        val primero = generarPdf("uno.txt")
        val segundo = generarPdf("dos.txt")
        val registros = historial.historial.first()
        val destino = registros.first { it.nombre == primero.name }

        repo.renombrar(destino, segundo.name.removeSuffix(".pdf"))

        val actualizado = historial.historial.first().first { it.id == destino.id }
        assertTrue(
            "No debe sobrescribir el otro archivo: ${actualizado.nombre}",
            actualizado.nombre.startsWith(segundo.name.removeSuffix(".pdf")) &&
                actualizado.nombre != segundo.name
        )
    }

    @Test
    fun unNombreVacioNoCambiaNada() = runBlocking {
        val pdf = generarPdf("vacio.txt")
        val registro = historial.historial.first().first { it.nombre == pdf.name }

        val resultado = repo.renombrar(registro, "   ")

        assertFalse(resultado.exito)
        val actualizado = historial.historial.first().first { it.id == registro.id }
        assertEquals(registro.nombre, actualizado.nombre)
    }
}
