package com.example.fileshifter.presentation.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.Aviso
import com.example.fileshifter.presentation.common.BorradorTexto
import com.example.fileshifter.presentation.common.CamaraCaptura
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.TarjetaResultado
import com.example.fileshifter.presentation.common.appViewModel

/** F7 - Escáner: foto, OCR con ML Kit y salida a PDF o al corrector. */
@Composable
fun ScannerScreen(navController: NavController) {
    val vm: ScannerViewModel = appViewModel { ScannerViewModel(it.documentoRepository, it.ocrService) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        tienePermiso = it
    }
    LaunchedEffect(Unit) {
        if (!tienePermiso) pedirPermiso.launch(Manifest.permission.CAMERA)
    }

    PantallaBase(titulo = "Escáner", onVolver = { navController.popBackStack() }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Aviso("Toma la foto centrando la hoja. El OCR lee el texto en el teléfono y puedes generar un PDF o mandarlo al corrector.")
            }
            item {
                if (tienePermiso) {
                    CamaraCaptura(
                        modifier = Modifier.fillMaxWidth().height(360.dp),
                        onCaptura = { archivo -> vm.onFotoCapturada(archivo) }
                    )
                } else {
                    Column {
                        Text("Se necesita permiso de cámara para escanear.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { pedirPermiso.launch(Manifest.permission.CAMERA) }) {
                            Text("Dar permiso")
                        }
                    }
                }
            }
            if (estado.procesando) {
                item {
                    Text("Leyendo texto (OCR)…", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            if (estado.texto.isNotBlank()) {
                item {
                    OutlinedTextField(
                        value = estado.texto,
                        onValueChange = vm::onTextoCambia,
                        label = { Text("Texto reconocido (editable)") },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.generarPdf() },
                            enabled = !estado.generando,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (estado.generando) "Generando…" else "Generar PDF") }
                        OutlinedButton(
                            onClick = {
                                BorradorTexto.texto = estado.texto
                                navController.navigate(Screen.Corrector.route)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Corregir") }
                    }
                }
            }
            estado.resultado?.let { resultado ->
                item {
                    TarjetaResultado(
                        resultado = resultado,
                        onValidar = { navController.navigate(Screen.Validator.route) },
                        onInicio = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
