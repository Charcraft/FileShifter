package com.example.fileshifter

import android.app.Application
import android.content.Context
import com.example.fileshifter.data.local.AjustesRepository
import com.example.fileshifter.data.local.AppDatabase
import com.example.fileshifter.data.repository.DocumentoRepository
import com.example.fileshifter.data.repository.DocumentoRepositoryImpl
import com.example.fileshifter.data.repository.HistorialRepository
import com.example.fileshifter.data.repository.ReferenciasRepository
import com.example.fileshifter.data.repository.ReferenciasRepositoryImpl
import com.example.fileshifter.data.source.OcrService
import com.example.fileshifter.data.source.Archivos
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.File

class FileShifterApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    private val baseDatos = AppDatabase.obtener(context)

    val carpetaSalida: File = Archivos.carpetaSalida(context)
    val historialRepository: HistorialRepository = HistorialRepository(baseDatos.historialDao())
    val ajustesRepository: AjustesRepository = AjustesRepository(context)
    val documentoRepository: DocumentoRepository =
        DocumentoRepositoryImpl(context, historialRepository)
    val referenciasRepository: ReferenciasRepository =
        ReferenciasRepositoryImpl(baseDatos.referenciaDao())
    val ocrService: OcrService = OcrService(context)
}
