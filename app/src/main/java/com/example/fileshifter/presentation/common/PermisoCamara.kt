package com.example.fileshifter.presentation.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class EstadoPermisoCamara(
    val concedido: Boolean,
    val bloqueado: Boolean,
    val solicitar: () -> Unit,
    val abrirAjustes: () -> Unit
)

private tailrec fun Context.actividadActual(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.actividadActual()
    else -> null
}

@Composable
fun recordarPermisoCamara(): EstadoPermisoCamara {
    val contexto = LocalContext.current
    val actividad = remember(contexto) { contexto.actividadActual() }
    val ciclo = LocalLifecycleOwner.current

    fun concedidoAhora(): Boolean =
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    var concedido by remember { mutableStateOf(concedidoAhora()) }
    var bloqueado by remember { mutableStateOf(false) }

    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { otorgado ->
        concedido = otorgado
        if (!otorgado) {
            bloqueado = actividad?.shouldShowRequestPermissionRationale(
                Manifest.permission.CAMERA
            ) == false
        }
    }

    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                concedido = concedidoAhora()
            }
        }
        ciclo.lifecycle.addObserver(observador)
        onDispose { ciclo.lifecycle.removeObserver(observador) }
    }

    return EstadoPermisoCamara(
        concedido = concedido,
        bloqueado = bloqueado,
        solicitar = { lanzador.launch(Manifest.permission.CAMERA) },
        abrirAjustes = {
            contexto.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", contexto.packageName, null)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    )
}
