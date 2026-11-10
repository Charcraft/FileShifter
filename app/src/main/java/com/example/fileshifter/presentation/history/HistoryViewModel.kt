package com.example.fileshifter.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.local.HistorialEntity
import com.example.fileshifter.data.repository.DocumentoRepository
import com.example.fileshifter.data.repository.HistorialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repositorio: HistorialRepository,
    private val documentos: DocumentoRepository
) : ViewModel() {

    val historial: StateFlow<List<HistorialEntity>> = repositorio.historial.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    /** Mensaje del último cambio de nombre, para mostrarlo en la pantalla. */
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    fun eliminar(id: Long) {
        viewModelScope.launch { repositorio.eliminar(id) }
    }

    fun limpiar() {
        viewModelScope.launch { repositorio.limpiar() }
    }

    /** Renombra el archivo real y su registro en el historial. */
    fun renombrar(item: HistorialEntity, nuevoNombre: String) {
        viewModelScope.launch {
            val resultado = documentos.renombrar(item, nuevoNombre)
            _aviso.value = resultado.mensaje
        }
    }

    fun descartarAviso() {
        _aviso.value = null
    }
}
