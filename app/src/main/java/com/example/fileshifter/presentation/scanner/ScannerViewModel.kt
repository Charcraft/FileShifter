package com.example.fileshifter.presentation.scanner

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.model.ResultadoConversion
import com.example.fileshifter.data.repository.DocumentoRepository
import com.example.fileshifter.data.source.Cuadrilatero
import com.example.fileshifter.data.source.FiltroEscaneo
import com.example.fileshifter.data.source.OcrService
import com.example.fileshifter.data.source.Punto
import com.example.fileshifter.data.source.ScanEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PaginaEscaneada(
    val id: Long,
    val rutaOriginal: String,
    val rutaProcesada: String,
    val miniatura: Bitmap,
    val filtro: FiltroEscaneo,
    val cuadrilatero: Cuadrilatero,
    val giros: Int,
    val ancho: Int,
    val alto: Int
)

data class EdicionEscaneo(
    val paginaId: Long,
    val rutaOriginal: String,
    val vista: Bitmap,
    val cuadrilatero: Cuadrilatero,
    val filtro: FiltroEscaneo,
    val giros: Int = 0,
    val previa: Bitmap? = null,
    val calculandoPrevia: Boolean = false
)

data class ScannerUiState(
    val paginas: List<PaginaEscaneada> = emptyList(),
    val edicion: EdicionEscaneo? = null,
    val revisando: Boolean = false,
    val procesando: Boolean = false,
    val generando: Boolean = false,
    val extrayendoTexto: Boolean = false,
    val texto: String = "",
    val resultado: ResultadoConversion? = null,
    val cuadroVivo: Cuadrilatero? = null,
    val error: String? = null
) {
    val capturando: Boolean get() = edicion == null && !revisando
}

