package com.example.fileshifter.presentation.convert

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.model.formatearTamano
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.Aviso
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.TarjetaResultado
import com.example.fileshifter.presentation.common.appViewModel

/** F1 - Convertir Universal: elige archivo, formato de salida y convierte. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen(navController: NavController) {
    val vm: ConvertViewModel = appViewModel { ConvertViewModel(it.documentoRepository) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    var menuAbierto by remember { mutableStateOf(false) }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        vm.onArchivosSeleccionados(uris.map(Uri::toString))
    }

    PantallaBase(titulo = "Convertir Universal", onVolver = { navController.popBackStack() }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Aviso("Formatos de entrada: PDF, DOCX, XLSX, PPTX, TXT, CSV, HTML e imágenes. Todo se procesa en tu teléfono.")
            }
            item {
                Button(onClick = { selector.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (estado.archivos.isEmpty()) "Añadir archivo"
                        else "Cambiar archivos (${estado.archivos.size})"
                    )
                }
            }
            items(estado.archivos) { archivo ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(archivo.nombre, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${archivo.tipo.name} · ${formatearTamano(archivo.tamanoBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (estado.salidasDisponibles.isNotEmpty()) {
                item {
                    ExposedDropdownMenuBox(
                        expanded = menuAbierto,
                        onExpandedChange = { menuAbierto = !menuAbierto }
                    ) {
                        OutlinedTextField(
                            value = estado.objetivo?.etiqueta ?: "Elige formato",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Convertir a") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuAbierto) },
                            modifier = Modifier.menuAnchor(
                                androidx.compose.material3.MenuAnchorType.PrimaryNotEditable
                            ).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = menuAbierto,
                            onDismissRequest = { menuAbierto = false }
                        ) {
                            estado.salidasDisponibles.forEach { formato ->
                                DropdownMenuItem(
                                    text = { Text(formato.etiqueta) },
                                    onClick = {
                                        vm.onObjetivoElegido(formato)
                                        menuAbierto = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { vm.convertir() },
                        enabled = estado.archivos.isNotEmpty() && estado.objetivo != null && !estado.cargando,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (estado.cargando) "Convirtiendo..." else "Convertir")
                    }
                }
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
                        onInicio = { navController.popBackStack() },
                        onCorregir = { navController.navigate(Screen.Corrector.route) },
                        onEntrega = { navController.navigate(Screen.Delivery1.route) }
                    )
                }
            }
        }
    }
}
