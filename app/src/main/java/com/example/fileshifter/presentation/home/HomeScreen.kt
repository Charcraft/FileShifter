package com.example.fileshifter.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.BarraInferior
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.navegarA

private data class TarjetaInicio(
    val titulo: String,
    val subtitulo: String,
    val icono: ImageVector,
    val ruta: String
)

/**
 * Pantalla de inicio: solo presenta las 8 funciones y navega.
 * No tiene logica, por eso no necesita ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val tarjetas = listOf(
        TarjetaInicio("Convertir", "Universal", Icons.Filled.SwapHoriz, Screen.Convert.route),
        TarjetaInicio("Imagen a PDF", "Rápida", Icons.Filled.Image, Screen.Image.route),
        TarjetaInicio("Editar PDF", "Por pasos", Icons.Filled.Edit, Screen.EditPicker.route),
        TarjetaInicio("Corrector", "Ortografía", Icons.Filled.Spellcheck, Screen.Corrector.route),
        TarjetaInicio("Modo Entrega", "Plantillas", Icons.Filled.Description, Screen.Delivery1.route),
        TarjetaInicio("Validador", "QR y hash", Icons.Filled.Verified, Screen.Validator.route),
        TarjetaInicio("Escáner", "OCR", Icons.Filled.DocumentScanner, Screen.Scanner.route),
        TarjetaInicio("QR / Barras", "Lector", Icons.Filled.QrCode, Screen.Qr.route)
    )

    PantallaBase(
        titulo = "FileShifter",
        acciones = {
            IconButton(onClick = { navController.navegarA(Screen.Settings.route) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
            }
        },
        barraInferior = { BarraInferior(navController, Screen.Home.route) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "100% offline · Sin límites · 8 funciones",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tarjetas) { tarjeta ->
                    Card(
                        onClick = { navController.navegarA(tarjeta.ruta) },
                        modifier = Modifier.height(112.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(tarjeta.icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text(tarjeta.titulo, style = MaterialTheme.typography.titleMedium)
                            Text(
                                tarjeta.subtitulo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
