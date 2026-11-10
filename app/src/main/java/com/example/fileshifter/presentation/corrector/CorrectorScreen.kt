package com.example.fileshifter.presentation.corrector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.presentation.common.BorradorTexto
import com.example.fileshifter.presentation.common.BotonPrincipal
import com.example.fileshifter.presentation.common.BotonSecundario
import com.example.fileshifter.presentation.common.CampoTexto
import com.example.fileshifter.presentation.common.ContenidoResponsive
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.ui.theme.Amber
import com.example.fileshifter.ui.theme.AmberBg
import com.example.fileshifter.ui.theme.AmberText
import com.example.fileshifter.ui.theme.Background
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary

/** F4 - Corrector con la vista del prototipo (P8). */
@Composable
fun CorrectorScreen(navController: NavController) {
    val vm: CorrectorViewModel = appViewModel { CorrectorViewModel() }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val teclado = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (BorradorTexto.texto.isNotBlank()) {
            vm.onTextoInicial(BorradorTexto.texto)
            BorradorTexto.texto = ""
        }
    }

    PantallaBase(titulo = "Corrector ortográfico", onVolver = { navController.popBackStack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (estado.revisado) {
                Surface(color = AmberBg) {
                    val mensaje = if (estado.sugerencias.isEmpty()) {
                        "Sin errores detectados"
                    } else {
                        "${estado.sugerencias.size} errores detectados"
                    }
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(mensaje) }
                            if (estado.sugerencias.isNotEmpty()) {
                                append("  ·  Toca una palabra subrayada para corregir")
                            }
                        },
                        color = AmberText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                ContenidoResponsive {
                    Column(Modifier.padding(16.dp)) {
                        Tarjeta(padding = 12.dp) {
                            CampoTexto(
                                valor = estado.texto,
                                onCambia = vm::onTextoCambia,
                                etiqueta = "Texto a revisar",
                                minLines = 3,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    teclado?.hide()
                                    vm.revisar()
                                }, enabled = estado.texto.isNotBlank()) {
                                    Text("Revisar", color = Primary)
                                }
                            }
                        }

                        if (estado.texto.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Tarjeta {
                                Text(
                                    "PÁRRAFO 1 DE ${estado.texto.split("\n\n").size.coerceAtLeast(1)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Spacer(Modifier.height(10.dp))
                                val texto = estado.texto
                                val anotado = buildAnnotatedString {
                                    append(texto)
                                    estado.sugerencias.forEach { sugerencia ->
                                        if (sugerencia.fin <= texto.length && sugerencia.inicio < sugerencia.fin) {
                                            addStyle(
                                                SpanStyle(
                                                    color = AmberText,
                                                    fontWeight = FontWeight.SemiBold,
                                                    textDecoration = TextDecoration.Underline
                                                ),
                                                sugerencia.inicio,
                                                sugerencia.fin
                                            )
                                        }
                                    }
                                }
                                Text(
                                    anotado,
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp)
                                )
                            }
                        }

                        if (estado.sugerencias.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Tarjeta(padding = 0.dp) {
                                Text(
                                    "Resumen de errores",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                                HorizontalDivider(color = Border)
                                estado.sugerencias.forEachIndexed { indice, sugerencia ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.foundation.layout.Box(
                                            Modifier.size(8.dp)
                                                .background(Amber, CircleShape)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                sugerencia.original,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary
                                            )
                                            Text(
                                                "→ ${sugerencia.correccion.ifBlank { "(eliminar)" }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                            )
                                        }
                                        TextButton(onClick = { vm.aplicarUna(sugerencia) }) {
                                            Text("Corregir", color = Primary)
                                        }
                                    }
                                    if (indice < estado.sugerencias.lastIndex) {
                                        HorizontalDivider(color = Border)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            Surface(color = Background) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BotonSecundario(
                        texto = "Omitir",
                        modifier = Modifier.weight(1f),
                        onClick = { vm.omitir() }
                    )
                    BotonPrincipal(
                        texto = "Aplicar",
                        modifier = Modifier.weight(1.6f),
                        enabled = estado.sugerencias.isNotEmpty(),
                        onClick = { vm.aplicarTodas() }
                    )
                }
            }
        }
    }
}
