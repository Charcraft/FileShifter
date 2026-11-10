package com.example.fileshifter.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cache exclusiva del visor: guarda miniaturas de los documentos para que el
 * historial se pueda mostrar rapido sin volver a abrir cada PDF.
 *
 * Vive en cacheDir/visor, nunca se mezcla con los documentos del usuario y el
 * sistema puede borrarla sin consecuencias: si falta una miniatura se vuelve a
 * generar a partir del archivo original.
 */
object CacheVisor {

    private const val LIMITE_MB = 40
    private const val DIAS = 7
    private const val ANCHO_MINIATURA = 360

    fun carpeta(context: Context): File =
        File(context.cacheDir, "visor").apply { mkdirs() }

    /** Nombre estable: si el archivo cambia de tamano o fecha, cambia la clave. */
    private fun clave(archivo: File): String =
        "${archivo.name}_${archivo.length()}_${archivo.lastModified()}.png"

    /**
     * Devuelve el PNG de la miniatura, generandola si no existe.
     * Para PDF se dibuja la primera pagina; para imagenes se reduce la propia foto.
     */
    suspend fun miniatura(context: Context, archivo: File): File? = withContext(Dispatchers.IO) {
        if (!archivo.exists()) return@withContext null
        val destino = File(carpeta(context), clave(archivo))
        if (destino.exists() && destino.length() > 0) return@withContext destino

        val mapa = when (archivo.extension.lowercase()) {
            "pdf" -> primeraPagina(archivo)
            "png", "jpg", "jpeg", "webp" -> imagenReducida(archivo)
            else -> null
        } ?: return@withContext null

        runCatching {
            destino.outputStream().use { mapa.compress(Bitmap.CompressFormat.PNG, 85, it) }
        }
        mapa.recycle()
        if (destino.length() > 0) destino else null
    }

    private fun primeraPagina(archivo: File): Bitmap? = runCatching {
        val descriptor = ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY)
        PdfRenderer(descriptor).use { renderer ->
            if (renderer.pageCount == 0) return null
            renderer.openPage(0).use { pagina ->
                val alto = (ANCHO_MINIATURA.toFloat() * pagina.height / pagina.width).toInt()
                val mapa = Bitmap.createBitmap(ANCHO_MINIATURA, alto, Bitmap.Config.ARGB_8888)
                mapa.eraseColor(Color.WHITE)
                pagina.render(mapa, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                mapa
            }
        }
    }.getOrNull()

    private fun imagenReducida(archivo: File): Bitmap? = runCatching {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.absolutePath, opciones)
        if (opciones.outWidth <= 0) return null

        var muestra = 1
        while (opciones.outWidth / muestra > ANCHO_MINIATURA * 2) muestra *= 2

        val mapa = BitmapFactory.decodeFile(
            archivo.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = muestra }
        ) ?: return null

        val alto = (ANCHO_MINIATURA.toFloat() * mapa.height / mapa.width).toInt()
        val reducido = Bitmap.createScaledBitmap(mapa, ANCHO_MINIATURA, alto, true)
        if (reducido !== mapa) mapa.recycle()
        reducido
    }.getOrNull()

    /** Borra miniaturas viejas y recorta el tamano total del cache. */
    fun limpiar(context: Context) {
        runCatching {
            val carpeta = carpeta(context)
            val limite = System.currentTimeMillis() - DIAS * 24L * 60L * 60L * 1000L
            carpeta.listFiles()?.forEach { archivo ->
                if (archivo.lastModified() < limite) archivo.delete()
            }

            val archivos = carpeta.listFiles()?.sortedByDescending { it.lastModified() } ?: return
            var total = 0L
            archivos.forEach { archivo ->
                total += archivo.length()
                if (total > LIMITE_MB * 1024L * 1024L) archivo.delete()
            }
        }
    }

    /** Borra la miniatura asociada a un archivo (por ejemplo al renombrarlo). */
    fun borrarMiniatura(context: Context, archivo: File) {
        runCatching { File(carpeta(context), clave(archivo)).delete() }
    }

    /** Tamano actual del cache, para mostrarlo en Ajustes. */
    fun tamanoBytes(context: Context): Long =
        carpeta(context).listFiles()?.sumOf { it.length() } ?: 0L
}
