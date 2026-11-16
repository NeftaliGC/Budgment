package com.nintech.budgment.security

import android.os.SystemClock
import com.nintech.budgment.data.session.AppLockConfig
import com.nintech.budgment.data.session.AppLockSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

enum class LockState {
    /** Aún no se lee la configuración: no mostrar datos todavía. */
    LOADING,
    LOCKED,
    UNLOCKED
}

/**
 * Decide cuándo la app está bloqueada. Arranca bloqueada (si el bloqueo está activo) y vuelve
 * a bloquearse cuando pasa en segundo plano más tiempo del configurado.
 *
 * El tiempo se mide con el reloj de arranque del teléfono, que no cambia si el usuario ajusta la
 * hora. Si el sistema cierra el proceso, al volver se empieza bloqueada.
 */
@Singleton
class AppLockManager @Inject constructor(settings: AppLockSettings) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val config: StateFlow<AppLockConfig?> = settings.config
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val unlocked = MutableStateFlow(false)
    private var backgroundSince: Long? = null

    val state: StateFlow<LockState> = combine(config, unlocked) { cfg, isUnlocked ->
        when {
            cfg == null -> LockState.LOADING
            !cfg.enabled || isUnlocked -> LockState.UNLOCKED
            else -> LockState.LOCKED
        }
    }.stateIn(scope, SharingStarted.Eagerly, LockState.LOADING)

    /** El usuario se identificó (PIN, biometría, contraseña o al iniciar sesión). */
    fun unlock() {
        unlocked.value = true
    }

    fun onAppBackground() {
        backgroundSince = SystemClock.elapsedRealtime()
    }

    fun onAppForeground() {
        val since = backgroundSince ?: return
        backgroundSince = null
        val cfg = config.value ?: return
        if (cfg.enabled && AppLockPolicy.shouldRelock(SystemClock.elapsedRealtime() - since, cfg.timeoutSeconds)) {
            unlocked.value = false
        }
    }
}
