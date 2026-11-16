package com.nintech.budgment.ui.lock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.session.AppLockSettings
import com.nintech.budgment.data.session.PinCheck
import com.nintech.budgment.security.AppLockManager
import com.nintech.budgment.security.AppLockPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PinSetupMode { CREATE, CHANGE, DISABLE }

enum class PinSetupStep { CURRENT, NEW, CONFIRM }

sealed interface PinSetupError {
    data class WrongCurrent(val attemptsLeft: Int) : PinSetupError
    data class LockedOut(val until: Long) : PinSetupError
    data object Mismatch : PinSetupError
}

data class PinSetupUiState(
    val mode: PinSetupMode = PinSetupMode.CREATE,
    val step: PinSetupStep = PinSetupStep.NEW,
    val entered: String = "",
    /** Largo del PIN actual (paso CURRENT) o del elegido (paso CONFIRM). */
    val expectedLength: Int = 0,
    val error: PinSetupError? = null,
    val errorCount: Int = 0,
    val isWorking: Boolean = false,
    val isDone: Boolean = false
)

/**
 * Crear PIN: elegir (4 a 6 dígitos) → repetir. Cambiar: PIN actual → elegir → repetir.
 * Desactivar: PIN actual. Pedir el actual evita que alguien con el teléfono desbloqueado
 * cambie o quite el bloqueo.
 */
@HiltViewModel
class PinSetupViewModel @Inject constructor(
    private val settings: AppLockSettings,
    private val appLock: AppLockManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mode = PinSetupMode.valueOf(
        savedStateHandle.get<String>(ARG_MODE)?.uppercase() ?: PinSetupMode.CREATE.name
    )
    private var chosenPin = ""

    private val _state = MutableStateFlow(
        PinSetupUiState(mode = mode, step = if (mode == PinSetupMode.CREATE) PinSetupStep.NEW else PinSetupStep.CURRENT)
    )
    val state: StateFlow<PinSetupUiState> = _state.asStateFlow()

    init {
        if (mode != PinSetupMode.CREATE) {
            viewModelScope.launch {
                val length = appLock.config.filterNotNull().first().pinLength
                _state.update { it.copy(expectedLength = length) }
            }
        }
    }

    fun onDigit(digit: Char) {
        val s = _state.value
        if (s.isWorking || s.isDone) return
        val limit = if (s.step == PinSetupStep.NEW) AppLockPolicy.MAX_PIN_LENGTH else s.expectedLength
        if (limit == 0 || s.entered.length >= limit) return
        val entered = s.entered + digit
        _state.update { it.copy(entered = entered, error = if (it.step == PinSetupStep.NEW) it.error else null) }
        when (s.step) {
            PinSetupStep.CURRENT -> if (entered.length == s.expectedLength) checkCurrent(entered)
            PinSetupStep.NEW -> if (entered.length == AppLockPolicy.MAX_PIN_LENGTH) onContinue()
            PinSetupStep.CONFIRM -> if (entered.length == s.expectedLength) confirm(entered)
        }
    }

    fun onDelete() {
        _state.update { it.copy(entered = it.entered.dropLast(1)) }
    }

    /** Paso NEW: acepta el PIN elegido (con 4 o 5 dígitos hay que pulsar Continuar). */
    fun onContinue() {
        val pin = _state.value.entered
        if (_state.value.step != PinSetupStep.NEW || !AppLockPolicy.isValidPin(pin)) return
        chosenPin = pin
        _state.update { it.copy(step = PinSetupStep.CONFIRM, entered = "", expectedLength = pin.length, error = null) }
    }

    private fun checkCurrent(pin: String) {
        _state.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            when (val result = settings.verifyPin(pin)) {
                PinCheck.Correct -> if (mode == PinSetupMode.DISABLE) {
                    settings.disable()
                    _state.update { it.copy(isWorking = false, isDone = true) }
                } else {
                    _state.update { it.copy(isWorking = false, step = PinSetupStep.NEW, entered = "", expectedLength = 0) }
                }
                is PinCheck.Wrong -> fail(
                    if (result.lockoutUntil > 0) PinSetupError.LockedOut(result.lockoutUntil)
                    else PinSetupError.WrongCurrent(result.attemptsLeft)
                )
                is PinCheck.LockedOut -> fail(PinSetupError.LockedOut(result.until))
            }
        }
    }

    private fun confirm(pin: String) {
        if (pin != chosenPin) {
            chosenPin = ""
            _state.update {
                it.copy(step = PinSetupStep.NEW, entered = "", expectedLength = 0, error = PinSetupError.Mismatch, errorCount = it.errorCount + 1)
            }
            return
        }
        _state.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            // Primero se marca desbloqueada: al activar el bloqueo no debe aparecer la pantalla de PIN.
            appLock.unlock()
            settings.setPin(pin)
            _state.update { it.copy(isWorking = false, isDone = true) }
        }
    }

    private fun fail(error: PinSetupError) {
        _state.update { it.copy(isWorking = false, entered = "", error = error, errorCount = it.errorCount + 1) }
    }

    companion object {
        const val ARG_MODE = "mode"
    }
}
