package com.nintech.budgment.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.ExchangeRateRepository
import com.nintech.budgment.data.repository.UserRepository
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val totalBalance: Long = 0,
    val primaryCurrency: String = SupportedCurrency.MXN.code,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val missingRateAccounts: Int = 0,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(HomeUiState(isLoading = true))
            } else {
                val defaultCurrency = userRepository.getUser(userId)?.defaultCurrency
                    ?: SupportedCurrency.MXN.code
                combine(
                    accountRepository.observeAccounts(userId),
                    categoryRepository.observeCategories(userId),
                    sessionManager.displayCurrency
                ) { accounts, categories, preferredCurrency ->
                    val displayCurrency = preferredCurrency ?: defaultCurrency
                    var missing = 0
                    var total = 0L
                    accounts.forEach { account ->
                        val converted = exchangeRateRepository.convert(
                            account.balanceCache,
                            account.currency,
                            displayCurrency
                        )
                        if (converted == null) {
                            missing++
                        } else {
                            total += converted
                        }
                    }
                    HomeUiState(
                        totalBalance = total,
                        primaryCurrency = displayCurrency,
                        accounts = accounts,
                        categories = categories,
                        missingRateAccounts = missing,
                        isLoading = false
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun onDisplayCurrencyChange(code: String) {
        viewModelScope.launch { sessionManager.setDisplayCurrency(code) }
    }
}