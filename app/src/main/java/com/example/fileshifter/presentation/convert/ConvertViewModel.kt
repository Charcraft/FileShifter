package com.example.fileshifter.presentation.convert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.model.ArchivoSeleccionado
import com.example.fileshifter.data.model.FormatoSalida
import com.example.fileshifter.data.model.ResultadoConversion
import com.example.fileshifter.data.model.salidasCompatibles
import com.example.fileshifter.data.repository.DocumentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConvertUiState(
    val archivos: List<ArchivoSeleccionado> = emptyList(),
    val salidasDisponibles: List<FormatoSalida> = emptyList(),
    val objetivo: FormatoSalida? = null,
    val cargando: Boolean = false,
    val resultado: ResultadoConversion? = null
)

class ConvertViewModel(private val repositorio: DocumentoRepository) : ViewModel() {
    private val _estado = MutableStateFlow(ConvertUiState())
    val estado: StateFlow<ConvertUiState> = _estado.asStateFlow()

    fun onArchivosSeleccionados(uris: List<String>) {
        viewModelScope.launch {
            val archivos = repositorio.describir(uris)
            val salidas = archivos.flatMap { salidasCompatibles(it.tipo) }.distinct()
            _estado.update {
                it.copy(
                    archivos = archivos,
                    salidasDisponibles = salidas,
                    objetivo = salidas.firstOrNull(),
                    resultado = null
                )
            }
        }
    }

    fun onObjetivoElegido(formato: FormatoSalida) {
        _estado.update { it.copy(objetivo = formato) }
    }

    fun quitar(uri: String) {
        _estado.update { estado ->
            estado.copy(archivos = estado.archivos.filterNot { it.uri == uri })
        }
    }

    fun convertir() {
        val actual = _estado.value
        val objetivo = actual.objetivo ?: return
        if (actual.archivos.isEmpty() || actual.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, resultado = null) }
            val resultado = repositorio.convertir(actual.archivos, objetivo)
            _estado.update { it.copy(cargando = false, resultado = resultado) }
        }
    }
}
