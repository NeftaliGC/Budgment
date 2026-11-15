package com.nintech.budgment.ui.common

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Pestañas principales: el chrome (barra superior e inferior) es verde oscuro en ambos temas,
 * así que los iconos del sistema van siempre en claro.
 */
@Composable
fun ChromeSystemBars() {
    SystemBarsAppearance(lightStatusBarIcons = true, lightNavigationBarIcons = true)
}

/**
 * Pantallas con hero oscuro arriba y contenido del tema abajo (login).
 */
@Composable
fun HeroSystemBars() {
    SystemBarsAppearance(lightStatusBarIcons = true, lightNavigationBarIcons = isSystemInDarkTheme())
}

/**
 * Pantallas cuyo fondo bajo las barras del sistema es el del tema (formularios, registro, categorías).
 */
@Composable
fun SurfaceSystemBars() {
    val dark = isSystemInDarkTheme()
    SystemBarsAppearance(lightStatusBarIcons = dark, lightNavigationBarIcons = dark)
}

/**
 * Cada pantalla declara el color de los iconos del sistema que necesita. Se aplica al entrar en
 * composición y de nuevo cuando la pantalla vuelve a primer plano, porque es estado de la ventana
 * y la pantalla anterior pudo haberlo cambiado.
 */
@Composable
private fun SystemBarsAppearance(lightStatusBarIcons: Boolean, lightNavigationBarIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(view, lifecycleOwner, lightStatusBarIcons, lightNavigationBarIcons) {
        fun apply() {
            val activity = (view.context as? Activity) ?: return
            WindowCompat.getInsetsController(activity.window, view).apply {
                // "AppearanceLight" se refiere al fondo: true = fondo claro = iconos oscuros.
                isAppearanceLightStatusBars = !lightStatusBarIcons
                isAppearanceLightNavigationBars = !lightNavigationBarIcons
            }
        }

        apply()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) apply()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
