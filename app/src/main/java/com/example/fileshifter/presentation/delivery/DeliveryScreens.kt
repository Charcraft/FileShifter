package com.example.fileshifter.presentation.delivery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.data.model.DatosEntrega
import com.example.fileshifter.data.model.PlantillaEntrega
import com.example.fileshifter.data.model.PosicionLogo
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.BotonPrincipal
import com.example.fileshifter.presentation.common.BotonSecundario
import com.example.fileshifter.presentation.common.CampoTexto
import com.example.fileshifter.presentation.common.ContenidoResponsive
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.Pasos
import com.example.fileshifter.presentation.common.Tarjeta
import com.example.fileshifter.presentation.common.TarjetaResultado
import com.example.fileshifter.presentation.common.appViewModel
import com.example.fileshifter.ui.theme.Background
import com.example.fileshifter.ui.theme.Border
import com.example.fileshifter.ui.theme.Primary
import com.example.fileshifter.ui.theme.PrimaryLight
import com.example.fileshifter.ui.theme.Surface
import com.example.fileshifter.ui.theme.SurfaceMuted
import com.example.fileshifter.ui.theme.TextMuted
import com.example.fileshifter.ui.theme.TextPrimary
import com.example.fileshifter.ui.theme.TextSecondary

private val DATOS_EJEMPLO = DatosEntrega(
    institucion = "Universidad Nacional",
    carrera = "Ingeniería en Sistemas",
    materia = "Programación Avanzada",
    titulo = "Sistema de gestión documental",
    nombre = "Ana López",
    matricula = "2024-0123",
    docente = "Mtra. Ruiz",
    fecha = "Noviembre 2026"
)

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
                        Nota("La portada se arma sola: los datos se acomodan de arriba hacia abajo y se centran automáticamente.")
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            Surface(color = Background) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    BotonPrincipal(texto = "Siguiente — Datos ›") {
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
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (seleccionada) PrimaryLight else Surface,
        border = BorderStroke(if (seleccionada) 1.5.dp else 1.dp, if (seleccionada) Primary else Border)
    ) {
        Column(Modifier.padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            PortadaPreview(
                plantilla = plantilla,
                datos = DATOS_EJEMPLO,
                modifier = Modifier.fillMaxWidth(),
                mostrarSombra = false
            )
            Spacer(Modifier.height(8.dp))
            Text(
                plantilla.etiqueta,
                style = MaterialTheme.typography.titleSmall,
                color = if (seleccionada) Primary else TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                plantilla.descripcion,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 0.sp),
                color = TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp)
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
                            Subtitulo("Institución")
                            Campo("Institución", datos.institucion, vm::onInstitucion, "Ej. Universidad Nacional")
                            Spacer(Modifier.height(10.dp))
                            Campo("Carrera", datos.carrera, vm::onCarrera, "Ej. Ingeniería en Sistemas")
                        }
                        Spacer(Modifier.height(12.dp))

                        Tarjeta {
                            Subtitulo("Trabajo")
                            Campo("Materia", datos.materia, vm::onMateria, "Ej. Programación Avanzada")
                            Spacer(Modifier.height(10.dp))
                            Campo("Título del trabajo *", datos.titulo, vm::onTitulo, "Ej. Sistema de gestión documental")
                        }
                        Spacer(Modifier.height(12.dp))

                        Tarjeta {
                            Subtitulo("Autor")
                            Campo("Nombre completo *", datos.nombre, vm::onNombre, "Ej. Ana López")
                            Spacer(Modifier.height(10.dp))
                            Campo("Matrícula", datos.matricula, vm::onMatricula, "Ej. 2024-0123")
                            Spacer(Modifier.height(10.dp))
                            Campo("Docente", datos.docente, vm::onDocente, "Ej. Mtra. Ruiz")
                            Spacer(Modifier.height(10.dp))
                            Campo("Fecha", datos.fecha, vm::onFecha, "Ej. Noviembre 2026")
                        }
                        Spacer(Modifier.height(12.dp))

                        Tarjeta {
                            Subtitulo("Contenido adicional")
                            CampoTexto(
                                valor = datos.textoExtra,
                                onCambia = vm::onTextoExtra,
                                etiqueta = "Texto adicional (opcional)",
                                minLines = 3
                            )
                            Spacer(Modifier.height(10.dp))
                            CampoTexto(
                                valor = datos.indice,
                                onCambia = vm::onIndice,
                                etiqueta = "Índice (una línea por punto)",
                                minLines = 4
                            )
                            Text(
                                "Todo lo que agregues aquí aparece debajo de los datos, en el mismo orden.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 8.dp)
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
                        texto = "Siguiente — Vista previa ›",
                        enabled = estado.formularioValido,
                        onClick = { navController.navigate(Screen.Delivery3.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    onCambia: (String) -> Unit,
    placeholder: String = "Escribe aquí…"
) {
    CampoTexto(
        valor = valor,
        onCambia = onCambia,
        etiqueta = etiqueta,
        singleLine = true,
        placeholder = placeholder
    )
}

@Composable
fun DeliveryStep3Screen(navController: NavController) {
    val vm = deliveryViewModel(navController)
    val estado by vm.estado.collectAsStateWithLifecycle()
    val datos = estado.datos
    val selectorLogo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.onLogo(uri.toString())
    }

    PantallaBase(titulo = "Vista previa", onVolver = { navController.popBackStack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Pasos(listOf("Plantilla", "Datos", "Vista previa"), actual = 3)

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                ContenidoResponsive {
                    Column(Modifier.padding(16.dp)) {
                        Tarjeta {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Logo de la portada",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (datos.logoUri != null) {
                                    TextButton(onClick = { vm.onLogo(null) }) {
                                        Text("Quitar", color = TextSecondary)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            BotonSecundario(
                                texto = if (datos.logoUri == null) "Elegir logo" else "Cambiar logo",
                                modifier = Modifier.fillMaxWidth()
                            ) { selectorLogo.launch("image/*") }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "POSICIÓN",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(7.dp))
                            PosicionesLogo(
                                seleccionada = datos.posicionLogo,
                                habilitado = datos.logoUri != null
                            ) { vm.onPosicionLogo(it) }

                            Spacer(Modifier.height(8.dp))
                            Text(
                                when {
                                    datos.logoUri == null ->
                                        "Usa un PNG con fondo transparente para que se vea limpio."
                                    datos.posicionLogo == PosicionLogo.FONDO_CENTRO ->
                                        "El logo va detrás del texto, atenuado como marca de agua."
                                    else ->
                                        "El logo se coloca en la esquina elegida, sin encimarse con los datos."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Así se verá tu portada",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f).semantics { heading() }
                            )
                            Surface(color = PrimaryLight, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    "A4",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        PortadaPreview(plantilla = estado.plantilla, datos = datos)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "La vista previa usa el mismo diseño que el PDF final.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PosicionesLogo(
    seleccionada: PosicionLogo,
    habilitado: Boolean,
    onSeleccion: (PosicionLogo) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PosicionLogo.entries.forEach { posicion ->
            val activa = seleccionada == posicion
            val puedeElegir = habilitado || posicion == PosicionLogo.NINGUNA
            Surface(
                onClick = { onSeleccion(posicion) },
                enabled = puedeElegir,
                shape = RoundedCornerShape(12.dp),
                color = when {
                    activa -> PrimaryLight
                    puedeElegir -> Surface
                    else -> SurfaceMuted
                },
                border = BorderStroke(1.dp, if (activa) Primary else Border)
            ) {
                Text(
                    posicion.etiqueta,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (activa) Primary else if (puedeElegir) TextSecondary else TextMuted,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun Subtitulo(texto: String) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Primary,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Nota(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted
    )
}
