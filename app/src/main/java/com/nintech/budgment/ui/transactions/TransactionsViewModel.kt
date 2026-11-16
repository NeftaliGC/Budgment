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
import com.nintech.budgment.data.local.entity.TransferEntity
import com.nintech.budgment.data.repository.TransferRepository
import com.nintech.budgment.data.model.SupportedCurrency

/** Datos de la otra cuenta cuando la fila representa una transferencia. */
data class TransferDetails(
    val transferId: String,
    val toAccountName: String,
    val toAccountDeleted: Boolean = false,
    /** Lo que llegó al destino, en su moneda; difiere de lo que salió si las monedas no coinciden. */
    val amountIn: Long,
    val currencyIn: String
)

data class TransactionWithDetails(
    val transaction: TransactionEntity,
    /** En una transferencia, la cuenta de origen. */
    val accountName: String,
    /** Vacío en las transferencias, que no tienen categoría. */
    val categoryName: String = "",
    /** null solo en las transferencias. */
    val categoryType: CategoryType? = null,
    val categoryIcon: String? = null,
    val categoryColor: String? = null,
    /** Moneda del movimiento: la propia si la tiene, si no la de su cuenta. */
    val currency: String = SupportedCurrency.MXN.code,
    /** La cuenta fue eliminada: el movimiento queda en el historial, solo de lectura. */
    val accountDeleted: Boolean = false,
    /** La categoría fue eliminada: el movimiento se conserva y se puede reasignar al editarlo. */
    val categoryDeleted: Boolean = false,
    /** No nulo si la fila es una transferencia entre cuentas. */
    val transfer: TransferDetails? = null
) {
    /** Solo lectura si alguna de sus cuentas ya no existe. */
    val isReadOnly: Boolean get() = accountDeleted || transfer?.toAccountDeleted == true
}

data class TransactionsUiState(
    val transactions: List<TransactionWithDetails> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Une cada movimiento con los datos de su cuenta y categoría, del más reciente al más antiguo.
 * Las listas deben incluir cuentas y categorías eliminadas para que el historial esté completo.
 *
 * Cada transferencia aparece una sola vez, representada por su salida; su entrada se omite.
 */
fun buildTransactionDetails(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    transfers: List<TransferEntity> = emptyList()
): List<TransactionWithDetails> {
    val accountsById = accounts.associateBy { it.id }
    val categoriesById = categories.associateBy { it.id }
    val transactionsById = transactions.associateBy { it.id }
    val transfersByOutgoingId = transfers.associateBy { it.fromTransactionId }

    fun currencyOf(tx: TransactionEntity) =
        tx.currency ?: accountsById[tx.accountId]?.currency ?: SupportedCurrency.MXN.code

    return transactions
        .sortedByDescending { it.date }
        .mapNotNull { tx ->
            val account = accountsById[tx.accountId]
            val categoryId = tx.categoryId
            if (categoryId == null) {
                // Sin categoría solo puede ser parte de una transferencia: se muestra la salida.
                val transfer = transfersByOutgoingId[tx.id] ?: return@mapNotNull null
                val incoming = transactionsById[transfer.toTransactionId] ?: return@mapNotNull null
                val toAccount = accountsById[incoming.accountId]
                return@mapNotNull TransactionWithDetails(
                    transaction = tx,
                    accountName = account?.name.orEmpty(),
                    currency = currencyOf(tx),
                    accountDeleted = account?.deletedAt != null,
                    transfer = TransferDetails(
                        transferId = transfer.id,
                        toAccountName = toAccount?.name.orEmpty(),
                        toAccountDeleted = toAccount?.deletedAt != null,
                        amountIn = incoming.amount,
                        currencyIn = currencyOf(incoming)
                    )
                )
            }
            val category = categoriesById[categoryId] ?: return@mapNotNull null
            TransactionWithDetails(
                transaction = tx,
                accountName = account?.name.orEmpty(),
                categoryName = category.name,
                categoryType = category.type,
                categoryIcon = category.icon,
                categoryColor = category.color,
                currency = currencyOf(tx),
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
    private val transferRepository: TransferRepository,
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
                    categoryRepository.observeCategoriesIncludingDeleted(userId),
                    transferRepository.observeTransfers(userId)
                ) { transactions, accounts, categories, transfers ->
                    val details = buildTransactionDetails(transactions, accounts, categories, transfers)
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

    /** Borra las dos patas de la transferencia y revierte ambos saldos. */
    fun deleteTransfer(id: String) {
        viewModelScope.launch { transferRepository.deleteTransfer(id) }
    }

    fun restoreTransfer(id: String) {
        viewModelScope.launch { transferRepository.restoreTransfer(id) }
    }
}