package com.example.fileshifter.presentation.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.local.HistorialEntity
import com.example.fileshifter.data.repository.DocumentoRepository
import com.example.fileshifter.data.repository.HistorialRepository
import com.example.fileshifter.data.source.OcrService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class QrUiState(
    val texto: String = "",
    val esQr: Boolean = true,
    val generando: Boolean = false,
    val rutaGenerada: String? = null,
    val escaneado: String? = null,
    val leyendoGaleria: Boolean = false,
    val error: String? = null
)

class QrViewModel(
    private val repositorio: DocumentoRepository,
    private val historial: HistorialRepository,
    private val ocr: OcrService
) : ViewModel() {
    private val _estado = MutableStateFlow(QrUiState())
    val estado: StateFlow<QrUiState> = _estado.asStateFlow()

    val codigos: StateFlow<List<HistorialEntity>> = historial.historial.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun onTextoCambia(texto: String) {
        _estado.update { it.copy(texto = texto) }
    }

    fun onTipoCambia(esQr: Boolean) {
        _estado.update { it.copy(esQr = esQr) }
    }

    fun usarTextoEscaneado() {
        val valor = _estado.value.escaneado ?: return
        _estado.update { it.copy(texto = valor, rutaGenerada = null) }
    }

    fun onCodigoEscaneado(valor: String) {
        if (_estado.value.escaneado != valor) {
            _estado.update { it.copy(escaneado = valor) }
        }
    }

    fun onGaleria(uri: String) {
        viewModelScope.launch {
            _estado.update { it.copy(leyendoGaleria = true, error = null) }
            val archivo = runCatching {
                repositorio.copiarTemporal(uri, "qr_${System.currentTimeMillis()}.jpg")
            }.getOrNull()
            val codigo = archivo?.let { runCatching { ocr.leerCodigo(it) }.getOrNull() }
            _estado.update {
                it.copy(
                    leyendoGaleria = false,
                    escaneado = codigo ?: it.escaneado,
                    error = if (codigo == null) "No se encontró un código en la imagen." else null
                )
            }
        }
    }

    fun generar() {
        val actual = _estado.value
        if (actual.texto.isBlank() || actual.generando) return
        viewModelScope.launch {
            _estado.update { it.copy(generando = true, error = null, rutaGenerada = null) }
            runCatching {
                repositorio.generarCodigo(actual.texto, actual.esQr)
            }.onSuccess { archivo: File ->
                _estado.update { it.copy(generando = false, rutaGenerada = archivo.absolutePath) }
            }.onFailure { error ->
                _estado.update { it.copy(generando = false, error = error.message) }
            }
        }
    }
}
