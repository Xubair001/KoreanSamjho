package com.koreansamjho.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.koreansamjho.app.SamjhoApp
import com.koreansamjho.app.di.AppContainer

/** Bridges manual DI into ViewModel construction without a DI framework. */
val CreationExtras.container: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SamjhoApp).container

inline fun <reified VM : ViewModel> samjhoFactory(crossinline create: (AppContainer) -> VM) =
    viewModelFactory { initializer { create(container) } }

/** Access to manual DI from composables that need a process singleton (e.g. TTS). */
@androidx.compose.runtime.Composable
fun localContainer(): AppContainer {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as SamjhoApp
    return app.container
}
