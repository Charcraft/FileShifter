package com.example.fileshifter.presentation.common

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.R
import com.example.fileshifter.data.model.ResultadoConversion
import com.example.fileshifter.data.source.Archivos
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.settings.SettingsViewModel
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.InfoStrip
import com.example.fileshifter.ui.theme.Navy
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.PrimaryLight
import com.example.fileshifter.ui.theme.Surface
import com.example.fileshifter.ui.theme.SurfaceMuted
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary
import java.io.File

// ------------------------------------------------------------------ Barra superior

/**
 * Barra azul marino del prototipo: botón de regreso cuadrado, título en blanco
 * y (en Inicio) el logotipo de la app.
 */
@Composable
fun BarraSuperior(
    titulo: String,
    onVolver: (() -> Unit)? = null,
    mostrarLogo: Boolean = false,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    Surface(color = Navy) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onVolver != null) {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    IconButton(onClick = onVolver, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                }
                Spacer(Modifier.width(11.dp))
            }
            if (mostrarLogo) {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_logo_blanco),
                        contentDescription = "FileShifter",
                        tint = Color.White,
                        modifier = Modifier.padding(5.dp).size(30.dp)
                    )
                }
                Spacer(Modifier.width(11.dp))
            }
            Text(
                text = titulo,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            acciones()
        }
    }
}

// ------------------------------------------------------------------ Estructura

/**
 * Scaffold base con barra superior y barra inferior opcional.
 * Los insets se manejan dentro de cada barra, por eso el Scaffold no los suma
 * otra vez (así la app se ve bien de borde a borde en cualquier dispositivo).
 */
@Composable
fun PantallaBase(
    titulo: String,
    onVolver: (() -> Unit)? = null,
    mostrarLogo: Boolean = false,
    acciones: @Composable RowScope.() -> Unit = {},
    barraInferior: @Composable () -> Unit = {},
    contenido: @Composable (PaddingValues) -> Unit
) {
    SistemaBarras(iconosOscuros = false)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            BarraSuperior(
                titulo = titulo,
                onVolver = onVolver,
                mostrarLogo = mostrarLogo,
                acciones = acciones
            )
        },
        bottomBar = barraInferior
    ) { padding -> contenido(padding) }
}

/** Centra el contenido en pantallas grandes (tablet, plegables, horizontal). */
@Composable
fun ContenidoResponsive(
    modifier: Modifier = Modifier,
    anchoMax: Dp = 620.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = anchoMax).fillMaxWidth(),
            content = content
        )
    }
}

// ------------------------------------------------------------------ Tiras de información

