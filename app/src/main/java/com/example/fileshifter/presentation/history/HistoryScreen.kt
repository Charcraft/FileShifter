package com.example.fileshifter.presentation.history

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.local.HistorialEntity
import com.example.fileshifter.data.model.formatearTamano
import com.example.fileshifter.data.source.CacheVisor
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.BarraInferior
import com.example.fileshifter.presentation.common.CampoTexto
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
import com.example.fileshifter.presentation.visor.VisorActivity
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.PrimaryLight
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val FILTROS = listOf("Todos", "PDF", "IMG", "QR", "DOC", "XLS")

@Composable
fun HistoryScreen(navController: NavController) {
    val vm: HistoryViewModel = appViewModel {
        HistoryViewModel(it.historialRepository, it.documentoRepository)
    }
    val historial by vm.historial.collectAsStateWithLifecycle()
    val aviso by vm.aviso.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var confirmarLimpiar by remember { mutableStateOf(false) }
    var filtro by remember { mutableStateOf("Todos") }
    var editando by remember { mutableStateOf<HistorialEntity?>(null) }
    var nombreNuevo by remember { mutableStateOf("") }

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
            aviso?.let { mensaje ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        mensaje,
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = vm::descartarAviso) { Text("Cerrar", color = TextMuted) }
                }
            }
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
                                            onVer = { VisorActivity.abrir(contexto, item.publica ?: item.ruta, item.nombre) },
                                            onRenombrar = {
                                                editando = item
                                                nombreNuevo = item.nombre.substringBeforeLast('.')
                                            },
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

    editando?.let { item ->
        AlertDialog(
            onDismissRequest = { editando = null },
            title = { Text("Cambiar nombre") },
            text = {
                Column {
                    CampoTexto(
                        valor = nombreNuevo,
                        onCambia = { nombreNuevo = it },
                        etiqueta = "Nombre del archivo",
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Se conserva la extensión .${item.nombre.substringAfterLast('.')} y se " +
                            "renombra también la copia de Documentos/FileShifter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.renombrar(item, nombreNuevo)
                        editando = null
                    },
                    enabled = nombreNuevo.isNotBlank()
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { editando = null }) { Text("Cancelar") }
            }
        )
    }

    if (confirmarLimpiar) {        AlertDialog(
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
    onVer: () -> Unit,
    onRenombrar: () -> Unit,
    onCompartir: () -> Unit,
    onEliminar: () -> Unit
) {
    val contexto = LocalContext.current
    val etiqueta = when {
        item.nombre.endsWith(".pdf", true) -> "PDF"
        item.nombre.matches(Regex(".*\\.(png|jpe?g|webp|heic|bmp)$", RegexOption.IGNORE_CASE)) -> "IMG"
        item.nombre.startsWith("Codigo_") || item.nombre.contains("QR", true) -> "QR"
        item.nombre.matches(Regex(".*\\.(xlsx?|csv)$", RegexOption.IGNORE_CASE)) -> "XLS"
        else -> "DOC"
    }

    // Se prefiere la copia publica: sigue existiendo aunque la app se reinstale.
    val destino = item.publica ?: item.ruta

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVer() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniaturaHistorial(item)
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
            OpcionMenu("Ver documento") { onVer() }
            OpcionMenu("Cambiar nombre") { onRenombrar() }
            OpcionMenu("Compartir") { onCompartir() }
            OpcionMenu("Quitar del historial") { onEliminar() }
        }
    }
}

/**
 * Miniatura del archivo usando el cache del visor. Si todavia se esta generando
 * o el tipo no se puede dibujar, se queda el icono de siempre.
 */
@Composable
private fun MiniaturaHistorial(item: HistorialEntity) {
    val contexto = LocalContext.current
    val mapa by produceState<Bitmap?>(initialValue = null, item.id, item.creadoEn) {
        value = withContext(Dispatchers.IO) {
            val archivo = File(item.ruta)
            if (!archivo.exists()) return@withContext null
            val miniatura = CacheVisor.miniatura(contexto, archivo)
            miniatura?.let { BitmapFactory.decodeFile(it.absolutePath) }
        }
    }

    val imagen = mapa
    if (imagen == null) {
        ChipIcono(iconoDeArchivo(item.nombre), tamano = 44.dp)
    } else {
        Image(
            bitmap = imagen.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryLight)
        )
    }
}

