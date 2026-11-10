package com.example.fileshifter.presentation.references

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.local.ReferenciaEntity
import com.example.fileshifter.data.model.Referencia
import com.example.fileshifter.data.repository.ReferenciasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReferenciasUiState(
    val consulta: String = "",
    val cargando: Boolean = false,
    val resultados: List<Referencia> = emptyList(),
    val error: String? = null,
    val sinResultados: Boolean = false,
    val desdeCache: Boolean = false,
    val aviso: String? = null
)

class ReferenciasViewModel(
    private val repositorio: ReferenciasRepository
) : ViewModel() {
    private val _estado = MutableStateFlow(ReferenciasUiState())
    val estado: StateFlow<ReferenciasUiState> = _estado.asStateFlow()

    val guardadas: StateFlow<List<ReferenciaEntity>> = repositorio.guardadas.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun onConsulta(valor: String) {
        _estado.update { it.copy(consulta = valor, aviso = null) }
    }

    fun buscar() {
        val consulta = _estado.value.consulta.trim()
        if (consulta.isEmpty() || _estado.value.cargando) return

        viewModelScope.launch {
            _estado.update {
                it.copy(
                    cargando = true,
                    error = null,
                    sinResultados = false,
                    desdeCache = false,
                    aviso = null
                )
            }

            repositorio.buscar(consulta).fold(
                onSuccess = { lista ->
                    _estado.update {
                        it.copy(cargando = false, resultados = lista, sinResultados = lista.isEmpty())
                    }
                },
                onFailure = {
                    val locales = repositorio.buscarGuardadas(consulta)
                    _estado.update {
                        it.copy(
                            cargando = false,
                            resultados = locales,
                            desdeCache = locales.isNotEmpty(),
                            sinResultados = locales.isEmpty(),
                            error = if (locales.isEmpty())
                                "No se pudo conectar con el catálogo. Revisa tu conexión e inténtalo de nuevo."
                            else
                                "Sin conexión. Se muestran las referencias guardadas que coinciden."
                        )
                    }
                }
            )
        }
    }

    fun guardar(referencia: Referencia) {
        viewModelScope.launch {
            repositorio.guardar(referencia)
            _estado.update { it.copy(aviso = "Referencia guardada en el dispositivo") }
        }
    }

    fun eliminar(doi: String) {
        viewModelScope.launch { repositorio.eliminar(doi) }
    }

    fun limpiarGuardadas() {
        viewModelScope.launch { repositorio.limpiar() }
    }

    fun descartarAviso() {
        _estado.update { it.copy(aviso = null) }
    }
}
