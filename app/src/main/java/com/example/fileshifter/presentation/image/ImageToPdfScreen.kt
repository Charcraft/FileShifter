package com.example.fileshifter.presentation.image

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.model.ArchivoSeleccionado
import com.example.fileshifter.data.model.formatearTamano
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.BotonPrincipal
import com.example.fileshifter.presentation.common.ContenidoResponsive
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.presentation.common.TarjetaResultado
import com.example.fileshifter.presentation.common.TiraInfo
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.ui.theme.Background
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.PrimaryLight
import com.example.fileshifter.ui.theme.SurfaceMuted
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary

@Composable
fun ImageToPdfScreen(navController: NavController) {
    val vm: ImageToPdfViewModel = appViewModel { ImageToPdfViewModel(it.documentoRepository) }
    val estado by vm.estado.collectAsStateWithLifecycle()

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        vm.onImagenesSeleccionadas(uris.map(Uri::toString))
    }

    val contexto = LocalContext.current
    var uriFoto by remember { mutableStateOf<Uri?>(null) }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { guardada ->
        if (guardada) {
            uriFoto?.let { vm.agregarImagenes(listOf(it.toString())) }
        }
    }

    fun tomarFoto() {
        val carpeta = File(contexto.cacheDir, "fotos").apply { mkdirs() }
        val destino = File(carpeta, "foto_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            destino
        )
        uriFoto = uri
        camara.launch(uri)
    }

    PantallaBase(titulo = "Imagen → PDF", onVolver = { navController.popBackStack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TiraInfo(listOf("Conversión rápida" to true, "Sin configuración extra" to false))

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
            ) {
                ContenidoResponsive {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (estado.imagenes.isEmpty()) "Sin imágenes"
                                else "${estado.imagenes.size} imagen(es)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { tomarFoto() }) {
                                Icon(
                                    Icons.Filled.PhotoCamera,
                                    contentDescription = "Tomar foto",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            TextButton(onClick = { selector.launch("image/*") }) {
                                Text("+  Añadir imágenes", color = Primary)
                            }
                        }

                        BoxWithConstraints {
                            val columnas = if (maxWidth < 340.dp) 2 else 3
                            val filas = (estado.imagenes.size + 1 + columnas - 1) / columnas
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                for (fila in 0 until filas) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        for (columna in 0 until columnas) {
                                            val indice = fila * columnas + columna
                                            Box(Modifier.weight(1f)) {
                                                when {
                                                    indice < estado.imagenes.size -> TarjetaImagen(
                                                        archivo = estado.imagenes[indice],
                                                        numero = indice + 1,
                                                        onQuitar = { vm.quitar(estado.imagenes[indice].uri) }
                                                    )
                                                    indice == estado.imagenes.size -> TarjetaAnadir {
                                                        selector.launch("image/*")
                                                    }
                                                    else -> Spacer(Modifier)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Tarjeta {
                            Text(
                                "Modo de salida",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OpcionSalida(
                                    titulo = "Unir en un PDF",
                                    detalle = "1 archivo",
                                    icono = Icons.Filled.Description,
                                    activa = estado.unir,
                                    modifier = Modifier.weight(1f)
                                ) { vm.onUnirCambia(true) }
                                OpcionSalida(
                                    titulo = "Un PDF por imagen",
                                    detalle = if (estado.imagenes.size == 1) "1 archivo" else "${estado.imagenes.size} archivos",
                                    icono = Icons.Filled.Image,
                                    activa = !estado.unir,
                                    modifier = Modifier.weight(1f)
                                ) { vm.onUnirCambia(false) }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Tarjeta {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Calidad",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    when (estado.calidad) {
                                        0 -> "Baja"
                                        1 -> "Media"
                                        else -> "Alta"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Primary
                                )
                            }
                            Slider(
                                value = estado.calidad.toFloat(),
                                onValueChange = { vm.onCalidad(it.roundToInt()) },
                                valueRange = 0f..2f,
                                steps = 0
                            )
                            Row(Modifier.fillMaxWidth()) {
                                Text("Baja", color = TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text("Media", color = TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text("Alta", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (estado.cargando) {
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }

                        estado.resultado?.let {
                            Spacer(Modifier.height(12.dp))
                            TarjetaResultado(
                                resultado = it,
                                onValidar = { navController.navigate(Screen.Validator.route) },
                                onInicio = { navController.popBackStack() },
                                onCorregir = { navController.navigate(Screen.Corrector.route) },
                                onEntrega = { navController.navigate(Screen.Delivery1.route) }
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            Surface(color = Background) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    BotonPrincipal(
                        texto = if (estado.unir) "Convertir a PDF (unido)" else "Convertir a PDF (separado)",
                        enabled = estado.imagenes.isNotEmpty() && !estado.cargando,
                        onClick = { vm.convertir() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaImagen(
    archivo: ArchivoSeleccionado,
    numero: Int,
    onQuitar: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .background(PrimaryLight, MaterialTheme.shapes.medium)
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(6.dp))
            Icon(Icons.Filled.Image, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                archivo.nombre.substringBeforeLast('.'),
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                formatearTamano(archivo.tamanoBytes),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Surface(
            color = Color.White,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp)
        ) {
            IconButton(onClick = onQuitar, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = TextSecondary, modifier = Modifier.size(14.dp))
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .size(22.dp)
                .background(Primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$numero", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TarjetaAnadir(onClick: () -> Unit) {
    val color = Border
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .drawBehind {
                drawRoundRect(
                    color = color,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )
                )
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = TextMuted)
            Spacer(Modifier.height(4.dp))
            Text("Añadir", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

@Composable
private fun OpcionSalida(
    titulo: String,
    detalle: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    activa: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (activa) PrimaryLight else SurfaceMuted,
        border = BorderStroke(1.dp, if (activa) Primary else Color.Transparent)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icono, contentDescription = null, tint = if (activa) Primary else TextMuted)
            Spacer(Modifier.height(6.dp))
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                color = if (activa) Primary else TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = if (activa) Primary else TextMuted
            )
        }
    }
}
