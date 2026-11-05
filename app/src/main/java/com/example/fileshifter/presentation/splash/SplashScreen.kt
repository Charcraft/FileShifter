package com.example.fileshifter.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fileshifter.R
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.BotonPrincipal
import com.example.fileshifter.presentation.common.ChipIcono
import com.example.fileshifter.presentation.common.SistemaBarras
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary

/** P1 del prototipo: presentación con el logotipo y los tres beneficios. */
@Composable
fun SplashScreen(navController: NavController) {
    SistemaBarras(iconosOscuros = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.7f))

        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(196.dp)
                    .border(1.dp, Primary.copy(alpha = 0.15f), CircleShape)
            )
            Box(
                Modifier
                    .size(160.dp)
                    .border(1.dp, Primary.copy(alpha = 0.30f), CircleShape)
            )
            Surface(
                color = Color.White,
                shape = MaterialTheme.shapes.large,
                shadowElevation = 6.dp
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo_marca),
                    contentDescription = "Logotipo de FileShifter",
                    modifier = Modifier.size(104.dp)
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            "FileShifter",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
            color = TextPrimary
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Convierte, edita y escanea.\nTodo offline. Sin límites.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))
        Tarjeta {
            Beneficio(Icons.Filled.PlayArrow, "100% Offline", "Tus archivos nunca salen del dispositivo")
            Spacer(Modifier.height(14.dp))
            Beneficio(Icons.Filled.Person, "Sin registro", "Úsalo de inmediato, sin cuentas")
            Spacer(Modifier.height(14.dp))
            Beneficio(Icons.Filled.WorkspacePremium, "Sin límites", "Archivos ilimitados, sin suscripción")
        }

        Spacer(Modifier.weight(0.5f))
        BotonPrincipal(texto = "Comenzar") {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Sin registro · Sin internet · Sin límites",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun Beneficio(icono: ImageVector, titulo: String, detalle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ChipIcono(icono, tamano = 42.dp)
        Spacer(Modifier.size(12.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
