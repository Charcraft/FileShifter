package com.example.fileshifter.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.local.AjustesRepository
import com.example.fileshifter.data.model.AjustesFlujo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ajustes del flujo configurable (DataStore). */
class SettingsViewModel(private val repositorio: AjustesRepository) : ViewModel() {

    val ajustes: StateFlow<AjustesFlujo> = repositorio.ajustes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AjustesFlujo()
    )

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
}
