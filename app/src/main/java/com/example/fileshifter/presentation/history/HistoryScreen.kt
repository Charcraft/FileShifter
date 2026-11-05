package com.example.fileshifter.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.model.formatearTamano
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.Aviso
import com.example.fileshifter.presentation.common.BarraInferior
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.presentation.common.compartirArchivo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Historial de archivos generados (carpeta FileShifter/ + Room). */
@Composable
fun HistoryScreen(navController: NavController) {
    val vm: HistoryViewModel = appViewModel { HistoryViewModel(it.historialRepository) }
    val historial by vm.historial.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var confirmarLimpiar by remember { mutableStateOf(false) }
    val formato = remember { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }

    PantallaBase(
        titulo = "Historial",
        onVolver = { navController.popBackStack() },
        acciones = {
            if (historial.isNotEmpty()) {
                TextButton(onClick = { confirmarLimpiar = true }) { Text("Limpiar") }
            }
        },
        barraInferior = { BarraInferior(navController, Screen.History.route) }
    ) { padding ->
        if (historial.isEmpty()) {
            Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
                Aviso("Todavía no hay archivos. Todo lo que generes se guarda en la carpeta FileShifter/ y aparece aquí.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historial, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.nombre, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${item.operacion} · ${formatearTamano(item.tamanoBytes)} · ${formato.format(Date(item.creadoEn))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            androidx.compose.foundation.layout.Row {
                                IconButton(onClick = {
                                    if (File(item.ruta).exists()) {
                                        compartirArchivo(contexto, item.ruta)
                                    }
                                }) {
                                    Icon(Icons.Filled.Share, contentDescription = "Compartir")
                                }
                                IconButton(onClick = { vm.eliminar(item.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Quitar del historial")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmarLimpiar) {
        AlertDialog(
            onDismissRequest = { confirmarLimpiar = false },
            title = { Text("Limpiar historial") },
            text = { Text("Se quitarán los registros. Los archivos de la carpeta FileShifter/ no se borran.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.limpiar()
                    confirmarLimpiar = false
                }) { Text("Limpiar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarLimpiar = false }) { Text("Cancelar") }
            }
        )
    }
}
