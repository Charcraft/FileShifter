package com.example.fileshifter.data.source

import android.content.Context
import android.net.Uri
import java.util.zip.ZipInputStream

/**
 * Extrae texto de DOCX, XLSX y PPTX sin dependencias pesadas:
 * un archivo de Office es un ZIP con XML dentro. Solo se lee el texto,
 * que es justo el alcance prometido ("Office basico, sin macros").
 */
object OfficeTextExtractor {

    fun extraer(context: Context, uri: Uri, nombre: String): String {
        val extension = nombre.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "docx" -> docx(context, uri)
            "xlsx" -> xlsx(context, uri)
            "pptx" -> pptx(context, uri)
            else -> ""
        }
    }

    // ------------------------------------------------------------------ DOCX

    private fun docx(context: Context, uri: Uri): String {
        val xml = leerEntrada(context, uri, "word/document.xml") ?: return ""
        val parrafos = xml.split("</w:p>")
        val texto = parrafos.mapNotNull { parrafo ->
            val piezas = Regex("<w:t[^>]*>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
                .findAll(parrafo)
                .map { decodificar(it.groupValues[1]) }
                .toList()
            val saltos = Regex("<w:br[^>]*/>").findAll(parrafo).count()
            val tabs = Regex("<w:tab[^>]*/>").findAll(parrafo).count()
            if (piezas.isEmpty() && saltos == 0 && tabs == 0) {
                null
            } else {
                piezas.joinToString("") + "\t".repeat(tabs) + "\n".repeat(saltos)
            }
        }
        return texto.joinToString("\n").trim()
    }

    // ------------------------------------------------------------------ XLSX

    private fun xlsx(context: Context, uri: Uri): String {
        val compartidos = leerEntrada(context, uri, "xl/sharedStrings.xml")
            ?.let { xml ->
                Regex("<si>(.*?)</si>", RegexOption.DOT_MATCHES_ALL).findAll(xml).map { bloque ->
                    Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                        .findAll(bloque.groupValues[1])
                        .joinToString("") { decodificar(it.groupValues[1]) }
                }.toList()
            } ?: emptyList()

        val hojas = leerEntradas(context, uri, "xl/worksheets/sheet")
            .toSortedMap()

        val salida = StringBuilder()
        hojas.values.forEach { xml ->
            Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach { fila ->
                val celdas = Regex("<c([^>]*)>(.*?)</c>", RegexOption.DOT_MATCHES_ALL)
                    .findAll(fila.groupValues[1])
                    .map { celda ->
                        val etiqueta = celda.groupValues[1]
                        val contenido = celda.groupValues[2]
                        val valor = Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL)
                            .find(contenido)?.groupValues?.get(1)
                        val enLinea = Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                            .find(contenido)?.groupValues?.get(1)
                        when {
                            enLinea != null -> decodificar(enLinea)
                            valor == null -> ""
                            etiqueta.contains("t=\"s\"") -> compartidos.getOrNull(valor.toIntOrNull() ?: -1) ?: ""
                            else -> decodificar(valor)
                        }
                    }
                    .toList()
                if (celdas.any { it.isNotBlank() }) {
                    salida.append(celdas.joinToString("\t")).append('\n')
                }
            }
            salida.append('\n')
        }
        return salida.toString().trim()
    }

    // ------------------------------------------------------------------ PPTX

    private fun pptx(context: Context, uri: Uri): String {
        val diapositivas = leerEntradas(context, uri, "ppt/slides/slide")
            .toSortedMap(compareBy { it.filter(Char::isDigit).toIntOrNull() ?: 0 })

        val salida = StringBuilder()
        diapositivas.entries.forEachIndexed { indice, entrada ->
            val xml = entrada.value
            salida.append("--- Diapositiva ${indice + 1} ---\n")
            xml.split("</a:p>").forEach { parrafo ->
                val texto = Regex("<a:t[^>]*>(.*?)</a:t>", RegexOption.DOT_MATCHES_ALL)
                    .findAll(parrafo)
                    .joinToString("") { decodificar(it.groupValues[1]) }
                if (texto.isNotBlank()) salida.append(texto).append('\n')
            }
            salida.append('\n')
        }
        return salida.toString().trim()
    }

    // ------------------------------------------------------------------ HTML / texto

    /** Limpia etiquetas HTML y deja texto legible. */
    fun htmlATexto(html: String): String = html
        .replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), "")
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</(p|div|h[1-6]|li|tr)>"), "\n")
        .replace(Regex("(?s)<[^>]+>"), "")
        .let { decodificar(it) }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

    // ------------------------------------------------------------------ Auxiliares

    private fun leerEntrada(context: Context, uri: Uri, nombreEntrada: String): String? {
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            ZipInputStream(entrada.buffered()).use { zip ->
                var actual = zip.nextEntry
                while (actual != null) {
                    if (actual.name == nombreEntrada) {
                        return zip.readBytes().toString(Charsets.UTF_8)
                    }
                    actual = zip.nextEntry
                }
            }
        }
        return null
    }

    private fun leerEntradas(context: Context, uri: Uri, prefijo: String): Map<String, String> {
        val resultado = mutableMapOf<String, String>()
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            ZipInputStream(entrada.buffered()).use { zip ->
                var actual = zip.nextEntry
                while (actual != null) {
                    if (actual.name.startsWith(prefijo) && actual.name.endsWith(".xml")) {
                        resultado[actual.name] = zip.readBytes().toString(Charsets.UTF_8)
                    }
                    actual = zip.nextEntry
                }
            }
        }
        return resultado
    }

    private fun decodificar(texto: String): String = texto
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
        .replace(Regex("&#(\\d+);")) { it.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: "" }
}
