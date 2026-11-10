package com.example.fileshifter.presentation.common

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Composable
fun CamaraPreview(
    modifier: Modifier = Modifier,
    onCamaraLista: (ImageCapture, androidx.camera.core.Camera) -> Unit
) {
    val ciclo = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { vista ->
                val futuro = ProcessCameraProvider.getInstance(ctx)
                futuro.addListener({
                    runCatching {
                        val proveedor = futuro.get()
                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(vista.surfaceProvider)
                        }
                        val captura = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        proveedor.unbindAll()
                        val camara = proveedor.bindToLifecycle(
                            ciclo,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            captura
                        )
                        onCamaraLista(captura, camara)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = modifier
    )
}

fun tomarFoto(
    captura: ImageCapture,
    ejecutor: Executor,
    archivo: java.io.File,
    onFoto: (java.io.File) -> Unit
) {
    val opciones = ImageCapture.OutputFileOptions.Builder(archivo).build()
    captura.takePicture(
        opciones,
        ejecutor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) = onFoto(archivo)
            override fun onError(exception: ImageCaptureException) = Unit
        }
    )
}

@Composable
fun CamaraCaptura(
    modifier: Modifier = Modifier,
    onCaptura: (java.io.File) -> Unit
) {
    val contexto = LocalContext.current
    val ejecutor = remember { Executors.newSingleThreadExecutor() }
    val capturador = remember { androidx.compose.runtime.mutableStateOf<ImageCapture?>(null) }

    DisposableEffect(Unit) { onDispose { ejecutor.shutdown() } }

    Box(modifier) {
        CamaraPreview(Modifier.fillMaxSize()) { captura, _ -> capturador.value = captura }
        Button(
            onClick = {
                val captura = capturador.value ?: return@Button
                val archivo = java.io.File(contexto.cacheDir, "captura_${System.currentTimeMillis()}.jpg")
                tomarFoto(captura, ejecutor, archivo, onCaptura)
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) { Text("Capturar") }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CamaraCodigos(
    modifier: Modifier = Modifier,
    onCodigo: (String) -> Unit
) {
    val ciclo = LocalLifecycleOwner.current
    val ejecutor = remember { Executors.newSingleThreadExecutor() }
    val escaner = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        onDispose {
            escaner.close()
            ejecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { vista ->
                val futuro = ProcessCameraProvider.getInstance(ctx)
                futuro.addListener({
                    runCatching {
                        val proveedor = futuro.get()
                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(vista.surfaceProvider)
                        }
                        val analisis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analisis.setAnalyzer(ejecutor) { proxy ->
                            val media = proxy.image
                            if (media == null) {
                                proxy.close()
                                return@setAnalyzer
                            }
                            val entrada = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                            escaner.process(entrada)
                                .addOnSuccessListener { codigos ->
                                    codigos.firstOrNull()?.rawValue?.let(onCodigo)
                                }
                                .addOnCompleteListener { proxy.close() }
                        }
                        proveedor.unbindAll()
                        proveedor.bindToLifecycle(
                            ciclo,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analisis
                        )
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = modifier
    )
}

/**
 * Cámara del escáner: además del visor y la captura, analiza los fotogramas para
 * mostrar en vivo el contorno del documento detectado.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun CamaraEscanner(
    modifier: Modifier = Modifier,
    onListo: (ImageCapture, androidx.camera.core.Camera) -> Unit,
    onBordes: (com.example.fileshifter.data.source.Cuadrilatero?) -> Unit
) {
    val ciclo = LocalLifecycleOwner.current
    val ejecutor = remember { Executors.newSingleThreadExecutor() }
    val principal = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val ultimoAnalisis = remember { longArrayOf(0L) }

    DisposableEffect(Unit) { onDispose { ejecutor.shutdown() } }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { vista ->
                vista.scaleType = PreviewView.ScaleType.FILL_CENTER
                val futuro = ProcessCameraProvider.getInstance(ctx)
                futuro.addListener({
                    runCatching {
                        val proveedor = futuro.get()
                        val preview = Preview.Builder()
                            .setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_4_3)
                            .build().apply {
                                setSurfaceProvider(vista.surfaceProvider)
                            }
                        val captura = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .setJpegQuality(100)
                            .setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_4_3)
                            .build()
                        val analisis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setTargetResolution(android.util.Size(640, 480))
                            .build()

                        analisis.setAnalyzer(ejecutor) { proxy ->
                            var detectado: com.example.fileshifter.data.source.Cuadrilatero? = null
                            try {
                                val ahora = System.currentTimeMillis()
                                if (ahora - ultimoAnalisis[0] < 260) return@setAnalyzer
                                ultimoAnalisis[0] = ahora

                                val imagen = proxy.image ?: return@setAnalyzer
                                val plano = imagen.planes.firstOrNull() ?: return@setAnalyzer
                                val paso = maxOf(2, imagen.width / 300)
                                val ancho = imagen.width / paso
                                val alto = imagen.height / paso
                                if (ancho < 24 || alto < 24) return@setAnalyzer

                                val buffer = plano.buffer
                                val inicio = buffer.position()
                                val fila = plano.rowStride
                                val gris = FloatArray(ancho * alto)
                                for (y in 0 until alto) {
                                    val base = inicio + y * paso * fila
                                    for (x in 0 until ancho) {
                                        gris[y * ancho + x] =
                                            (buffer.get(base + x * paso).toInt() and 0xFF).toFloat()
                                    }
                                }
                                detectado = com.example.fileshifter.data.source.DetectorDocumento
                                    .detectarGris(gris, ancho, alto)
                                    ?.girar(proxy.imageInfo.rotationDegrees)
                            } catch (_: Exception) {
                                detectado = null
                            } finally {
                                proxy.close()
                                val valor = detectado
                                principal.post { onBordes(valor) }
                            }
                        }

                        proveedor.unbindAll()
                        val camara = proveedor.bindToLifecycle(
                            ciclo,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            captura,
                            analisis
                        )
                        onListo(captura, camara)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = modifier
    )
}
