package com.example.fileshifter.presentation.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.repository.DocumentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de QR / Barras (F8). */
data class QrUiState(
    val texto: String = "",
    val esQr: Boolean = true,
    val generando: Boolean = false,
    val rutaGenerada: String? = null,
    val escaneado: String? = null,
    val error: String? = null
)

class QrViewModel(private val repositorio: DocumentoRepository) : ViewModel() {

    private val _estado = MutableStateFlow(QrUiState())
    val estado: StateFlow<QrUiState> = _estado.asStateFlow()

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

    fun generar() {
        val actual = _estado.value
        if (actual.texto.isBlank() || actual.generando) return
        viewModelScope.launch {
            _estado.update { it.copy(generando = true, error = null, rutaGenerada = null) }
            runCatching {
                repositorio.generarCodigo(actual.texto, actual.esQr)
            }.onSuccess { archivo ->
                _estado.update { it.copy(generando = false, rutaGenerada = archivo.absolutePath) }
            }.onFailure { error ->
                _estado.update { it.copy(generando = false, error = error.message) }
            }
        }
    }
}
