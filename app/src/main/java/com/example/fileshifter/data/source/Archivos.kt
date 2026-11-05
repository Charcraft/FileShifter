package com.example.fileshifter.data.source

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.fileshifter.data.model.ArchivoSeleccionado
import com.example.fileshifter.data.model.tipoPorNombre
import java.io.File
import java.io.FileOutputStream

/**
 * Utilidades de archivos compartidas por los repositorios.
 * Ninguna pantalla usa esta clase directamente: siempre pasa por el Repository.
 */
object Archivos {

    /** Carpeta publica de la app: Android/data/<paquete>/files/FileShifter */
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
        // Si el proveedor no reporta el tamanio, copiamos a cache para medirlo.
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

    /** Archivo temporal con el contenido de la Uri (algunos flujos lo necesitan). */
    fun temporal(context: Context, uri: Uri, nombre: String): File {
        val destino = File(context.cacheDir, nombre)
        return copiarA(context, uri, destino)
    }

    /** Uri segura para compartir con FileProvider. */
    fun uriCompartir(context: Context, ruta: String): Uri {
        val archivo = File(ruta)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )
    }

    /** Nombre unico dentro de la carpeta FileShifter/. */
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
}
