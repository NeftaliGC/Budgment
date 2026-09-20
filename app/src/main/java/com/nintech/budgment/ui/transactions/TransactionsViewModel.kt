package com.nintech.budgment.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.TransactionRepository
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionWithDetails(
    val transaction: TransactionEntity,
    val accountName: String,
    val categoryName: String,
    val categoryType: CategoryType
)

data class TransactionsUiState(
    val transactions: List<TransactionWithDetails> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<TransactionsUiState> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(TransactionsUiState(isLoading = true))
            } else {
                combine(
                    transactionRepository.observeTransactions(userId),
                    accountRepository.observeAccounts(userId),
                    categoryRepository.observeCategories(userId)
                ) { transactions, accounts, categories ->
                    val accountsById = accounts.associateBy { it.id }
                    val categoriesById = categories.associateBy { it.id }
                    val details = transactions
                        .sortedByDescending { it.date }
                        .mapNotNull { tx ->
                            val category = categoriesById[tx.categoryId] ?: return@mapNotNull null
                            TransactionWithDetails(
                                transaction = tx,
                                accountName = accountsById[tx.accountId]?.name.orEmpty(),
                                categoryName = category.name,
                                categoryType = category.type
                            )
                        }
                    TransactionsUiState(transactions = details, isLoading = false)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsUiState())

    fun deleteTransaction(id: String) {
        viewModelScope.launch { transactionRepository.deleteTransaction(id) }
    }
}