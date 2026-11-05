package com.example.fileshifter

import android.app.Application
import android.content.Context
import com.example.fileshifter.data.local.AjustesRepository
import com.example.fileshifter.data.local.AppDatabase
import com.example.fileshifter.data.repository.DocumentoRepository
import com.example.fileshifter.data.repository.DocumentoRepositoryImpl
import com.example.fileshifter.data.repository.HistorialRepository
import com.example.fileshifter.data.source.OcrService
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * Application: crea el contenedor de dependencias una sola vez.
 * Es el equivalente manual a Hilt, para que la inyeccion se vea clara en clase.
 */
class FileShifterApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        container = AppContainer(this)
    }
}

/** Contenedor de dependencias de la app. */
class AppContainer(context: Context) {

    private val baseDatos = AppDatabase.obtener(context)

    val historialRepository: HistorialRepository = HistorialRepository(baseDatos.historialDao())
    val ajustesRepository: AjustesRepository = AjustesRepository(context)
    val documentoRepository: DocumentoRepository =
        DocumentoRepositoryImpl(context, historialRepository)
    val ocrService: OcrService = OcrService(context)
}
