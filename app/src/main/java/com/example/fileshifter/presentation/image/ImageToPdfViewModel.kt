package com.example.fileshifter.presentation.image

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.model.ArchivoSeleccionado
import com.example.fileshifter.data.model.ResultadoConversion
import com.example.fileshifter.data.repository.DocumentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de Imagen a PDF Rápida (F2). */
data class ImageToPdfUiState(
    val imagenes: List<ArchivoSeleccionado> = emptyList(),
    val unir: Boolean = true,
    val cargando: Boolean = false,
    val resultado: ResultadoConversion? = null
)

class ImageToPdfViewModel(private val repositorio: DocumentoRepository) : ViewModel() {

    private val _estado = MutableStateFlow(ImageToPdfUiState())
    val estado: StateFlow<ImageToPdfUiState> = _estado.asStateFlow()

    fun onImagenesSeleccionadas(uris: List<String>) {
        viewModelScope.launch {
            val imagenes = repositorio.describir(uris)
            _estado.update { it.copy(imagenes = imagenes, resultado = null) }
        }
    }

    fun onUnirCambia(valor: Boolean) {
        _estado.update { it.copy(unir = valor) }
    }

    fun convertir() {
        val actual = _estado.value
        if (actual.imagenes.isEmpty() || actual.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, resultado = null) }
            val resultado = repositorio.imagenesAPdf(actual.imagenes.map { it.uri }, actual.unir)
            _estado.update { it.copy(cargando = false, resultado = resultado) }
        }
    }
}
