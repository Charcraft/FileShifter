package com.example.fileshifter.presentation.convert

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.model.formatearTamano
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.BotonPrincipal
import com.example.fileshifter.presentation.common.ContenidoResponsive
import com.example.fileshifter.presentation.common.FilaArchivo
import com.example.fileshifter.presentation.common.Migas
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.presentation.common.TarjetaResultado
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.presentation.common.iconoDeArchivo
import com.example.fileshifter.presentation.settings.SettingsViewModel
import com.example.fileshifter.ui.theme.Background
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.PrimaryLight
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary

/** F1 - Convertir Universal con la vista del prototipo (P3). */
@Composable
fun ConvertScreen(navController: NavController) {
    val vm: ConvertViewModel = appViewModel { ConvertViewModel(it.documentoRepository) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val ajustesVm: SettingsViewModel = appViewModel {
        SettingsViewModel(it.ajustesRepository, it.historialRepository, it.carpetaSalida)
    }
    val ajustes by ajustesVm.ajustes.collectAsStateWithLifecycle()

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        vm.onArchivosSeleccionados(uris.map(Uri::toString))
    }

    PantallaBase(titulo = "Convertir archivo", onVolver = { navController.popBackStack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Migas(listOf("Herramientas", "Convertir"))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                ContenidoResponsive {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (estado.archivos.isEmpty()) "Selecciona archivos"
                                else "${estado.archivos.size} archivo(s) seleccionado(s)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { selector.launch("*/*") }) {
                                Text("+  Añadir archivo", color = Primary)
                            }
                        }

                        if (estado.archivos.isEmpty()) {
                            Tarjeta {
                                Text(
                                    "Formatos: PDF, DOCX, XLSX, PPTX, TXT, CSV, HTML e imágenes.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            Tarjeta(padding = 0.dp) {
                                estado.archivos.forEachIndexed { indice, archivo ->
                                    FilaArchivo(
                                        nombre = archivo.nombre,
                                        detalle = "${formatearTamano(archivo.tamanoBytes)} · ${archivo.tipo.name}",
                                        icono = iconoDeArchivo(archivo.nombre),
                                        onQuitar = { vm.quitar(archivo.uri) }
                                    )
                                    if (indice < estado.archivos.lastIndex) {
                                        HorizontalDivider(color = Border)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Tarjeta {
                            Text(
                                "Convertir a:",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (estado.salidasDisponibles.isEmpty()) {
                                    Text(
                                        "Añade un archivo para ver los formatos disponibles",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                                estado.salidasDisponibles.forEach { formato ->
                                    val activo = estado.objetivo == formato
                                    Surface(
                                        onClick = { vm.onObjetivoElegido(formato) },
                                        shape = MaterialTheme.shapes.small,
                                        color = if (activo) PrimaryLight else Background,
                                        border = BorderStroke(1.dp, if (activo) Primary else Border)
                                    ) {
                                        Text(
                                            formato.etiqueta,
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (activo) Primary else TextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Tarjeta {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "No preguntar validación",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        "Ir directo al historial al convertir",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = ajustes.noPreguntarMas,
                                    onCheckedChange = ajustesVm::setNoPreguntarMas
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
                                onInicio = { navController.popBackStack() },
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
                        texto = if (estado.objetivo == null) "Selecciona un formato"
                        else "Convertir a ${estado.objetivo!!.etiqueta}",
                        enabled = estado.archivos.isNotEmpty() && estado.objetivo != null && !estado.cargando,
                        onClick = { vm.convertir() }
                    )
                }
            }
        }
    }
}
