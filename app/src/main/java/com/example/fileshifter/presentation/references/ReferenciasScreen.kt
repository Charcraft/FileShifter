package com.example.fileshifter.presentation.references

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.local.ReferenciaEntity
import com.example.fileshifter.data.model.Referencia
import com.example.fileshifter.data.repository.aModelo
import com.example.fileshifter.presentation.common.BotonPrincipal
import com.example.fileshifter.presentation.common.BotonSecundario
import com.example.fileshifter.presentation.common.CampoTexto
import com.example.fileshifter.presentation.common.ChipIcono
import com.example.fileshifter.presentation.common.ContenidoResponsive
import com.example.fileshifter.presentation.common.EncabezadoSeccion
import com.example.fileshifter.presentation.common.EtiquetaTipo
import com.example.fileshifter.presentation.common.Migas
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.presentation.common.TiraInfo
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.ui.theme.Amber
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.PrimaryLight
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary

/**
 * F9 - Referencias: busca bibliografia en Crossref y la guarda para citarla
 * en el Modo Entrega. Es la funcion que usa la conexion a internet de la app.
 */
@Composable
fun ReferenciasScreen(navController: NavController) {
    val vm: ReferenciasViewModel = appViewModel { ReferenciasViewModel(it.referenciasRepository) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val guardadas by vm.guardadas.collectAsStateWithLifecycle()
    val portapapeles = LocalClipboardManager.current
    var confirmarVaciar by remember { mutableStateOf(false) }

    PantallaBase(
        titulo = "Referencias",
        onVolver = { navController.popBackStack() }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Migas(listOf("Herramientas", "Referencias"))

            ContenidoResponsive {
                Column(Modifier.padding(16.dp)) {

                    Tarjeta {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChipIcono(Icons.Filled.MenuBook, tamano = 42.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Buscar bibliografía",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    "Consulta libros, artículos y tesis del catálogo abierto Crossref. " +
                                        "Lo que guardes queda en el teléfono y se puede citar sin internet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        CampoTexto(
                            valor = estado.consulta,
                            onCambia = vm::onConsulta,
                            etiqueta = "Título, autor o tema",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { vm.buscar() })
                        )
                        Spacer(Modifier.height(12.dp))
                        BotonPrincipal(
                            texto = if (estado.cargando) "Buscando…" else "Buscar en el catálogo",
                            enabled = estado.consulta.isNotBlank() && !estado.cargando
                        ) { vm.buscar() }
                    }

                    estado.aviso?.let { aviso ->
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                aviso,
                                style = MaterialTheme.typography.bodySmall,
                                color = Primary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = vm::descartarAviso) {
                                Text("Cerrar", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }

                    if (estado.cargando) {
                        Spacer(Modifier.height(16.dp))
                        Tarjeta {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Consultando el catálogo…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    estado.error?.let { mensaje ->
                        Spacer(Modifier.height(16.dp))
                        Tarjeta {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ChipIcono(
                                    Icons.Filled.CloudOff,
                                    tamano = 38.dp,
                                    fondo = PrimaryLight,
                                    tinte = Amber
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    mensaje,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            BotonSecundario(texto = "Reintentar") { vm.buscar() }
                        }
                    }

                    if (estado.sinResultados && !estado.cargando) {
                        Spacer(Modifier.height(16.dp))
                        Tarjeta {
                            Text(
                                "No se encontraron resultados para «${estado.consulta.trim()}».",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Prueba con el apellido del autor, una sola palabra del título o el DOI.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    if (estado.resultados.isNotEmpty()) {
                        Spacer(Modifier.height(22.dp))
                        EncabezadoSeccion(
                            titulo = if (estado.desdeCache) "Guardadas que coinciden" else "Resultados",
                            accion = "${estado.resultados.size}"
                        )
                        Spacer(Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            estado.resultados.forEach { referencia ->
                                TarjetaReferencia(
                                    referencia = referencia,
                                    guardada = guardadas.any { it.doi == referencia.doi },
                                    onGuardar = { vm.guardar(referencia) },
                                    onCopiar = { portapapeles.setText(AnnotatedString(referencia.citaApa())) }
                                )
                            }
                        }
                    }

                    if (guardadas.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        EncabezadoSeccion(
                            titulo = "Guardadas",
                            accion = "Vaciar",
                            onAccion = { confirmarVaciar = true }
                        )
                        Spacer(Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            guardadas.forEach { entidad ->
                                FilaGuardada(
                                    entidad = entidad,
                                    onCopiar = {
                                        portapapeles.setText(AnnotatedString(entidad.aModelo().citaApa()))
                                    },
                                    onEliminar = { vm.eliminar(entidad.doi) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Fuente: Crossref (api.crossref.org). Las citas se generan en formato APA 7.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }

    if (confirmarVaciar) {
        AlertDialog(
            onDismissRequest = { confirmarVaciar = false },
            title = { Text("Vaciar referencias guardadas") },
            text = { Text("Se eliminarán todas las referencias guardadas en este dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.limpiarGuardadas()
                    confirmarVaciar = false
                }) { Text("Vaciar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarVaciar = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TarjetaReferencia(
    referencia: Referencia,
    guardada: Boolean,
    onGuardar: () -> Unit,
    onCopiar: () -> Unit
) {
    Tarjeta(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EtiquetaTipo(referencia.tipoLegible().uppercase())
            Spacer(Modifier.width(8.dp))
            if (referencia.anio.isNotBlank()) {
                Text(
                    referencia.anio,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            Spacer(Modifier.weight(1f))
            if (guardada) {
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = "Guardada",
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            referencia.titulo,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )

        if (referencia.autores.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                referencia.autores,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        if (referencia.fuente.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                referencia.fuente,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        if (referencia.doi.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "doi:${referencia.doi}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextMuted
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onGuardar, enabled = !guardada) {
                Text(
                    if (guardada) "Guardada" else "Guardar",
                    fontSize = 13.sp,
                    color = if (guardada) TextMuted else Primary
                )
            }
            TextButton(onClick = onCopiar) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Copiar cita", fontSize = 13.sp, color = Primary)
            }
        }
    }
}

@Composable
private fun FilaGuardada(
    entidad: ReferenciaEntity,
    onCopiar: () -> Unit,
    onEliminar: () -> Unit
) {
    Tarjeta(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    entidad.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    listOf(entidad.autores, entidad.anio)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            IconButton(onClick = onCopiar) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copiar cita",
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
