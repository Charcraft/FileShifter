package com.example.fileshifter.presentation.visor

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as ColorAndroid
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fileshifter.data.source.Archivos
import com.example.fileshifter.data.source.OfficeTextExtractor
import com.example.fileshifter.presentation.common.BotonSecundario
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.ui.theme.FileShifterTheme
import com.example.fileshifter.ui.theme.Surface as Superficie
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Visor interno. Abre un archivo generado por la app sin salir a otra aplicacion:
 * PDF pagina por pagina con PdfRenderer, imagenes, texto plano y una vista previa
 * del texto de los documentos de Office.
 */
class VisorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(ColorAndroid.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(ColorAndroid.TRANSPARENT, ColorAndroid.TRANSPARENT)
        )

        val uri = intent.getStringExtra(EXTRA_URI).orEmpty()
        val nombre = intent.getStringExtra(EXTRA_NOMBRE)
            ?: uri.substringAfterLast('/').ifBlank { "Documento" }

        setContent {
            FileShifterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VisorContenido(uri = uri, nombre = nombre, onVolver = { finish() })
                }
            }
        }
    }

    companion object {
        const val EXTRA_URI = "uri"
        const val EXTRA_NOMBRE = "nombre"

        /** Abre el visor desde cualquier pantalla. */
        fun abrir(context: Context, uri: String, nombre: String? = null) {
            val intent = Intent(context, VisorActivity::class.java).apply {
                putExtra(EXTRA_URI, uri)
                if (nombre != null) putExtra(EXTRA_NOMBRE, nombre)
            }
            context.startActivity(intent)
        }
    }
}

@Composable
fun VisorContenido(uri: String, nombre: String, onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val direccion = remember(uri) { Uri.parse(uri) }
    val extension = nombre.substringAfterLast('.', "").lowercase()

    PantallaBase(
        titulo = nombre,
        onVolver = onVolver,
        acciones = {
            IconButton(
                onClick = {
                    runCatching { contexto.startActivity(intentCompartir(contexto, direccion, nombre)) }
                }
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "Compartir documento",
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
            IconButton(
                onClick = { abrirConOtraApp(contexto, direccion, nombre) }
            ) {
                Icon(
                    Icons.Filled.OpenInNew,
                    contentDescription = "Abrir con otra aplicación",
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (extension) {
                "pdf" -> VisorPdf(contexto, direccion)
                "png", "jpg", "jpeg", "webp" -> VisorImagen(contexto, direccion)
                "txt", "csv", "html", "htm", "md" -> VisorTexto(contexto, direccion)
                "docx", "xlsx", "pptx" -> VisorOffice(contexto, direccion, nombre)
                else -> SinVistaPrevia(contexto, direccion, nombre)
            }
        }
    }
}

// ------------------------------------------------------------------ PDF

@Composable
private fun VisorPdf(contexto: Context, direccion: Uri) {
    val paginas by produceState<Int?>(initialValue = null, direccion) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                abrirPdf(contexto, direccion)?.use { it.pageCount }
            }.getOrNull()
        }
    }

    val total = paginas
    when {
        total == null -> Cargando("Preparando el documento…")
        total == 0 -> Mensaje("El PDF no tiene páginas.")
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items((0 until total).toList()) { indice ->
                PaginaPdf(contexto, direccion, indice, total)
            }
        }
    }
}

