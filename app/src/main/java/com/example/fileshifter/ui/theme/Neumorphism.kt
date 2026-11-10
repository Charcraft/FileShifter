package com.example.fileshifter.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neumorphism ("soft UI"): el elemento y el fondo comparten el mismo color y la
 * forma se percibe por dos sombras suaves, una clara (arriba-izquierda) y otra
 * oscura (abajo-derecha). Los elementos hundidos invierten la luz.
 *
 * Se dibuja con capas de rectángulos redondeados en lugar de `shadowLayer`
 * porque esa API se ignora con aceleración por hardware en Android.
 */

/** Superficie elevada (tarjetas, botones, chips). */
fun Modifier.neumorphRaised(
    corner: Dp = 20.dp,
    elevacion: Dp = 8.dp,
    fondo: Color = NeumorphBase,
    luz: Color = NeumorphLight,
    sombra: Color = NeumorphDark
): Modifier = drawBehind {
    val radio = corner.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
    val alcance = elevacion.toPx()
    val capas = 14
    val intensidad = 0.05f

    // Sombra oscura hacia abajo-derecha
    for (i in capas downTo 1) {
        val t = i / capas.toFloat()
        val d = alcance * t
        val desplazamiento = d * 0.42f
        drawRoundRect(
            color = sombra.copy(alpha = intensidad * (1.05f - t * 0.55f)),
            topLeft = Offset(-d / 2f + desplazamiento, -d / 2f + desplazamiento),
            size = Size(size.width + d, size.height + d),
            cornerRadius = CornerRadius(radio + d / 2f)
        )
    }

    // Luz hacia arriba-izquierda
    for (i in capas downTo 1) {
        val t = i / capas.toFloat()
        val d = alcance * t
        val desplazamiento = d * 0.42f
        drawRoundRect(
            color = luz.copy(alpha = intensidad * 1.05f * (1.05f - t * 0.55f)),
            topLeft = Offset(-d / 2f - desplazamiento, -d / 2f - desplazamiento),
            size = Size(size.width + d, size.height + d),
            cornerRadius = CornerRadius(radio + d / 2f)
        )
    }

    // Relleno del elemento (tapa el interior de las sombras)
    drawRoundRect(
        color = fondo,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius(radio)
    )
}

/** Superficie hundida (campos de texto, pestañas activas, indicadores). */
fun Modifier.neumorphInset(
    corner: Dp = 16.dp,
    profundidad: Dp = 6.dp,
    fondo: Color = NeumorphBase,
    luz: Color = NeumorphLight,
    sombra: Color = NeumorphDark
): Modifier = drawWithContent {
    val radio = corner.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
    val p = profundidad.toPx()
    val camino = Path().apply {
        addRoundRect(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                radiusX = radio,
                radiusY = radio
            )
        )
    }

    clipPath(camino) {
        drawRect(fondo)
        // Oscuridad desde arriba-izquierda
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(sombra.copy(alpha = 0.85f), Color.Transparent),
                start = Offset.Zero,
                end = Offset(p * 5f, p * 5f)
            )
        )
        // Luz desde abajo-derecha
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, luz.copy(alpha = 0.9f)),
                start = Offset(size.width - p * 6f, size.height - p * 6f),
                end = Offset(size.width, size.height)
            )
        )
    }
    drawContent()
}
