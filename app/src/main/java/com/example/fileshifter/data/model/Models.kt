package com.example.fileshifter.data.model

/**
 * Modelos de dominio de FileShifter.
 * Son clases puras (no dependen de Android) para poder probarlas con JUnit.
 */

/** Tipos de archivo que la app sabe reconocer. */
enum class TipoDocumento { PDF, DOCX, XLSX, PPTX, TXT, CSV, HTML, IMAGEN, DESCONOCIDO }

/** Formatos de salida del modulo Convertir. */
enum class FormatoSalida(val etiqueta: String, val extension: String) {
    PDF("PDF", "pdf"),
    TXT("TXT", "txt")
}

/** Un archivo elegido por el usuario (la Uri se guarda como texto). */
data class ArchivoSeleccionado(
    val uri: String,
    val nombre: String,
    val tamanoBytes: Long,
    val tipo: TipoDocumento
)

/** Tipos de edicion de PDF disponibles (F3). */
enum class TipoEdicion(val etiqueta: String, val descripcion: String) {
    UNIR("Unir", "Combina varios PDF en uno solo"),
    DIVIDIR("Dividir", "Genera un PDF por cada pagina"),
    COMPRIMIR("Comprimir", "Reduce el peso del archivo"),
    ROTAR("Rotar", "Gira las paginas 90, 180 o 270 grados"),
    ELIMINAR("Eliminar", "Quita las paginas que indiques"),
    REORDENAR("Reordenar", "Cambia el orden de las paginas"),
    MARCA_AGUA("Marca de agua", "Escribe un texto sobre cada pagina")
}

/** Opciones que el usuario captura en el paso 3 de Editar PDF. */
data class OpcionesEdicion(
    val grados: Int = 90,
    val paginas: String = "",
    val orden: String = "",
    val textoMarcaAgua: String = ""
)

/** Resultado de cualquier operacion de conversion/edicion. */
data class ResultadoConversion(
    val exito: Boolean,
    val mensaje: String,
    val archivos: List<String> = emptyList()
)

/** Plantillas del Modo Entrega (F5). */
enum class PlantillaEntrega(val etiqueta: String, val descripcion: String) {
    ESCOLAR("Escolar", "Portada simple con datos del alumno"),
    OFICIO("Oficio", "Formato formal con membrete y margenes"),
    MINIMAL("Minimal", "Solo titulo y datos, sin adornos")
}

/** Datos del formulario del Modo Entrega. */
data class DatosEntrega(
    val nombre: String = "",
    val matricula: String = "",
    val materia: String = "",
    val titulo: String = "",
    val indice: String = ""
)

/** Un chequeo del semaforo del Validador (F6). */
data class Chequeo(
    val nombre: String,
    val correcto: Boolean,
    val detalle: String
)

enum class Semaforo { VERDE, AMARILLO, ROJO }

/** Reporte del Validador: tamanio, paginas, hash y chequeos. */
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

/** Preferencias del flujo configurable (Ajustes). */
data class AjustesFlujo(
    val sugerirCorrector: Boolean = true,
    val sugerirEntrega: Boolean = true,
    val sugerirValidador: Boolean = true,
    val noPreguntarMas: Boolean = false
)

/** Medidas de una pagina PDF. */
data class InfoPdf(val paginas: Int, val ancho: Float, val alto: Float)

/** Formatea bytes a un texto legible (KB / MB). */
fun formatearTamano(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format("%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}

/** Deduce el tipo de documento por la extension del nombre. */
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

/** Formatos de salida validos para un tipo de entrada. */
fun salidasCompatibles(tipo: TipoDocumento): List<FormatoSalida> = when (tipo) {
    TipoDocumento.PDF -> listOf(FormatoSalida.TXT)
    TipoDocumento.IMAGEN -> listOf(FormatoSalida.PDF)
    TipoDocumento.DOCX, TipoDocumento.XLSX, TipoDocumento.PPTX,
    TipoDocumento.TXT, TipoDocumento.CSV, TipoDocumento.HTML -> listOf(FormatoSalida.PDF, FormatoSalida.TXT)
    TipoDocumento.DESCONOCIDO -> emptyList()
}
