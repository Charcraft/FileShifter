package com.example.fileshifter.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.local.HistorialEntity
import com.example.fileshifter.data.repository.HistorialRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Inicio muestra las herramientas y los archivos recientes (Room). */
class HomeViewModel(private val historial: HistorialRepository) : ViewModel() {

    val recientes: StateFlow<List<HistorialEntity>> = historial.historial.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun eliminar(id: Long) {
        viewModelScope.launch { historial.eliminar(id) }
    }
}
