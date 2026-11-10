package com.example.fileshifter.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.local.AjustesRepository
import com.example.fileshifter.data.model.AjustesFlujo
import com.example.fileshifter.data.repository.HistorialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(
    private val repositorio: AjustesRepository,
    private val historial: HistorialRepository,
    private val carpeta: File
) : ViewModel() {
    val ajustes: StateFlow<AjustesFlujo> = repositorio.ajustes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AjustesFlujo()
    )

    private val _almacenamiento = MutableStateFlow(0L)
    val almacenamiento: StateFlow<Long> = _almacenamiento.asStateFlow()

    init {
        recalcular()
    }

    fun setSugerirCorrector(valor: Boolean) {
        viewModelScope.launch { repositorio.setSugerirCorrector(valor) }
    }

    fun setSugerirEntrega(valor: Boolean) {
        viewModelScope.launch { repositorio.setSugerirEntrega(valor) }
    }

    fun setSugerirValidador(valor: Boolean) {
        viewModelScope.launch { repositorio.setSugerirValidador(valor) }
    }

    fun setNoPreguntarMas(valor: Boolean) {
        viewModelScope.launch { repositorio.setNoPreguntarMas(valor) }
    }

    fun recalcular() {
        viewModelScope.launch {
            _almacenamiento.value = withContext(Dispatchers.IO) {
                carpeta.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }
        }
    }

    fun liberarEspacio() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                carpeta.listFiles()?.forEach { it.deleteRecursively() }
                historial.limpiar()
            }
            recalcular()
        }
    }
}
