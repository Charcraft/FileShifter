package com.example.fileshifter.presentation.image

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fileshifter.navigation.Screen
import com.example.fileshifter.presentation.common.Aviso
import com.example.fileshifter.presentation.common.PantallaBase
import com.example.fileshifter.presentation.common.TarjetaResultado
import com.example.fileshifter.presentation.common.appViewModel

/** F2 - Imagen a PDF Rápida: une imágenes o genera un PDF por imagen. */
@Composable
fun ImageToPdfScreen(navController: NavController) {
    val vm: ImageToPdfViewModel = appViewModel { ImageToPdfViewModel(it.documentoRepository) }
    val estado by vm.estado.collectAsStateWithLifecycle()

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        vm.onImagenesSeleccionadas(uris.map(Uri::toString))
    }

    PantallaBase(titulo = "Imagen a PDF", onVolver = { navController.popBackStack() }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Aviso("Añade solo imágenes. Puedes unirlas en un PDF o generar uno por imagen. 100% offline.")
            }
            item {
                Button(onClick = { selector.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (estado.imagenes.isEmpty()) "Añadir imágenes"
                        else "Cambiar imágenes (${estado.imagenes.size})"
                    )
                }
            }
            if (estado.imagenes.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = estado.unir, onClick = { vm.onUnirCambia(true) })
                        Text("Unir en un solo PDF")
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !estado.unir, onClick = { vm.onUnirCambia(false) })
                        Text("Uno por imagen")
                    }
                }
                items(estado.imagenes) { imagen ->
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            imagen.nombre,
                            Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                item {
                    Button(
                        onClick = { vm.convertir() },
                        enabled = !estado.cargando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (estado.cargando) "Convirtiendo..." else "Convertir a PDF")
                    }
                    if (estado.cargando) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
            estado.resultado?.let { resultado ->
                item {
                    TarjetaResultado(
                        resultado = resultado,
                        onValidar = { navController.navigate(Screen.Validator.route) },
                        onInicio = { navController.popBackStack() },
                        onCorregir = { navController.navigate(Screen.Corrector.route) },
                        onEntrega = { navController.navigate(Screen.Delivery1.route) }
                    )
                }
            }
        }
    }
}
