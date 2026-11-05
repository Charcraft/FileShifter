package com.example.fileshifter.data.repository

import android.content.Context
import android.net.Uri
import com.example.fileshifter.data.local.HistorialDao
import com.example.fileshifter.data.local.HistorialEntity
import com.example.fileshifter.data.model.ArchivoSeleccionado
import com.example.fileshifter.data.model.Chequeo
import com.example.fileshifter.data.model.DatosEntrega
import com.example.fileshifter.data.model.FormatoSalida
import com.example.fileshifter.data.model.InfoPdf
import com.example.fileshifter.data.model.OpcionesEdicion
import com.example.fileshifter.data.model.PlantillaEntrega
import com.example.fileshifter.data.model.ReporteValidacion
import com.example.fileshifter.data.model.ResultadoConversion
import com.example.fileshifter.data.model.Semaforo
import com.example.fileshifter.data.model.TipoDocumento
import com.example.fileshifter.data.model.TipoEdicion
import com.example.fileshifter.data.source.Archivos
import com.example.fileshifter.data.source.GeneradorCodigos
import com.example.fileshifter.data.source.OfficeTextExtractor
import com.example.fileshifter.data.source.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** Historial persistido con Room. */
class HistorialRepository(private val dao: HistorialDao) {

    val historial: Flow<List<HistorialEntity>> = dao.observarTodo()

    suspend fun registrar(
        nombre: String,
        ruta: String,
        operacion: String,
        tamanoBytes: Long,
        tipoMime: String
    ) = withContext(Dispatchers.IO) {
        dao.insertar(
            HistorialEntity(
                nombre = nombre,
                ruta = ruta,
                operacion = operacion,
                tamanoBytes = tamanoBytes,
                tipoMime = tipoMime,
                creadoEn = System.currentTimeMillis()
            )
        )
    }

    suspend fun eliminar(id: Long) = withContext(Dispatchers.IO) { dao.eliminar(id) }

    suspend fun limpiar() = withContext(Dispatchers.IO) { dao.limpiar() }
}

/**
 * Contrato unico de acceso a datos de documentos.
 * La UI y los ViewModel SOLO conocen esta interfaz (DIP de SOLID).
 */
interface DocumentoRepository {
    suspend fun describir(uris: List<String>): List<ArchivoSeleccionado>
    suspend fun imagenesAPdf(uris: List<String>, unir: Boolean): ResultadoConversion
    suspend fun convertir(archivos: List<ArchivoSeleccionado>, objetivo: FormatoSalida): ResultadoConversion
    suspend fun editarPdf(tipo: TipoEdicion, uris: List<String>, opciones: OpcionesEdicion): ResultadoConversion
    suspend fun crearEntregaPdf(plantilla: PlantillaEntrega, datos: DatosEntrega): ResultadoConversion
    suspend fun validar(archivo: ArchivoSeleccionado): ReporteValidacion
    suspend fun textoDe(archivo: ArchivoSeleccionado): String
    suspend fun textoAPdf(texto: String, titulo: String): ResultadoConversion
    suspend fun generarCodigo(texto: String, esQr: Boolean): File
}

/**
 * Implementacion real: PDFBox para PDF, extractores ZIP/XML para Office,
 * Room para el historial. Todo el trabajo pesado corre en Dispatchers.IO.
 */