/** Franja celeste bajo la barra: "• 100% Offline · Sin límites". */
@Composable
fun TiraInfo(partes: List<Pair<String, Boolean>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InfoStrip)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(Primary, CircleShape)
        )
        partes.forEachIndexed { indice, (texto, destacado) ->
            if (indice > 0) {
                Text("·", color = TextMuted, fontSize = 13.sp)
            }
            Text(
                text = texto,
                color = if (destacado) Primary else TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (destacado) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Migas de pan: "Herramientas › Convertir". */
@Composable
fun Migas(partes: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InfoStrip)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        partes.forEachIndexed { indice, texto ->
            if (indice > 0) {
                Text("›", color = TextMuted, modifier = Modifier.padding(horizontal = 8.dp))
            }
            Text(
                text = texto,
                color = if (indice == partes.lastIndex) Primary else TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (indice == partes.lastIndex) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

/** Pasos numerados: 1 Plantilla — 2 Datos — 3 Vista previa. */
@Composable
fun Pasos(pasos: List<String>, actual: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        pasos.forEachIndexed { indice, texto ->
            val numero = indice + 1
            val activo = numero <= actual
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        if (activo) Primary else SurfaceMuted,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$numero",
                    color = if (activo) Color.White else TextMuted,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.width(7.dp))
            Text(
                text = texto,
                color = if (numero == actual) Primary else TextMuted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (numero == actual) FontWeight.SemiBold else FontWeight.Normal
            )
            if (indice < pasos.lastIndex) {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .height(1.dp)
                        .background(Border)
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Tarjetas y controles

@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
fun EncabezadoSeccion(
    titulo: String,
    accion: String? = null,
    onAccion: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (accion != null && onAccion != null) {
            TextButton(onClick = onAccion) {
                Text(accion, color = Primary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Cuadro celeste con el icono de la función (como en el prototipo). */
@Composable
fun ChipIcono(
    icono: ImageVector,
    modifier: Modifier = Modifier,
    tamano: Dp = 46.dp,
    fondo: Color = PrimaryLight,
    tinte: Color = Primary
) {
    Box(
        modifier
            .size(tamano)
            .background(fondo, RoundedCornerShape(tamano * 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = tinte, modifier = Modifier.size(tamano * 0.52f))
    }
}

@Composable
fun BotonPrincipal(
    texto: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = Color.White,
            disabledContainerColor = SurfaceMuted,
            disabledContentColor = TextMuted
        )
    ) {
        Text(texto, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp))
    }
}

@Composable
fun BotonSecundario(
    texto: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = SurfaceMuted,
            contentColor = TextSecondary
        ),
        elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
    ) {
        Text(texto, style = MaterialTheme.typography.titleMedium)
    }
}

/** Etiqueta pequeña: PDF, IMG, QR... */
@Composable
fun EtiquetaTipo(texto: String) {
    Surface(color = PrimaryLight, shape = RoundedCornerShape(6.dp)) {
        Text(
            texto,
            color = Primary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MenuTresPuntos(contenido: @Composable ColumnScope.() -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones", tint = TextMuted)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            Column { contenido() }
        }
    }
}

@Composable
fun OpcionMenu(texto: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(texto) }, onClick = onClick)
}

/** Fila de archivo con icono, nombre, detalle y botón de quitar. */
@Composable
fun FilaArchivo(
    nombre: String,
    detalle: String,
    icono: ImageVector,
    onQuitar: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    etiqueta: String? = null
) {
    val clicavel = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clicavel)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChipIcono(icono, tamano = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (etiqueta != null) {
                    EtiquetaTipo(etiqueta)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onQuitar != null) {
            Spacer(Modifier.width(8.dp))
            Surface(
                color = SurfaceMuted,
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.size(30.dp)
            ) {
                IconButton(onClick = onQuitar, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Quitar",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Navegación inferior

@Composable
fun BarraInferior(navController: NavController, rutaActual: String) {
    NavigationBar(
        containerColor = Surface,
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            Triple("Inicio", Icons.Filled.Home, Screen.Home.route),
            Triple("Editar", Icons.Filled.Edit, Screen.EditPicker.route),
            Triple("Escanear", Icons.Filled.DocumentScanner, Screen.Scanner.route),
            Triple("QR", Icons.Filled.QrCode, Screen.Qr.route),
            Triple("Historial", Icons.Filled.History, Screen.History.route)
        )
        items.forEach { (texto, icono, ruta) ->
            val seleccionado = when (ruta) {
                Screen.EditPicker.route -> rutaActual.startsWith("edit")
                Screen.Scanner.route -> rutaActual == Screen.Scanner.route
                else -> rutaActual == ruta
            }
            NavigationBarItem(
                selected = seleccionado,
                onClick = { if (!seleccionado) navController.navegarA(ruta) },
                icon = { Icon(icono, contentDescription = texto, modifier = Modifier.size(24.dp)) },
                label = { Text(texto, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    selectedTextColor = Primary,
                    indicatorColor = PrimaryLight,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}

/** Navega sin apilar copias de la misma pantalla. */
fun NavController.navegarA(ruta: String) {
    navigate(ruta) {
        launchSingleTop = true
        popUpTo(Screen.Home.route) { saveState = true }
        restoreState = true
    }
}

// ------------------------------------------------------------------ Resultado

@Composable
fun TarjetaResultado(
    resultado: ResultadoConversion,
    onValidar: (() -> Unit)? = null,
    onInicio: (() -> Unit)? = null,
    onCorregir: (() -> Unit)? = null,
    onEntrega: (() -> Unit)? = null
) {
    val contexto = LocalContext.current
    val ajustes by appViewModel {
        SettingsViewModel(it.ajustesRepository, it.historialRepository, it.carpetaSalida)
    }.ajustes.collectAsStateWithLifecycle()
    val mostrarCorrector = onCorregir != null && ajustes.sugerirCorrector && !ajustes.noPreguntarMas
    val mostrarEntrega = onEntrega != null && ajustes.sugerirEntrega && !ajustes.noPreguntarMas
    val mostrarValidador = onValidar != null && ajustes.sugerirValidador && !ajustes.noPreguntarMas

    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChipIcono(
                icono = if (resultado.exito) Icons.Filled.CheckCircle else Icons.Filled.MoreVert,
                tamano = 40.dp,
                fondo = if (resultado.exito) PrimaryLight else MaterialTheme.colorScheme.errorContainer,
                tinte = if (resultado.exito) Primary else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (resultado.exito) "Listo" else "No se pudo completar",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    resultado.mensaje,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        resultado.archivos.take(4).forEach { ruta ->
            Spacer(Modifier.height(6.dp))
            Text(
                File(ruta).name,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (resultado.archivos.isNotEmpty()) {
                TextButton(
                    onClick = { compartirArchivo(contexto, resultado.archivos.first()) },
                    contentPadding = PaddingValues(0.dp)
                ) { Text("Compartir", color = Primary) }
            }
            if (mostrarCorrector) {
                TextButton(onClick = onCorregir!!, contentPadding = PaddingValues(0.dp)) {
                    Text("Corregir", color = Primary)
                }
            }
            if (mostrarEntrega) {
                TextButton(onClick = onEntrega!!, contentPadding = PaddingValues(0.dp)) {
                    Text("Entrega", color = Primary)
                }
            }
            if (mostrarValidador) {
                TextButton(onClick = onValidar!!, contentPadding = PaddingValues(0.dp)) {
                    Text("Validar", color = Primary)
                }
            }
            if (onInicio != null) {
                TextButton(onClick = onInicio, contentPadding = PaddingValues(0.dp)) {
                    Text("Inicio", color = TextMuted)
                }
            }
        }
    }
}

/** Comparte un archivo generado con FileProvider. */
fun compartirArchivo(context: Context, ruta: String) {
    runCatching {
        val uri = Archivos.uriCompartir(context, ruta)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = when (File(ruta).extension.lowercase()) {
                "pdf" -> "application/pdf"
                "txt" -> "text/plain"
                "png" -> "image/png"
                else -> "*/*"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir archivo"))
    }
}
