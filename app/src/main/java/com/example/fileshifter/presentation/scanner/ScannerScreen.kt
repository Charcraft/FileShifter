package com.example.fileshifter.presentation.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.BorradorTexto
import com.example.fileshifter.presentation.common.BotonPrincipal
import com.example.fileshifter.presentation.common.BotonSecundario
import com.example.fileshifter.presentation.common.CamaraPreview
import com.example.fileshifter.presentation.common.SistemaBarras
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.presentation.common.tomarFoto
import com.example.fileshifter.ui.theme.CameraBar
import com.example.fileshifter.ui.theme.CameraDark
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary
import java.io.File
import java.util.concurrent.Executors

/** F7 - Escáner a pantalla completa como el prototipo (P13). */
@Composable
fun ScannerScreen(navController: NavController) {
    SistemaBarras(iconosOscuros = false)
    val vm: ScannerViewModel = appViewModel { ScannerViewModel(it.documentoRepository, it.ocrService) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    var auto by remember { mutableStateOf(true) }
    var capturador by remember { mutableStateOf<ImageCapture?>(null) }
    var camaraActiva by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var linterna by remember { mutableStateOf(false) }
    val ejecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { ejecutor.shutdown() } }

    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        tienePermiso = it
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.onGaleria(it.toString()) }
    }
    LaunchedEffect(Unit) {
        if (!tienePermiso) pedirPermiso.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize().background(CameraDark)) {
        if (tienePermiso) {
            CamaraPreview(Modifier.fillMaxSize()) { captura, camara ->
                capturador = captura
                camaraActiva = camara
            }
        } else {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Se necesita permiso de cámara", color = Color.White)
                Spacer(Modifier.height(10.dp))
                BotonPrincipal(texto = "Dar permiso", modifier = Modifier.width(220.dp)) {
                    pedirPermiso.launch(Manifest.permission.CAMERA)
                }
            }
        }

        // Marco de detección
        Canvas(Modifier.fillMaxSize()) {
            val puntos = listOf(
                Offset(size.width * 0.14f, size.height * 0.20f),
                Offset(size.width * 0.86f, size.height * 0.16f),
                Offset(size.width * 0.82f, size.height * 0.72f),
                Offset(size.width * 0.18f, size.height * 0.76f)
            )
            val camino = Path().apply {
                moveTo(puntos[0].x, puntos[0].y)
                puntos.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(camino, color = Primary, style = Stroke(width = 3.dp.toPx()))
            drawLine(
                color = Primary.copy(alpha = 0.35f),
                start = Offset(size.width * 0.22f, size.height * 0.47f),
                end = Offset(size.width * 0.78f, size.height * 0.45f),
                strokeWidth = 2.dp.toPx()
            )
            puntos.forEach { punto ->
                drawCircle(color = CameraDark, radius = 9.dp.toPx(), center = punto)
                drawCircle(color = Primary, radius = 6.dp.toPx(), center = punto)
            }
        }

        // Controles superiores
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(12.dp)) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(
                onClick = { auto = true },
                color = if (auto) Primary else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    "Auto",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = { auto = false },
                color = if (!auto) Primary else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    "Manual",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.weight(1f))
            Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(12.dp)) {
                IconButton(onClick = {
                    linterna = !linterna
                    camaraActiva?.cameraControl?.enableTorch(linterna)
                }) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = "Flash",
                        tint = if (linterna) Primary else Color.White
                    )
                }
            }
        }

        // Controles inferiores
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (estado.procesando) {
                Text("Leyendo texto (OCR)…", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Primary)
                Spacer(Modifier.height(10.dp))
            }

            if (estado.texto.isNotBlank()) {
                Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Texto reconocido",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                if (estado.capturas == 1) "1 página" else "${estado.capturas} páginas",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            estado.texto,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        estado.resultado?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(it.mensaje, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BotonSecundario(
                                texto = "Otra foto",
                                modifier = Modifier.weight(1f),
                                onClick = { vm.limpiar() }
                            )
                            BotonPrincipal(
                                texto = "Generar PDF",
                                modifier = Modifier.weight(1.4f),
                                enabled = !estado.generando,
                                onClick = { vm.generarPdf() }
                            )
                        }
                        TextButton(
                            onClick = {
                                BorradorTexto.texto = estado.texto
                                navController.navigate(Screen.Corrector.route)
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Corregir texto", color = Primary)
                        }
                    }
                }
            } else {
                if (!estado.procesando) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            "Documento detectado",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (estado.capturas > 0) {
                        Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                            Text(
                                "  ${estado.capturas} página(s) capturada(s)  ",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = CameraBar,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        IconButton(onClick = { galeria.launch("image/*") }) {
                            Icon(Icons.Filled.Image, contentDescription = "Galería", tint = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .background(Color.White, CircleShape)
                            .clickable(
                                onClickLabel = "Capturar página",
                                role = androidx.compose.ui.semantics.Role.Button
                            ) {
                                val captura = capturador ?: return@clickable
                                val archivo = File(contexto.cacheDir, "captura_${System.currentTimeMillis()}.jpg")
                                tomarFoto(captura, ejecutor, archivo) { vm.onFotoCapturada(it) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(60.dp).background(Primary, CircleShape))
                    }

                    Spacer(Modifier.size(52.dp))
                }
            }
        }
    }
}
