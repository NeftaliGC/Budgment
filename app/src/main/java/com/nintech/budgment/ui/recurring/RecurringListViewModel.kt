package com.nintech.budgment.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.RecurringRepository
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.ui.common.dayTicks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringListUiState(
    val items: List<RecurringItem> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecurringListViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<RecurringListUiState> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(RecurringListUiState())
            } else {
                combine(
                    recurringRepository.observeRecurring(userId),
                    accountRepository.observeAccountsIncludingDeleted(userId),
                    categoryRepository.observeCategoriesIncludingDeleted(userId),
                    dayTicks()
                ) { recurrings, accounts, categories, now ->
                    RecurringListUiState(
                        items = buildRecurringItems(recurrings, accounts, categories, now),
                        isLoading = false
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecurringListUiState())

    fun setActive(id: String, active: Boolean) {
        viewModelScope.launch { recurringRepository.setActive(id, active) }
    }

    /** «Omitir esta vez»: pasa a la siguiente ocurrencia sin registrar nada. */
    fun skip(item: RecurringItem) {
        viewModelScope.launch { recurringRepository.skipOccurrence(item.recurring.id, item.recurring.nextOccurrence) }
    }

    fun delete(id: String) {
        viewModelScope.launch { recurringRepository.deleteRecurring(id) }
    }

    fun restore(id: String) {
        viewModelScope.launch { recurringRepository.restoreRecurring(id) }
    }
}
