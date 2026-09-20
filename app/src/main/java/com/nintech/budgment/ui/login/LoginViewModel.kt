package com.nintech.budgment.ui.login

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

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: AuthError? = null,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) { _state.value = _state.value.copy(username = value, errorMessage = null) }
    fun onPasswordChange(value: String) { _state.value = _state.value.copy(password = value, errorMessage = null) }
    fun onRememberMeChange(value: Boolean) { _state.value = _state.value.copy(rememberMe = value) }

    fun login() {
        val s = _state.value
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.login(s.username, s.password)) {
                is AuthResult.Success -> {
                    sessionManager.login(result.userId, s.rememberMe)
                    _state.value = _state.value.copy(isLoading = false, loginSuccess = true)
                }
                is AuthResult.Error -> _state.value = _state.value.copy(isLoading = false, errorMessage = result.reason)
            }
        }
    }
}
