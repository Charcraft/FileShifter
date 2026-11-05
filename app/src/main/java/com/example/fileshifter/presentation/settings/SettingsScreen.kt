package com.example.fileshifter.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.presentation.common.Aviso
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.appViewModel

/** Ajustes: qué pasos opcionales sugiere el flujo. */
@Composable
fun SettingsScreen(navController: NavController) {
    val vm: SettingsViewModel = appViewModel { SettingsViewModel(it.ajustesRepository) }
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()

    PantallaBase(titulo = "Ajustes", onVolver = { navController.popBackStack() }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Aviso("Cada función trabaja sola. Aquí decides qué pasos opcionales se sugieren al terminar una conversión.")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    FilaAjuste(
                        titulo = "Sugerir Corrector",
                        detalle = "Al terminar, ofrece revisar la ortografía",
                        valor = ajustes.sugerirCorrector,
                        onCambia = vm::setSugerirCorrector
                    )
                    FilaAjuste(
                        titulo = "Sugerir Modo Entrega",
                        detalle = "Al terminar, ofrece generar una portada",
                        valor = ajustes.sugerirEntrega,
                        onCambia = vm::setSugerirEntrega
                    )
                    FilaAjuste(
                        titulo = "Sugerir Validador",
                        detalle = "Al terminar, ofrece revisar el PDF",
                        valor = ajustes.sugerirValidador,
                        onCambia = vm::setSugerirValidador
                    )
                    FilaAjuste(
                        titulo = "No preguntar más",
                        detalle = "Oculta las sugerencias del flujo",
                        valor = ajustes.noPreguntarMas,
                        onCambia = vm::setNoPreguntarMas
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "FileShifter · 253432 · Carlos de Jesús Martínez Pineda",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Arquitectura MVVM · Compose + Material 3 · 100% offline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilaAjuste(
    titulo: String,
    detalle: String,
    valor: Boolean,
    onCambia: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = valor, onCheckedChange = onCambia)
    }
}
