package com.example.fileshifter.presentation.qr

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.Aviso
import com.example.fileshifter.presentation.common.BarraInferior
import com.example.fileshifter.presentation.common.CamaraCodigos
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.appViewModel

/** F8 - Lector y generador de códigos QR y de barras. */
@Composable
fun QrScreen(navController: NavController) {
    val vm: QrViewModel = appViewModel { QrViewModel(it.documentoRepository) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    var pestana by remember { mutableIntStateOf(0) }

    PantallaBase(
        titulo = "QR / Barras",
        onVolver = { navController.popBackStack() },
        barraInferior = { BarraInferior(navController, Screen.Qr.route) }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = pestana) {
                Tab(
                    selected = pestana == 0,
                    onClick = { pestana = 0 },
                    text = { Text("Escanear") }
                )
                Tab(
                    selected = pestana == 1,
                    onClick = { pestana = 1 },
                    text = { Text("Generar") }
                )
            }

            if (pestana == 0) {
                LazyColumn(
                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Aviso("Apunta la cámara al código. La lectura es offline con ML Kit.")
                    }
                    item {
                        CamaraCodigos(
                            modifier = Modifier.fillMaxWidth().height(360.dp)
                        ) { valor -> vm.onCodigoEscaneado(valor) }
                    }
                    estado.escaneado?.let { valor ->
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Contenido detectado", style = MaterialTheme.typography.titleSmall)
                                    Text(valor, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = {
                                            vm.usarTextoEscaneado()
                                            pestana = 1
                                        }) { Text("Usar para generar") }
                                        OutlinedButton(onClick = { navController.navigate(Screen.History.route) }) {
                                            Text("Historial")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Aviso("Escribe un texto o URL. Puedes generar un QR o un código de barras CODE-128.")
                    }
                    item {
                        OutlinedTextField(
                            value = estado.texto,
                            onValueChange = vm::onTextoCambia,
                            label = { Text("Texto o URL") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = estado.esQr,
                                onClick = { vm.onTipoCambia(true) },
                                label = { Text("QR") }
                            )
                            FilterChip(
                                selected = !estado.esQr,
                                onClick = { vm.onTipoCambia(false) },
                                label = { Text("Barras") }
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = { vm.generar() },
                            enabled = estado.texto.isNotBlank() && !estado.generando,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (estado.generando) "Generando…" else "Generar") }
                    }
                    estado.error?.let { mensaje ->
                        item { Text(mensaje, color = MaterialTheme.colorScheme.error) }
                    }
                    estado.rutaGenerada?.let { ruta ->
                        item {
                            val mapa = remember(ruta) { BitmapFactory.decodeFile(ruta) }
                            Card(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    mapa?.let {
                                        Image(
                                            bitmap = it.asImageBitmap(),
                                            contentDescription = "Código generado",
                                            modifier = Modifier.size(240.dp)
                                        )
                                    }
                                    Text(
                                        "Guardado en FileShifter/",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(onClick = { navController.navigate(Screen.History.route) }) {
                                        Text("Ver en Historial")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
