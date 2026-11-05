package com.example.fileshifter.presentation.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilterChip
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
import com.example.fileshifter.data.model.TipoEdicion
import com.example.fileshifter.data.model.formatearTamano
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.Aviso
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.TarjetaResultado
import com.example.fileshifter.presentation.common.appViewModel

/** Obtiene el ViewModel compartido del grafo edit_flow. */
@Composable
private fun editViewModel(navController: NavController): EditFlowViewModel {
    val padre = remember(navController) { navController.getBackStackEntry(Screen.EditGraph.route) }
    return appViewModel(padre) { EditFlowViewModel(it.documentoRepository) }
}

// ------------------------------------------------------------------ Paso 1: selector

@Composable
fun EditPickerScreen(navController: NavController) {
    val vm = editViewModel(navController)
    val estado by vm.estado.collectAsStateWithLifecycle()

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        vm.onArchivosSeleccionados(uris.map(Uri::toString))
    }

    PantallaBase(titulo = "Editar PDF · Paso 1", onVolver = { navController.popBackStack() }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Aviso("Elige uno o varios PDF. Para Unir necesitas al menos dos; el resto trabaja con uno.") }
            item {
                Button(onClick = { selector.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (estado.archivos.isEmpty()) "Elegir PDF" else "Cambiar selección (${estado.archivos.size})")
                }
            }
            items(estado.archivos) { archivo ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(archivo.nombre, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatearTamano(archivo.tamanoBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { navController.navigate(Screen.EditType.route) },
                    enabled = estado.archivos.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Siguiente") }
            }
        }
    }
}

// ------------------------------------------------------------------ Paso 2: tipo

@Composable
fun EditTypeScreen(navController: NavController) {
    val vm = editViewModel(navController)
    val estado by vm.estado.collectAsStateWithLifecycle()

    PantallaBase(titulo = "Editar PDF · Paso 2", onVolver = { navController.popBackStack() }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Aviso("Elige el tipo de edición. El siguiente paso muestra solo las opciones de ese tipo.") }
            items(TipoEdicion.entries.toList()) { tipo ->
                Card(
                    onClick = {
                        vm.onTipoElegido(tipo)
                        navController.navigate(Screen.EditMode.route)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(tipo.etiqueta, style = MaterialTheme.typography.titleMedium)
                        Text(
                            tipo.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Paso 3: aplicar

@Composable
fun EditModeScreen(navController: NavController) {
    val vm = editViewModel(navController)
    val estado by vm.estado.collectAsStateWithLifecycle()
    val tipo = estado.tipo

    PantallaBase(titulo = "Editar PDF · Paso 3", onVolver = { navController.popBackStack() }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(tipo?.etiqueta ?: "Sin tipo", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${estado.archivos.size} PDF seleccionado(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (tipo) {
                TipoEdicion.ROTAR -> item {
                    Text("Grados a girar", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(90, 180, 270).forEach { grados ->
                            FilterChip(
                                selected = estado.opciones.grados == grados,
                                onClick = { vm.onGrados(grados) },
                                label = { Text("$grados°") }
                            )
                        }
                    }
                }

                TipoEdicion.ELIMINAR -> item {
                    OutlinedTextField(
                        value = estado.opciones.paginas,
                        onValueChange = vm::onPaginas,
                        label = { Text("Páginas a eliminar") },
                        placeholder = { Text("Ejemplo: 2,4-6") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TipoEdicion.REORDENAR -> item {
                    OutlinedTextField(
                        value = estado.opciones.orden,
                        onValueChange = vm::onOrden,
                        label = { Text("Nuevo orden de páginas") },
                        placeholder = { Text("Ejemplo: 3,1,2") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TipoEdicion.MARCA_AGUA -> item {
                    OutlinedTextField(
                        value = estado.opciones.textoMarcaAgua,
                        onValueChange = vm::onMarcaAgua,
                        label = { Text("Texto de la marca de agua") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> item {
                    Aviso("Esta edición no necesita opciones extra. Pulsa Aplicar.")
                }
            }

            item {
                Button(
                    onClick = { vm.aplicar() },
                    enabled = tipo != null && !estado.cargando && estado.archivos.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (estado.cargando) "Aplicando..." else "Aplicar") }
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
                        onInicio = { navController.navigate(Screen.Home.route) },
                        onCorregir = { navController.navigate(Screen.Corrector.route) },
                        onEntrega = { navController.navigate(Screen.Delivery1.route) }
                    )
                }
            }
        }
    }
}
