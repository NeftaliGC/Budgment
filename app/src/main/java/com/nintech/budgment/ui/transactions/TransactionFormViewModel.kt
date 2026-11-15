package com.nintech.budgment.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.CategoryRepository
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

data class TransactionFormState(
    /** Gasto o ingreso. Decide qué categorías se ofrecen y el signo del monto. */
    val type: CategoryType = CategoryType.EXPENSE,
    val amountText: String = "",
    val categoryId: String? = null,
    val accountId: String? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val description: String = "",
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val amountError: AmountError? = null,
    val categoryError: Boolean = false,
    val accountError: Boolean = false,
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true
) {
    /** Categorías del tipo elegido: las únicas que se pueden asignar. */
    val categoriesOfType: List<CategoryEntity> get() = categories.filter { it.type == type }
    val selectedAccount: AccountEntity? get() = accounts.firstOrNull { it.id == accountId }
}

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: String? = savedStateHandle.get<String>("transactionId")?.ifBlank { null }
    private var existingTransaction: TransactionEntity? = null

    private val _state = MutableStateFlow(TransactionFormState(isEditing = transactionId != null))
    val state: StateFlow<TransactionFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = sessionManager.currentUserId.filterNotNull().first()

            // Se carga antes de observar las listas para que el formulario aparezca ya relleno.
            val tx = transactionId?.let { transactionRepository.getTransaction(it) }
            if (tx != null) {
                existingTransaction = tx
                _state.update {
                    it.copy(
                        type = if (tx.amount < 0) CategoryType.EXPENSE else CategoryType.INCOME,
                        amountText = centsToInputText(abs(tx.amount)),
                        categoryId = tx.categoryId,
                        accountId = tx.accountId,
                        dateMillis = tx.date,
                        description = tx.description.orEmpty()
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
            existingTransaction?.let { tx ->
                transactionRepository.updateTransaction(tx, account.id, category.id, signedAmount, description, s.dateMillis)
            } ?: run {
                val userId = sessionManager.currentUserId.filterNotNull().first()
                transactionRepository.createTransaction(userId, account.id, category.id, signedAmount, description, s.dateMillis)
            }
            _state.update { it.copy(isSaved = true) }
        }
    }
}
