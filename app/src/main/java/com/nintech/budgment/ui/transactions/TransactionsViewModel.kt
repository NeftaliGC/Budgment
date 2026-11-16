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
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.model.SupportedCurrency

data class TransactionWithDetails(
    val transaction: TransactionEntity,
    val accountName: String,
    val categoryName: String,
    val categoryType: CategoryType,
    val categoryIcon: String? = null,
    val categoryColor: String? = null,
    /** Moneda del movimiento: la propia si la tiene, si no la de su cuenta. */
    val currency: String = SupportedCurrency.MXN.code,
    /** La cuenta fue eliminada: el movimiento queda en el historial, solo de lectura. */
    val accountDeleted: Boolean = false,
    /** La categoría fue eliminada: el movimiento se conserva y se puede reasignar al editarlo. */
    val categoryDeleted: Boolean = false
)

data class TransactionsUiState(
    val transactions: List<TransactionWithDetails> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Une cada movimiento con los datos de su cuenta y categoría, del más reciente al más antiguo.
 * Las listas deben incluir cuentas y categorías eliminadas para que el historial esté completo.
 */
fun buildTransactionDetails(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>
): List<TransactionWithDetails> {
    val accountsById = accounts.associateBy { it.id }
    val categoriesById = categories.associateBy { it.id }
    return transactions
        .sortedByDescending { it.date }
        .mapNotNull { tx ->
            val category = categoriesById[tx.categoryId] ?: return@mapNotNull null
            val account = accountsById[tx.accountId]
            TransactionWithDetails(
                transaction = tx,
                accountName = account?.name.orEmpty(),
                categoryName = category.name,
                categoryType = category.type,
                categoryIcon = category.icon,
                categoryColor = category.color,
                currency = tx.currency ?: account?.currency ?: SupportedCurrency.MXN.code,
                accountDeleted = account?.deletedAt != null,
                categoryDeleted = category.deletedAt != null
            )
        }
}

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
                    accountRepository.observeAccountsIncludingDeleted(userId),
                    categoryRepository.observeCategoriesIncludingDeleted(userId)
                ) { transactions, accounts, categories ->
                    val details = buildTransactionDetails(transactions, accounts, categories)
                    TransactionsUiState(transactions = details, isLoading = false)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsUiState())

    fun deleteTransaction(id: String) {
        viewModelScope.launch { transactionRepository.deleteTransaction(id) }
    }

    fun restoreTransaction(id: String) {
        viewModelScope.launch { transactionRepository.restoreTransaction(id) }
    }
}