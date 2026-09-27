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

data class AccountFormState(
    val name: String = "",
    val accountType: AccountType = AccountType.CASH,
    val currency: String = "MXN",
    val icon: String? = null,
    val color: String? = null,
    val creditLimitText: String = "",
    val statementDayText: String = "",
    val paymentDueDayText: String = "",
    val isEditing: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class AccountFormViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: String? = savedStateHandle.get<String>("accountId")
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
                        creditLimitText = account.creditLimit?.let { (it / 100.0).toString() } ?: "",
                        statementDayText = account.statementDay?.toString() ?: "",
                        paymentDueDayText = account.paymentDueDay?.toString() ?: ""
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value) }
    fun onAccountTypeChange(value: AccountType) { _state.value = _state.value.copy(accountType = value) }
    fun onCurrencyChange(value: String) { _state.value = _state.value.copy(currency = value.uppercase()) }
    fun onIconChange(value: String?) { _state.value = _state.value.copy(icon = value) }
    fun onColorChange(value: String?) { _state.value = _state.value.copy(color = value) }
    fun onCreditLimitChange(value: String) { _state.value = _state.value.copy(creditLimitText = value) }
    fun onStatementDayChange(value: String) { _state.value = _state.value.copy(statementDayText = value) }
    fun onPaymentDueDayChange(value: String) { _state.value = _state.value.copy(paymentDueDayText = value) }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            val isCredit = s.accountType == AccountType.CREDIT
            val creditLimit = if (isCredit) s.creditLimitText.toDoubleOrNull()?.let { (it * 100).toLong() } else null
            val statementDay = if (isCredit) s.statementDayText.toIntOrNull() else null
            val paymentDueDay = if (isCredit) s.paymentDueDayText.toIntOrNull() else null

            existingAccount?.let { account ->
                accountRepository.updateAccount(account, s.name, s.accountType, s.currency, creditLimit, statementDay, paymentDueDay, s.icon, s.color)
            } ?: run {
                val userId = sessionManager.currentUserId.filterNotNull().first()
                accountRepository.createAccount(userId, s.name, s.accountType, s.currency, creditLimit, statementDay, paymentDueDay, s.icon, s.color)
            }
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}