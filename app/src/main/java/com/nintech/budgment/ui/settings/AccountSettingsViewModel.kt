package com.nintech.budgment.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.repository.AuthRepository
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class AccountDialog { NONE, CHANGE_PASSWORD, PASSWORD_CHANGED, DELETE_USER }

data class AccountSettingsState(
    val dialog: AccountDialog = AccountDialog.NONE,
    val working: Boolean = false,
    /** La contraseña actual que se escribió no es la correcta. */
    val wrongPassword: Boolean = false,
    val failed: Boolean = false
)

/** Cambiar la contraseña y eliminar la cuenta del usuario que tiene la sesión abierta. */
@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(AccountSettingsState())
    val state: StateFlow<AccountSettingsState> = _state.asStateFlow()

    fun open(dialog: AccountDialog) {
        _state.value = AccountSettingsState(dialog = dialog)
    }

    fun dismiss() {
        if (!_state.value.working) _state.value = AccountSettingsState()
    }

    /** Al corregir la contraseña se quita el aviso de error. */
    fun onPasswordEdited() {
        _state.update { it.copy(wrongPassword = false, failed = false) }
    }

    fun changePassword(current: String, new: String) {
        if (_state.value.working) return
        _state.update { it.copy(working = true, wrongPassword = false, failed = false) }
        viewModelScope.launch {
            _state.value = try {
                val userId = sessionManager.currentUserId.first()
                if (userId != null && authRepository.changePassword(userId, current, new)) {
                    AccountSettingsState(dialog = AccountDialog.PASSWORD_CHANGED)
                } else {
                    _state.value.copy(working = false, wrongPassword = true)
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo cambiar la contraseña", e)
                _state.value.copy(working = false, failed = true)
            }
        }
    }

    fun deleteUser(password: String) {
        if (_state.value.working) return
        _state.update { it.copy(working = true, wrongPassword = false, failed = false) }
        viewModelScope.launch {
            try {
                val userId = sessionManager.currentUserId.first()
                // Cerrar la sesión destruye esta pantalla: que no se interrumpa a medias.
                val deleted = withContext(NonCancellable) {
                    val ok = userId != null && authRepository.deleteUser(userId, password)
                    if (ok) sessionManager.logout()
                    ok
                }
                if (!deleted) _state.update { it.copy(working = false, wrongPassword = true) }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo eliminar la cuenta", e)
                _state.update { it.copy(working = false, failed = true) }
            }
        }
    }

    private companion object {
        const val TAG = "AccountSettings"
    }
}
