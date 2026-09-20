package com.nintech.budgment.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.repository.AuthError
import com.nintech.budgment.data.repository.AuthRepository
import com.nintech.budgment.data.repository.AuthResult
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "", val username: String = "", val email: String = "",
    val password: String = "", val confirmPassword: String = "",
    val isLoading: Boolean = false, val errorMessage: AuthError? = null, val registerSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, errorMessage = null) }
    fun onUsernameChange(v: String) { _state.value = _state.value.copy(username = v, errorMessage = null) }
    fun onEmailChange(v: String) { _state.value = _state.value.copy(email = v) }
    fun onPasswordChange(v: String) { _state.value = _state.value.copy(password = v, errorMessage = null) }
    fun onConfirmPasswordChange(v: String) { _state.value = _state.value.copy(confirmPassword = v, errorMessage = null) }

    fun register() {
        val s = _state.value
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.register(s.name, s.username, s.email, s.password, s.confirmPassword)) {
                is AuthResult.Success -> {
                    sessionManager.login(result.userId, rememberMe = true) // al registrarse, queda logueado
                    _state.value = _state.value.copy(isLoading = false, registerSuccess = true)
                }
                is AuthResult.Error -> _state.value = _state.value.copy(isLoading = false, errorMessage = result.reason)
            }
        }
    }
}