package com.nintech.budgment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.security.AppLockManager
import com.nintech.budgment.security.LockState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SessionUiState {
    object Loading : SessionUiState
    data class LoggedIn(val userId: String) : SessionUiState
    object LoggedOut : SessionUiState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionManager: SessionManager,
    appLock: AppLockManager
) : ViewModel() {

    private var previous: SessionUiState = SessionUiState.Loading

    val state: StateFlow<SessionUiState> = sessionManager.currentUserId
        .map { id -> if (id != null) SessionUiState.LoggedIn(id) else SessionUiState.LoggedOut }
        .onEach { current ->
            // Quien acaba de iniciar sesión ya se identificó con su contraseña: no se le pide el PIN.
            if (previous == SessionUiState.LoggedOut && current is SessionUiState.LoggedIn) appLock.unlock()
            previous = current
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionUiState.Loading)

    val lockState: StateFlow<LockState> = appLock.state
}
