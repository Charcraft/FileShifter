package com.example.fileshifter.presentation.delivery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.model.PlantillaEntrega
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.BotonPrincipal
import com.example.fileshifter.presentation.common.CampoTexto
import com.example.fileshifter.presentation.common.ContenidoResponsive
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.Pasos
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.presentation.common.TarjetaResultado
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.ui.theme.Background
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.Navy
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.PrimaryLight
import com.example.fileshifter.ui.theme.Surface
import com.example.fileshifter.ui.theme.SurfaceMuted
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary

@Composable
private fun deliveryViewModel(navController: NavController): DeliveryViewModel {
    val padre = remember(navController) { navController.getBackStackEntry(Screen.DeliveryGraph.route) }
    return appViewModel(padre) { DeliveryViewModel(it.documentoRepository) }
}

@Composable
fun DeliveryStep1Screen(navController: NavController) {
    val vm = deliveryViewModel(navController)
    val estado by vm.estado.collectAsStateWithLifecycle()

    PantallaBase(titulo = "Modo Entrega", onVolver = { navController.popBackStack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Pasos(listOf("Plantilla", "Datos", "Vista previa"), actual = 1)

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                ContenidoResponsive {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Elige una plantilla de portada para el documento.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PlantillaEntrega.entries.forEach { plantilla ->
                                MiniPlantilla(
                                    plantilla = plantilla,
                                    seleccionada = estado.plantilla == plantilla,
                                    modifier = Modifier.weight(1f)
                                ) { vm.onPlantilla(plantilla) }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            Surface(color = Background) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    BotonPrincipal(texto = "Siguiente — Datos  ›") {
                        navController.navigate(Screen.Delivery2.route)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlantilla(
    plantilla: PlantillaEntrega,
    seleccionada: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorCabecera = when (plantilla) {
        PlantillaEntrega.ESCOLAR -> Primary
        PlantillaEntrega.OFICIO -> Navy
        PlantillaEntrega.MINIMAL -> Color(0xFFE8EDF2)
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (seleccionada) PrimaryLight else Surface,
        border = BorderStroke(if (seleccionada) 1.5.dp else 1.dp, if (seleccionada) Primary else Border)
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .background(Surface, RoundedCornerShape(8.dp))
            ) {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .background(colorCabecera, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    )
                    Column(Modifier.padding(8.dp)) {
                        Box(Modifier.fillMaxWidth(0.7f).height(5.dp).background(Border, RoundedCornerShape(3.dp)))
                        Spacer(Modifier.height(5.dp))
                        Box(Modifier.fillMaxWidth(0.9f).height(4.dp).background(SurfaceMuted, RoundedCornerShape(3.dp)))
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.fillMaxWidth(0.8f).height(4.dp).background(SurfaceMuted, RoundedCornerShape(3.dp)))
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.fillMaxWidth(0.6f).height(4.dp).background(SurfaceMuted, RoundedCornerShape(3.dp)))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                plantilla.etiqueta,
                style = MaterialTheme.typography.titleSmall,
                color = if (seleccionada) Primary else TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                when (plantilla) {
                    PlantillaEntrega.ESCOLAR -> "Universidad / Instituto"
                    PlantillaEntrega.OFICIO -> "Formal / Empresa"
                    PlantillaEntrega.MINIMAL -> "Sin encabezado"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
fun DeliveryStep2Screen(navController: NavController) {
    val vm = deliveryViewModel(navController)
    val estado by vm.estado.collectAsStateWithLifecycle()
    val datos = estado.datos

    PantallaBase(titulo = "Datos de la entrega", onVolver = { navController.popBackStack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Pasos(listOf("Plantilla", "Datos", "Vista previa"), actual = 2)

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                ContenidoResponsive {
                    Column(Modifier.padding(16.dp)) {
                        Tarjeta {
                            Campo("Nombre completo *", datos.nombre, vm::onNombre)
                            Spacer(Modifier.height(10.dp))
                            Campo("Matrícula", datos.matricula, vm::onMatricula)
                            Spacer(Modifier.height(10.dp))
                            Campo("Materia", datos.materia, vm::onMateria)
                            Spacer(Modifier.height(10.dp))
                            Campo("Título del trabajo *", datos.titulo, vm::onTitulo)
                            Spacer(Modifier.height(10.dp))
                            CampoTexto(
                                valor = datos.indice,
                                onCambia = vm::onIndice,
                                etiqueta = "Índice (una línea por punto)",
                                minLines = 4
                            )
                        }
                        if (!estado.formularioValido) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Completa el nombre y el título para continuar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            Surface(color = Background) {
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BotonPrincipal(
                        texto = "Siguiente — Vista previa  ›",
                        enabled = estado.formularioValido,
                        onClick = { navController.navigate(Screen.Delivery3.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Campo(etiqueta: String, valor: String, onCambia: (String) -> Unit) {
    CampoTexto(
        valor = valor,
        onCambia = onCambia,
        etiqueta = etiqueta.removeSuffix(" *"),
        singleLine = true
    )
}

@Composable
fun DeliveryStep3Screen(navController: NavController) {
    val vm = deliveryViewModel(navController)
    val estado by vm.estado.collectAsStateWithLifecycle()
    val datos = estado.datos

    PantallaBase(titulo = "Vista previa", onVolver = { navController.popBackStack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Pasos(listOf("Plantilla", "Datos", "Vista previa"), actual = 3)

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                ContenidoResponsive {
                    Column(Modifier.padding(16.dp)) {
                        Tarjeta(padding = 0.dp) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(if (estado.plantilla == PlantillaEntrega.OFICIO) Navy else Primary)
                            )
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    datos.materia.ifBlank { "Materia" },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Primary
                                )
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    datos.titulo.ifBlank { "Título del trabajo" },
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(18.dp))
                                Text("Alumno", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(datos.nombre, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                Spacer(Modifier.height(6.dp))
                                Text("Matrícula", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(datos.matricula, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                if (datos.indice.isNotBlank()) {
                                    Spacer(Modifier.height(18.dp))
                                    HorizontalDivider(color = Border)
                                    Spacer(Modifier.height(12.dp))
                                    Text("Índice", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Spacer(Modifier.height(6.dp))
                                    datos.indice.split("\n").filter { it.isNotBlank() }.forEach { linea ->
                                        Text("• $linea", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                                Spacer(Modifier.height(18.dp))
                                Text(
                                    "Plantilla: ${estado.plantilla.etiqueta}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        if (estado.cargando) {
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }

                        estado.resultado?.let {
                            Spacer(Modifier.height(12.dp))
                            TarjetaResultado(
                                resultado = it,
                                onValidar = { navController.navigate(Screen.Validator.route) },
                                onInicio = { navController.navigate(Screen.Home.route) }
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            Surface(color = Background) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    BotonPrincipal(
                        texto = if (estado.cargando) "Generando…" else "Generar PDF",
                        enabled = !estado.cargando,
                        onClick = { vm.generar() }
                    )
                }
            }
        }
    }
}
