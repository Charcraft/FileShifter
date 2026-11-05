package com.example.fileshifter.presentation.validator

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.model.Semaforo
import com.example.fileshifter.presentation.common.BotonPrincipal
import com.example.fileshifter.presentation.common.BotonSecundario
import com.example.fileshifter.presentation.common.ContenidoResponsive
import com.example.fileshifter.presentation.common.EncabezadoSeccion
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.ui.theme.Amber
import com.example.fileshifter.ui.theme.AmberBg
import com.example.fileshifter.ui.theme.AmberText
import com.example.fileshifter.ui.theme.Background
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.PrimaryLight
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary

/** F6 - Validador con la vista del prototipo (P12). */
@Composable
fun ValidatorScreen(navController: NavController) {
    val vm: ValidatorViewModel = appViewModel { ValidatorViewModel(it.documentoRepository) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = androidx.compose.ui.platform.LocalContext.current

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.onPdfSeleccionado(it.toString()) }
    }

    PantallaBase(titulo = "Validador", onVolver = { navController.popBackStack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                ContenidoResponsive {
                    Column(Modifier.padding(16.dp)) {

                        if (estado.reporte == null) {
                            Tarjeta {
                                Text(
                                    "Elige un PDF para revisar el peso, las páginas y generar su QR de entrega.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(12.dp))
                                BotonPrincipal(
                                    texto = if (estado.analizando) "Analizando…" else "Elegir PDF",
                                    enabled = !estado.analizando,
                                    onClick = { selector.launch("application/pdf") }
                                )
                            }
                        }

                        if (estado.analizando) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }

                        estado.error?.let {
                            Spacer(Modifier.height(12.dp))
                            Tarjeta {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        estado.reporte?.let { reporte ->
                            val (colorFondo, colorBorde, colorTexto, titulo, subtitulo, icono) = when (reporte.semaforo) {
                                Semaforo.VERDE -> Quinteto(
                                    PrimaryLight, Primary, Primary,
                                    "Documento válido", "Listo para entregar", Icons.Filled.Check
                                )
                                Semaforo.AMARILLO -> Quinteto(
                                    AmberBg, Amber, AmberText,
                                    "Revisa las advertencias", "Hay detalles por corregir", Icons.Filled.Warning
                                )
                                Semaforo.ROJO -> Quinteto(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.error,
                                    MaterialTheme.colorScheme.error,
                                    "Archivo no válido", "No se pudo leer el PDF", Icons.Filled.Warning
                                )
                            }

                            Surface(
                                color = colorFondo,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.5.dp, colorBorde),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier.size(52.dp).background(colorBorde, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icono, contentDescription = null, tint = Color.White)
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column {
                                        Text(titulo, style = MaterialTheme.typography.titleLarge, color = colorTexto)
                                        Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = colorTexto)
                                    }
                                }
                            }

                            Spacer(Modifier.height(18.dp))
                            EncabezadoSeccion("Revisión automática")
                            Spacer(Modifier.height(8.dp))
                            Tarjeta(padding = 0.dp) {
                                reporte.chequeos.forEachIndexed { indice, chequeo ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            Modifier
                                                .size(38.dp)
                                                .background(
                                                    if (chequeo.correcto) PrimaryLight else AmberBg,
                                                    MaterialTheme.shapes.small
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                if (chequeo.correcto) Icons.Filled.Check else Icons.Filled.Warning,
                                                contentDescription = null,
                                                tint = if (chequeo.correcto) Primary else Amber,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(chequeo.nombre, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                            Text(
                                                chequeo.detalle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                    if (indice < reporte.chequeos.lastIndex) HorizontalDivider(color = Border)
                                }
                            }

                            Spacer(Modifier.height(18.dp))
                            EncabezadoSeccion("QR de entrega")
                            Spacer(Modifier.height(8.dp))
                            Tarjeta {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    estado.qr?.let { qr ->
                                        Image(
                                            bitmap = qr.asImageBitmap(),
                                            contentDescription = "QR del hash del PDF",
                                            modifier = Modifier.size(104.dp)
                                        )
                                        Spacer(Modifier.width(14.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            reporte.nombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text("Hash SHA-256", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        Text(
                                            "${reporte.hash.take(6)}…${reporte.hash.takeLast(4)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Etiqueta("PDF")
                                            Etiqueta(reporte.tamanoLegible)
                                            Etiqueta("${reporte.paginas} págs.")
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            androidx.compose.material3.TextButton(onClick = { selector.launch("application/pdf") }) {
                                Text("Elegir otro PDF", color = Primary)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            Surface(color = Background) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BotonSecundario(
                        texto = "Omitir",
                        modifier = Modifier.weight(1f),
                        onClick = { navController.popBackStack() }
                    )
                    BotonPrincipal(
                        texto = "Compartir",
                        modifier = Modifier.weight(1.6f),
                        enabled = estado.reporte != null,
                        onClick = {
                            val reporte = estado.reporte ?: return@BotonPrincipal
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "FileShifter · ${reporte.nombre}\nHash SHA-256: ${reporte.hash}\n" +
                                        "${reporte.paginas} páginas · ${reporte.tamanoLegible}"
                                )
                            }
                            contexto.startActivity(Intent.createChooser(intent, "Compartir"))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Surface(color = PrimaryLight, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            texto,
            color = Primary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private data class Quinteto(
    val fondo: Color,
    val borde: Color,
    val texto: Color,
    val titulo: String,
    val subtitulo: String,
    val icono: androidx.compose.ui.graphics.vector.ImageVector
)
