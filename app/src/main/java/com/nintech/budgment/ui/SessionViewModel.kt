package com.nintech.budgment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SessionUiState {
    object Loading : SessionUiState
    data class LoggedIn(val userId: String) : SessionUiState
    object LoggedOut : SessionUiState
}

@HiltViewModel
class SessionViewModel @Inject constructor(sessionManager: SessionManager) : ViewModel() {
    val state: StateFlow<SessionUiState> = sessionManager.currentUserId
        .map { id -> if (id != null) SessionUiState.LoggedIn(id) else SessionUiState.LoggedOut }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionUiState.Loading)
}