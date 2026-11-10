package com.example.fileshifter

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fileshifter.data.source.PdfEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfEngineTest {
    private lateinit var contexto: Context
    private lateinit var carpeta: File

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        carpeta = File(contexto.cacheDir, "pdf-test").apply {
            mkdirs()
            listFiles()?.forEach { it.delete() }
        }
    }

    @Test
    fun crearPdfDeTextoYLeerlo() {
        val pdf = PdfEngine.textoAPdf(
            texto = "Linea uno\nLinea dos\nInformación con acentos",
            titulo = "Prueba",
            salida = File(carpeta, "base.pdf")
        )

        assertTrue(pdf.exists() && pdf.length() > 0)
        val info = PdfEngine.info(contexto, Uri.fromFile(pdf))
        assertEquals(1, info.paginas)

        val texto = PdfEngine.pdfATexto(contexto, Uri.fromFile(pdf))
        assertTrue(texto.contains("Linea uno"))
    }

    @Test
    fun rotarMarcaAguaYComprimir() {
        val base = PdfEngine.textoAPdf("Contenido", "Original", File(carpeta, "base.pdf"))
        val uri = Uri.fromFile(base)

        val rotado = PdfEngine.rotar(contexto, uri, 90, File(carpeta, "rotado.pdf"))
        assertTrue(rotado.length() > 0)

        val marca = PdfEngine.marcaAgua(contexto, uri, "COPIA", File(carpeta, "marca.pdf"))
        assertTrue(marca.length() > 0)

        val comprimido = PdfEngine.comprimir(contexto, uri, File(carpeta, "comprimido.pdf"))
        assertTrue(comprimido.length() > 0)
    }

    @Test
    fun unirYDividir() {
        val uno = PdfEngine.textoAPdf("Uno", "Documento uno", File(carpeta, "uno.pdf"))
        val dos = PdfEngine.textoAPdf("Dos", "Documento dos", File(carpeta, "dos.pdf"))

        val unido = PdfEngine.unir(
            contexto,
            listOf(Uri.fromFile(uno), Uri.fromFile(dos)),
            File(carpeta, "unido.pdf")
        )
        assertEquals(2, PdfEngine.info(contexto, Uri.fromFile(unido)).paginas)

        val paginas = PdfEngine.dividir(contexto, Uri.fromFile(unido), carpeta, "dividido")
        assertEquals(2, paginas.size)
        assertTrue(paginas.all { it.length() > 0 })
    }

    @Test
    fun eliminarYReordenarPaginas() {
        val largo = (1..80).joinToString("\n") { "Linea $it" }
        val base = PdfEngine.textoAPdf(largo, "Dos paginas", File(carpeta, "base.pdf"))
        assertEquals(2, PdfEngine.info(contexto, Uri.fromFile(base)).paginas)

        val recortado = PdfEngine.eliminarPaginas(
            contexto, Uri.fromFile(base), listOf(1), File(carpeta, "recortado.pdf")
        )
        assertEquals(1, PdfEngine.info(contexto, Uri.fromFile(recortado)).paginas)

        val reordenado = PdfEngine.reordenar(
            contexto, Uri.fromFile(base), listOf(2, 1), File(carpeta, "reordenado.pdf")
        )
        assertEquals(2, PdfEngine.info(contexto, Uri.fromFile(reordenado)).paginas)
    }

    @Test
    fun parseaRangosDePaginas() {
        assertEquals(listOf(2, 4, 5, 6), PdfEngine.parsearPaginas("2,4-6"))
        assertEquals(listOf(1, 3), PdfEngine.parsearPaginas("1; 3"))
        assertTrue(PdfEngine.parsearPaginas("").isEmpty())
    }
}
