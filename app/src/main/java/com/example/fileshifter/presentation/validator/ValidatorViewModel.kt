package com.example.fileshifter.presentation.validator

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.model.ArchivoSeleccionado
import com.example.fileshifter.data.model.ReporteValidacion
import com.example.fileshifter.data.repository.DocumentoRepository
import com.example.fileshifter.data.source.GeneradorCodigos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del Validador (F6). */
data class ValidatorUiState(
    val archivo: ArchivoSeleccionado? = null,
    val reporte: ReporteValidacion? = null,
    val qr: Bitmap? = null,
    val analizando: Boolean = false,
    val error: String? = null
)

class ValidatorViewModel(private val repositorio: DocumentoRepository) : ViewModel() {

    private val _estado = MutableStateFlow(ValidatorUiState())
    val estado: StateFlow<ValidatorUiState> = _estado.asStateFlow()

    fun onPdfSeleccionado(uri: String) {
        viewModelScope.launch {
            _estado.update { it.copy(analizando = true, error = null, reporte = null, qr = null) }
            runCatching {
                val archivo = repositorio.describir(listOf(uri)).firstOrNull()
                    ?: error("No se pudo leer el archivo")
                val reporte = repositorio.validar(archivo)
                val qr = GeneradorCodigos.qr("FileShifter|${reporte.hash}", 480)
                Triple(archivo, reporte, qr)
            }.onSuccess { (archivo, reporte, qr) ->
                _estado.update {
                    it.copy(archivo = archivo, reporte = reporte, qr = qr, analizando = false)
                }
            }.onFailure { error ->
                _estado.update { it.copy(analizando = false, error = error.message) }
            }
        }
    }
}
