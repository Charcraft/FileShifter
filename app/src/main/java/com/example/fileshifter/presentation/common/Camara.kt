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
import androidx.compose.runtime.mutableStateOf
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
import java.io.File
import java.util.concurrent.Executors

/** Vista de cámara con botón Capturar (para el Escáner). */
@Composable
fun CamaraCaptura(
    modifier: Modifier = Modifier,
    onCaptura: (File) -> Unit
) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val ejecutor = remember { Executors.newSingleThreadExecutor() }
    val capturador = remember { mutableStateOf<ImageCapture?>(null) }

    DisposableEffect(Unit) { onDispose { ejecutor.shutdown() } }

    Box(modifier) {
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
                            capturador.value = captura
                            proveedor.unbindAll()
                            proveedor.bindToLifecycle(
                                ciclo,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                captura
                            )
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Button(
            onClick = {
                val captura = capturador.value ?: return@Button
                val archivo = File(contexto.cacheDir, "captura_${System.currentTimeMillis()}.jpg")
                val opciones = ImageCapture.OutputFileOptions.Builder(archivo).build()
                captura.takePicture(
                    opciones,
                    ejecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            onCaptura(archivo)
                        }

                        override fun onError(exception: ImageCaptureException) = Unit
                    }
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) { Text("Capturar") }
    }
}

/** Vista de cámara que detecta códigos QR y de barras con ML Kit. */
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
