package com.example.fileshifter.data.source

import android.content.Context
import android.graphics.BitmapFactory
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OCR offline con ML Kit (reconoce texto en espanol sin enviar la imagen a la nube).
 */
class OcrService(private val context: Context) {

    private val cliente = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val lector = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()

    suspend fun reconocer(archivo: File): String {
        val bitmap = BitmapFactory.decodeFile(archivo.absolutePath) ?: return ""
        return try {
            val imagen = InputImage.fromBitmap(bitmap, 0)
            cliente.process(imagen).esperar().text
        } finally {
            bitmap.recycle()
        }
    }

    /** Lee un QR o código de barras desde una imagen de la galería. */
    suspend fun leerCodigo(archivo: File): String? {
        val bitmap = BitmapFactory.decodeFile(archivo.absolutePath) ?: return null
        return try {
            lector.process(InputImage.fromBitmap(bitmap, 0)).esperar()
                .firstOrNull()?.rawValue
        } finally {
            bitmap.recycle()
        }
    }
}

/** Convierte un Task de Google Play Services en una funcion suspendida. */
suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { continuacion ->
    addOnSuccessListener { resultado ->
        if (continuacion.isActive) continuacion.resume(resultado)
    }
    addOnFailureListener { error ->
        if (continuacion.isActive) continuacion.resumeWithException(error)
    }
}