class DocumentoRepositoryImpl(
    private val context: Context,
    private val historial: HistorialRepository
) : DocumentoRepository {

    override suspend fun describir(uris: List<String>): List<ArchivoSeleccionado> =
        withContext(Dispatchers.IO) {
            uris.map { Archivos.describir(context, Uri.parse(it)) }
        }

    override suspend fun imagenesAPdf(uris: List<String>, unir: Boolean): ResultadoConversion =
        withContext(Dispatchers.IO) {
            runCatching {
                val carpeta = Archivos.carpetaSalida(context)
                val base = "Imagenes_${marcaTiempo()}"
                val generados = PdfEngine.imagenesAPdf(
                    context = context,
                    uris = uris.map(Uri::parse),
                    unir = unir,
                    carpeta = carpeta,
                    base = base
                )
                registrarTodo(generados, "Imagen a PDF")
                ResultadoConversion(
                    exito = generados.isNotEmpty(),
                    mensaje = if (generados.isEmpty()) {
                        "No se pudo leer ninguna imagen."
                    } else {
                        "${generados.size} archivo(s) creado(s) en FileShifter/."
                    },
                    archivos = generados.map { it.absolutePath }
                )
            }.getOrElse { error ->
                ResultadoConversion(false, "Error al convertir: ${error.message}")
            }
        }

    override suspend fun convertir(
        archivos: List<ArchivoSeleccionado>,
        objetivo: FormatoSalida
    ): ResultadoConversion = withContext(Dispatchers.IO) {
        runCatching {
            val carpeta = Archivos.carpetaSalida(context)
            val generados = mutableListOf<File>()
            archivos.forEach { archivo ->
                val uri = Uri.parse(archivo.uri)
                val base = Archivos.limpiarNombre(archivo.nombre)
                when (objetivo) {
                    FormatoSalida.PDF -> when (archivo.tipo) {
                        TipoDocumento.IMAGEN -> {
                            generados += PdfEngine.imagenesAPdf(
                                context, listOf(uri), false, carpeta, base
                            )
                        }
                        TipoDocumento.PDF -> Unit
                        TipoDocumento.DESCONOCIDO -> Unit
                        else -> {
                            val texto = when (archivo.tipo) {
                                TipoDocumento.DOCX, TipoDocumento.XLSX, TipoDocumento.PPTX ->
                                    OfficeTextExtractor.extraer(context, uri, archivo.nombre)
                                TipoDocumento.HTML ->
                                    OfficeTextExtractor.htmlATexto(Archivos.leerTexto(context, uri))
                                else -> Archivos.leerTexto(context, uri)
                            }
                            if (texto.isBlank()) {
                                error("No se encontro texto en ${archivo.nombre}")
                            }
                            generados += PdfEngine.textoAPdf(
                                texto = texto,
                                titulo = archivo.nombre,
                                salida = File(carpeta, "$base.pdf")
                            )
                        }
                    }

                    FormatoSalida.TXT -> {
                        val texto = when (archivo.tipo) {
                            TipoDocumento.PDF -> PdfEngine.pdfATexto(context, uri)
                            TipoDocumento.DOCX, TipoDocumento.XLSX, TipoDocumento.PPTX ->
                                OfficeTextExtractor.extraer(context, uri, archivo.nombre)
                            TipoDocumento.HTML ->
                                OfficeTextExtractor.htmlATexto(Archivos.leerTexto(context, uri))
                            TipoDocumento.IMAGEN ->
                                error("Una imagen necesita OCR. Usa Escaner.")
                            TipoDocumento.DESCONOCIDO ->
                                error("Formato no compatible")
                            else -> Archivos.leerTexto(context, uri)
                        }
                        if (texto.isBlank()) error("No se encontro texto en ${archivo.nombre}")
                        val salida = File(carpeta, "$base.txt")
                        salida.writeText(texto)
                        generados += salida
                    }
                }
            }
            registrarTodo(generados, "Convertir")
            if (generados.isEmpty()) {
                ResultadoConversion(false, "No se genero ningun archivo. Revisa el formato de origen.")
            } else {
                ResultadoConversion(
                    true,
                    "${generados.size} archivo(s) convertido(s) a ${objetivo.etiqueta}.",
                    generados.map { it.absolutePath }
                )
            }
        }.getOrElse { error ->
            ResultadoConversion(false, "Error al convertir: ${error.message}")
        }
    }

    override suspend fun editarPdf(
        tipo: TipoEdicion,
        uris: List<String>,
        opciones: OpcionesEdicion
    ): ResultadoConversion = withContext(Dispatchers.IO) {
        runCatching {
            require(uris.isNotEmpty()) { "Selecciona al menos un PDF" }
            if (tipo == TipoEdicion.UNIR) {
                require(uris.size >= 2) { "Para unir se necesitan al menos dos PDF" }
            } else {
                require(uris.size == 1) { "Esta edicion trabaja con un solo PDF" }
            }
            val carpeta = Archivos.carpetaSalida(context)
            val base = "Edicion_${marcaTiempo()}"
            val generados = PdfEngine.editar(
                context = context,
                tipo = tipo,
                uris = uris.map(Uri::parse),
                opciones = opciones,
                carpeta = carpeta,
                base = base
            )
            registrarTodo(generados, tipo.etiqueta)
            ResultadoConversion(
                true,
                "${generados.size} archivo(s) generado(s): ${tipo.etiqueta}.",
                generados.map { it.absolutePath }
            )
        }.getOrElse { error ->
            ResultadoConversion(false, "No se pudo editar: ${error.message}")
        }
    }

    override suspend fun crearEntregaPdf(
        plantilla: PlantillaEntrega,
        datos: DatosEntrega
    ): ResultadoConversion = withContext(Dispatchers.IO) {
        runCatching {
            val carpeta = Archivos.carpetaSalida(context)
            val salida = File(carpeta, "Entrega_${marcaTiempo()}.pdf")
            PdfEngine.portadaEntrega(
                nombrePlantilla = plantilla.name,
                nombre = datos.nombre,
                matricula = datos.matricula,
                materia = datos.materia,
                titulo = datos.titulo,
                indice = datos.indice,
                salida = salida
            )
            registrarTodo(listOf(salida), "Modo Entrega")
            ResultadoConversion(true, "Portada generada: ${salida.name}", listOf(salida.absolutePath))
        }.getOrElse { error ->
            ResultadoConversion(false, "No se pudo generar la portada: ${error.message}")
        }
    }

    override suspend fun validar(archivo: ArchivoSeleccionado): ReporteValidacion =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(archivo.uri)
            val hash = calcularHash(uri)
            val info: InfoPdf = runCatching { PdfEngine.info(context, uri) }
                .getOrElse { InfoPdf(0, 0f, 0f) }

            val chequeos = listOf(
                Chequeo("PDF legible", info.paginas > 0, "${info.paginas} pagina(s)"),
                Chequeo(
                    "Tamano",
                    archivo.tamanoBytes <= 25L * 1024L * 1024L,
                    "${com.example.fileshifter.data.model.formatearTamano(archivo.tamanoBytes)} (limite 25 MB)"
                ),
                Chequeo("Numero de paginas", info.paginas in 1..500, "${info.paginas} de 500 maximo"),
                Chequeo(
                    "Dimension de pagina",
                    info.ancho >= 200f && info.alto >= 200f,
                    "${info.ancho.toInt()} x ${info.alto.toInt()} pt"
                )
            )
            val semaforo = when {
                info.paginas <= 0 -> Semaforo.ROJO
                chequeos.all { it.correcto } -> Semaforo.VERDE
                else -> Semaforo.AMARILLO
            }
            ReporteValidacion(
                nombre = archivo.nombre,
                tamanoBytes = archivo.tamanoBytes,
                paginas = info.paginas,
                ancho = info.ancho,
                alto = info.alto,
                hash = hash,
                chequeos = chequeos,
                semaforo = semaforo
            )
        }

    override suspend fun textoDe(archivo: ArchivoSeleccionado): String =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(archivo.uri)
            when (archivo.tipo) {
                TipoDocumento.PDF -> PdfEngine.pdfATexto(context, uri)
                TipoDocumento.DOCX, TipoDocumento.XLSX, TipoDocumento.PPTX ->
                    OfficeTextExtractor.extraer(context, uri, archivo.nombre)
                TipoDocumento.HTML ->
                    OfficeTextExtractor.htmlATexto(Archivos.leerTexto(context, uri))
                else -> Archivos.leerTexto(context, uri)
            }
        }

    override suspend fun textoAPdf(texto: String, titulo: String): ResultadoConversion =
        withContext(Dispatchers.IO) {
            runCatching {
                if (texto.isBlank()) error("No hay texto para convertir")
                val carpeta = Archivos.carpetaSalida(context)
                val salida = File(carpeta, "${Archivos.limpiarNombre(titulo)}_${marcaTiempo()}.pdf")
                PdfEngine.textoAPdf(texto, titulo, salida)
                registrarTodo(listOf(salida), "Texto a PDF")
                ResultadoConversion(true, "PDF generado: ${salida.name}", listOf(salida.absolutePath))
            }.getOrElse { error ->
                ResultadoConversion(false, "No se pudo generar el PDF: ${error.message}")
            }
        }

    override suspend fun generarCodigo(texto: String, esQr: Boolean): File =
        withContext(Dispatchers.IO) {
            val mapa = if (esQr) GeneradorCodigos.qr(texto) else GeneradorCodigos.barras(texto)
            val salida = Archivos.nombreUnico(context, "Codigo_${marcaTiempo()}", "png")
            salida.outputStream().use { mapa.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            mapa.recycle()
            historial.registrar(
                nombre = salida.name,
                ruta = salida.absolutePath,
                operacion = if (esQr) "Generar QR" else "Generar barras",
                tamanoBytes = salida.length(),
                tipoMime = "image/png"
            )
            salida
        }

    // ------------------------------------------------------------------ Auxiliares

    private suspend fun registrarTodo(archivos: List<File>, operacion: String) {
        archivos.forEach { archivo ->
            historial.registrar(
                nombre = archivo.name,
                ruta = archivo.absolutePath,
                operacion = operacion,
                tamanoBytes = archivo.length(),
                tipoMime = when (archivo.extension.lowercase()) {
                    "pdf" -> "application/pdf"
                    "txt" -> "text/plain"
                    else -> "application/octet-stream"
                }
            )
        }
    }

    private fun calcularHash(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            val buffer = ByteArray(8192)
            var leidos = entrada.read(buffer)
            while (leidos > 0) {
                digest.update(buffer, 0, leidos)
                leidos = entrada.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun marcaTiempo(): String =
        java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            .format(java.util.Date())
}
