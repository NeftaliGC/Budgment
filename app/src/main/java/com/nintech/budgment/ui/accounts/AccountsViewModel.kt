package com.nintech.budgment.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) flowOf(AccountsUiState(isLoading = true))
            else accountRepository.observeAccounts(userId).map { AccountsUiState(accounts = it, isLoading = false) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountsUiState())

    fun deleteAccount(id: String) {
        viewModelScope.launch { accountRepository.deleteAccount(id) }
    }
}