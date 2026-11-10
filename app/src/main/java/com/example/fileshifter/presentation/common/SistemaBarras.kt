package com.example.fileshifter.presentation.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private tailrec fun Context.actividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.actividad()
    else -> null
}

@Composable
fun SistemaBarras(iconosOscuros: Boolean) {
    val vista = LocalView.current
    val contexto = LocalContext.current
    DisposableEffect(iconosOscuros) {
        val ventana = contexto.actividad()?.window
        if (ventana != null) {
            WindowCompat.getInsetsController(ventana, vista).apply {
                isAppearanceLightStatusBars = iconosOscuros
                isAppearanceLightNavigationBars = true
            }
        }
        onDispose { }
    }
}
