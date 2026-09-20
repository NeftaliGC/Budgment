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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TransactionFormError { MISSING_FIELDS, INVALID_AMOUNT }

data class TransactionFormState(
    val accountId: String? = null,
    val accountName: String = "",
    val categoryId: String? = null,
    val categoryName: String = "",
    val categoryType: CategoryType? = null,
    val amountText: String = "",
    val description: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: TransactionFormError? = null
)

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
            val accounts = accountRepository.observeAccounts(userId).first()
            val categories = categoryRepository.observeCategories(userId).first()

            _state.value = _state.value.copy(accounts = accounts, categories = categories, isLoading = false)

            val tx = transactionId?.let { transactionRepository.getTransaction(it) }
            if (tx != null) {
                existingTransaction = tx
                val account = accounts.find { it.id == tx.accountId }
                val category = categories.find { it.id == tx.categoryId }
                _state.value = _state.value.copy(
                    accountId = tx.accountId,
                    accountName = account?.name.orEmpty(),
                    categoryId = tx.categoryId,
                    categoryName = category?.name.orEmpty(),
                    categoryType = category?.type,
                    amountText = (kotlin.math.abs(tx.amount) / 100.0).toString(),
                    description = tx.description.orEmpty(),
                    dateMillis = tx.date
                )
            } else {
                accounts.firstOrNull()?.let { acc ->
                    _state.value = _state.value.copy(accountId = acc.id, accountName = acc.name)
                }
            }
        }
    }

    fun onAccountSelected(account: AccountEntity) {
        _state.value = _state.value.copy(accountId = account.id, accountName = account.name, errorMessage = null)
    }

    fun onCategorySelected(category: CategoryEntity) {
        _state.value = _state.value.copy(
            categoryId = category.id, categoryName = category.name, categoryType = category.type, errorMessage = null
        )
    }

    fun onAmountChange(value: String) { _state.value = _state.value.copy(amountText = value, errorMessage = null) }
    fun onDescriptionChange(value: String) { _state.value = _state.value.copy(description = value) }

    fun save() {
        val s = _state.value
        val accountId = s.accountId
        val categoryId = s.categoryId
        val categoryType = s.categoryType
        val magnitude = s.amountText.toDoubleOrNull()

        if (accountId == null || categoryId == null || categoryType == null || s.amountText.isBlank()) {
            _state.value = s.copy(errorMessage = TransactionFormError.MISSING_FIELDS)
            return
        }
        if (magnitude == null || magnitude <= 0) {
            _state.value = s.copy(errorMessage = TransactionFormError.INVALID_AMOUNT)
            return
        }

        val cents = (magnitude * 100).toLong()
        val signedAmount = if (categoryType == CategoryType.EXPENSE) -cents else cents

        viewModelScope.launch {
            existingTransaction?.let { tx ->
                transactionRepository.updateTransaction(tx, accountId, categoryId, signedAmount, s.description.ifBlank { null }, s.dateMillis)
            } ?: run {
                val userId = sessionManager.currentUserId.filterNotNull().first()
                transactionRepository.createTransaction(userId, accountId, categoryId, signedAmount, s.description.ifBlank { null }, s.dateMillis)
            }
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}