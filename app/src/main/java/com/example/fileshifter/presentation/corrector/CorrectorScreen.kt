package com.example.fileshifter.presentation.corrector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.presentation.common.Aviso
import com.example.fileshifter.presentation.common.BorradorTexto
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.appViewModel

/** F4 - Corrector ortográfico y gramatical en español (offline). */
@Composable
fun CorrectorScreen(navController: NavController) {
    val vm: CorrectorViewModel = appViewModel { CorrectorViewModel() }
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (BorradorTexto.texto.isNotBlank()) {
            vm.onTextoInicial(BorradorTexto.texto)
            BorradorTexto.texto = ""
        }
    }

    PantallaBase(titulo = "Corrector", onVolver = { navController.popBackStack() }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Aviso("Pega el texto o tráelo del Escáner. El corrector revisa tildes, palabras repetidas y errores frecuentes sin conexión.")
            }
            item {
                OutlinedTextField(
                    value = estado.texto,
                    onValueChange = vm::onTextoCambia,
                    label = { Text("Texto a revisar") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.revisar() }, enabled = estado.texto.isNotBlank()) {
                        Text("Revisar")
                    }
                    OutlinedButton(onClick = { vm.aplicarTodas() }, enabled = estado.sugerencias.isNotEmpty()) {
                        Text("Aplicar todas")
                    }
                    TextButton(onClick = { vm.omitir() }, enabled = estado.sugerencias.isNotEmpty()) {
                        Text("Omitir")
                    }
                }
            }
            if (estado.revisado) {
                item {
                    Text(
                        if (estado.sugerencias.isEmpty()) "Sin errores detectados."
                        else "${estado.sugerencias.size} sugerencia(s). Toca una para aplicarla.",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                if (estado.sugerencias.isNotEmpty()) {
                    item {
                        val colorError = MaterialTheme.colorScheme.error
                        val anotado = buildAnnotatedString {
                            append(estado.texto)
                            estado.sugerencias.forEach { sugerencia ->
                                if (sugerencia.fin <= estado.texto.length) {
                                    addStyle(
                                        SpanStyle(
                                            color = colorError,
                                            textDecoration = TextDecoration.Underline
                                        ),
                                        sugerencia.inicio,
                                        sugerencia.fin
                                    )
                                }
                            }
                        }
                        Card(Modifier.fillMaxWidth()) {
                            Text(
                                anotado,
                                Modifier.padding(12.dp).height(140.dp).verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    items(estado.sugerencias) { sugerencia ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text(
                                    "${sugerencia.original} → ${sugerencia.correccion.ifBlank { "(eliminar)" }}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    sugerencia.motivo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                TextButton(onClick = { vm.aplicarUna(sugerencia) }) { Text("Aplicar") }
                            }
                        }
                    }
                }
            }
        }
    }
}
