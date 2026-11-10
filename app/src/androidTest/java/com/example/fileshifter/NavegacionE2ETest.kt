package com.example.fileshifter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavegacionE2ETest {
    @get:Rule
    val regla = createAndroidComposeRule<MainActivity>()

    private fun entrarAlInicio() {
        regla.onNodeWithText("Comenzar").performClick()
        regla.waitForIdle()
        regla.onNodeWithText("Herramientas").assertIsDisplayed()
    }

    @Test
    fun laBienvenidaLlevaAlInicioConLasNueveHerramientas() {
        regla.onNodeWithText("FileShifter").assertExists()
        regla.onNodeWithText("Comenzar").assertIsDisplayed()

        entrarAlInicio()

        val herramientas = listOf(
            "Convertir",
            "Imagen → PDF",
            "Editar PDF",
            "Escanear",
            "Corrector",
            "Referencias",
            "Modo Entrega",
            "QR / Barras",
            "Validador",
            "Historial"
        )
        herramientas.forEach { nombre ->
            regla.onAllNodesWithText(nombre).onFirst().assertExists()
        }
    }

    @Test
    fun abrirConvertir() {
        entrarAlInicio()
        regla.onAllNodesWithText("Convertir").onFirst().performClick()
        regla.waitForIdle()
        regla.onNodeWithText("Convertir a:").assertIsDisplayed()
    }

    @Test
    fun abrirImagenAPdf() {
        entrarAlInicio()
        regla.onAllNodesWithText("Imagen → PDF").onFirst().performClick()
        regla.waitForIdle()
        regla.onNodeWithText("Modo de salida").assertIsDisplayed()
    }

    @Test
    fun abrirReferencias() {
        entrarAlInicio()
        regla.onAllNodesWithText("Referencias").onFirst().performClick()
        regla.waitForIdle()
        regla.onNodeWithText("Buscar bibliografía").assertIsDisplayed()
    }

    @Test
    fun abrirModoEntrega() {
        entrarAlInicio()
        regla.onAllNodesWithText("Modo Entrega").onFirst().performClick()
        regla.waitForIdle()
        regla.onNodeWithText("Elige una plantilla de portada para el documento.").assertIsDisplayed()
    }

    @Test
    fun abrirCorrectorYValidador() {
        entrarAlInicio()

        regla.onAllNodesWithText("Corrector").onFirst().performClick()
        regla.waitForIdle()
        regla.onNodeWithText("Corrector ortográfico").assertExists()

        regla.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        regla.waitForIdle()

        regla.onAllNodesWithText("Validador").onFirst().performClick()
        regla.waitForIdle()
        regla.onNodeWithText("Elige un PDF para revisar el peso, las páginas y generar su QR de entrega.")
            .assertExists()
    }
}
