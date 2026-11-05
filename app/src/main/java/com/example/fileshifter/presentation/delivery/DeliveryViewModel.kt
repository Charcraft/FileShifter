package com.example.fileshifter.presentation.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fileshifter.data.model.DatosEntrega
import com.example.fileshifter.data.model.PlantillaEntrega
import com.example.fileshifter.data.model.ResultadoConversion
import com.example.fileshifter.data.repository.DocumentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado compartido por los tres pasos del Modo Entrega (F5). */
data class DeliveryUiState(
    val plantilla: PlantillaEntrega = PlantillaEntrega.ESCOLAR,
    val datos: DatosEntrega = DatosEntrega(),
    val cargando: Boolean = false,
    val resultado: ResultadoConversion? = null
) {
    val formularioValido: Boolean
        get() = datos.nombre.isNotBlank() && datos.titulo.isNotBlank()
}

class DeliveryViewModel(private val repositorio: DocumentoRepository) : ViewModel() {

    private val _estado = MutableStateFlow(DeliveryUiState())
    val estado: StateFlow<DeliveryUiState> = _estado.asStateFlow()

    fun onPlantilla(plantilla: PlantillaEntrega) {
        _estado.update { it.copy(plantilla = plantilla, resultado = null) }
    }

    fun onNombre(valor: String) = actualizar { it.copy(nombre = valor) }
    fun onMatricula(valor: String) = actualizar { it.copy(matricula = valor) }
    fun onMateria(valor: String) = actualizar { it.copy(materia = valor) }
    fun onTitulo(valor: String) = actualizar { it.copy(titulo = valor) }
    fun onIndice(valor: String) = actualizar { it.copy(indice = valor) }

    fun generar() {
        val actual = _estado.value
        if (!actual.formularioValido || actual.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, resultado = null) }
            val resultado = repositorio.crearEntregaPdf(actual.plantilla, actual.datos)
            _estado.update { it.copy(cargando = false, resultado = resultado) }
        }
    }

    private fun actualizar(transformacion: (DatosEntrega) -> DatosEntrega) {
        _estado.update { it.copy(datos = transformacion(it.datos), resultado = null) }
    }
}
