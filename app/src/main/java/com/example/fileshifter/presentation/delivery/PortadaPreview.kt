package com.example.fileshifter.presentation.delivery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fileshifter.data.model.DatosEntrega
import com.example.fileshifter.data.model.PlantillaEntrega
import com.example.fileshifter.data.model.PosicionLogo
import com.example.fileshifter.data.source.PdfEngine
import com.example.fileshifter.data.source.PortadaLayoutBuilder
import com.example.fileshifter.ui.theme.Border
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class LogoListo(val imagen: ImageBitmap, val aspecto: Float)

/**
 * Vista previa que dibuja exactamente el mismo layout que se escribe en el PDF:
 * mismas coordenadas, tamaños y colores, escalados al ancho disponible.
 */
@Composable
fun PortadaPreview(
    plantilla: PlantillaEntrega,
    datos: DatosEntrega,
    modifier: Modifier = Modifier,
    mostrarSombra: Boolean = true
) {
    val contexto = LocalContext.current
    val uriLogo = remember(datos.logoUri, datos.posicionLogo) {
        datos.logoUri?.takeIf { datos.posicionLogo != PosicionLogo.NINGUNA }
    }
    val logoListo by produceState<LogoListo?>(initialValue = null, key1 = uriLogo) {
        value = if (uriLogo == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                PdfEngine.cargarLogoBitmap(contexto, uriLogo)?.let { bitmap ->
                    LogoListo(bitmap.asImageBitmap(), bitmap.width.toFloat() / bitmap.height)
                }
            }
        }
    }
    val layout = remember(plantilla, datos, logoListo?.aspecto) {
        PortadaLayoutBuilder.componer(plantilla, datos, logoListo?.aspecto)
    }
    val escalaFuente = LocalDensity.current.fontScale.coerceAtLeast(0.1f)
    val forma = RoundedCornerShape(3.dp)
    val sombra = if (mostrarSombra) Modifier.shadow(10.dp, forma) else Modifier

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(layout.proporcion)
            .then(sombra)
            .clip(forma)
            .background(Color.White)
            .border(1.dp, Border, forma)
    ) {
        val factor = maxWidth.value / layout.ancho

        Canvas(Modifier.fillMaxSize()) {
            val ex = size.width / layout.ancho
            val ey = size.height / layout.alto
            layout.fondos.forEach { rect ->
                drawRect(
                    color = Color(rect.color.r, rect.color.g, rect.color.b),
                    topLeft = Offset(rect.x * ex, (layout.alto - rect.y - rect.alto) * ey),
                    size = Size(rect.ancho * ex, rect.alto * ey)
                )
            }
        }

        val logo = layout.logo
        val imagen = logoListo?.imagen
        if (logo != null && imagen != null) {
            Image(
                bitmap = imagen,
                contentDescription = null,
                alpha = logo.opacidad,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset(
                        x = (logo.x * factor).dp,
                        y = ((layout.alto - logo.y - logo.alto) * factor).dp
                    )
                    .size((logo.ancho * factor).dp, (logo.alto * factor).dp)
            )
        }

        layout.textos.forEach { texto ->
            val tamano = (texto.tamano * factor / escalaFuente).sp
            Text(
                text = texto.texto,
                color = Color(texto.color.r, texto.color.g, texto.color.b),
                style = TextStyle(
                    fontSize = tamano,
                    lineHeight = tamano * 1.15f,
                    fontWeight = if (texto.negrita) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = (texto.espaciado * factor / escalaFuente).sp
                ),
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(
                        y = ((layout.alto - texto.yArriba - texto.tamano * 0.12f) * factor).dp
                    )
            )
        }
    }
}
