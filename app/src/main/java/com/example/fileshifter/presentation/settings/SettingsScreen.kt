package com.example.fileshifter.presentation.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.model.formatearTamano
import com.example.fileshifter.presentation.common.BotonSecundario
import com.example.fileshifter.presentation.common.ChipIcono
import com.example.fileshifter.presentation.common.ContenidoResponsive
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.presentation.common.recordarPermisoCamara
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.PrimaryLight
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary

@Composable
fun SettingsScreen(navController: NavController) {
    val vm: SettingsViewModel = appViewModel {
        SettingsViewModel(it.ajustesRepository, it.historialRepository, it.carpetaSalida)
    }
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val almacenamiento by vm.almacenamiento.collectAsStateWithLifecycle()
    val permisoCamara = recordarPermisoCamara()
    var confirmar by remember { mutableStateOf(false) }

    val limite = 200L * 1024L * 1024L
    val porcentaje = (almacenamiento.toFloat() / limite).coerceIn(0f, 1f)

    PantallaBase(titulo = "Ajustes", onVolver = { navController.popBackStack() }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            ContenidoResponsive {
                Column(Modifier.padding(16.dp)) {
                    Text("Pipeline de procesamiento", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(
                        "Controla qué pasos se sugieren al convertir o escanear.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))

                    Tarjeta(padding = 0.dp) {
                        FilaAjuste(
                            icono = Icons.Filled.Spellcheck,
                            titulo = "Sugerir Corrector ortográfico",
                            detalle = "Proponer revisión al finalizar la conversión",
                            valor = ajustes.sugerirCorrector,
                            onCambia = vm::setSugerirCorrector
                        )
                        HorizontalDivider(color = Border)
                        FilaAjuste(
                            icono = Icons.Filled.Person,
                            titulo = "Sugerir Modo Entrega",
                            detalle = "Proponer plantilla académica u oficina",
                            valor = ajustes.sugerirEntrega,
                            onCambia = vm::setSugerirEntrega
                        )
                        HorizontalDivider(color = Border)
                        FilaAjuste(
                            icono = Icons.Filled.Check,
                            titulo = "Sugerir Validador",
                            detalle = "Verificar tamaño, resolución y errores",
                            valor = ajustes.sugerirValidador,
                            onCambia = vm::setSugerirValidador
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Recordar elección", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(
                        "Si activas «No preguntar más», las sugerencias dejan de aparecer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))

                    Tarjeta(padding = 0.dp) {
                        FilaAjuste(
                            icono = Icons.Filled.Description,
                            titulo = "No preguntar más",
                            detalle = if (ajustes.noPreguntarMas) "Sugerencias ocultas" else "Preguntar siempre",
                            valor = ajustes.noPreguntarMas,
                            onCambia = vm::setNoPreguntarMas,
                            inactivo = !ajustes.noPreguntarMas
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Permisos", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(
                        "La cámara se usa en el escáner y en el lector de códigos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))

                    Tarjeta(padding = 0.dp) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChipIcono(Icons.Filled.PhotoCamera, tamano = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Cámara",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    when {
                                        permisoCamara.concedido -> "Concedido"
                                        permisoCamara.bloqueado -> "Bloqueado: actívalo en los ajustes del sistema"
                                        else -> "No concedido"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (permisoCamara.concedido) Primary else TextMuted
                                )
                            }
                            TextButton(onClick = {
                                when {
                                    permisoCamara.concedido || permisoCamara.bloqueado ->
                                        permisoCamara.abrirAjustes()
                                    else -> permisoCamara.solicitar()
                                }
                            }) {
                                Text(
                                    if (permisoCamara.concedido) "Ajustes"
                                    else if (permisoCamara.bloqueado) "Abrir ajustes"
                                    else "Permitir",
                                    color = Primary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Almacenamiento", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(10.dp))

                    Tarjeta {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Archivos de FileShifter",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                formatearTamano(almacenamiento),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { porcentaje },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .background(PrimaryLight, RoundedCornerShape(4.dp)),
                            color = Primary,
                            trackColor = PrimaryLight
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${(porcentaje * 100).toInt()}% de 200 MB usados",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                "Los archivos también se copian a Documentos/FileShifter."
                            } else {
                                "Los archivos se guardan en el almacenamiento de la app y se comparten con el botón Compartir."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        BotonSecundario(
                            texto = "Liberar espacio",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { confirmar = true }
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Text(
                        "FileShifter · 253432 · Carlos de Jesús Martínez Pineda",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Text(
                        "MVVM · Compose + Material 3 · 100% offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Liberar espacio") },
            text = {
                Text(
                    "Se borrarán los archivos generados de la carpeta FileShifter/ y el historial. " +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.liberarEspacio()
                    confirmar = false
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun FilaAjuste(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    valor: Boolean,
    onCambia: (Boolean) -> Unit,
    inactivo: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = valor, onValueChange = onCambia, role = Role.Switch)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChipIcono(icono, tamano = 40.dp, fondo = PrimaryLight)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = if (inactivo) TextMuted else Primary
            )
        }
        Switch(checked = valor, onCheckedChange = null)
    }
}
