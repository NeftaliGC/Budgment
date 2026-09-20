package com.nintech.budgment.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val userRepository: UserRepository
) : ViewModel() {

    val userName: StateFlow<String> = sessionManager.currentUserId
        .flatMapLatest { id ->
            if (id == null) flowOf("")
            else flow { emit(userRepository.getUser(id)?.name.orEmpty()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun logout() {
        viewModelScope.launch { sessionManager.logout() }
    }
}