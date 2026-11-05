package com.example.fileshifter.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.fileshifter.presentation.convert.ConvertScreen
import com.example.fileshifter.presentation.corrector.CorrectorScreen
import com.example.fileshifter.presentation.delivery.DeliveryStep1Screen
import com.example.fileshifter.presentation.delivery.DeliveryStep2Screen
import com.example.fileshifter.presentation.delivery.DeliveryStep3Screen
import com.example.fileshifter.presentation.edit.EditModeScreen
import com.example.fileshifter.presentation.edit.EditPickerScreen
import com.example.fileshifter.presentation.edit.EditTypeScreen
import com.example.fileshifter.presentation.history.HistoryScreen
import com.example.fileshifter.presentation.home.HomeScreen
import com.example.fileshifter.presentation.image.ImageToPdfScreen
import com.example.fileshifter.presentation.qr.QrScreen
import com.example.fileshifter.presentation.scanner.ScannerScreen
import com.example.fileshifter.presentation.settings.SettingsScreen
import com.example.fileshifter.presentation.validator.ValidatorScreen

/** Rutas de navegación de la app (una sola Activity, pila de navegación). */
sealed class Screen(val route: String, val label: String) {
    data object Home : Screen("home", "Inicio")
    data object Convert : Screen("convert", "Convertir")
    data object Image : Screen("image", "Imagen")
    data object EditGraph : Screen("edit_flow", "Editar")
    data object EditPicker : Screen("edit_picker", "Selector")
    data object EditType : Screen("edit_type", "Tipo")
    data object EditMode : Screen("edit_mode", "Modo")
    data object Corrector : Screen("corrector", "Corrector")
    data object DeliveryGraph : Screen("delivery_flow", "Entrega")
    data object Delivery1 : Screen("delivery_1", "Plantilla")
    data object Delivery2 : Screen("delivery_2", "Datos")
    data object Delivery3 : Screen("delivery_3", "Vista previa")
    data object Validator : Screen("validator", "Validador")
    data object Scanner : Screen("scanner", "Escáner")
    data object Qr : Screen("qr", "QR")
    data object History : Screen("history", "Historial")
    data object Settings : Screen("settings", "Ajustes")
}

@Composable
fun AppNav(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
        }
    ) {
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Convert.route) { ConvertScreen(navController) }
        composable(Screen.Image.route) { ImageToPdfScreen(navController) }

        // Flujo Editar PDF: un grafo con ViewModel compartido entre los 3 pasos.
        navigation(startDestination = Screen.EditPicker.route, route = Screen.EditGraph.route) {
            composable(Screen.EditPicker.route) { EditPickerScreen(navController) }
            composable(Screen.EditType.route) { EditTypeScreen(navController) }
            composable(Screen.EditMode.route) { EditModeScreen(navController) }
        }

        // Flujo Modo Entrega: mismo patrón.
        navigation(startDestination = Screen.Delivery1.route, route = Screen.DeliveryGraph.route) {
            composable(Screen.Delivery1.route) { DeliveryStep1Screen(navController) }
            composable(Screen.Delivery2.route) { DeliveryStep2Screen(navController) }
            composable(Screen.Delivery3.route) { DeliveryStep3Screen(navController) }
        }

        composable(Screen.Corrector.route) { CorrectorScreen(navController) }
        composable(Screen.Validator.route) { ValidatorScreen(navController) }
        composable(Screen.Scanner.route) { ScannerScreen(navController) }
        composable(Screen.Qr.route) { QrScreen(navController) }
        composable(Screen.History.route) { HistoryScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
    }
}
