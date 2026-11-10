package com.example.fileshifter.data.model

/**
 * Referencia bibliografica recuperada del catalogo Crossref.
 * Se guarda ya formateada para poder consultarla sin conexion.
 */
data class Referencia(
    val doi: String,
    val titulo: String,
    val autores: String,
    val anio: String,
    val fuente: String,
    val tipo: String
) {

    /** Cita lista para pegar en la bibliografia (APA 7). */
    fun citaApa(): String {
        val partes = mutableListOf<String>()
        if (autores.isNotBlank()) partes.add("$autores.")
        if (anio.isNotBlank()) partes.add("($anio).")
        partes.add("$titulo.")
        if (fuente.isNotBlank()) partes.add("$fuente.")
        if (doi.isNotBlank()) partes.add("https://doi.org/$doi")
        return partes.joinToString(" ").replace("..", ".")
    }

    /** Nombre del tipo de publicacion tal como lo entiende el alumno. */
    fun tipoLegible(): String = when (tipo) {
        "journal-article" -> "Articulo"
        "book" -> "Libro"
        "book-chapter" -> "Capitulo"
        "proceedings-article" -> "Congreso"
        "dissertation" -> "Tesis"
        "report" -> "Informe"
        "posted-content" -> "Preprint"
        "reference-entry" -> "Referencia"
        else -> "Documento"
    }
}
