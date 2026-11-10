package com.example.fileshifter.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.local.HistorialEntity
import com.example.fileshifter.data.model.formatearTamano
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.BarraInferior
import com.example.fileshifter.presentation.common.ChipIcono
import com.example.fileshifter.presentation.common.ContenidoResponsive
import com.example.fileshifter.presentation.common.EtiquetaTipo
import com.example.fileshifter.presentation.common.MenuTresPuntos
import com.example.fileshifter.presentation.common.OpcionMenu
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.presentation.common.compartirArchivo
import com.example.fileshifter.presentation.common.formatearFecha
import com.example.fileshifter.presentation.common.iconoDeArchivo
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.PrimaryLight
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary
import java.io.File

private val FILTROS = listOf("Todos", "PDF", "IMG", "QR", "DOC", "XLS")

@Composable
fun HistoryScreen(navController: NavController) {
    val vm: HistoryViewModel = appViewModel { HistoryViewModel(it.historialRepository) }
    val historial by vm.historial.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var confirmarLimpiar by remember { mutableStateOf(false) }
    var filtro by remember { mutableStateOf("Todos") }

    val filtrados = historial.filter { item ->
        when (filtro) {
            "Todos" -> true
            "PDF" -> item.nombre.endsWith(".pdf", true)
            "IMG" -> item.nombre.matches(Regex(".*\\.(png|jpe?g|webp|heic|bmp)$", RegexOption.IGNORE_CASE))
            "QR" -> item.nombre.startsWith("Codigo_") || item.nombre.contains("QR", true)
            "DOC" -> item.nombre.matches(Regex(".*\\.(docx?|txt)$", RegexOption.IGNORE_CASE))
            "XLS" -> item.nombre.matches(Regex(".*\\.(xlsx?|csv)$", RegexOption.IGNORE_CASE))
            else -> true
        }
    }

    PantallaBase(
        titulo = "Historial",
        onVolver = { navController.popBackStack() },
        acciones = {
            if (historial.isNotEmpty()) {
                TextButton(onClick = { confirmarLimpiar = true }) {
                    Text("Limpiar", color = Color.White)
                }
            }
        },
        barraInferior = { BarraInferior(navController, Screen.History.route) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FILTROS) { etiqueta ->
                    val activo = filtro == etiqueta
                    FilterChip(
                        selected = activo,
                        onClick = { filtro = etiqueta },
                        label = { Text(etiqueta) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = activo,
                            borderColor = Border,
                            selectedBorderColor = Primary
                        )
                    )
                }
            }

            Text(
                "${filtrados.size} ARCHIVOS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
            )

            if (filtrados.isEmpty()) {
                ContenidoResponsive {
                    Column(Modifier.padding(16.dp)) {
                        Tarjeta {
                            Text(
                                if (historial.isEmpty()) {
                                    "Todavía no hay archivos. Todo lo que generes se guarda en la carpeta FileShifter/."
                                } else {
                                    "No hay archivos de este tipo."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        ContenidoResponsive {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                Tarjeta(padding = 0.dp) {
                                    filtrados.forEachIndexed { indice, item ->
                                        FilaHistorial(
                                            item = item,
                                            onCompartir = {
                                                if (File(item.ruta).exists()) compartirArchivo(contexto, item.ruta)
                                            },
                                            onEliminar = { vm.eliminar(item.id) }
                                        )
                                        if (indice < filtrados.lastIndex) HorizontalDivider(color = Border)
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
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

@Composable
private fun FilaHistorial(
    item: HistorialEntity,
    onCompartir: () -> Unit,
    onEliminar: () -> Unit
) {
    val etiqueta = when {
        item.nombre.endsWith(".pdf", true) -> "PDF"
        item.nombre.matches(Regex(".*\\.(png|jpe?g|webp|heic|bmp)$", RegexOption.IGNORE_CASE)) -> "IMG"
        item.nombre.startsWith("Codigo_") || item.nombre.contains("QR", true) -> "QR"
        item.nombre.matches(Regex(".*\\.(xlsx?|csv)$", RegexOption.IGNORE_CASE)) -> "XLS"
        else -> "DOC"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChipIcono(iconoDeArchivo(item.nombre), tamano = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.nombre,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                EtiquetaTipo(etiqueta)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${formatearTamano(item.tamanoBytes)} · ${formatearFecha(item.creadoEn)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
        MenuTresPuntos {
            OpcionMenu("Compartir") { onCompartir() }
            OpcionMenu("Quitar del historial") { onEliminar() }
        }
    }
}
