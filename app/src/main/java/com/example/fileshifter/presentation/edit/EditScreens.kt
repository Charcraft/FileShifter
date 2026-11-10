package com.example.fileshifter.presentation.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.model.TipoEdicion
import com.example.fileshifter.data.model.formatearTamano
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.BotonPrincipal
import com.example.fileshifter.presentation.common.CampoTexto
import com.example.fileshifter.presentation.common.ChipIcono
import com.example.fileshifter.presentation.common.ContenidoResponsive
import com.example.fileshifter.presentation.common.FilaArchivo
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.Pasos
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.presentation.common.TarjetaResultado
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.presentation.common.iconoDeArchivo
import com.example.fileshifter.ui.theme.Background
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.PrimaryLight
import com.example.fileshifter.ui.theme.SurfaceMuted
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary

@Composable
private fun editViewModel(navController: NavController): EditFlowViewModel {
    val padre = remember(navController) { navController.getBackStackEntry(Screen.EditGraph.route) }
    return appViewModel(padre) { EditFlowViewModel(it.documentoRepository) }
}

private fun iconoEdicion(tipo: TipoEdicion): ImageVector = when (tipo) {
    TipoEdicion.UNIR -> Icons.Filled.MergeType
    TipoEdicion.DIVIDIR -> Icons.Filled.CallSplit
    TipoEdicion.COMPRIMIR -> Icons.Filled.Compress
    TipoEdicion.ROTAR -> Icons.Filled.Rotate90DegreesCcw
    TipoEdicion.ELIMINAR -> Icons.Filled.Delete
    TipoEdicion.REORDENAR -> Icons.Filled.SwapVert
    TipoEdicion.MARCA_AGUA -> Icons.Filled.BrandingWatermark
}

// ------------------------------------------------------------------ Paso 1

