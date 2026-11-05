package com.example.fileshifter.presentation.delivery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.model.PlantillaEntrega
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.Aviso
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.TarjetaResultado
import com.example.fileshifter.presentation.common.appViewModel

/** ViewModel compartido del grafo delivery_flow. */
@Composable
private fun deliveryViewModel(navController: NavController): DeliveryViewModel {
    val padre = remember(navController) { navController.getBackStackEntry(Screen.DeliveryGraph.route) }
    return appViewModel(padre) { DeliveryViewModel(it.documentoRepository) }
}

// ------------------------------------------------------------------ Paso 1

@Composable
fun DeliveryStep1Screen(navController: NavController) {
    val vm = deliveryViewModel(navController)
    val estado by vm.estado.collectAsStateWithLifecycle()

    PantallaBase(titulo = "Modo Entrega · Paso 1", onVolver = { navController.popBackStack() }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Aviso("Elige plantilla. El paso 2 pide tus datos y el 3 muestra la vista previa antes de generar el PDF.") }
            items(PlantillaEntrega.entries.toList()) { plantilla ->
                Card(
                    onClick = {
                        vm.onPlantilla(plantilla)
                        navController.navigate(Screen.Delivery2.route)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (estado.plantilla == plantilla) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(plantilla.etiqueta, style = MaterialTheme.typography.titleMedium)
                        Text(
                            plantilla.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { navController.navigate(Screen.Delivery2.route) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Siguiente") }
            }
        }
    }
}

// ------------------------------------------------------------------ Paso 2

@Composable
fun DeliveryStep2Screen(navController: NavController) {
    val vm = deliveryViewModel(navController)
    val estado by vm.estado.collectAsStateWithLifecycle()
    val datos = estado.datos

    PantallaBase(titulo = "Modo Entrega · Paso 2", onVolver = { navController.popBackStack() }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Aviso("Plantilla elegida: ${estado.plantilla.etiqueta}") }
            item {
                OutlinedTextField(
                    value = datos.nombre,
                    onValueChange = vm::onNombre,
                    label = { Text("Nombre completo *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = datos.matricula,
                    onValueChange = vm::onMatricula,
                    label = { Text("Matrícula") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = datos.materia,
                    onValueChange = vm::onMateria,
                    label = { Text("Materia") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = datos.titulo,
                    onValueChange = vm::onTitulo,
                    label = { Text("Título del trabajo *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = datos.indice,
                    onValueChange = vm::onIndice,
                    label = { Text("Índice (una línea por punto)") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(
                    onClick = { navController.navigate(Screen.Delivery3.route) },
                    enabled = estado.formularioValido,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Siguiente") }
            }
        }
    }
}

// ------------------------------------------------------------------ Paso 3

@Composable
fun DeliveryStep3Screen(navController: NavController) {
    val vm = deliveryViewModel(navController)
    val estado by vm.estado.collectAsStateWithLifecycle()
    val datos = estado.datos

    PantallaBase(titulo = "Modo Entrega · Paso 3", onVolver = { navController.popBackStack() }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            datos.materia.ifBlank { "Materia" },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            datos.titulo.ifBlank { "Título del trabajo" },
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Alumno: ${datos.nombre}", style = MaterialTheme.typography.bodyMedium)
                        Text("Matrícula: ${datos.matricula}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        Text("Índice", style = MaterialTheme.typography.titleSmall)
                        datos.indice.split("\n").filter { it.isNotBlank() }.forEach { linea ->
                            Text("• $linea", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Plantilla: ${estado.plantilla.etiqueta}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { vm.generar() },
                    enabled = !estado.cargando,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (estado.cargando) "Generando..." else "Generar PDF") }
                if (estado.cargando) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            estado.resultado?.let { resultado ->
                item {
                    TarjetaResultado(
                        resultado = resultado,
                        onValidar = { navController.navigate(Screen.Validator.route) },
                        onInicio = { navController.navigate(Screen.Home.route) }
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vista previa paginada lista para generar.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
