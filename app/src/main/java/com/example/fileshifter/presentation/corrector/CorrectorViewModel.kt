package com.example.fileshifter.presentation.corrector

import androidx.lifecycle.ViewModel
import com.example.fileshifter.domain.CorrectorOrtografico
import com.example.fileshifter.domain.Sugerencia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CorrectorUiState(
    val texto: String = "",
    val sugerencias: List<Sugerencia> = emptyList(),
    val revisado: Boolean = false
)

class CorrectorViewModel : ViewModel() {
    private val _estado = MutableStateFlow(CorrectorUiState())
    val estado: StateFlow<CorrectorUiState> = _estado.asStateFlow()

    fun onTextoCambia(texto: String) {
        _estado.update { it.copy(texto = texto, revisado = false, sugerencias = emptyList()) }
    }

    fun onTextoInicial(texto: String) {
        _estado.update { it.copy(texto = texto, revisado = false, sugerencias = emptyList()) }
    }

    fun revisar() {
        val actual = _estado.value
        _estado.update {
            it.copy(
                sugerencias = CorrectorOrtografico.revisar(actual.texto),
                revisado = true
            )
        }
    }

    fun aplicarUna(sugerencia: Sugerencia) {
        val actual = _estado.value
        val nuevoTexto = CorrectorOrtografico.aplicar(actual.texto, listOf(sugerencia))
        _estado.update {
            it.copy(
                texto = nuevoTexto,
                sugerencias = CorrectorOrtografico.revisar(nuevoTexto),
                revisado = true
            )
        }
    }

    fun aplicarTodas() {
        val actual = _estado.value
        val nuevoTexto = CorrectorOrtografico.aplicar(actual.texto, actual.sugerencias)
        _estado.update {
            it.copy(
                texto = nuevoTexto,
                sugerencias = CorrectorOrtografico.revisar(nuevoTexto),
                revisado = true
            )
        }
    }

    fun omitir() {
        _estado.update { it.copy(sugerencias = emptyList(), revisado = false) }
    }
}
