package com.example.fileshifter.presentation.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.source.Cuadrilatero
import com.example.fileshifter.data.source.FiltroEscaneo
import com.example.fileshifter.data.source.Punto
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.BarraSuperior
import com.example.fileshifter.presentation.common.BorradorTexto
import com.example.fileshifter.presentation.common.BotonPrincipal
import com.example.fileshifter.presentation.common.BotonSecundario
import com.example.fileshifter.presentation.common.CamaraEscanner
import com.example.fileshifter.presentation.common.SistemaBarras
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.presentation.common.TarjetaResultado
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.presentation.common.recordarPermisoCamara
import com.example.fileshifter.presentation.common.tomarFoto
import com.example.fileshifter.ui.theme.AmberBg
import com.example.fileshifter.ui.theme.AmberText
import com.example.fileshifter.ui.theme.Background
import com.example.fileshifter.ui.theme.CameraBar
import com.example.fileshifter.ui.theme.CameraDark
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

@Composable
fun ScannerScreen(navController: NavController) {
    val vm: ScannerViewModel = appViewModel {
        ScannerViewModel(it.documentoRepository, it.ocrService, it.carpetaSalida)
    }
    val estado by vm.estado.collectAsStateWithLifecycle()
    SistemaBarras(iconosOscuros = estado.revisando)
    val contexto = LocalContext.current
    val ejecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { ejecutor.shutdown() } }

    val permiso = recordarPermisoCamara()
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.onGaleria(it.toString()) }
    }

    Box(Modifier.fillMaxSize().background(CameraDark)) {
        when {
            estado.edicion != null -> EditorEscaneo(
                edicion = estado.edicion!!,
                procesando = estado.procesando,
                error = estado.error,
                onMover = vm::moverEsquina,
                onAuto = vm::autoDetectar,
                onGirar = vm::girar,
                onFiltro = vm::cambiarFiltro,
                onCancelar = vm::cancelarEdicion,
                onAplicar = vm::aplicarEdicion,
                onDescartarError = vm::descartarError
            )

            estado.revisando -> RevisarEscaneo(
                estado = estado,
                navController = navController,
                onVolver = vm::cerrarRevisor,
                onAnadir = vm::cerrarRevisor,
                onAbrir = vm::reabrirPagina,
                onEliminar = vm::eliminarPagina,
                onPdf = vm::generarPdf,
                onTexto = vm::extraerTexto,
                onTextoPdf = vm::textoAPdf,
                onDescartarResultado = vm::descartarResultado,
                onLimpiar = vm::limpiar
            )

            else -> PantallaCamara(
                estado = estado,
                vm = vm,
                tienePermiso = permiso.concedido,
                permisoBloqueado = permiso.bloqueado,
                onPedirPermiso = permiso.solicitar,
                onAbrirAjustes = permiso.abrirAjustes,
                onGaleria = { galeria.launch("image/*") },
                onVolver = { navController.popBackStack() },
                onCapturar = { capturador ->
                    val archivo = File(
                        contexto.cacheDir,
                        "captura_${System.currentTimeMillis()}.jpg"
                    )
                    tomarFoto(capturador, ejecutor, archivo) { vm.onFotoCapturada(it) }
                }
            )
        }

        if (estado.procesando) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Primary)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Procesando imagen…",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------------- cámara

