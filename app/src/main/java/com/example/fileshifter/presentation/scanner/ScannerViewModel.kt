package com.example.fileshifter.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.model.ResultadoConversion
import com.example.fileshifter.data.repository.DocumentoRepository
import com.example.fileshifter.data.source.OcrService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** Estado del Escáner (F7). */
data class ScannerUiState(
    val foto: String? = null,
    val texto: String = "",
    val capturas: Int = 0,
    val procesando: Boolean = false,
    val generando: Boolean = false,
    val resultado: ResultadoConversion? = null
)

class ScannerViewModel(
    private val repositorio: DocumentoRepository,
    private val ocr: OcrService
) : ViewModel() {

    private val _estado = MutableStateFlow(ScannerUiState())
    val estado: StateFlow<ScannerUiState> = _estado.asStateFlow()

    fun onFotoCapturada(archivo: File) {
        viewModelScope.launch {
            _estado.update {
                it.copy(procesando = true, resultado = null, foto = archivo.absolutePath, capturas = it.capturas + 1)
            }
            val texto = runCatching { ocr.reconocer(archivo) }.getOrDefault("")
            _estado.update {
                it.copy(
                    procesando = false,
                    texto = texto.ifBlank { it.texto },
                    resultado = if (texto.isBlank()) {
                        ResultadoConversion(false, "No se detectó texto en la foto. Intenta con más luz.")
                    } else null
                )
            }
        }
    }

    fun onGaleria(uri: String) {
        viewModelScope.launch {
            _estado.update { it.copy(procesando = true, resultado = null) }
            val archivo = runCatching { repositorio.copiarTemporal(uri, "galeria_${System.currentTimeMillis()}.jpg") }
                .getOrNull()
            if (archivo == null) {
                _estado.update {
                    it.copy(procesando = false, resultado = ResultadoConversion(false, "No se pudo abrir la imagen."))
                }
            } else {
                onFotoCapturada(archivo)
            }
        }
    }

    fun onTextoCambia(texto: String) {
        _estado.update { it.copy(texto = texto) }
    }

    fun limpiar() {
        _estado.update { it.copy(texto = "", foto = null, resultado = null) }
    }

    fun generarPdf() {
        val actual = _estado.value
        if (actual.texto.isBlank() || actual.generando) return
        viewModelScope.launch {
            _estado.update { it.copy(generando = true, resultado = null) }
            val resultado = repositorio.textoAPdf(actual.texto, "Escaneo")
            _estado.update { it.copy(generando = false, resultado = resultado) }
        }
    }
}
