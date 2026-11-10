package com.example.fileshifter.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fileshifter.AppContainer
import com.example.fileshifter.FileShifterApp

@Composable
inline fun <reified VM : ViewModel> appViewModel(
    viewModelStoreOwner: ViewModelStoreOwner? = null,
    crossinline crear: (AppContainer) -> VM
): VM {
    val contenedor = (LocalContext.current.applicationContext as FileShifterApp).container
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = crear(contenedor) as T
    }
    return if (viewModelStoreOwner == null) {
        viewModel(factory = factory)
    } else {
        viewModel(viewModelStoreOwner = viewModelStoreOwner, factory = factory)
    }
}
