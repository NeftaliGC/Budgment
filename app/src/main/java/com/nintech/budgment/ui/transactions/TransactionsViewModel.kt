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
import com.nintech.budgment.ui.common.dayTicks
import javax.inject.Inject
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.local.entity.TransferEntity
import com.nintech.budgment.data.repository.TransferRepository
import com.nintech.budgment.data.model.SupportedCurrency

/** Datos de la otra cuenta cuando la fila representa una transferencia. */
data class TransferDetails(
    val transferId: String,
    val toAccountId: String,
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

/** Cuenta o categoría que se puede elegir en los filtros. */
data class FilterOption(
    val id: String,
    val name: String,
    val deleted: Boolean = false,
    val categoryType: CategoryType? = null,
    val icon: String? = null,
    val color: String? = null
)

/** Totales de lo filtrado; solo si todo está en una moneda (no se suman monedas distintas). */
data class FilterSummary(val count: Int, val income: Long?, val expense: Long?, val currency: String?)

data class TransactionsUiState(
    /** Movimientos ya filtrados. */
    val transactions: List<TransactionWithDetails> = emptyList(),
    /** Cuántos hay sin filtrar: distingue «no hay movimientos» de «ninguno coincide». */
    val totalCount: Int = 0,
    val filter: TransactionFilter = TransactionFilter(),
    val summary: FilterSummary? = null,
    val accountOptions: List<FilterOption> = emptyList(),
    val categoryOptions: List<FilterOption> = emptyList(),
    val isLoading: Boolean = true
)

/** Resumen de los resultados: cuántos y, si comparten moneda, cuánto entró y salió. */
fun summarize(items: List<TransactionWithDetails>): FilterSummary {
    val currencies = items.filter { it.transfer == null }.map { it.currency }.distinct()
    if (currencies.size != 1) return FilterSummary(items.size, null, null, null)
    val categorized = items.filter { it.transfer == null }
    return FilterSummary(
        count = items.size,
        income = categorized.filter { it.transaction.amount > 0 }.sumOf { it.transaction.amount },
        expense = categorized.filter { it.transaction.amount < 0 }.sumOf { -it.transaction.amount },
        currency = currencies.single()
    )
}

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
                        toAccountId = incoming.accountId,
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

    private val filter = MutableStateFlow(TransactionFilter())

    val uiState: StateFlow<TransactionsUiState> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(TransactionsUiState(isLoading = true))
            } else {
                val data = combine(
                    transactionRepository.observeTransactions(userId),
                    accountRepository.observeAccountsIncludingDeleted(userId),
                    categoryRepository.observeCategoriesIncludingDeleted(userId),
                    transferRepository.observeTransfers(userId)
                ) { transactions, accounts, categories, transfers ->
                    val details = buildTransactionDetails(transactions, accounts, categories, transfers)
                    // Las eliminadas solo se ofrecen si tienen movimientos: así se puede revisar su historial.
                    val usedAccounts = transactions.map { it.accountId }.toSet()
                    val usedCategories = transactions.mapNotNull { it.categoryId }.toSet()
                    val accountOptions = accounts
                        .filter { it.deletedAt == null || it.id in usedAccounts }
                        .sortedWith(compareBy({ it.deletedAt != null }, { it.name.lowercase() }))
                        .map { FilterOption(it.id, it.name, deleted = it.deletedAt != null) }
                    val categoryOptions = categories
                        .filter { it.deletedAt == null || it.id in usedCategories }
                        .sortedWith(compareBy({ it.type }, { it.deletedAt != null }, { it.name.lowercase() }))
                        .map { FilterOption(it.id, it.name, it.deletedAt != null, it.type, it.icon, it.color) }
                    Triple(details, accountOptions, categoryOptions)
                }
                combine(data, filter, dayTicks()) { (details, accountOptions, categoryOptions), currentFilter, now ->
                    val filtered = applyFilter(details, currentFilter, now)
                    TransactionsUiState(
                        transactions = filtered,
                        totalCount = details.size,
                        filter = currentFilter,
                        summary = if (currentFilter.isActive) summarize(filtered) else null,
                        accountOptions = accountOptions,
                        categoryOptions = categoryOptions,
                        isLoading = false
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsUiState())

    fun onQueryChange(query: String) = filter.update { it.copy(query = query) }
    fun onKindsChange(kinds: Set<TransactionKind>) = filter.update { it.copy(kinds = kinds) }
    fun onAccountsChange(ids: Set<String>) = filter.update { it.copy(accountIds = ids) }
    fun onCategoriesChange(ids: Set<String>) = filter.update { it.copy(categoryIds = ids) }
    fun onDateChange(date: DateFilter?) = filter.update { it.copy(date = date) }
    fun clearFilters() = filter.update { TransactionFilter() }

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