@Composable
private fun PaginaPdf(contexto: Context, direccion: Uri, indice: Int, total: Int) {
    val mapa by produceState<Bitmap?>(initialValue = null, direccion, indice) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                abrirPdf(contexto, direccion)?.use { renderer ->
                    renderer.openPage(indice).use { pagina ->
                        val escala = 2f
                        val ancho = (pagina.width * escala).toInt()
                        val alto = (pagina.height * escala).toInt()
                        Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888).also { bmp ->
                            bmp.eraseColor(ColorAndroid.WHITE)
                            pagina.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }
            }.getOrNull()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val imagen = mapa
        if (imagen == null) {
            Box(
                Modifier.fillMaxWidth().height(220.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
        } else {
            Image(
                bitmap = imagen.asImageBitmap(),
                contentDescription = "Página ${indice + 1} de $total",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Superficie)
            )
        }
        Text(
            "Página ${indice + 1} de $total",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

private fun abrirPdf(contexto: Context, direccion: Uri): PdfRenderer? = runCatching {
    val descriptor = if (direccion.scheme == "content") {
        contexto.contentResolver.openFileDescriptor(direccion, "r")
    } else {
        ParcelFileDescriptor.open(File(direccion.path!!), ParcelFileDescriptor.MODE_READ_ONLY)
    }
    descriptor?.let { PdfRenderer(it) }
}.getOrNull()

// ------------------------------------------------------------------ imágenes y texto

@Composable
private fun VisorImagen(contexto: Context, direccion: Uri) {
    val mapa by produceState<Bitmap?>(initialValue = null, direccion) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val entrada = if (direccion.scheme == "content") {
                    contexto.contentResolver.openInputStream(direccion)
                } else {
                    File(direccion.path!!).inputStream()
                }
                entrada?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
    }

    val imagen = mapa
    if (imagen == null) {
        Cargando("Abriendo la imagen…")
    } else {
        Box(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                bitmap = imagen.asImageBitmap(),
                contentDescription = "Vista completa de la imagen",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VisorTexto(contexto: Context, direccion: Uri) {
    val texto by produceState<String?>(initialValue = null, direccion) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val contenido = if (direccion.scheme == "content") {
                    contexto.contentResolver.openInputStream(direccion)?.bufferedReader()?.readText()
                } else {
                    File(direccion.path!!).readText()
                }.orEmpty()
                if (contenido.length > 200_000) contenido.take(200_000) + "\n\n(Texto recortado)" else contenido
            }.getOrNull()
        }
    }

    val contenido = texto
    if (contenido == null) {
        Cargando("Leyendo el archivo…")
    } else {
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(
                text = contenido,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun VisorOffice(contexto: Context, direccion: Uri, nombre: String) {
    val texto by produceState<String?>(initialValue = null, direccion) {
        value = withContext(Dispatchers.IO) {
            runCatching { OfficeTextExtractor.extraer(contexto, direccion, nombre) }.getOrNull()
        }
    }

    val contenido = texto
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Tarjeta {
            Text(
                "Vista previa del contenido",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Este formato no se puede dibujar igual que en Word, así que se muestra el " +
                    "texto que contiene. Para verlo con su formato original usa «Abrir con».",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Spacer(Modifier.height(12.dp))
        if (contenido == null) {
            Cargando("Extrayendo el texto…")
        } else if (contenido.isBlank()) {
            Mensaje("No se encontró texto. El archivo puede contener solo imágenes.")
        } else {
            Text(
                text = contenido,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(16.dp))
        BotonSecundario(
            texto = "Abrir con otra aplicación",
            modifier = Modifier.fillMaxWidth()
        ) { abrirConOtraApp(contexto, direccion, nombre) }
    }
}

@Composable
private fun SinVistaPrevia(contexto: Context, direccion: Uri, nombre: String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Tarjeta {
            Text("Sin vista previa", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Este tipo de archivo no se puede mostrar dentro de la app.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Spacer(Modifier.height(16.dp))
        BotonSecundario(
            texto = "Abrir con otra aplicación",
            modifier = Modifier.fillMaxWidth()
        ) { abrirConOtraApp(contexto, direccion, nombre) }
    }
}

// ------------------------------------------------------------------ auxiliares

@Composable
private fun Cargando(mensaje: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp)
            Spacer(Modifier.height(12.dp))
            Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
    }
}

@Composable
private fun Mensaje(texto: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}

private fun intentCompartir(contexto: Context, direccion: Uri, nombre: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = Archivos.mimeDe(nombre)
        putExtra(Intent.EXTRA_STREAM, direccion)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setPackage(null)
    }

private fun abrirConOtraApp(contexto: Context, direccion: Uri, nombre: String) {
    runCatching {
        contexto.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(direccion, Archivos.mimeDe(nombre))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.onFailure {
        contexto.startActivity(
            Intent.createChooser(intentCompartir(contexto, direccion, nombre), "Abrir con")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
