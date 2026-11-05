package com.example.fileshifter.presentation.validator

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.model.Semaforo
import com.example.fileshifter.presentation.common.Aviso
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.appViewModel

/** F6 - Validador: semáforo de tamaño/páginas y QR con hash verificable. */
@Composable
fun ValidatorScreen(navController: NavController) {
    val vm: ValidatorViewModel = appViewModel { ValidatorViewModel(it.documentoRepository) }
    val estado by vm.estado.collectAsStateWithLifecycle()

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.onPdfSeleccionado(it.toString()) }
    }

    PantallaBase(titulo = "Validador", onVolver = { navController.popBackStack() }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Aviso("Elige un PDF. Se revisa el peso, las páginas y se genera un QR con el hash SHA-256 para verificarlo sin conexión.")
            }
            item {
                Button(onClick = { selector.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (estado.archivo == null) "Elegir PDF" else "Cambiar PDF")
                }
                if (estado.analizando) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            estado.error?.let { mensaje ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(mensaje, Modifier.padding(12.dp))
                    }
                }
            }
            estado.reporte?.let { reporte ->
                item {
                    val (color, etiqueta) = when (reporte.semaforo) {
                        Semaforo.VERDE -> MaterialTheme.colorScheme.primary to "VERDE · Listo para entregar"
                        Semaforo.AMARILLO -> Color(0xFFB45309) to "AMARILLO · Revisa las advertencias"
                        Semaforo.ROJO -> MaterialTheme.colorScheme.error to "ROJO · Archivo no válido"
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(reporte.nombre, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(etiqueta, color = color, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${reporte.paginas} página(s) · ${reporte.tamanoLegible} · ${reporte.ancho.toInt()} x ${reporte.alto.toInt()} pt",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "SHA-256: ${reporte.hash.take(32)}…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(reporte.chequeos) { chequeo ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (chequeo.correcto) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            contentDescription = null,
                            tint = if (chequeo.correcto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(chequeo.nombre, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                chequeo.detalle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                estado.qr?.let { qr ->
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    bitmap = qr.asImageBitmap(),
                                    contentDescription = "Código QR con el hash del PDF",
                                    modifier = Modifier.size(220.dp)
                                )
                                Text("Escanea para verificar el hash", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
