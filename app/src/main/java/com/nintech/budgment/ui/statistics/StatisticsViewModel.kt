package com.nintech.budgment.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.ExchangeRateRepository
import com.nintech.budgment.data.repository.TransactionRepository
import com.nintech.budgment.data.repository.UserRepository
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.ui.common.dayTicks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val mode: StatsRangeMode = StatsRangeMode.MONTH,
    /** 0 = el mes o año actual; cada paso hacia atrás suma 1. */
    val offset: Int = 0,
    val currency: String = SupportedCurrency.MXN.code,
    val data: StatisticsData = StatisticsData(StatsRange.of(StatsRangeMode.MONTH, 0)),
    val isLoading: Boolean = true
) {
    val canGoForward: Boolean get() = offset > 0
}

private data class Selection(val mode: StatsRangeMode, val offset: Int)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val selection = MutableStateFlow(Selection(StatsRangeMode.MONTH, 0))

    val uiState: StateFlow<StatisticsUiState> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) return@flatMapLatest flowOf(StatisticsUiState())

            // Moneda de visualización: la elegida en la app o, si no, la por defecto del usuario.
            val currency = combine(sessionManager.displayCurrency, userRepository.observeUser(userId)) { preferred, user ->
                preferred ?: user?.defaultCurrency ?: SupportedCurrency.MXN.code
            }
            val context = combine(selection, currency, dayTicks()) { sel, cur, now -> Triple(sel, cur, now) }

            combine(
                transactionRepository.observeTransactions(userId),
                // Incluye las eliminadas: su historial sigue contando.
                accountRepository.observeAccountsIncludingDeleted(userId),
                categoryRepository.observeCategoriesIncludingDeleted(userId),
                context
            ) { transactions, accounts, categories, (sel, displayCurrency, now) ->
                // Un factor por moneda, no una consulta por movimiento.
                val rates = accounts.map { it.currency }
                    .plus(transactions.mapNotNull { it.currency })
                    .distinct()
                    .associateWith { code ->
                        exchangeRateRepository.getRateMicros(code, displayCurrency)?.let { it / 1_000_000.0 }
                    }
                val range = StatsRange.of(sel.mode, sel.offset, now)
                val previous = StatsRange.of(sel.mode, sel.offset + 1, now)
                StatisticsUiState(
                    mode = sel.mode,
                    offset = sel.offset,
                    currency = displayCurrency,
                    data = computeStatistics(transactions, accounts, categories, { rates[it] }, range, previous, now),
                    isLoading = false
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsUiState())

    fun onModeChange(mode: StatsRangeMode) {
        selection.update { if (it.mode == mode) it else Selection(mode, 0) }
    }

    fun onPrevious() {
        selection.update { it.copy(offset = it.offset + 1) }
    }

    fun onNext() {
        selection.update { it.copy(offset = (it.offset - 1).coerceAtLeast(0)) }
    }

    fun onCurrencyChange(code: String) {
        viewModelScope.launch { sessionManager.setDisplayCurrency(code) }
    }
}
