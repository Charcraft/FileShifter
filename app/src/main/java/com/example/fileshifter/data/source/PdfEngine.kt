package com.example.fileshifter.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.fileshifter.data.model.InfoPdf
import com.example.fileshifter.data.model.OpcionesEdicion
import com.example.fileshifter.data.model.TipoEdicion
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.util.Matrix
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Motor PDF basado en PDFBox-Android (100% offline).
 * Solo lo usa el repositorio; la UI nunca llama esta clase directamente.
 */
object PdfEngine {

    private const val ANCHO_TEXTO = 88f
    private const val MARGEN = 50f
    private const val TAMANO_FUENTE = 11f
    private const val INTERLINEADO = 15f

    // ---------------------------------------------------------------- Imagen a PDF

    /**
     * Convierte una o varias imagenes a PDF.
     * @param unir true = un solo PDF con una pagina por imagen; false = un PDF por imagen.
     * @return lista de archivos generados.
     */
    fun imagenesAPdf(
        context: Context,
        uris: List<Uri>,
        unir: Boolean,
        carpeta: File,
        base: String
    ): List<File> {
        carpeta.mkdirs()
        val generados = mutableListOf<File>()
        if (unir) {
            val doc = PDDocument()
            try {
                uris.forEach { uri ->
                    val bitmap = decodificar(context, uri) ?: return@forEach
                    agregarPaginaConImagen(doc, bitmap)
                    bitmap.recycle()
                }
                val salida = File(carpeta, "${base}_unido.pdf")
                doc.save(salida)
                generados += salida
            } finally {
                doc.close()
            }
        } else {
            uris.forEachIndexed { indice, uri ->
                val bitmap = decodificar(context, uri) ?: return@forEachIndexed
                val doc = PDDocument()
                try {
                    agregarPaginaConImagen(doc, bitmap)
                    val salida = File(carpeta, "${base}_${indice + 1}.pdf")
                    doc.save(salida)
                    generados += salida
                } finally {
                    doc.close()
                    bitmap.recycle()
                }
            }
        }
        return generados
    }

    private fun agregarPaginaConImagen(doc: PDDocument, bitmap: Bitmap) {
        val horizontal = bitmap.width > bitmap.height
        val pagina = if (horizontal) {
            PDPage(PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width))
        } else {
            PDPage(PDRectangle.A4)
        }
        doc.addPage(pagina)

        val imagen = LosslessFactory.createFromImage(doc, bitmap)
        val anchoPagina = pagina.mediaBox.width
        val altoPagina = pagina.mediaBox.height
        val escala = minOf(anchoPagina / imagen.width, altoPagina / imagen.height) * 0.94f
        val ancho = imagen.width * escala
        val alto = imagen.height * escala

