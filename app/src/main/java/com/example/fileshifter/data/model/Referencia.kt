package com.example.fileshifter.data.model

data class Referencia(
    val doi: String,
    val titulo: String,
    val autores: String,
    val anio: String,
    val fuente: String,
    val tipo: String
) {
    fun citaApa(): String {
        val partes = mutableListOf<String>()
        if (autores.isNotBlank()) partes.add("$autores.")
        if (anio.isNotBlank()) partes.add("($anio).")
        partes.add("$titulo.")
        if (fuente.isNotBlank()) partes.add("$fuente.")
        if (doi.isNotBlank()) partes.add("https://doi.org/$doi")
        return partes.joinToString(" ").replace("..", ".")
    }

    fun tipoLegible(): String = when (tipo) {
        "journal-article" -> "Artículo"
        "book" -> "Libro"
        "book-chapter" -> "Capítulo"
        "proceedings-article" -> "Congreso"
        "dissertation" -> "Tesis"
        "report" -> "Informe"
        "posted-content" -> "Preprint"
        "reference-entry" -> "Referencia"
        else -> "Documento"
    }
}
