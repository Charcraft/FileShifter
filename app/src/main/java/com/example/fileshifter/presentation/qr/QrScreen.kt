package com.example.fileshifter.presentation.qr

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.local.HistorialEntity
import com.example.fileshifter.data.model.formatearTamano
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.BarraInferior
import com.example.fileshifter.presentation.common.BotonPrincipal
import com.example.fileshifter.presentation.common.BotonSecundario
import com.example.fileshifter.presentation.common.CamaraCodigos
import com.example.fileshifter.presentation.common.CampoNeumorph
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
import com.example.fileshifter.presentation.common.navegarA
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.CameraDark
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.PrimaryLight
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary
import java.io.File

/** F8 - Lector y generador de QR con las pestañas del prototipo (P14). */
@Composable
fun QrScreen(navController: NavController) {
    val vm: QrViewModel = appViewModel { QrViewModel(it.documentoRepository, it.historialRepository, it.ocrService) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val codigos by vm.codigos.collectAsStateWithLifecycle()
    var pestana by remember { mutableIntStateOf(0) }
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.onGaleria(it.toString()) }
    }

    PantallaBase(
        titulo = "FileShifter",
        mostrarLogo = true,
        acciones = {
            IconButton(onClick = { navController.navegarA(Screen.Settings.route) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = Color.White)
            }
        },
        barraInferior = { BarraInferior(navController, Screen.Qr.route) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = pestana,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Primary,
                divider = { HorizontalDivider(color = Border) },
                indicator = { posiciones ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(posiciones[pestana]),
                        color = Primary
                    )
                }
            ) {
                listOf("Escanear", "Generar", "Historial").forEachIndexed { indice, titulo ->
                    Tab(
                        selected = pestana == indice,
                        onClick = { pestana = indice },
                        text = {
                            Text(
                                titulo,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (pestana == indice) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = Primary,
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            when (pestana) {
                0 -> EscanearTab(
                    escaneado = estado.escaneado,
                    leyendo = estado.leyendoGaleria,
                    error = estado.error,
                    onGaleria = { galeria.launch("image/*") },
                    onUsar = {
                        vm.usarTextoEscaneado()
                        pestana = 1
                    },
                    onCodigo = vm::onCodigoEscaneado
                )
                1 -> GenerarTab(
                    texto = estado.texto,
                    esQr = estado.esQr,
                    generando = estado.generando,
                    rutaGenerada = estado.rutaGenerada,
                    error = estado.error,
                    onTexto = vm::onTextoCambia,
                    onTipo = vm::onTipoCambia,
                    onGenerar = vm::generar
                )
                else -> HistorialTab(
                    codigos = codigos,
                    onVerTodo = { navController.navegarA(Screen.History.route) },
                    onCompartir = { ruta -> compartirArchivo(contexto, ruta) }
                )
            }
        }
    }
}

@Composable
private fun EscanearTab(
    escaneado: String?,
    leyendo: Boolean,
    error: String?,
    onGaleria: () -> Unit,
    onUsar: () -> Unit,
    onCodigo: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        ContenidoResponsive {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CameraDark)
                ) {
                    CamaraCodigos(Modifier.fillMaxSize(), onCodigo = onCodigo)
                    Canvas(Modifier.fillMaxSize()) {
                        val margen = 28.dp.toPx()
                        val largo = 30.dp.toPx()
                        val color = Primary
                        val grosor = 3.dp.toPx()
                        // Esquinas superiores
                        drawLine(color, Offset(margen, margen + largo), Offset(margen, margen), grosor)
                        drawLine(color, Offset(margen, margen), Offset(margen + largo, margen), grosor)
                        drawLine(color, Offset(size.width - margen - largo, margen), Offset(size.width - margen, margen), grosor)
                        drawLine(color, Offset(size.width - margen, margen), Offset(size.width - margen, margen + largo), grosor)
                        // Esquinas inferiores
                        drawLine(color, Offset(margen, size.height - margen - largo), Offset(margen, size.height - margen), grosor)
                        drawLine(color, Offset(margen, size.height - margen), Offset(margen + largo, size.height - margen), grosor)
                        drawLine(color, Offset(size.width - margen - largo, size.height - margen), Offset(size.width - margen, size.height - margen), grosor)
                        drawLine(color, Offset(size.width - margen, size.height - margen), Offset(size.width - margen, size.height - margen - largo), grosor)
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(18.dp)
                    ) {
                        Text(
                            "Apunta al código QR o de barras",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                BotonSecundario(
                    texto = if (leyendo) "Leyendo imagen…" else "Abrir desde galería",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onGaleria
                )

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (escaneado != null) {
                    Spacer(Modifier.height(12.dp))
                    Tarjeta {
                        Text("Contenido detectado", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(escaneado, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(Modifier.height(10.dp))
                        BotonPrincipal(texto = "Usar para generar", onClick = onUsar)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GenerarTab(
    texto: String,
    esQr: Boolean,
    generando: Boolean,
    rutaGenerada: String?,
    error: String?,
    onTexto: (String) -> Unit,
    onTipo: (Boolean) -> Unit,
    onGenerar: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        ContenidoResponsive {
            Column {
                Tarjeta {
                    Text("Contenido del código", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    CampoNeumorph(
                        valor = texto,
                        onCambia = onTexto,
                        etiqueta = "Texto o URL",
                        minLines = 3
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = { onTipo(true) },
                            color = if (esQr) PrimaryLight else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                "QR",
                                color = if (esQr) Primary else TextSecondary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Surface(
                            onClick = { onTipo(false) },
                            color = if (!esQr) PrimaryLight else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                "Barras",
                                color = if (!esQr) Primary else TextSecondary,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    BotonPrincipal(
                        texto = if (generando) "Generando…" else "Generar",
                        enabled = texto.isNotBlank() && !generando,
                        onClick = onGenerar
                    )
                }

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                rutaGenerada?.let { ruta ->
                    Spacer(Modifier.height(12.dp))
                    val mapa = remember(ruta) { BitmapFactory.decodeFile(ruta) }
                    Tarjeta {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            mapa?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Código generado",
                                    modifier = Modifier.size(220.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Guardado en FileShifter/",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HistorialTab(
    codigos: List<HistorialEntity>,
    onVerTodo: () -> Unit,
    onCompartir: (String) -> Unit
) {
    val propios = codigos.filter { it.nombre.startsWith("Codigo_") }
    Column(Modifier.fillMaxSize()) {
        if (propios.isEmpty()) {
            ContenidoResponsive {
                Column(Modifier.padding(16.dp)) {
                    Tarjeta {
                        Text(
                            "Todavía no has generado códigos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                item {
                    ContenidoResponsive {
                        Column(Modifier.padding(16.dp)) {
                            Tarjeta(padding = 0.dp) {
                                propios.forEachIndexed { indice, item ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ChipIcono(Icons.Filled.QrCode, tamano = 42.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                item.nombre,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                EtiquetaTipo("QR")
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    "${formatearTamano(item.tamanoBytes)} · ${formatearFecha(item.creadoEn)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                        MenuTresPuntos {
                                            OpcionMenu("Compartir") { onCompartir(item.ruta) }
                                        }
                                    }
                                    if (indice < propios.lastIndex) HorizontalDivider(color = Border)
                                }
                            }
                        }
                    }
                }
            }
        }
        TextButton(
            onClick = onVerTodo,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Ver historial completo", color = Primary)
        }
    }
}
