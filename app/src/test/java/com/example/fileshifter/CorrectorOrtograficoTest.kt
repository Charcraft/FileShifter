package com.example.fileshifter

import com.example.fileshifter.domain.CorrectorOrtografico
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectorOrtograficoTest {
    @Test
    fun `detecta tildes faltantes`() {
        val sugerencias = CorrectorOrtografico.revisar("aqui tambien falta informacion")
        val correcciones = sugerencias.map { it.correccion }

        assertTrue(correcciones.contains("aquí"))
        assertTrue(correcciones.contains("también"))
        assertTrue(correcciones.contains("información"))
    }

    @Test
    fun `detecta palabras repetidas`() {
        val sugerencias = CorrectorOrtografico.revisar("la la casa es grande")
        assertEquals(1, sugerencias.count { it.motivo == "Palabra repetida" })
    }

    @Test
    fun `detecta errores frecuentes`() {
        val sugerencias = CorrectorOrtografico.revisar("havia una vez")
        assertEquals("había", sugerencias.first().correccion)
    }

    @Test
    fun `aplicar corrige todo el texto`() {
        val original = "aqui tambien havia informacion"
        val sugerencias = CorrectorOrtografico.revisar(original)
        val corregido = CorrectorOrtografico.aplicar(original, sugerencias)

        assertEquals("aquí también había información", corregido)
    }

    @Test
    fun `no marca texto correcto`() {
        val sugerencias = CorrectorOrtografico.revisar("Aquí también había información correcta.")
        assertTrue(sugerencias.isEmpty())
    }

    @Test
    fun `texto vacio no produce sugerencias`() {
        assertTrue(CorrectorOrtografico.revisar("").isEmpty())
    }
}
