package com.nintech.budgment

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nintech.budgment.security.AppLockManager
import com.nintech.budgment.ui.BudgmentRoot
import com.nintech.budgment.ui.theme.BudgmentTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

// FragmentActivity (y no solo ComponentActivity): el diálogo de huella o rostro lo necesita.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var appLock: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        hideFromRecentsWhenLocked()
        setContent {
            BudgmentTheme {
                BudgmentRoot()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLock.onAppForeground()
    }

    override fun onStop() {
        super.onStop()
        // Girar la pantalla también detiene la actividad: eso no cuenta como salir de la app.
        if (!isChangingConfigurations) appLock.onAppBackground()
    }

    /**
     * Con el bloqueo activo, la vista de apps recientes no muestra saldos (Android 13+). No usa
     * FLAG_SECURE para no impedir las capturas de pantalla.
     */
    private fun hideFromRecentsWhenLocked() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                appLock.config.filterNotNull().map { it.enabled }.distinctUntilChanged().collect { enabled ->
                    setRecentsScreenshotEnabled(!enabled)
                }
            }
        }
    }
}
