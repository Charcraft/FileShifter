package com.example.fileshifter

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.fileshifter.navigation.AppNav
import com.example.fileshifter.ui.theme.FileShifterTheme

class MainActivity : ComponentActivity() {
    private val permisoAlmacenamiento = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (!concedido) {
            Toast.makeText(
                this,
                "Sin acceso a Documentos, los archivos se guardan en la carpeta privada de la app y puedes compartirlos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pedirAlmacenamientoSiHaceFalta()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            FileShifterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNav(navController = navController)
                }
            }
        }
    }

    private fun pedirAlmacenamientoSiHaceFalta() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) return
        val concedido = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (!concedido) {
            permisoAlmacenamiento.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}
