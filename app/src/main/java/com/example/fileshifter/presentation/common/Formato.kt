package com.example.fileshifter.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatearFecha(millis: Long): String {
    val ahora = Calendar.getInstance()
    val fecha = Calendar.getInstance().apply { timeInMillis = millis }
    val mismoDia = ahora.get(Calendar.YEAR) == fecha.get(Calendar.YEAR) &&
        ahora.get(Calendar.DAY_OF_YEAR) == fecha.get(Calendar.DAY_OF_YEAR)
    val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    return if (mismoDia) {
        "Hoy, $hora"
    } else {
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))
    }
}

fun iconoDeArchivo(nombre: String): ImageVector = when (nombre.substringAfterLast('.', "").lowercase()) {
    "pdf", "doc", "docx", "txt" -> Icons.Filled.Description
    "png", "jpg", "jpeg", "webp", "heic", "bmp" -> Icons.Filled.Image
    "xls", "xlsx", "csv" -> Icons.Filled.TableChart
    else -> if (nombre.startsWith("Codigo_") || nombre.startsWith("QR")) {
        Icons.Filled.QrCode
    } else {
        Icons.Filled.InsertDriveFile
    }
}
