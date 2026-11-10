package com.example.fileshifter

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fileshifter.data.local.AppDatabase
import com.example.fileshifter.data.model.ArchivoSeleccionado
import com.example.fileshifter.data.model.FormatoSalida
import com.example.fileshifter.data.model.TipoDocumento
import com.example.fileshifter.data.repository.DocumentoRepositoryImpl
import com.example.fileshifter.data.repository.HistorialRepository
import com.example.fileshifter.data.source.PdfEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ConversionE2ETest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: DocumentoRepositoryImpl

    private val textoLargo = """
        Acta de la reunion del proyecto FileShifter
        Fecha: 10 de noviembre de 2026
        Se revisaron las nueve funciones de la aplicacion.
        Acuerdo: entregar el APK con la version 1.0.0.
    """.trimIndent()

    @Before
    fun preparar() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = DocumentoRepositoryImpl(context, HistorialRepository(db.historialDao()))
    }

    @After
    fun cerrar() = runBlocking {
        db.close()
    }

    private fun archivoTemporal(nombre: String, contenido: String): ArchivoSeleccionado {
        val archivo = File(context.cacheDir, nombre)
        archivo.writeText(contenido)
        return describir(archivo, nombre)
    }

    private fun describir(archivo: File, nombre: String): ArchivoSeleccionado =
        ArchivoSeleccionado(
            uri = Uri.fromFile(archivo).toString(),
            nombre = nombre,
            tamanoBytes = archivo.length(),
            tipo = com.example.fileshifter.data.model.tipoPorNombre(nombre)
        )

    private fun archivoDocx(nombre: String, texto: String): ArchivoSeleccionado {
        val archivo = File(context.cacheDir, nombre)
        fun parrafo(t: String) =
            "<w:p><w:r><w:t xml:space=\"preserve\">${t.replace("&", "&amp;").replace("<", "&lt;")}</w:t></w:r></w:p>"

        ZipOutputStream(archivo.outputStream()).use { zip ->
            fun entrar(ruta: String, contenido: String) {
                zip.putNextEntry(ZipEntry(ruta))
                zip.write(contenido.toByteArray())
                zip.closeEntry()
            }
            entrar(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>"""
            )
            entrar(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""
            )
            entrar(
                "word/document.xml",
                """<?xml version="1.0" encoding="UTF-8"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>${parrafo(texto)}</w:body></w:document>"""
            )
        }
        return describir(archivo, nombre)
    }

    private fun textoDeDocx(archivo: File): String {
        ZipFile(archivo).use { zip ->
            val entrada = zip.getEntry("word/document.xml")
                ?: error("El DOCX no tiene word/document.xml")
            return zip.getInputStream(entrada).bufferedReader().readText()
        }
    }

    @Test
    fun txtAPdf_generaUnPdfValido() = runBlocking {
        val origen = archivoTemporal("acta.txt", textoLargo)
        val resultado = repo.convertir(listOf(origen), FormatoSalida.PDF)

        assertTrue(resultado.exito)
        assertEquals(1, resultado.archivos.size)
        val pdf = File(resultado.archivos.first())
        assertTrue("El PDF debe existir", pdf.exists())
        assertTrue("El PDF debe pesar algo", pdf.length() > 500)

        val info = PdfEngine.info(context, Uri.fromFile(pdf))
        assertEquals(1, info.paginas)
        assertTrue(info.ancho > 500f)
    }

    @Test
    fun docxAPdf_extraeElTextoYLoImprime() = runBlocking {
        val origen = archivoDocx("informe.docx", "Contenido del informe de pruebas")
        val resultado = repo.convertir(listOf(origen), FormatoSalida.PDF)

        assertTrue(resultado.exito)
        val pdf = File(resultado.archivos.first())
        val textoDelPdf = PdfEngine.pdfATexto(context, Uri.fromFile(pdf))
        assertTrue(
            "El PDF debe contener el texto del DOCX: $textoDelPdf",
            textoDelPdf.contains("Contenido del informe de pruebas", ignoreCase = true)
        )
    }

    @Test
    fun pdfADocx_esLaConversionInversa() = runBlocking {
        val origen = archivoTemporal("resumen.txt", textoLargo)
        val pdfGenerado = repo.convertir(listOf(origen), FormatoSalida.PDF)
        assertTrue(pdfGenerado.exito)

        val pdf = describir(File(pdfGenerado.archivos.first()), "resumen.pdf")
        val resultado = repo.convertir(listOf(pdf), FormatoSalida.DOCX)

        assertTrue(resultado.exito)
        val docx = File(resultado.archivos.first())
        assertTrue(docx.name.endsWith(".docx"))
        val xml = textoDeDocx(docx)
        assertTrue(
            "El DOCX debe contener el texto que tenia el PDF",
            xml.contains("FileShifter") || xml.contains("Acta de la reunion")
        )
    }

    @Test
    fun pdfATxt_recuperaElTexto() = runBlocking {
        val origen = archivoTemporal("carta.txt", textoLargo)
        val pdf = repo.convertir(listOf(origen), FormatoSalida.PDF)
        val archivoPdf = describir(File(pdf.archivos.first()), "carta.pdf")

        val resultado = repo.convertir(listOf(archivoPdf), FormatoSalida.TXT)
        assertTrue(resultado.exito)
        val texto = File(resultado.archivos.first()).readText()
        assertTrue(texto.contains("Acuerdo", ignoreCase = true))
    }

    @Test
    fun htmlYcsvAPdf_tambienFuncionan() = runBlocking {
        val html = archivoTemporal(
            "pagina.html",
            "<html><body><h1>Reporte</h1><p>Datos del proyecto</p></body></html>"
        )
        val htmlPdf = repo.convertir(listOf(html), FormatoSalida.PDF)
        assertTrue("HTML a PDF", htmlPdf.exito)

        val csv = archivoTemporal("tabla.csv", "nombre,calificacion\nCarlos,95\nAna,88\n")
        val csvPdf = repo.convertir(listOf(csv), FormatoSalida.PDF)
        assertTrue("CSV a PDF", csvPdf.exito)

        val textoCsv = PdfEngine.pdfATexto(context, Uri.fromFile(File(csvPdf.archivos.first())))
        assertTrue(textoCsv.contains("Carlos"))
    }

    @Test
    fun textoADocx_abreComoDocumentoDeWord() = runBlocking {
        val origen = archivoTemporal("notas.txt", textoLargo)
        val resultado = repo.convertir(listOf(origen), FormatoSalida.DOCX)

        assertTrue(resultado.exito)
        val docx = File(resultado.archivos.first())
        val xml = textoDeDocx(docx)
        assertTrue(xml.contains("w:document"))
        assertTrue(xml.contains("Acta de la reunion"))
    }

    @Test
    fun variosArchivosAUnMismoFormato() = runBlocking {
        val uno = archivoTemporal("uno.txt", "Primer documento")
        val dos = archivoTemporal("dos.txt", "Segundo documento")
        val resultado = repo.convertir(listOf(uno, dos), FormatoSalida.PDF)

        assertTrue(resultado.exito)
        assertEquals(2, resultado.archivos.size)
        resultado.archivos.forEach { ruta ->
            assertTrue(File(ruta).length() > 400)
        }
    }
}
