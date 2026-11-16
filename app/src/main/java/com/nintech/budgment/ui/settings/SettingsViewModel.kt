package com.nintech.budgment.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.data.repository.UserRepository
import com.nintech.budgment.data.session.AppLockConfig
import com.nintech.budgment.data.session.AppLockSettings
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.security.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val defaultCurrency: String = SupportedCurrency.MXN.code,
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    appLock: AppLockManager,
    private val lockSettings: AppLockSettings
) : ViewModel() {

    /** null mientras se lee. */
    val lockConfig: StateFlow<AppLockConfig?> = appLock.config

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { lockSettings.setBiometricEnabled(enabled) }
    }

    fun setLockTimeout(seconds: Int) {
        viewModelScope.launch { lockSettings.setTimeoutSeconds(seconds) }
    }

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = sessionManager.currentUserId.filterNotNull().first()
            val currency = userRepository.getUser(userId)?.defaultCurrency ?: SupportedCurrency.MXN.code
            _state.update { it.copy(defaultCurrency = currency, isLoading = false) }
        }
    }

    /**
     * Guarda la moneda por defecto del usuario y la aplica de inmediato a los totales,
     * reemplazando la que se hubiera elegido con el selector compacto.
     */
    fun onDefaultCurrencyChange(code: String) {
        _state.update { it.copy(defaultCurrency = code) }
        viewModelScope.launch {
            val userId = sessionManager.currentUserId.filterNotNull().first()
            userRepository.setDefaultCurrency(userId, code)
            sessionManager.setDisplayCurrency(code)
        }
    }
}
