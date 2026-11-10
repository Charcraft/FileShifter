package com.example.fileshifter.data.source

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object OfficeWriter {
    fun docxDesdeTexto(texto: String, titulo: String, salida: File): File {
        salida.parentFile?.mkdirs()
        val parrafos = construirParrafos(titulo, texto)

        ZipOutputStream(salida.outputStream().buffered()).use { zip ->
            agregar(zip, "[Content_Types].xml", CONTENT_TYPES)
            agregar(zip, "_rels/.rels", RELS)
            agregar(zip, "word/_rels/document.xml.rels", DOC_RELS)
            agregar(zip, "word/styles.xml", ESTILOS)
            agregar(zip, "word/document.xml", documento(parrafos))
        }
        return salida
    }

    private fun agregar(zip: ZipOutputStream, ruta: String, contenido: String) {
        zip.putNextEntry(ZipEntry(ruta))
        zip.write(contenido.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun construirParrafos(titulo: String, texto: String): List<Pair<String, Boolean>> {
        val lista = mutableListOf<Pair<String, Boolean>>()
        val nombre = titulo.substringBeforeLast('.').trim()
        if (nombre.isNotEmpty()) lista += nombre to true
        texto.split("\n").forEach { linea ->
            val limpia = linea.replace("\r", "").trimEnd()
            lista += limpia to false
        }
        return lista
    }

    private fun documento(parrafos: List<Pair<String, Boolean>>): String {
        val cuerpo = parrafos.joinToString("") { (texto, esTitulo) ->
            val estilo = if (esTitulo) "Titulo" else "Normal"
            """<w:p><w:pPr><w:pStyle w:val="$estilo"/></w:pPr>""" +
                """<w:r><w:t xml:space="preserve">${escapar(texto)}</w:t></w:r></w:p>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>$cuerpo<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/></w:sectPr></w:body>
</w:document>"""
    }

    private fun escapar(texto: String): String = texto
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""

    private const val RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private const val DOC_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private const val ESTILOS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:style w:type="paragraph" w:default="1" w:styleId="Normal">
<w:name w:val="Normal"/><w:rPr><w:sz w:val="22"/></w:rPr>
</w:style>
<w:style w:type="paragraph" w:styleId="Titulo">
<w:name w:val="Título"/><w:basedOn w:val="Normal"/>
<w:rPr><w:b/><w:sz w:val="32"/></w:rPr>
</w:style>
</w:styles>"""
}