        PDPageContentStream(doc, pagina).use { contenido ->
            contenido.drawImage(
                imagen,
                (anchoPagina - ancho) / 2f,
                (altoPagina - alto) / 2f,
                ancho,
                alto
            )
        }
    }

    private fun decodificar(context: Context, uri: Uri): Bitmap? {
        val opciones = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }

        var muestra = 1
        val ladoMayor = maxOf(opciones.outWidth, opciones.outHeight)
        while (ladoMayor / muestra > 2400) muestra *= 2

        val opcionesFinales = BitmapFactory.Options().apply {
            inSampleSize = muestra
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opcionesFinales)
        }
    }

    // ---------------------------------------------------------------- Texto a PDF

    /** Escribe texto plano en un PDF con varias paginas y ajuste de linea. */
    fun textoAPdf(texto: String, titulo: String, salida: File): File {
        val limpio = sanitizar(texto)
        val lineas = mutableListOf<String>()
        lineas += titulo
        lineas += ""
        limpio.split("\n", "\r\n").forEach { parrafo ->
            if (parrafo.isBlank()) {
                lineas += ""
            } else {
                lineas += ajustar(parrafo, ANCHO_TEXTO)
            }
        }

        val porPagina = 48
        val paginas = lineas.chunked(porPagina).ifEmpty { listOf(listOf("")) }
        val doc = PDDocument()
        try {
            paginas.forEach { bloque ->
                val pagina = PDPage(PDRectangle.A4)
                doc.addPage(pagina)
                PDPageContentStream(doc, pagina).use { contenido ->
                    contenido.beginText()
                    contenido.setFont(PDType1Font.HELVETICA, TAMANO_FUENTE)
                    contenido.setLeading(INTERLINEADO)
                    contenido.newLineAtOffset(MARGEN, pagina.mediaBox.height - MARGEN)
                    bloque.forEach { linea ->
                        contenido.showText(linea)
                        contenido.newLine()
                    }
                    contenido.endText()
                }
            }
            doc.save(salida)
        } finally {
            doc.close()
        }
        return salida
    }

    private fun ajustar(texto: String, ancho: Float): List<String> {
        val palabras = texto.split(" ")
        val resultado = mutableListOf<String>()
        var actual = StringBuilder()
        palabras.forEach { palabra ->
            if (actual.isEmpty()) {
                actual.append(palabra)
            } else if (actual.length + 1 + palabra.length <= ancho) {
                actual.append(' ').append(palabra)
            } else {
                resultado += actual.toString()
                actual = StringBuilder(palabra)
            }
        }
        if (actual.isNotEmpty()) resultado += actual.toString()
        return resultado
    }

    // ---------------------------------------------------------------- Edicion

    /** Ejecuta la operacion de edicion elegida y devuelve los archivos generados. */
    fun editar(
        context: Context,
        tipo: TipoEdicion,
        uris: List<Uri>,
        opciones: OpcionesEdicion,
        carpeta: File,
        base: String
    ): List<File> {
        carpeta.mkdirs()
        return when (tipo) {
            TipoEdicion.UNIR -> listOf(unir(context, uris, File(carpeta, "${base}_unido.pdf")))
            TipoEdicion.DIVIDIR -> dividir(context, uris.first(), carpeta, base)
            TipoEdicion.COMPRIMIR -> listOf(comprimir(context, uris.first(), File(carpeta, "${base}_comprimido.pdf")))
            TipoEdicion.ROTAR -> listOf(rotar(context, uris.first(), opciones.grados, File(carpeta, "${base}_rotado.pdf")))
            TipoEdicion.ELIMINAR -> listOf(
                eliminarPaginas(context, uris.first(), parsearPaginas(opciones.paginas), File(carpeta, "${base}_recortado.pdf"))
            )
            TipoEdicion.REORDENAR -> listOf(
                reordenar(context, uris.first(), parsearPaginas(opciones.orden), File(carpeta, "${base}_reordenado.pdf"))
            )
            TipoEdicion.MARCA_AGUA -> listOf(
                marcaAgua(context, uris.first(), opciones.textoMarcaAgua.ifBlank { "FileShifter" }, File(carpeta, "${base}_marca.pdf"))
            )
        }
    }

    fun unir(context: Context, uris: List<Uri>, salida: File): File {
        val unidor = PDFMergerUtility()
        val entradas = uris.mapNotNull { context.contentResolver.openInputStream(it) }
        try {
            entradas.forEach { unidor.addSource(it) }
            unidor.destinationFileName = salida.absolutePath
            unidor.mergeDocuments(MemoryUsageSetting.setupMixed(20L * 1024L * 1024L))
        } finally {
            entradas.forEach { entrada -> runCatching { entrada.close() } }
        }
        return salida
    }

    fun dividir(context: Context, uri: Uri, carpeta: File, base: String): List<File> {
        val generados = mutableListOf<File>()
        abrir(context, uri).use { doc ->
            for (indice in 0 until doc.numberOfPages) {
                val unaPagina = PDDocument()
                try {
                    unaPagina.importPage(doc.getPage(indice))
                    val salida = File(carpeta, "${base}_pagina_${indice + 1}.pdf")
                    unaPagina.save(salida)
                    generados += salida
                } finally {
                    unaPagina.close()
                }
            }
        }
        return generados
    }

    fun comprimir(context: Context, uri: Uri, salida: File): File {
        abrir(context, uri).use { doc ->
            doc.isAllSecurityToBeRemoved = true
            doc.save(salida)
        }
        return salida
    }

    fun rotar(context: Context, uri: Uri, grados: Int, salida: File): File {
        abrir(context, uri).use { doc ->
            doc.pages.forEachIndexed { indice, pagina ->
                val actual = doc.getPage(indice).rotation
                pagina.rotation = ((actual + grados) % 360 + 360) % 360
            }
            doc.save(salida)
        }
        return salida
    }

    fun eliminarPaginas(context: Context, uri: Uri, paginas: List<Int>, salida: File): File {
        abrir(context, uri).use { doc ->
            val total = doc.numberOfPages
            paginas.sortedDescending().forEach { numero ->
                if (numero in 1..total) doc.removePage(numero - 1)
            }
            doc.save(salida)
        }
        return salida
    }

    fun reordenar(context: Context, uri: Uri, orden: List<Int>, salida: File): File {
        abrir(context, uri).use { origen ->
            val total = origen.numberOfPages
            val indices = orden.filter { it in 1..total }.map { it - 1 }
                .ifEmpty { (0 until total).toList() }
            val nuevo = PDDocument()
            try {
                indices.forEach { indice -> nuevo.importPage(origen.getPage(indice)) }
                nuevo.save(salida)
            } finally {
                nuevo.close()
            }
        }
        return salida
    }

    fun marcaAgua(context: Context, uri: Uri, texto: String, salida: File): File {
        abrir(context, uri).use { doc ->
            doc.pages.forEach { pagina ->
                PDPageContentStream(
                    doc,
                    pagina,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                ).use { contenido ->
                    val ancho = pagina.mediaBox.width
                    val alto = pagina.mediaBox.height
                    contenido.beginText()
                    contenido.setFont(PDType1Font.HELVETICA_BOLD, 42f)
                    contenido.setNonStrokingColor(0.62f, 0.68f, 0.74f)
                    contenido.setTextMatrix(
                        Matrix.getRotateInstance(Math.toRadians(45.0), ancho / 5f, alto / 3f)
                    )
                    contenido.showText(sanitizar(texto))
                    contenido.endText()
                }
            }
            doc.save(salida)
        }
        return salida
    }

    // ---------------------------------------------------------------- Lectura y datos

    fun pdfATexto(context: Context, uri: Uri): String = abrir(context, uri).use { doc ->
        PDFTextStripper().getText(doc)
    }

    fun info(context: Context, uri: Uri): InfoPdf = abrir(context, uri).use { doc ->
        val primera = if (doc.numberOfPages > 0) doc.getPage(0) else null
        InfoPdf(
            paginas = doc.numberOfPages,
            ancho = primera?.mediaBox?.width ?: 0f,
            alto = primera?.mediaBox?.height ?: 0f
        )
    }

    fun abrir(context: Context, uri: Uri): PDDocument =
        context.contentResolver.openInputStream(uri)?.use { PDDocument.load(it) }
            ?: error("No se pudo abrir el PDF")

    // ---------------------------------------------------------------- Portada de entrega

    /**
     * Genera la portada + indice del Modo Entrega con la plantilla elegida.
     */
    fun portadaEntrega(
        nombrePlantilla: String,
        nombre: String,
        matricula: String,
        materia: String,
        titulo: String,
        indice: String,
        salida: File
    ) {
        val doc = PDDocument()
        try {
            val pagina = PDPage(PDRectangle.A4)
            doc.addPage(pagina)
            val ancho = pagina.mediaBox.width
            val alto = pagina.mediaBox.height

            PDPageContentStream(doc, pagina).use { contenido ->
                // Barra superior segun plantilla
                if (nombrePlantilla != "MINIMAL") {
                    contenido.setNonStrokingColor(0.05f, 0.65f, 0.91f)
                    contenido.addRect(0f, alto - 18f, ancho, 18f)
                    contenido.fill()
                }

                var y = alto - 120f
                contenido.beginText()
                contenido.setNonStrokingColor(0.06f, 0.09f, 0.16f)
                contenido.setFont(PDType1Font.HELVETICA_BOLD, 26f)
                contenido.newLineAtOffset(MARGEN, y)
                contenido.showText(sanitizar(materia.ifBlank { "Trabajo" }))
                contenido.endText()

                y -= 70f
                contenido.beginText()
                contenido.setFont(PDType1Font.HELVETICA_BOLD, 18f)
                contenido.newLineAtOffset(MARGEN, y)
                ajustar(sanitizar(titulo.ifBlank { "Sin titulo" }), 46f).forEach { linea ->
                    contenido.showText(linea)
                    contenido.newLine()
                }
                contenido.endText()

                y -= 90f
                contenido.beginText()
                contenido.setFont(PDType1Font.HELVETICA, 13f)
                contenido.newLineAtOffset(MARGEN, y)
                listOf(
                    "Alumno: ${nombre.ifBlank { "-" }}",
                    "Matricula: ${matricula.ifBlank { "-" }}",
                    "Materia: ${materia.ifBlank { "-" }}"
                ).forEach { linea ->
                    contenido.showText(sanitizar(linea))
                    contenido.newLine()
                }
                contenido.endText()

                if (indice.isNotBlank()) {
                    y -= 70f
                    contenido.beginText()
                    contenido.setFont(PDType1Font.HELVETICA_BOLD, 14f)
                    contenido.newLineAtOffset(MARGEN, y)
                    contenido.showText("Indice")
                    contenido.newLine()
                    contenido.setFont(PDType1Font.HELVETICA, 11f)
                    contenido.newLine()
                    sanitizar(indice).split("\n").take(25).forEach { linea ->
                        contenido.showText(linea.take(80))
                        contenido.newLine()
                    }
                    contenido.endText()
                }

                // Pie
                contenido.beginText()
                contenido.setFont(PDType1Font.HELVETICA, 9f)
                contenido.setNonStrokingColor(0.45f, 0.52f, 0.6f)
                contenido.newLineAtOffset(MARGEN, 40f)
                contenido.showText("Generado con FileShifter - sin conexion")
                contenido.endText()
            }
            doc.save(salida)
        } finally {
            doc.close()
        }
    }

    // ---------------------------------------------------------------- Auxiliares

    /** "2,4-6" -> {2,4,5,6} */
    fun parsearPaginas(texto: String): List<Int> {
        val resultado = linkedSetOf<Int>()
        texto.split(',', ' ', ';').filter { it.isNotBlank() }.forEach { parte ->
            if (parte.contains('-')) {
                val (inicio, fin) = parte.split('-').mapNotNull { it.trim().toIntOrNull() }
                    .let { if (it.size == 2) it[0] to it[1] else null } ?: return@forEach
                if (inicio <= fin) (inicio..fin).forEach { if (it > 0) resultado += it }
            } else {
                parte.trim().toIntOrNull()?.let { if (it > 0) resultado += it }
            }
        }
        return resultado.toList()
    }

    /** Quita caracteres que la fuente Helvetica no puede dibujar. */
    fun sanitizar(texto: String): String = texto.map { caracter ->
        when {
            caracter == '\n' || caracter == '\r' -> caracter
            caracter.code in 32..255 -> caracter
            caracter == '\t' -> ' '
            else -> ' '
        }
    }.joinToString("")

    /** Convierte puntos a pixeles aproximados (72 dpi base). */
    fun puntosAPixeles(puntos: Float): Int = (puntos * 1.5f).roundToInt()
}
