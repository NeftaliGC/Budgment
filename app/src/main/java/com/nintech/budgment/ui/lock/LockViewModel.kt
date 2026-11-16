package com.nintech.budgment.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.repository.AuthRepository
import com.nintech.budgment.data.session.AppLockSettings
import com.nintech.budgment.data.session.PinCheck
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.security.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LockMessage {
    data class WrongPin(val attemptsLeft: Int) : LockMessage
    data object LockedOut : LockMessage
}

data class LockUiState(
    val pinLength: Int = 0,
    val entered: String = "",
    val biometricEnabled: Boolean = false,
    val message: LockMessage? = null,
    /** Hasta cuándo (reloj del sistema) no se aceptan intentos. */
    val lockoutUntil: Long = 0,
    val isChecking: Boolean = false,
    /** Cambia con cada error para sacudir los puntos. */
    val errorCount: Int = 0
)

private data class Input(
    val entered: String = "",
    val message: LockMessage? = null,
    val isChecking: Boolean = false,
    val errorCount: Int = 0
)

@HiltViewModel
class LockViewModel @Inject constructor(
    private val appLock: AppLockManager,
    private val settings: AppLockSettings,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val input = MutableStateFlow(Input())

    val state: StateFlow<LockUiState> = combine(appLock.config.filterNotNull(), input) { cfg, inp ->
        LockUiState(
            pinLength = cfg.pinLength,
            entered = inp.entered,
            biometricEnabled = cfg.biometricEnabled,
            message = inp.message,
            lockoutUntil = cfg.lockoutUntil,
            isChecking = inp.isChecking,
            errorCount = inp.errorCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LockUiState())

    fun onDigit(digit: Char) {
        val current = state.value
        if (current.isChecking || current.pinLength == 0 || isLockedOut(current)) return
        val entered = current.entered + digit
        input.update { it.copy(entered = entered, message = null) }
        if (entered.length == current.pinLength) check(entered)
    }

    fun onDelete() {
        input.update { it.copy(entered = it.entered.dropLast(1)) }
    }

    fun onBiometricSuccess() = unlock()

    private fun check(pin: String) {
        input.update { it.copy(isChecking = true) }
        viewModelScope.launch {
            when (val result = settings.verifyPin(pin)) {
                PinCheck.Correct -> unlock()
                is PinCheck.Wrong -> fail(
                    if (result.lockoutUntil > 0) LockMessage.LockedOut else LockMessage.WrongPin(result.attemptsLeft)
                )
                is PinCheck.LockedOut -> fail(LockMessage.LockedOut)
            }
        }
    }

    private fun fail(message: LockMessage) {
        input.update { Input(message = message, errorCount = it.errorCount + 1) }
    }

    private fun unlock() {
        input.value = Input()
        appLock.unlock()
    }

    /**
     * «¿Olvidaste tu PIN?»: con la contraseña de la cuenta se desbloquea y se quita el PIN para
     * crear otro. Devuelve false si la contraseña no coincide.
     */
    suspend fun recoverWithPassword(password: String): Boolean {
        val userId = sessionManager.currentUserId.filterNotNull().first()
        if (!authRepository.verifyPassword(userId, password)) return false
        settings.disable()
        unlock()
        return true
    }

    private fun isLockedOut(state: LockUiState) = System.currentTimeMillis() < state.lockoutUntil
}
