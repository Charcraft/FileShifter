package com.example.fileshifter.presentation.common

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.model.ResultadoConversion
import com.example.fileshifter.data.source.Archivos
import com.example.fileshifter.navigation.Screen
import java.io.File

/** Barra inferior con los cinco destinos del flujo. */
@Composable
fun BarraInferior(navController: NavController, rutaActual: String) {
    NavigationBar {
        NavigationBarItem(
            selected = rutaActual == Screen.Home.route,
            onClick = { navController.navegarA(Screen.Home.route) },
            icon = { Icon(Icons.Filled.Home, null) },
            label = { Text("Inicio") }
        )
        NavigationBarItem(
            selected = rutaActual.startsWith("edit"),
            onClick = { navController.navegarA(Screen.EditPicker.route) },
            icon = { Icon(Icons.Filled.Edit, null) },
            label = { Text("Editar") }
        )
        NavigationBarItem(
            selected = rutaActual == Screen.Scanner.route,
            onClick = { navController.navegarA(Screen.Scanner.route) },
            icon = { Icon(Icons.Filled.DocumentScanner, null) },
            label = { Text("Escanear") }
        )
        NavigationBarItem(
            selected = rutaActual == Screen.Qr.route,
            onClick = { navController.navegarA(Screen.Qr.route) },
            icon = { Icon(Icons.Filled.QrCode, null) },
            label = { Text("QR") }
        )
        NavigationBarItem(
            selected = rutaActual == Screen.History.route,
            onClick = { navController.navegarA(Screen.History.route) },
            icon = { Icon(Icons.Filled.History, null) },
            label = { Text("Historial") }
        )
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

/** Estructura comun: barra superior, boton de regreso y contenido. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaBase(
    titulo: String,
    onVolver: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
    barraInferior: @Composable () -> Unit = {},
    contenido: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    if (onVolver != null) {
                        IconButton(onClick = onVolver) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = acciones
            )
        },
        bottomBar = barraInferior
    ) { padding -> contenido(padding) }
}

@Composable
fun Aviso(texto: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun TarjetaResultado(
    resultado: ResultadoConversion,
    onValidar: (() -> Unit)? = null,
    onInicio: (() -> Unit)? = null,
    onCorregir: (() -> Unit)? = null,
    onEntrega: (() -> Unit)? = null
) {
    val contexto = LocalContext.current
    val ajustes by appViewModel { com.example.fileshifter.presentation.settings.SettingsViewModel(it.ajustesRepository) }
        .ajustes.collectAsStateWithLifecycle()
    val mostrarCorrector = onCorregir != null && ajustes.sugerirCorrector && !ajustes.noPreguntarMas
    val mostrarEntrega = onEntrega != null && ajustes.sugerirEntrega && !ajustes.noPreguntarMas
    val mostrarValidador = onValidar != null && ajustes.sugerirValidador && !ajustes.noPreguntarMas
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (resultado.exito) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = if (resultado.exito) "Listo" else "No se pudo completar",
                style = MaterialTheme.typography.titleSmall
            )
            Text(resultado.mensaje, style = MaterialTheme.typography.bodySmall)
            resultado.archivos.take(4).forEach { ruta ->
                Text(File(ruta).name, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (resultado.archivos.isNotEmpty()) {
                    TextButton(onClick = { compartirArchivo(contexto, resultado.archivos.first()) }) {
                        Text("Compartir")
                    }
                }
                if (mostrarCorrector) {
                    TextButton(onClick = onCorregir!!) { Text("Corregir") }
                }
                if (mostrarEntrega) {
                    TextButton(onClick = onEntrega!!) { Text("Entrega") }
                }
                if (mostrarValidador) {
                    TextButton(onClick = onValidar!!) { Text("Validar") }
                }
                if (onInicio != null) {
                    TextButton(onClick = onInicio) { Text("Inicio") }
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
