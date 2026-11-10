package com.example.fileshifter.data.model

enum class TipoDocumento { PDF, DOCX, XLSX, PPTX, TXT, CSV, HTML, IMAGEN, DESCONOCIDO }

enum class FormatoSalida(val etiqueta: String, val extension: String) {
    PDF("PDF", "pdf"),
    TXT("TXT", "txt"),
    DOCX("DOCX", "docx")
}

data class ArchivoSeleccionado(
    val uri: String,
    val nombre: String,
    val tamanoBytes: Long,
    val tipo: TipoDocumento
)

enum class TipoEdicion(val etiqueta: String, val descripcion: String) {
    UNIR("Unir", "Combina varios PDF en uno solo"),
    DIVIDIR("Dividir", "Genera un PDF por cada pagina"),
    COMPRIMIR("Comprimir", "Reduce el peso del archivo"),
    ROTAR("Rotar", "Gira las paginas 90, 180 o 270 grados"),
    ELIMINAR("Eliminar", "Quita las paginas que indiques"),
    REORDENAR("Reordenar", "Cambia el orden de las paginas"),
    MARCA_AGUA("Marca de agua", "Escribe un texto sobre cada pagina")
}

data class OpcionesEdicion(
    val grados: Int = 90,
    val paginas: String = "",
    val orden: String = "",
    val textoMarcaAgua: String = ""
)

data class ResultadoConversion(
    val exito: Boolean,
    val mensaje: String,
    val archivos: List<String> = emptyList()
)

enum class PlantillaEntrega(val etiqueta: String, val descripcion: String) {
    ESCOLAR("Escolar", "Portada simple con datos del alumno"),
    OFICIO("Oficio", "Formato formal con membrete y margenes"),
    MINIMAL("Minimal", "Solo titulo y datos, sin adornos")
}

data class DatosEntrega(
    val nombre: String = "",
    val matricula: String = "",
    val materia: String = "",
    val titulo: String = "",
    val indice: String = ""
)

data class Chequeo(
    val nombre: String,
    val correcto: Boolean,
    val detalle: String
)

enum class Semaforo { VERDE, AMARILLO, ROJO }

data class ReporteValidacion(
    val nombre: String,
    val tamanoBytes: Long,
    val paginas: Int,
    val ancho: Float,
    val alto: Float,
    val hash: String,
    val chequeos: List<Chequeo>,
    val semaforo: Semaforo
) {
    val tamanoLegible: String get() = formatearTamano(tamanoBytes)
}

data class AjustesFlujo(
    val sugerirCorrector: Boolean = true,
    val sugerirEntrega: Boolean = true,
    val sugerirValidador: Boolean = true,
    val noPreguntarMas: Boolean = false
)

data class InfoPdf(val paginas: Int, val ancho: Float, val alto: Float)

fun formatearTamano(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format("%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}

fun tipoPorNombre(nombre: String): TipoDocumento = when (nombre.substringAfterLast('.', "").lowercase()) {
    "pdf" -> TipoDocumento.PDF
    "doc", "docx" -> TipoDocumento.DOCX
    "xls", "xlsx" -> TipoDocumento.XLSX
    "ppt", "pptx" -> TipoDocumento.PPTX
    "txt" -> TipoDocumento.TXT
    "csv" -> TipoDocumento.CSV
    "html", "htm" -> TipoDocumento.HTML
    "jpg", "jpeg", "png", "webp", "heic", "heif", "bmp" -> TipoDocumento.IMAGEN
    else -> TipoDocumento.DESCONOCIDO
}

fun salidasCompatibles(tipo: TipoDocumento): List<FormatoSalida> = when (tipo) {
    TipoDocumento.PDF -> listOf(FormatoSalida.TXT, FormatoSalida.DOCX)
    TipoDocumento.IMAGEN -> listOf(FormatoSalida.PDF)
    TipoDocumento.DOCX, TipoDocumento.XLSX, TipoDocumento.PPTX,
    TipoDocumento.TXT, TipoDocumento.CSV, TipoDocumento.HTML ->
        listOf(FormatoSalida.PDF, FormatoSalida.TXT, FormatoSalida.DOCX)
    TipoDocumento.DESCONOCIDO -> emptyList()
}
