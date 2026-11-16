package com.nintech.budgment.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.local.entity.RecurringTransactionEntity
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.data.model.Recurrence
import com.nintech.budgment.data.model.RecurrenceFrequency
import com.nintech.budgment.data.model.frequency
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.RecurringRepository
import com.nintech.budgment.data.repository.TransactionRepository
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.ui.common.centsToInputText
import com.nintech.budgment.ui.common.parseAmountToCents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

enum class AmountError { REQUIRED, INVALID }

/** Qué hace el formulario. Todos comparten campos; cambian el título, la fecha y al guardar. */
enum class TransactionFormKind {
    /** Movimiento nuevo; puede marcarse para repetirse. */
    NEW,
    EDIT,
    /** Confirmar una ocurrencia vencida de un recurrente, ajustando lo que haga falta. */
    OCCURRENCE,
    RECURRING_NEW,
    RECURRING_EDIT;

    val isRecurringTemplate: Boolean get() = this == RECURRING_NEW || this == RECURRING_EDIT
}

data class TransactionFormState(
    val kind: TransactionFormKind = TransactionFormKind.NEW,
    /** Gasto o ingreso. Decide qué categorías se ofrecen y el signo del monto. */
    val type: CategoryType = CategoryType.EXPENSE,
    val amountText: String = "",
    val categoryId: String? = null,
    val accountId: String? = null,
    /** En un recurrente, la fecha desde la que se repite. */
    val dateMillis: Long = System.currentTimeMillis(),
    val description: String = "",
    /** null = no se repite. En un recurrente siempre tiene valor. */
    val frequency: RecurrenceFrequency? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val amountError: AmountError? = null,
    val categoryError: Boolean = false,
    val accountError: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true
) {
    val isEditing: Boolean get() = kind != TransactionFormKind.NEW && kind != TransactionFormKind.RECURRING_NEW

    /** El selector de frecuencia aparece al crear un movimiento y en los recurrentes. */
    val showsFrequency: Boolean
        get() = kind == TransactionFormKind.NEW || kind.isRecurringTemplate

    /** Categorías del tipo elegido: las únicas que se pueden asignar. */
    val categoriesOfType: List<CategoryEntity> get() = categories.filter { it.type == type }
    val selectedAccount: AccountEntity? get() = accounts.firstOrNull { it.id == accountId }
}

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: String? = savedStateHandle.get<String>(ARG_TRANSACTION_ID)?.ifBlank { null }
    private val recurringId: String? = savedStateHandle.get<String>(ARG_RECURRING_ID)?.ifBlank { null }
    private val mode: String? = savedStateHandle.get<String>(ARG_MODE)?.ifBlank { null }

    private val kind = when {
        transactionId != null -> TransactionFormKind.EDIT
        mode == MODE_OCCURRENCE && recurringId != null -> TransactionFormKind.OCCURRENCE
        mode == MODE_TEMPLATE && recurringId != null -> TransactionFormKind.RECURRING_EDIT
        mode == MODE_TEMPLATE -> TransactionFormKind.RECURRING_NEW
        else -> TransactionFormKind.NEW
    }

    private var existingTransaction: TransactionEntity? = null
    private var existingRecurring: RecurringTransactionEntity? = null

    /** Ocurrencia que se está confirmando (modo OCCURRENCE). */
    private var occurrence: Long? = null

    private val _state = MutableStateFlow(
        TransactionFormState(
            kind = kind,
            frequency = if (kind.isRecurringTemplate) RecurrenceFrequency.MONTHLY else null
        )
    )
    val state: StateFlow<TransactionFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = sessionManager.currentUserId.filterNotNull().first()

            // Se carga antes de observar las listas para que el formulario aparezca ya relleno.
            transactionId?.let { transactionRepository.getTransaction(it) }?.let { tx ->
                existingTransaction = tx
                prefill(tx.amount, tx.categoryId, tx.accountId, tx.description, tx.date, frequency = null)
            }
            recurringId?.let { recurringRepository.getRecurring(it) }?.let { recurring ->
                existingRecurring = recurring
                if (kind == TransactionFormKind.OCCURRENCE) {
                    occurrence = recurring.nextOccurrence
                    prefill(
                        recurring.baseAmount, recurring.categoryId, recurring.accountId, recurring.description,
                        Recurrence.transactionDateFor(recurring.nextOccurrence), frequency = null
                    )
                } else {
                    prefill(
                        recurring.baseAmount, recurring.categoryId, recurring.accountId, recurring.description,
                        recurring.anchorDate, frequency = recurring.frequency ?: RecurrenceFrequency.MONTHLY
                    )
                }
            }

            // Las listas se observan (no se leen una sola vez) para que una cuenta o categoría
            // creada desde este formulario aparezca al volver.
            combine(
                accountRepository.observeAccounts(userId),
                categoryRepository.observeCategories(userId)
            ) { accounts, categories -> accounts to categories }
                .collect { (accounts, categories) ->
                    _state.update { s ->
                        s.copy(
                            accounts = accounts,
                            categories = categories,
                            // Con una sola cuenta, o al crear, se preselecciona la primera.
                            accountId = s.accountId?.takeIf { id -> accounts.any { it.id == id } }
                                ?: accounts.firstOrNull()?.id,
                            // Si la categoría ya no existe (fue eliminada) hay que elegir otra.
                            categoryId = s.categoryId?.takeIf { id -> categories.any { it.id == id } },
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun prefill(
        amount: Long, categoryId: String?, accountId: String, description: String?, date: Long,
        frequency: RecurrenceFrequency?
    ) {
        _state.update {
            it.copy(
                type = if (amount < 0) CategoryType.EXPENSE else CategoryType.INCOME,
                amountText = centsToInputText(abs(amount)),
                categoryId = categoryId,
                accountId = accountId,
                dateMillis = date,
                description = description.orEmpty(),
                frequency = frequency ?: it.frequency
            )
        }
    }

    fun onTypeChange(type: CategoryType) {
        _state.update { s ->
            val keepsCategory = s.categories.firstOrNull { it.id == s.categoryId }?.type == type
            s.copy(type = type, categoryId = if (keepsCategory) s.categoryId else null, categoryError = false)
        }
    }

    fun onAmountChange(value: String) {
        _state.update { it.copy(amountText = value, amountError = null) }
    }

    fun onCategorySelected(categoryId: String) {
        _state.update { it.copy(categoryId = categoryId, categoryError = false) }
    }

    fun onAccountSelected(accountId: String) {
        _state.update { it.copy(accountId = accountId, accountError = false) }
    }

    fun onDateChange(dateMillis: Long) {
        _state.update { it.copy(dateMillis = dateMillis) }
    }

    fun onDescriptionChange(value: String) {
        _state.update { it.copy(description = value) }
    }

    /** Un recurrente no puede quedarse sin frecuencia. */
    fun onFrequencyChange(frequency: RecurrenceFrequency?) {
        if (frequency == null && kind.isRecurringTemplate) return
        _state.update { it.copy(frequency = frequency) }
    }

    fun save() {
        val s = _state.value
        val cents = parseAmountToCents(s.amountText)
        val amountError = when {
            s.amountText.isBlank() -> AmountError.REQUIRED
            cents == null || cents <= 0 -> AmountError.INVALID
            else -> null
        }
        val category = s.categoriesOfType.firstOrNull { it.id == s.categoryId }
        val account = s.selectedAccount

        if (amountError != null || category == null || account == null || cents == null) {
            _state.update {
                it.copy(amountError = amountError, categoryError = category == null, accountError = account == null)
            }
            return
        }

        val signedAmount = if (s.type == CategoryType.EXPENSE) -cents else cents
        val description = s.description.trim().ifBlank { null }

        viewModelScope.launch {
            val userId = sessionManager.currentUserId.filterNotNull().first()
            val frequency = s.frequency
            when (kind) {
                TransactionFormKind.EDIT -> existingTransaction?.let { tx ->
                    transactionRepository.updateTransaction(tx, account.id, category.id, signedAmount, description, s.dateMillis)
                }
                TransactionFormKind.NEW -> if (frequency != null) {
                    recurringRepository.createTransactionAndRecurring(
                        userId, account.id, category.id, signedAmount, description, frequency, s.dateMillis
                    )
                } else {
                    transactionRepository.createTransaction(userId, account.id, category.id, signedAmount, description, s.dateMillis)
                }
                TransactionFormKind.OCCURRENCE -> {
                    val id = recurringId
                    val pendingOccurrence = occurrence
                    if (id != null && pendingOccurrence != null) {
                        recurringRepository.registerOccurrence(
                            id, pendingOccurrence, account.id, category.id, signedAmount, description, s.dateMillis
                        )
                    }
                }
                TransactionFormKind.RECURRING_NEW -> recurringRepository.createRecurring(
                    userId, account.id, category.id, signedAmount, description,
                    frequency ?: RecurrenceFrequency.MONTHLY, s.dateMillis
                )
                TransactionFormKind.RECURRING_EDIT -> existingRecurring?.let { recurring ->
                    recurringRepository.updateRecurring(
                        recurring, account.id, category.id, signedAmount, description,
                        frequency ?: RecurrenceFrequency.MONTHLY, s.dateMillis
                    )
                }
            }
            _state.update { it.copy(isSaved = true) }
        }
    }

    companion object {
        const val ARG_TRANSACTION_ID = "transactionId"
        const val ARG_RECURRING_ID = "recurringId"
        const val ARG_MODE = "mode"
        const val MODE_OCCURRENCE = "occurrence"
        const val MODE_TEMPLATE = "template"
    }
}
