package com.example.fileshifter

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de arranque: el Application crea el contenedor de dependencias
 * (repositorios, Room, DataStore) sin errores.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun elContenedorDeDependenciasSeCrea() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.carlos.fileshifter", contexto.packageName)

        val app = contexto.applicationContext as FileShifterApp
        assertNotNull(app.container)
        assertNotNull(app.container.documentoRepository)
        assertNotNull(app.container.historialRepository)
        assertNotNull(app.container.ajustesRepository)
    }
}
