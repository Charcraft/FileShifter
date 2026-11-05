package com.example.fileshifter.presentation.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.model.ArchivoSeleccionado
import com.example.fileshifter.data.model.OpcionesEdicion
import com.example.fileshifter.data.model.ResultadoConversion
import com.example.fileshifter.data.model.TipoEdicion
import com.example.fileshifter.data.repository.DocumentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado compartido por los tres pasos de Editar PDF (F3).
 * Vive en el grafo de navegación "edit_flow", así sobrevive a los cambios de paso
 * y a la rotación de pantalla.
 */
data class EditFlowUiState(
    val archivos: List<ArchivoSeleccionado> = emptyList(),
    val tipo: TipoEdicion? = null,
    val opciones: OpcionesEdicion = OpcionesEdicion(),
    val cargando: Boolean = false,
    val resultado: ResultadoConversion? = null
)

class EditFlowViewModel(private val repositorio: DocumentoRepository) : ViewModel() {

    private val _estado = MutableStateFlow(EditFlowUiState())
    val estado: StateFlow<EditFlowUiState> = _estado.asStateFlow()

    fun onArchivosSeleccionados(uris: List<String>) {
        viewModelScope.launch {
            val archivos = repositorio.describir(uris)
            _estado.update { it.copy(archivos = archivos, resultado = null) }
        }
    }

    fun onTipoElegido(tipo: TipoEdicion) {
        _estado.update { it.copy(tipo = tipo, resultado = null) }
    }

    fun onGrados(grados: Int) {
        _estado.update { it.copy(opciones = it.opciones.copy(grados = grados)) }
    }

    fun onPaginas(texto: String) {
        _estado.update { it.copy(opciones = it.opciones.copy(paginas = texto)) }
    }

    fun onOrden(texto: String) {
        _estado.update { it.copy(opciones = it.opciones.copy(orden = texto)) }
    }

    fun onMarcaAgua(texto: String) {
        _estado.update { it.copy(opciones = it.opciones.copy(textoMarcaAgua = texto)) }
    }

    fun aplicar() {
        val actual = _estado.value
        val tipo = actual.tipo ?: return
        if (actual.archivos.isEmpty() || actual.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, resultado = null) }
            val resultado = repositorio.editarPdf(
                tipo = tipo,
                uris = actual.archivos.map { it.uri },
                opciones = actual.opciones
            )
            _estado.update { it.copy(cargando = false, resultado = resultado) }
        }
    }
}