@Composable
fun EditPickerScreen(navController: NavController) {
    val vm = editViewModel(navController)
    val estado by vm.estado.collectAsStateWithLifecycle()

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        vm.onArchivosSeleccionados(uris.map(Uri::toString))
    }

    PantallaBase(titulo = "Editar PDF", onVolver = { navController.popBackStack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Pasos(listOf("Seleccionar PDF", "Tipo edición", "Editar"), actual = 1)

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                ContenidoResponsive {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Selecciona uno o más PDFs para editar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (estado.archivos.isEmpty()) "Sin archivos"
                                else "${estado.archivos.size} archivo(s)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { selector.launch("application/pdf") }) {
                                Text("+  Añadir PDF", color = Primary)
                            }
                        }
                        Spacer(Modifier.height(6.dp))

                        if (estado.archivos.isEmpty()) {
                            Tarjeta {
                                Text(
                                    "Elige uno o varios PDF. Para unir se necesitan al menos dos.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            Tarjeta(padding = 0.dp) {
                                estado.archivos.forEachIndexed { indice, archivo ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            Modifier
                                                .size(22.dp)
                                                .background(Primary, androidx.compose.foundation.shape.RoundedCornerShape(7.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.material3.Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        ChipIcono(iconoDeArchivo(archivo.nombre), tamano = 44.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                archivo.nombre,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary,
                                                maxLines = 1
                                            )
                                            Text(
                                                formatearTamano(archivo.tamanoBytes),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                            )
                                        }
                                        TextButton(onClick = { vm.quitar(archivo.uri) }) {
                                            Text("Quitar", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    if (indice < estado.archivos.lastIndex) {
                                        HorizontalDivider(color = Border)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            Surface(color = Background) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    BotonPrincipal(
                        texto = if (estado.archivos.isEmpty()) "Siguiente"
                        else "Siguiente (${estado.archivos.size} PDF)  ›",
                        enabled = estado.archivos.isNotEmpty(),
                        onClick = { navController.navigate(Screen.EditType.route) }
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Paso 2

@Composable
fun EditTypeScreen(navController: NavController) {
    val vm = editViewModel(navController)
    val estado by vm.estado.collectAsStateWithLifecycle()

    PantallaBase(titulo = "Tipo de edición", onVolver = { navController.popBackStack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Pasos(listOf("Seleccionar PDF", "Tipo edición", "Editar"), actual = 2)

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                ContenidoResponsive {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "¿Qué quieres hacer con el PDF?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(14.dp))

                        TipoEdicion.entries.chunked(2).forEach { fila ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                fila.forEach { tipo ->
                                    TarjetaTipo(
                                        tipo = tipo,
                                        seleccionado = estado.tipo == tipo,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        vm.onTipoElegido(tipo)
                                        navController.navigate(Screen.EditMode.route)
                                    }
                                }
                                if (fila.size == 1) Spacer(Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaTipo(
    tipo: TipoEdicion,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (seleccionado) PrimaryLight else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (seleccionado) Primary else Border)
    ) {
        Column(Modifier.padding(14.dp)) {
            ChipIcono(iconoEdicion(tipo), tamano = 42.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                if (tipo == TipoEdicion.ELIMINAR) "Eliminar" else tipo.etiqueta,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                tipo.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                minLines = 2
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Seleccionar  ›",
                style = MaterialTheme.typography.labelLarge,
                color = Primary
            )
        }
    }
}

// ------------------------------------------------------------------ Paso 3

@Composable
fun EditModeScreen(navController: NavController) {
    val vm = editViewModel(navController)
    val estado by vm.estado.collectAsStateWithLifecycle()
    val tipo = estado.tipo

    PantallaBase(titulo = tipo?.etiqueta ?: "Editar", onVolver = { navController.popBackStack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Pasos(listOf("PDF", "Tipo", tipo?.etiqueta ?: "Editar"), actual = 3)

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                ContenidoResponsive {
                    Column(Modifier.padding(16.dp)) {
                        Tarjeta {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ChipIcono(iconoEdicion(tipo ?: TipoEdicion.UNIR), tamano = 44.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        tipo?.etiqueta ?: "Sin tipo",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        "${estado.archivos.size} PDF seleccionado(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        when (tipo) {
                            TipoEdicion.ROTAR -> Tarjeta {
                                Text("Grados a girar", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(10.dp))
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

                            TipoEdicion.ELIMINAR -> Tarjeta {
                                Text("Páginas a eliminar", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                CampoTexto(
                                    valor = estado.opciones.paginas,
                                    onCambia = vm::onPaginas,
                                    etiqueta = "Ejemplo: 2,4-6"
                                )
                            }

                            TipoEdicion.REORDENAR -> Tarjeta {
                                Text("Nuevo orden de páginas", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                CampoTexto(
                                    valor = estado.opciones.orden,
                                    onCambia = vm::onOrden,
                                    etiqueta = "Ejemplo: 3,1,2"
                                )
                            }

                            TipoEdicion.MARCA_AGUA -> Tarjeta {
                                Text("Texto de la marca", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                CampoTexto(
                                    valor = estado.opciones.textoMarcaAgua,
                                    onCambia = vm::onMarcaAgua,
                                    etiqueta = "Ejemplo: COPIA"
                                )
                            }

                            else -> Tarjeta {
                                Text(
                                    when (tipo) {
                                        TipoEdicion.UNIR -> "Se combinarán ${estado.archivos.size} PDF en uno solo."
                                        TipoEdicion.DIVIDIR -> "Se generará un PDF por cada página."
                                        TipoEdicion.COMPRIMIR -> "Se reducirá el peso del archivo manteniendo el contenido."
                                        else -> "Pulsa Aplicar para continuar."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (estado.cargando) {
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }

                        estado.resultado?.let {
                            Spacer(Modifier.height(12.dp))
                            TarjetaResultado(
                                resultado = it,
                                onValidar = { navController.navigate(Screen.Validator.route) },
                                onInicio = { navController.navigate(Screen.Home.route) },
                                onCorregir = { navController.navigate(Screen.Corrector.route) },
                                onEntrega = { navController.navigate(Screen.Delivery1.route) }
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            Surface(color = Background) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    BotonPrincipal(
                        texto = if (estado.cargando) "Aplicando…" else "Aplicar · ${tipo?.etiqueta ?: ""}",
                        enabled = tipo != null && !estado.cargando && estado.archivos.isNotEmpty(),
                        onClick = { vm.aplicar() }
                    )
                }
            }
        }
    }
}
