package com.example.fileshifter.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.local.HistorialEntity
import com.example.fileshifter.data.model.formatearTamano
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.BarraInferior
import com.example.fileshifter.presentation.common.ChipIcono
import com.example.fileshifter.presentation.common.ContenidoResponsive
import com.example.fileshifter.presentation.common.EncabezadoSeccion
import com.example.fileshifter.presentation.common.MenuTresPuntos
import com.example.fileshifter.presentation.common.OpcionMenu
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.presentation.common.TiraInfo
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.presentation.common.compartirArchivo
import com.example.fileshifter.presentation.common.formatearFecha
import com.example.fileshifter.presentation.common.iconoDeArchivo
import com.example.fileshifter.presentation.common.navegarA
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.Surface
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary
import com.example.fileshifter.ui.theme.neumorphRaised

private data class Herramienta(
    val titulo: String,
    val subtitulo: String,
    val icono: ImageVector,
    val ruta: String?
)

/** P2 del prototipo: herramientas, recientes y navegación inferior. */
@Composable
fun HomeScreen(navController: NavController) {
    val vm: HomeViewModel = appViewModel { HomeViewModel(it.historialRepository) }
    val recientes by vm.recientes.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    val herramientas = listOf(
        Herramienta("Convertir", "PDF, DOCX, XLSX…", Icons.Filled.SwapHoriz, Screen.Convert.route),
        Herramienta("Imagen → PDF", "JPG, PNG, HEIC, WebP", Icons.Filled.Image, Screen.Image.route),
        Herramienta("Editar PDF", "Unir, dividir, rotar", Icons.Filled.Edit, Screen.EditPicker.route),
        Herramienta("Escanear", "Cámara inteligente", Icons.Filled.DocumentScanner, Screen.Scanner.route),
        Herramienta("Corrector", "Ortografía y estilo", Icons.Filled.Spellcheck, Screen.Corrector.route),
        Herramienta("Referencias", "Buscar y citar", Icons.Filled.MenuBook, Screen.Referencias.route),
        Herramienta("Modo Entrega", "Portada + plantillas", Icons.Filled.Description, Screen.Delivery1.route),
        Herramienta("QR / Barras", "Leer y generar", Icons.Filled.QrCode, Screen.Qr.route),
        Herramienta("Validador", "Verificar documento", Icons.Filled.Verified, Screen.Validator.route),
        Herramienta("Historial", "Archivos recientes", Icons.Filled.History, Screen.History.route)
    )

    PantallaBase(
        titulo = "FileShifter",
        mostrarLogo = true,
        acciones = {
            IconButton(onClick = { navController.navegarA(Screen.Settings.route) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = androidx.compose.ui.graphics.Color.White)
            }
        },
        barraInferior = { BarraInferior(navController, Screen.Home.route) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TiraInfo(
                listOf(
                    "100% Offline" to true,
                    "Sin límites" to true,
                    "Gratis para siempre" to false
                )
            )

            ContenidoResponsive {
                Column(Modifier.padding(16.dp)) {
                    EncabezadoSeccion("Herramientas")
                    Spacer(Modifier.height(12.dp))

                    BoxWithConstraints {
                        val columnas = if (maxWidth < 340.dp) 2 else 3
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            herramientas.chunked(columnas).forEach { fila ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    fila.forEach { herramienta ->
                                        TarjetaHerramienta(
                                            herramienta = herramienta,
                                            modifier = Modifier.weight(1f)
                                        ) { navController.navegarA(herramienta.ruta!!) }
                                    }
                                    repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    EncabezadoSeccion(
                        titulo = "Recientes",
                        accion = "Ver historial",
                        onAccion = { navController.navegarA(Screen.History.route) }
                    )
                    Spacer(Modifier.height(10.dp))

                    if (recientes.isEmpty()) {
                        Tarjeta {
                            Text(
                                "Todavía no hay archivos. Todo lo que generes aparecerá aquí.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    } else {
                        Tarjeta(padding = 0.dp) {
                            recientes.take(3).forEachIndexed { indice, item ->
                                FilaReciente(
                                    item = item,
                                    onCompartir = { compartirArchivo(contexto, item.ruta) },
                                    onEliminar = { vm.eliminar(item.id) }
                                )
                                if (indice < minOf(2, recientes.size - 1)) {
                                    HorizontalDivider(color = Border)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaHerramienta(
    herramienta: Herramienta,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(122.dp)
            .neumorphRaised(corner = 16.dp, elevacion = 6.dp, fondo = Surface),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = null,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            ChipIcono(herramienta.icono, tamano = 38.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                herramienta.titulo,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                herramienta.subtitulo,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 13.sp),
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilaReciente(
    item: HistorialEntity,
    onCompartir: () -> Unit,
    onEliminar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChipIcono(iconoDeArchivo(item.nombre), tamano = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.nombre,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${formatearTamano(item.tamanoBytes)} · ${formatearFecha(item.creadoEn)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        MenuTresPuntos {
            OpcionMenu("Compartir") { onCompartir() }
            OpcionMenu("Quitar del historial") { onEliminar() }
        }
    }
}
