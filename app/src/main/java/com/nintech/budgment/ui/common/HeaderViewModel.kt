package com.nintech.budgment.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.repository.NotificationRepository
import com.nintech.budgment.data.repository.RecurringRepository
import com.nintech.budgment.data.repository.UserRepository
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HeaderViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val recurringRepository: RecurringRepository
) : ViewModel() {

    val userName: StateFlow<String> = sessionManager.currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf("")
            else flow { emit(userRepository.getUser(id)?.name.orEmpty()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    /** Avisos sin leer: se marcan con un punto en el ícono de perfil. */
    val unreadNotifications: StateFlow<Int> = sessionManager.currentUserId
        .flatMapLatest { id -> if (id == null) flowOf(0) else notificationRepository.observeUnreadCount(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Con la app abierta la bandeja no espera al recordatorio diario: se actualiza al cambiar
        // los recurrentes y al empezar cada día.
        viewModelScope.launch {
            sessionManager.currentUserId
                .filterNotNull()
                .flatMapLatest { id -> combine(recurringRepository.observeRecurring(id), dayTicks()) { _, now -> id to now } }
                .collect { (id, now) -> notificationRepository.syncRecurring(id, now) }
        }
    }

    fun logout() {
        viewModelScope.launch { sessionManager.logout() }
    }
}