@Composable
private fun PantallaCamara(
    estado: ScannerUiState,
    vm: ScannerViewModel,
    tienePermiso: Boolean,
    permisoBloqueado: Boolean,
    onPedirPermiso: () -> Unit,
    onAbrirAjustes: () -> Unit,
    onGaleria: () -> Unit,
    onVolver: () -> Unit,
    onCapturar: (ImageCapture) -> Unit
) {
    var capturador by remember { mutableStateOf<ImageCapture?>(null) }
    var camaraActiva by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var linterna by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val aspectoCamara = if (maxWidth >= maxHeight) 4f / 3f else 3f / 4f

        // El visor respeta el mismo encuadre que la foto (4:3 del sensor). Así la hoja
        // ocupa el marco completo en la captura y no se pierde resolución al recortarla.
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .aspectRatio(aspectoCamara, matchHeightConstraintsFirst = false)
        ) {
            if (tienePermiso) {
                CamaraEscanner(
                    modifier = Modifier.fillMaxSize(),
                    onListo = { captura, camara ->
                        capturador = captura
                        camaraActiva = camara
                    },
                    onBordes = vm::onCuadroVivo
                )

                // contorno en vivo (mismas coordenadas que el visor)
                Canvas(Modifier.fillMaxSize()) {
                    val detectado = estado.cuadroVivo
                    if (detectado != null) {
                        fun mapear(punto: Punto) = Offset(
                            punto.x * size.width,
                            punto.y * size.height
                        )

                        val puntos = detectado.puntos.map(::mapear)
                        val contorno = Path().apply {
                            moveTo(puntos[0].x, puntos[0].y)
                            for (i in 1 until puntos.size) lineTo(puntos[i].x, puntos[i].y)
                            close()
                        }
                        val sombra = Path().apply {
                            fillType = PathFillType.EvenOdd
                            addRect(Rect(Offset.Zero, size))
                            moveTo(puntos[0].x, puntos[0].y)
                            for (i in 1 until puntos.size) lineTo(puntos[i].x, puntos[i].y)
                            close()
                        }
                        drawPath(sombra, Color.Black.copy(alpha = 0.32f), style = Fill)
                        drawPath(contorno, Primary.copy(alpha = 0.30f), style = Stroke(width = 10.dp.toPx()))
                        drawPath(contorno, Primary, style = Stroke(width = 3.dp.toPx()))
                        puntos.forEach { punto ->
                            drawCircle(Color.White, radius = 7.dp.toPx(), center = punto)
                            drawCircle(Primary, radius = 4.dp.toPx(), center = punto)
                        }
                    } else {
                        val guia = listOf(
                            Offset(size.width * 0.14f, size.height * 0.22f),
                            Offset(size.width * 0.86f, size.height * 0.18f),
                            Offset(size.width * 0.82f, size.height * 0.70f),
                            Offset(size.width * 0.18f, size.height * 0.74f)
                        )
                        val camino = Path().apply {
                            moveTo(guia[0].x, guia[0].y)
                            for (i in 1 until guia.size) lineTo(guia[i].x, guia[i].y)
                            close()
                        }
                        drawPath(camino, Color.White.copy(alpha = 0.55f), style = Stroke(width = 2.dp.toPx()))
                        guia.forEach { punto ->
                            drawCircle(Color.White.copy(alpha = 0.55f), radius = 4.dp.toPx(), center = punto)
                        }
                    }
                }
            }
        }

        if (!tienePermiso) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Permiso de cámara", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (permisoBloqueado) {
                        "La cámara está bloqueada. Actívala en los ajustes del sistema para escanear documentos."
                    } else {
                        "Para escanear documentos necesitamos usar la cámara. También puedes importar una foto desde la galería."
                    },
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                BotonPrincipal(
                    texto = if (permisoBloqueado) "Abrir ajustes" else "Permitir cámara",
                    modifier = Modifier.width(230.dp)
                ) {
                    if (permisoBloqueado) onAbrirAjustes() else onPedirPermiso()
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onGaleria) {
                    Text("Importar desde galería", color = Color.White)
                }
            }
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(12.dp)) {
            IconButton(onClick = onVolver) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(
            color = if (estado.cuadroVivo != null) Primary else Color.White.copy(alpha = 0.14f),
            shape = RoundedCornerShape(50)
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(
                            if (estado.cuadroVivo != null) Color.White else TextMuted,
                            CircleShape
                        )
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    if (estado.cuadroVivo != null) "Documento detectado" else "Buscando bordes…",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(12.dp)) {
            IconButton(onClick = {
                linterna = !linterna
                camaraActiva?.cameraControl?.enableTorch(linterna)
            }) {
                Icon(
                    Icons.Filled.Bolt,
                    contentDescription = "Linterna",
                    tint = if (linterna) Primary else Color.White
                )
            }
        }
    }

    estado.error?.let { mensaje ->
        Surface(
            color = AmberBg,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 68.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    mensaje,
                    color = AmberText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = vm::descartarError) {
                    Text("OK", color = AmberText)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (estado.paginas.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(estado.paginas, key = { it.id }) { pagina ->
                        Box(
                            Modifier
                                .size(width = 46.dp, height = 60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { vm.reabrirPagina(pagina.id) }
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = pagina.miniatura.asImageBitmap(),
                                contentDescription = "Página escaneada",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }
                TextButton(onClick = vm::abrirRevisor) {
                    Text(
                        "Revisar (${estado.paginas.size})",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        if (estado.procesando) {
            Text(
                "Preparando la página…",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Primary)
            Spacer(Modifier.height(10.dp))
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
                IconButton(onClick = onGaleria) {
                    Icon(Icons.Filled.Image, contentDescription = "Galería", tint = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .size(78.dp)
                    .background(Color.White, CircleShape)
                    .clickable(
                        enabled = !estado.procesando,
                        onClickLabel = "Capturar página",
                        role = androidx.compose.ui.semantics.Role.Button
                    ) {
                        capturador?.let(onCapturar)
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(60.dp).background(Primary, CircleShape))
            }

            Surface(
                color = if (estado.paginas.isEmpty()) CameraBar else Primary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(52.dp)
            ) {
                IconButton(onClick = { vm.abrirRevisor() }) {
                    Icon(
                        if (estado.paginas.isEmpty()) Icons.Filled.Description else Icons.Filled.Check,
                        contentDescription = "Revisar páginas",
                        tint = Color.White
                    )
                }
            }
        }

        if (estado.paginas.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            BotonPrincipal(
                texto = "Crear PDF (${estado.paginas.size} página${if (estado.paginas.size == 1) "" else "s"})",
                enabled = !estado.generando
            ) {
                vm.abrirRevisor()
            }
        }
    }
    }
}

// -------------------------------------------------------------------- editor

@Composable
private fun EditorEscaneo(
    edicion: EdicionEscaneo,
    procesando: Boolean,
    error: String?,
    onMover: (Int, Float, Float) -> Unit,
    onAuto: () -> Unit,
    onGirar: () -> Unit,
    onFiltro: (FiltroEscaneo) -> Unit,
    onCancelar: () -> Unit,
    onAplicar: () -> Unit,
    onDescartarError: () -> Unit
) {
    var verBordes by remember(edicion.rutaOriginal) { mutableStateOf(true) }
    var arrastrando by remember { mutableStateOf(-1) }
    val cuadrilateroActual by rememberUpdatedState(edicion.cuadrilatero)

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(12.dp)) {
                IconButton(onClick = onCancelar) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancelar", tint = Color.White)
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (edicion.paginaId == 0L) "Ajustar documento" else "Editar página",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (verBordes) "Arrastra las esquinas" else "Así se verá tu escaneo",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Surface(
                color = if (procesando) Color.White.copy(alpha = 0.14f) else Primary,
                shape = RoundedCornerShape(12.dp)
            ) {
                IconButton(onClick = onAplicar, enabled = !procesando) {
                    Icon(Icons.Filled.Check, contentDescription = "Aplicar", tint = Color.White)
                }
            }
        }

        if (error != null) {
            Surface(
                color = AmberBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        error,
                        color = AmberText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDescartarError) { Text("OK", color = AmberText) }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val imagen = if (verBordes) edicion.vista else (edicion.previa ?: edicion.vista)
            val imagenBitmap = remember(imagen) { imagen.asImageBitmap() }

            Canvas(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (verBordes) {
                            Modifier.pointerInput(edicion.vista) {
                                detectDragGestures(
                                    onDragStart = { posicion ->
                                        val rect = rectanguloImagen(
                                            size.width.toFloat(),
                                            size.height.toFloat(),
                                            edicion.vista.width,
                                            edicion.vista.height
                                        )
                                        var indice = -1
                                        var mejor = 58.dp.toPx()
                                        cuadrilateroActual.puntos.forEachIndexed { i, punto ->
                                            val px = rect.left + punto.x * rect.width
                                            val py = rect.top + punto.y * rect.height
                                            val distancia = hypot(
                                                posicion.x - px,
                                                posicion.y - py
                                            )
                                            if (distancia < mejor) {
                                                mejor = distancia
                                                indice = i
                                            }
                                        }
                                        arrastrando = indice
                                    },
                                    onDragEnd = { arrastrando = -1 },
                                    onDragCancel = { arrastrando = -1 },
                                    onDrag = { cambio, _ ->
                                        if (arrastrando >= 0) {
                                            val rect = rectanguloImagen(
                                                size.width.toFloat(),
                                                size.height.toFloat(),
                                                edicion.vista.width,
                                                edicion.vista.height
                                            )
                                            onMover(
                                                arrastrando,
                                                (cambio.position.x - rect.left) / rect.width,
                                                (cambio.position.y - rect.top) / rect.height
                                            )
                                        }
                                        cambio.consume()
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                val rect = rectanguloImagen(
                    size.width,
                    size.height,
                    imagenBitmap.width,
                    imagenBitmap.height
                )
                drawImage(
                    image = imagenBitmap,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(imagenBitmap.width, imagenBitmap.height),
                    dstOffset = IntOffset(rect.left.toInt(), rect.top.toInt()),
                    dstSize = IntSize(rect.width.toInt(), rect.height.toInt())
                )

                if (verBordes) {
                    val puntos = cuadrilateroActual.puntos.map { punto ->
                        Offset(
                            rect.left + punto.x * rect.width,
                            rect.top + punto.y * rect.height
                        )
                    }
                    val sombra = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(Offset.Zero, size))
                        moveTo(puntos[0].x, puntos[0].y)
                        for (i in 1 until puntos.size) lineTo(puntos[i].x, puntos[i].y)
                        close()
                    }
                    drawPath(sombra, Color.Black.copy(alpha = 0.45f), style = Fill)

                    val contorno = Path().apply {
                        moveTo(puntos[0].x, puntos[0].y)
                        for (i in 1 until puntos.size) lineTo(puntos[i].x, puntos[i].y)
                        close()
                    }
                    drawPath(contorno, Primary.copy(alpha = 0.35f), style = Stroke(width = 9.dp.toPx()))
                    drawPath(contorno, Primary, style = Stroke(width = 2.5.dp.toPx()))

                    puntos.forEachIndexed { indice, punto ->
                        drawCircle(
                            Color.Black.copy(alpha = 0.35f),
                            radius = 15.dp.toPx(),
                            center = punto
                        )
                        drawCircle(
                            if (indice == arrastrando) Primary else Color.White,
                            radius = 11.dp.toPx(),
                            center = punto
                        )
                        drawCircle(
                            if (indice == arrastrando) Color.White else Primary,
                            radius = 5.5.dp.toPx(),
                            center = punto
                        )
                    }
                } else {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.25f),
                        topLeft = Offset(rect.left, rect.top),
                        size = androidx.compose.ui.geometry.Size(rect.width, rect.height),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SelectorPequeno("Bordes", verBordes) { verBordes = true }
                SelectorPequeno("Vista", !verBordes) { verBordes = false }
            }

            if (!verBordes && edicion.calculandoPrevia) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Aplicando filtro…",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroEscaneo.entries.forEach { filtro ->
                    Surface(
                        onClick = {
                            onFiltro(filtro)
                            verBordes = false
                        },
                        color = if (edicion.filtro == filtro) Primary else Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            filtro.etiqueta,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onAuto,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Reajustar",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Surface(
                    onClick = onGirar,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.RotateRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Girar",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            BotonPrincipal(
                texto = if (edicion.paginaId == 0L) "Aplicar y añadir página" else "Guardar cambios",
                enabled = !procesando
            ) {
                onAplicar()
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Consejo: deja un pequeño margen alrededor de la hoja para que el recorte quede completo.",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SelectorPequeno(texto: String, activo: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (activo) Primary else Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            texto,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private fun rectanguloImagen(
    anchoVista: Float,
    altoVista: Float,
    anchoImagen: Int,
    altoImagen: Int
): Rect {
    if (anchoImagen <= 0 || altoImagen <= 0) return Rect(0f, 0f, anchoVista, altoVista)
    val escala = min(anchoVista / anchoImagen, altoVista / altoImagen)
    val ancho = anchoImagen * escala
    val alto = altoImagen * escala
    return Rect(
        left = (anchoVista - ancho) / 2f,
        top = (altoVista - alto) / 2f,
        right = (anchoVista + ancho) / 2f,
        bottom = (altoVista + alto) / 2f
    )
}

// -------------------------------------------------------------------- revisor

@Composable
private fun RevisarEscaneo(
    estado: ScannerUiState,
    navController: NavController,
    onVolver: () -> Unit,
    onAnadir: () -> Unit,
    onAbrir: (Long) -> Unit,
    onEliminar: (Long) -> Unit,
    onPdf: () -> Unit,
    onTexto: () -> Unit,
    onTextoPdf: () -> Unit,
    onDescartarResultado: () -> Unit,
    onLimpiar: () -> Unit
) {
    var confirmarVaciar by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Background)) {
        BarraSuperior(titulo = "Escaneo", onVolver = onVolver)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${estado.paginas.size} página${if (estado.paginas.size == 1) "" else "s"} escaneada${if (estado.paginas.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                "Toca una página para ajustar sus bordes o el filtro.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (estado.paginas.isNotEmpty()) {
                            TextButton(onClick = { confirmarVaciar = true }) {
                                Text("Vaciar", color = TextMuted)
                            }
                        }
                    }
                }
            }

            items(estado.paginas, key = { it.id }) { pagina ->
                Tarjeta(padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            bitmap = pagina.miniatura.asImageBitmap(),
                            contentDescription = "Página",
                            modifier = Modifier
                                .height(74.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAbrir(pagina.id) },
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable { onAbrir(pagina.id) }
                        ) {
                            Text(
                                "Página ${estado.paginas.indexOf(pagina) + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                "${pagina.filtro.etiqueta} · ${pagina.ancho}×${pagina.alto}px",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                "Toca para editar",
                                style = MaterialTheme.typography.bodySmall,
                                color = Primary
                            )
                        }
                        IconButton(onClick = { onEliminar(pagina.id) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Eliminar página",
                                tint = TextMuted
                            )
                        }
                    }
                }
            }

            if (estado.texto.isNotBlank()) {
                item {
                    Tarjeta {
                        Text(
                            "Texto reconocido",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            estado.texto,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TextButton(
                                onClick = {
                                    BorradorTexto.texto = estado.texto
                                    navController.navigate(Screen.Corrector.route)
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) { Text("Corregir texto", color = Primary) }
                            TextButton(
                                onClick = onTextoPdf,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) { Text("PDF de texto", color = Primary) }
                        }
                    }
                }
            }

            estado.resultado?.let { resultado ->
                item {
                    Column {
                        TarjetaResultado(
                            resultado = resultado,
                            onValidar = { navController.navigate(Screen.Validator.route) }
                        )
                        TextButton(
                            onClick = onDescartarResultado,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("Cerrar aviso", color = TextMuted)
                        }
                    }
                }
            }

            if (estado.paginas.isEmpty()) {
                item {
                    Tarjeta {
                        Text(
                            "Todavía no hay páginas. Vuelve a la cámara y captura tu documento.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Surface(color = Background, shadowElevation = 8.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                if (estado.paginas.isNotEmpty()) {
                    BotonPrincipal(
                        texto = if (estado.generando) "Creando PDF…" else "Crear PDF",
                        enabled = !estado.generando && !estado.extrayendoTexto
                    ) { onPdf() }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BotonSecundario(
                            texto = if (estado.extrayendoTexto) "Leyendo…" else "Extraer texto",
                            modifier = Modifier.weight(1f),
                            onClick = onTexto
                        )
                        BotonSecundario(
                            texto = "Añadir página",
                            modifier = Modifier.weight(1f),
                            onClick = onAnadir
                        )
                    }
                } else {
                    BotonPrincipal(texto = "Volver a la cámara") { onAnadir() }
                }
            }
        }
    }

    if (confirmarVaciar) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmarVaciar = false },
            title = { Text("¿Vaciar el escaneo?") },
            text = {
                Text("Se eliminarán las páginas capturadas. Los PDF que ya hayas creado no se tocan.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarVaciar = false
                    onLimpiar()
                }) {
                    Text("Vaciar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarVaciar = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}
