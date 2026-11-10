package com.example.fileshifter

import com.example.fileshifter.data.model.FormatoSalida
import com.example.fileshifter.data.model.TipoDocumento
import com.example.fileshifter.data.model.salidasCompatibles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Comprueba que cada tipo de archivo ofrece las salidas que promete la pantalla. */
class FormatosTest {

    @Test
    fun unPdfSePuedeConvertirATxtYADocx() {
        val salidas = salidasCompatibles(TipoDocumento.PDF)
        assertEquals(listOf(FormatoSalida.TXT, FormatoSalida.DOCX), salidas)
    }

    @Test
    fun unaImagenSoloSeConvierteAPdf() {
        assertEquals(listOf(FormatoSalida.PDF), salidasCompatibles(TipoDocumento.IMAGEN))
    }

    @Test
    fun losFormatosDeTextoOfrecenLasTresSalidas() {
        listOf(
            TipoDocumento.DOCX,
            TipoDocumento.XLSX,
            TipoDocumento.PPTX,
            TipoDocumento.TXT,
            TipoDocumento.CSV,
            TipoDocumento.HTML
        ).forEach { tipo ->
            val salidas = salidasCompatibles(tipo)
            assertTrue("$tipo debe ofrecer PDF", salidas.contains(FormatoSalida.PDF))
            assertTrue("$tipo debe ofrecer TXT", salidas.contains(FormatoSalida.TXT))
            assertTrue("$tipo debe ofrecer DOCX", salidas.contains(FormatoSalida.DOCX))
        }
    }

    @Test
    fun unArchivoDesconocidoNoOfreceSalidas() {
        assertTrue(salidasCompatibles(TipoDocumento.DESCONOCIDO).isEmpty())
    }
}