class ScannerViewModel(
    private val repositorio: DocumentoRepository,
    private val ocr: OcrService,
    carpetaBase: File
) : ViewModel() {

    private val carpetaPaginas = File(carpetaBase, "Escaneos").apply { mkdirs() }
    private val carpetaOriginales = File(carpetaPaginas, "originales").apply { mkdirs() }
    private val _estado = MutableStateFlow(ScannerUiState())
    val estado: StateFlow<ScannerUiState> = _estado.asStateFlow()

    private var trabajoPrevia: Job? = null

    // ------------------------------------------------------------- captura

    fun onCuadroVivo(cuadrilatero: Cuadrilatero?) {
        if (_estado.value.edicion != null || _estado.value.revisando) return
        _estado.update { it.copy(cuadroVivo = cuadrilatero) }
    }

    fun onFotoCapturada(archivo: File) {
        if (_estado.value.procesando) return
        viewModelScope.launch {
            _estado.update {
                it.copy(procesando = true, resultado = null, error = null, cuadroVivo = null)
            }
            val original = withContext(Dispatchers.IO) { asegurarOriginal(archivo) }
            val preparado = withContext(Dispatchers.Default) {
                runCatching {
                    val vista = ScanEngine.cargarVista(original) ?: return@runCatching null
                    val cuadrilatero = ScanEngine.detectar(vista) ?: Cuadrilatero.COMPLETO
                    vista to cuadrilatero
                }.getOrNull()
            }
            if (preparado == null) {
                _estado.update {
                    it.copy(
                        procesando = false,
                        error = "No se pudo leer la foto. Intenta de nuevo."
                    )
                }
                return@launch
            }
            _estado.update {
                it.copy(
                    procesando = false,
                    edicion = EdicionEscaneo(
                        paginaId = 0L,
                        rutaOriginal = original.absolutePath,
                        vista = preparado.first,
                        cuadrilatero = preparado.second,
                        filtro = FiltroEscaneo.MEJORADO
                    )
                )
            }
            actualizarPrevia()
        }
    }

    /** Guarda la foto en una carpeta estable para poder reeditarla más adelante. */
    private fun asegurarOriginal(archivo: File): File {
        if (archivo.parentFile?.absolutePath == carpetaOriginales.absolutePath) return archivo
        return runCatching {
            val destino = File(carpetaOriginales, "original_${System.currentTimeMillis()}.jpg")
            archivo.copyTo(destino, overwrite = true)
            archivo.delete()
            destino
        }.getOrDefault(archivo)
    }

    fun onGaleria(uri: String) {
        viewModelScope.launch {
            _estado.update { it.copy(procesando = true, error = null) }
            val archivo = withContext(Dispatchers.IO) {
                runCatching {
                    repositorio.copiarTemporal(uri, "escaneo_${System.currentTimeMillis()}.jpg")
                }.getOrNull()
            }
            _estado.update { it.copy(procesando = false) }
            if (archivo == null) {
                _estado.update { it.copy(error = "No se pudo abrir la imagen.") }
            } else {
                onFotoCapturada(archivo)
            }
        }
    }

    // -------------------------------------------------------------- editor

    fun moverEsquina(indice: Int, x: Float, y: Float) {
        val edicion = _estado.value.edicion ?: return
        val puntos = edicion.cuadrilatero.puntos.toMutableList()
        puntos[indice] = Punto(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
        val nuevo = Cuadrilatero(puntos[0], puntos[1], puntos[2], puntos[3])
        if (!valido(nuevo)) return
        _estado.update { it.copy(edicion = edicion.copy(cuadrilatero = nuevo)) }
        actualizarPrevia()
    }

    private fun valido(c: Cuadrilatero): Boolean {
        if (c.areaAproximada() < 0.03f) return false
        val p = c.puntos
        var signo = 0
        for (i in 0 until 4) {
            val a = p[i]
            val b = p[(i + 1) % 4]
            val d = p[(i + 2) % 4]
            val cruz = (b.x - a.x) * (d.y - b.y) - (b.y - a.y) * (d.x - b.x)
            val actual = if (cruz > 0) 1 else if (cruz < 0) -1 else 0
            if (actual == 0) return false
            if (signo == 0) signo = actual else if (signo != actual) return false
        }
        return true
    }

    fun autoDetectar() {
        val edicion = _estado.value.edicion ?: return
        viewModelScope.launch {
            _estado.update {
                it.copy(edicion = edicion.copy(calculandoPrevia = true))
            }
            val detectado = withContext(Dispatchers.Default) {
                runCatching { ScanEngine.detectar(edicion.vista) }.getOrNull()
            }
            val actual = _estado.value.edicion ?: return@launch
            _estado.update {
                it.copy(
                    edicion = when {
                        detectado != null -> actual.copy(
                            cuadrilatero = detectado,
                            calculandoPrevia = false
                        )
                        else -> actual.copy(calculandoPrevia = false)
                    },
                    error = if (detectado == null) {
                        "No se detectaron los bordes. Ajusta las esquinas a mano."
                    } else null
                )
            }
            actualizarPrevia()
        }
    }

    fun cambiarFiltro(filtro: FiltroEscaneo) {
        val edicion = _estado.value.edicion ?: return
        if (edicion.filtro == filtro) return
        _estado.update { it.copy(edicion = edicion.copy(filtro = filtro)) }
        actualizarPrevia()
    }

    fun girar() {
        val edicion = _estado.value.edicion ?: return
        val girada = ScanEngine.rotar(edicion.vista, 90)
        if (girada === edicion.vista) return
        _estado.update {
            it.copy(
                edicion = edicion.copy(
                    vista = girada,
                    cuadrilatero = edicion.cuadrilatero.girar90(),
                    giros = (edicion.giros + 90) % 360,
                    previa = null
                )
            )
        }
        actualizarPrevia()
    }

    private fun actualizarPrevia() {
        val edicion = _estado.value.edicion ?: return
        trabajoPrevia?.cancel()
        trabajoPrevia = viewModelScope.launch {
            delay(180)
            val base = _estado.value.edicion ?: return@launch
            if (base.cuadrilatero != edicion.cuadrilatero || base.filtro != edicion.filtro ||
                base.rutaOriginal != edicion.rutaOriginal
            ) {
                return@launch
            }
            _estado.update { estado ->
                val actual = estado.edicion ?: return@update estado
                estado.copy(edicion = actual.copy(calculandoPrevia = true))
            }
            val previa = withContext(Dispatchers.Default) {
                runCatching {
                    ScanEngine.vistaPrevia(base.vista, base.cuadrilatero, base.filtro, 1000)
                }.getOrNull()
            }
            _estado.update { estado ->
                val actual = estado.edicion ?: return@update estado
                if (actual.rutaOriginal != base.rutaOriginal ||
                    actual.cuadrilatero != base.cuadrilatero ||
                    actual.filtro != base.filtro
                ) {
                    estado
                } else {
                    estado.copy(edicion = actual.copy(previa = previa, calculandoPrevia = false))
                }
            }
        }
    }

    fun cancelarEdicion() {
        trabajoPrevia?.cancel()
        _estado.update { it.copy(edicion = null, error = null) }
    }

    fun aplicarEdicion() {
        val edicion = _estado.value.edicion ?: return
        if (_estado.value.procesando) return
        trabajoPrevia?.cancel()
        viewModelScope.launch {
            _estado.update { it.copy(procesando = true, error = null) }
            val pagina = withContext(Dispatchers.Default) {
                runCatching { generarPagina(edicion) }.getOrNull()
            }
            if (pagina == null) {
                _estado.update {
                    it.copy(
                        procesando = false,
                        error = "No se pudo procesar la página. Revisa las esquinas."
                    )
                }
                return@launch
            }
            _estado.update { estado ->
                val paginas = if (edicion.paginaId != 0L) {
                    estado.paginas.map { if (it.id == edicion.paginaId) pagina else it }
                } else {
                    estado.paginas + pagina
                }
                estado.copy(
                    paginas = paginas,
                    edicion = null,
                    procesando = false,
                    error = null,
                    texto = ""
                )
            }
        }
    }

    private fun generarPagina(edicion: EdicionEscaneo): PaginaEscaneada? {
        val original = ScanEngine.cargarProceso(File(edicion.rutaOriginal)) ?: return null
        var trabajo = original
        if (edicion.giros % 360 != 0) {
            trabajo = ScanEngine.rotar(original, edicion.giros)
            if (trabajo !== original) original.recycle()
        }
        val enderezado = ScanEngine.corregirPerspectiva(trabajo, edicion.cuadrilatero)
        trabajo.recycle()

        val filtrado = ScanEngine.aplicarFiltro(enderezado, edicion.filtro)
        if (filtrado !== enderezado) enderezado.recycle()

        try {
            val id = if (edicion.paginaId != 0L) edicion.paginaId else System.currentTimeMillis()
            val archivo = File(carpetaPaginas, "escaneo_$id.jpg")
            ScanEngine.guardarJpeg(filtrado, archivo)
            var mini = ScanEngine.miniatura(filtrado, 240)
            if (mini === filtrado) {
                mini = filtrado.copy(Bitmap.Config.ARGB_8888, false) ?: filtrado
            }
            return PaginaEscaneada(
                id = id,
                rutaOriginal = edicion.rutaOriginal,
                rutaProcesada = archivo.absolutePath,
                miniatura = mini,
                filtro = edicion.filtro,
                cuadrilatero = edicion.cuadrilatero,
                giros = edicion.giros,
                ancho = filtrado.width,
                alto = filtrado.height
            )
        } finally {
            filtrado.recycle()
        }
    }

    // -------------------------------------------------------------- paginas

    fun abrirRevisor() {
        _estado.update { it.copy(revisando = true) }
    }

    fun cerrarRevisor() {
        _estado.update { it.copy(revisando = false) }
    }

    fun reabrirPagina(id: Long) {
        val pagina = _estado.value.paginas.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            _estado.update { it.copy(procesando = true, revisando = false) }
            val vista = withContext(Dispatchers.Default) {
                runCatching { ScanEngine.cargarVista(File(pagina.rutaOriginal)) }.getOrNull()
            }
            if (vista == null) {
                _estado.update {
                    it.copy(procesando = false, error = "No se pudo abrir la página.")
                }
                return@launch
            }
            val girada = if (pagina.giros % 360 != 0) {
                ScanEngine.rotar(vista, pagina.giros).also { if (it !== vista) vista.recycle() }
            } else {
                vista
            }
            _estado.update {
                it.copy(
                    procesando = false,
                    edicion = EdicionEscaneo(
                        paginaId = pagina.id,
                        rutaOriginal = pagina.rutaOriginal,
                        vista = girada,
                        cuadrilatero = pagina.cuadrilatero,
                        filtro = pagina.filtro,
                        giros = pagina.giros
                    )
                )
            }
            actualizarPrevia()
        }
    }

    fun eliminarPagina(id: Long) {
        val pagina = _estado.value.paginas.firstOrNull { it.id == id } ?: return
        runCatching { File(pagina.rutaProcesada).delete() }
        _estado.update { estado ->
            estado.copy(paginas = estado.paginas.filterNot { it.id == id })
        }
    }

    fun limpiar() {
        trabajoPrevia?.cancel()
        val estado = _estado.value
        estado.paginas.forEach { pagina ->
            runCatching { File(pagina.rutaProcesada).delete() }
            runCatching { File(pagina.rutaOriginal).delete() }
        }
        estado.edicion?.let { runCatching { File(it.rutaOriginal).delete() } }
        _estado.value = ScannerUiState()
    }

    // ----------------------------------------------------------------- pdf

    fun generarPdf() {
        val paginas = _estado.value.paginas
        if (paginas.isEmpty() || _estado.value.generando) return
        viewModelScope.launch {
            _estado.update { it.copy(generando = true, resultado = null, error = null) }
            val resultado = repositorio.escaneoAPdf(paginas.map { it.rutaProcesada })
            _estado.update { it.copy(generando = false, resultado = resultado) }
        }
    }

    fun extraerTexto() {
        val paginas = _estado.value.paginas
        if (paginas.isEmpty() || _estado.value.extrayendoTexto) return
        viewModelScope.launch {
            _estado.update { it.copy(extrayendoTexto = true, resultado = null) }
            val textos = mutableListOf<String>()
            paginas.forEach { pagina ->
                val texto = withContext(Dispatchers.IO) {
                    runCatching { ocr.reconocer(File(pagina.rutaProcesada)) }.getOrNull()
                }
                if (!texto.isNullOrBlank()) textos += texto.trim()
            }
            _estado.update {
                it.copy(
                    extrayendoTexto = false,
                    texto = textos.joinToString("\n\n"),
                    resultado = if (textos.isEmpty()) {
                        ResultadoConversion(false, "No se detectó texto. Prueba con más luz.")
                    } else null
                )
            }
        }
    }

    fun textoAPdf() {
        val texto = _estado.value.texto
        if (texto.isBlank() || _estado.value.generando) return
        viewModelScope.launch {
            _estado.update { it.copy(generando = true, resultado = null) }
            val resultado = repositorio.textoAPdf(texto, "Escaneo")
            _estado.update { it.copy(generando = false, resultado = resultado) }
        }
    }

    fun descartarResultado() {
        _estado.update { it.copy(resultado = null) }
    }

    fun descartarError() {
        _estado.update { it.copy(error = null) }
    }
}
