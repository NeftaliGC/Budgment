package com.nintech.budgment.ui.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.nintech.budgment.data.session.SessionManager
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.nintech.budgment.ui.common.parseAmountToCents
import com.nintech.budgment.ui.common.centsToInputText

data class AccountFormState(
    val name: String = "",
    val accountType: AccountType = AccountType.CASH,
    val currency: String = "MXN",
    val icon: String? = null,
    val color: String? = null,
    val creditLimitText: String = "",
    val statementDayText: String = "",
    val paymentDueDayText: String = "",
    val initialBalanceText: String = "",
    val initialBalanceError: Boolean = false,
    val nameError: Boolean = false,
    val creditLimitError: Boolean = false,
    val statementDayError: Boolean = false,
    val paymentDueDayError: Boolean = false,
    val isEditing: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class AccountFormViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Al crear, la ruta trae el argumento vacío ("account_form?accountId="), no null.
    private val accountId: String? = savedStateHandle.get<String>("accountId")?.ifBlank { null }
    private var existingAccount: AccountEntity? = null

    private val _state = MutableStateFlow(AccountFormState(isEditing = accountId != null))
    val state: StateFlow<AccountFormState> = _state.asStateFlow()

    init {
        accountId?.let { id ->
            viewModelScope.launch {
                accountRepository.getAccount(id)?.let { account ->
                    existingAccount = account
                    _state.value = _state.value.copy(
                        name = account.name,
                        accountType = account.accountType,
                        currency = account.currency,
                        icon = account.icon,
                        color = account.color,
                        creditLimitText = account.creditLimit?.let { centsToInputText(it) } ?: "",
                        statementDayText = account.statementDay?.toString() ?: "",
                        paymentDueDayText = account.paymentDueDay?.toString() ?: ""
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value, nameError = false) }
    fun onAccountTypeChange(value: AccountType) { _state.value = _state.value.copy(accountType = value) }
    fun onCurrencyChange(value: String) { _state.value = _state.value.copy(currency = value.uppercase()) }
    fun onIconChange(value: String?) { _state.value = _state.value.copy(icon = value) }
    fun onColorChange(value: String?) { _state.value = _state.value.copy(color = value) }
    fun onCreditLimitChange(value: String) { _state.value = _state.value.copy(creditLimitText = value, creditLimitError = false) }
    fun onStatementDayChange(value: String) { _state.value = _state.value.copy(statementDayText = value, statementDayError = false) }
    fun onPaymentDueDayChange(value: String) { _state.value = _state.value.copy(paymentDueDayText = value, paymentDueDayError = false) }

    fun onInitialBalanceChange(value: String) {
        _state.value = _state.value.copy(initialBalanceText = value, initialBalanceError = false)
    }

    fun save() {
        val s = _state.value
        val name = s.name.trim()
        val isCredit = s.accountType == AccountType.CREDIT

        // Los datos de crédito son opcionales, pero si se escriben deben ser válidos.
        val creditLimit = if (isCredit) parseAmountToCents(s.creditLimitText) else null
        val statementDay = if (isCredit) s.statementDayText.trim().toIntOrNull() else null
        val paymentDueDay = if (isCredit) s.paymentDueDayText.trim().toIntOrNull() else null
        val creditLimitError = isCredit && s.creditLimitText.isNotBlank() && creditLimit == null
        val statementDayError = isCredit && s.statementDayText.isNotBlank() && (statementDay == null || statementDay !in 1..31)
        val paymentDueDayError = isCredit && s.paymentDueDayText.isNotBlank() && (paymentDueDay == null || paymentDueDay !in 1..31)

        // El saldo inicial solo se pide al crear. Vacío = empieza en cero.
        val asksInitialBalance = !s.isEditing && s.initialBalanceText.isNotBlank()
        val initialCents = if (asksInitialBalance) parseAmountToCents(s.initialBalanceText) else 0L
        val initialBalanceError = initialCents == null

        val nameError = name.isBlank()
        if (nameError || creditLimitError || statementDayError || paymentDueDayError || initialCents == null) {
            _state.value = s.copy(
                nameError = nameError,
                creditLimitError = creditLimitError,
                statementDayError = statementDayError,
                paymentDueDayError = paymentDueDayError,
                initialBalanceError = initialBalanceError
            )
            return
        }
        // En crédito se captura la deuda como número positivo y se guarda en negativo.
        val initialBalance = if (isCredit) -initialCents else initialCents

        viewModelScope.launch {
            existingAccount?.let { account ->
                accountRepository.updateAccount(account, name, s.accountType, s.currency, creditLimit, statementDay, paymentDueDay, s.icon, s.color)
            } ?: run {
                val userId = sessionManager.currentUserId.filterNotNull().first()
                accountRepository.createAccount(userId, name, s.accountType, s.currency, creditLimit, statementDay, paymentDueDay, s.icon, s.color, initialBalance)
            }
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
