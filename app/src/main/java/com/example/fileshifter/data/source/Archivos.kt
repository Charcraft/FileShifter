package com.example.fileshifter.data.source

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.fileshifter.data.model.ArchivoSeleccionado
import com.example.fileshifter.data.model.tipoPorNombre
import java.io.File
import java.io.FileOutputStream

object Archivos {
    fun carpetaSalida(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, "FileShifter").apply { mkdirs() }
    }

    fun nombreDe(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val indice = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (indice >= 0 && cursor.moveToFirst()) {
                return cursor.getString(indice) ?: "archivo"
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "archivo"
    }

    fun tamanoDe(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val indice = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (indice >= 0 && cursor.moveToFirst() && !cursor.isNull(indice)) {
                return cursor.getLong(indice)
            }
        }

        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
        }.getOrDefault(0L)
    }

    fun describir(context: Context, uri: Uri): ArchivoSeleccionado {
        val nombre = nombreDe(context, uri)
        return ArchivoSeleccionado(
            uri = uri.toString(),
            nombre = nombre,
            tamanoBytes = tamanoDe(context, uri),
            tipo = tipoPorNombre(nombre)
        )
    }

    fun leerTexto(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()

    fun copiarA(context: Context, uri: Uri, destino: File): File {
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            FileOutputStream(destino).use { salida -> entrada.copyTo(salida) }
        } ?: error("No se pudo abrir el archivo de origen")
        return destino
    }

    fun temporal(context: Context, uri: Uri, nombre: String): File {
        val destino = File(context.cacheDir, nombre)
        return copiarA(context, uri, destino)
    }

    fun uriCompartir(context: Context, ruta: String): Uri {
        val archivo = File(ruta)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )
    }

    fun nombreUnico(context: Context, base: String, extension: String): File {
        val carpeta = carpetaSalida(context)
        var candidato = File(carpeta, "$base.$extension")
        var contador = 1
        while (candidato.exists()) {
            candidato = File(carpeta, "${base}_$contador.$extension")
            contador++
        }
        return candidato
    }

    fun limpiarNombre(nombre: String): String =
        nombre.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9_\\- ]"), "_").trim().ifBlank { "archivo" }

    fun mimeDe(nombre: String): String = when (nombre.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "csv" -> "text/csv"
        "html", "htm" -> "text/html"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "application/octet-stream"
    }

    fun publicarEnDocumentos(context: Context, archivo: File): String? = runCatching {
        if (!archivo.exists()) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val valores = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, archivo.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeDe(archivo.name))
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + "/FileShifter"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val coleccion = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(coleccion, valores) ?: return null
            resolver.openOutputStream(uri)?.use { salida ->
                archivo.inputStream().use { entrada -> entrada.copyTo(salida) }
            }
            valores.clear()
            valores.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, valores, null, null)
            "Documentos/FileShifter/${archivo.name}"
        } else {
            @Suppress("DEPRECATION")
            val carpeta = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "FileShifter"
            ).apply { mkdirs() }
            val destino = File(carpeta, archivo.name)
            archivo.copyTo(destino, overwrite = true)
            destino.absolutePath
        }
    }.getOrNull()
}
