package com.example.fileshifter.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.local.HistorialEntity
import com.example.fileshifter.data.repository.HistorialRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Historial ilimitado de archivos generados, guardado en Room. */
class HistoryViewModel(private val repositorio: HistorialRepository) : ViewModel() {

    val historial: StateFlow<List<HistorialEntity>> = repositorio.historial.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun eliminar(id: Long) {
        viewModelScope.launch { repositorio.eliminar(id) }
    }

    fun limpiar() {
        viewModelScope.launch { repositorio.limpiar() }
    }
}
