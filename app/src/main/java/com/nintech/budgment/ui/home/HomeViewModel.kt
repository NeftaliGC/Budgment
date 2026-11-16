package com.nintech.budgment.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.model.BudgetPeriod
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.ExchangeRateRepository
import com.nintech.budgment.data.repository.TransactionRepository
import com.nintech.budgment.data.repository.TransferRepository
import com.nintech.budgment.data.repository.RecurringRepository
import com.nintech.budgment.ui.common.dayTicks
import com.nintech.budgment.ui.recurring.RecurringItem
import com.nintech.budgment.ui.recurring.buildRecurringItems
import java.util.Calendar
import com.nintech.budgment.data.repository.GoalRepository
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.data.session.UiPreferences
import com.nintech.budgment.ui.goals.GoalItem
import com.nintech.budgment.ui.goals.buildGoalItems
import com.nintech.budgment.ui.budgets.BudgetOverviewSource
import com.nintech.budgment.ui.budgets.BudgetWithProgress
import com.nintech.budgment.ui.transactions.TransactionWithDetails
import com.nintech.budgment.ui.transactions.buildTransactionDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val primaryCurrency: String = SupportedCurrency.MXN.code,
    val hasAccounts: Boolean = false,
    val totalBalance: Long = 0,
    val missingRateAccounts: Int = 0,
    /**
     * Límite y gastado del periodo vigente: los del presupuesto global si hay; si no, la suma de los
     * presupuestos por categoría. Mismas cifras que el resumen de la pestaña Presupuestos.
     */
    val budgetAllocated: Long = 0,
    val budgetSpent: Long = 0,
    val budgetFromGlobal: Boolean = false,
    val topBudgets: List<BudgetWithProgress> = emptyList(),
    /** Periodo del resumen: da el título de la tarjeta. */
    val budgetPeriod: BudgetPeriod? = null,
    /** Días que faltan para que el presupuesto del resumen se renueve, contando hoy. */
    val budgetDaysLeft: Int = 1,
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    /** Recurrentes vencidos que esperan que el usuario los registre, posponga u omita. */
    val pendingRecurring: List<RecurringItem> = emptyList(),
    /** Vencidos que el usuario pospuso: no se anuncian, pero se pueden registrar desde la tarjeta. */
    val snoozedRecurring: List<RecurringItem> = emptyList(),
    val goals: List<GoalItem> = emptyList(),
    val goalsExpanded: Boolean = true,
    val isLoading: Boolean = true
) {
    val hasBudgets: Boolean get() = budgetAllocated > 0
    val budgetRemaining: Long get() = budgetAllocated - budgetSpent

    val budgetSpentFraction: Float get() = if (hasBudgets) budgetSpent.toFloat() / budgetAllocated else 0f

    /** Lo que se puede gastar por día para llegar al final del periodo sin pasarse. */
    val budgetPerDayLeft: Long get() = if (budgetRemaining > 0) budgetRemaining / budgetDaysLeft else 0
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val transferRepository: TransferRepository,
    private val recurringRepository: RecurringRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val budgetOverviewSource: BudgetOverviewSource,
    private val goalRepository: GoalRepository,
    private val uiPreferences: UiPreferences,
    private val sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(HomeUiState(isLoading = true))
            } else {
                val recentTransactions = combine(
                    transactionRepository.observeTransactions(userId),
                    accountRepository.observeAccountsIncludingDeleted(userId),
                    categoryRepository.observeCategoriesIncludingDeleted(userId),
                    transferRepository.observeTransfers(userId)
                ) { transactions, accounts, categories, transfers ->
                    buildTransactionDetails(transactions, accounts, categories, transfers).take(RECENT_TRANSACTIONS)
                }

                val pendingRecurring = combine(
                    recurringRepository.observeRecurring(userId),
                    accountRepository.observeAccountsIncludingDeleted(userId),
                    categoryRepository.observeCategoriesIncludingDeleted(userId),
                    // Al cambiar de día puede vencer otro o terminar un «más tarde».
                    dayTicks()
                ) { recurrings, accounts, categories, now ->
                    buildRecurringItems(recurrings, accounts, categories, now).filter { it.isDue }
                }

                val goals = combine(
                    goalRepository.observeGoals(userId),
                    // Incluye las eliminadas para avisar si se borró la cuenta de una meta.
                    accountRepository.observeAccountsIncludingDeleted(userId),
                    uiPreferences.homeGoalsExpanded,
                    dayTicks()
                ) { goals, accounts, expanded, now -> buildGoalItems(goals, accounts, now) to expanded }

                combine(
                    accountRepository.observeAccounts(userId),
                    // Misma fuente que la pestaña Presupuestos: las cifras coinciden y ya vienen
                    // convertidas a la moneda de visualización.
                    budgetOverviewSource.observe(userId),
                    recentTransactions,
                    pendingRecurring,
                    goals
                ) { accounts, budgets, transactions, pending, (goalItems, goalsExpanded) ->
                    val displayCurrency = budgets.primaryCurrency
                    var missing = 0
                    var total = 0L
                    accounts.forEach { account ->
                        val converted = exchangeRateRepository.convert(
                            account.balanceCache,
                            account.currency,
                            displayCurrency
                        )
                        if (converted == null) missing++ else total += converted
                    }

                    HomeUiState(
                        primaryCurrency = displayCurrency,
                        hasAccounts = accounts.isNotEmpty(),
                        totalBalance = total,
                        missingRateAccounts = missing,
                        budgetAllocated = budgets.totalAllocated,
                        budgetSpent = budgets.totalSpent,
                        budgetFromGlobal = budgets.hasGlobal,
                        // Los más avanzados primero: son los que piden atención. El global que da
                        // las cifras de la tarjeta no se repite en la lista.
                        topBudgets = (budgets.global + budgets.category)
                            .filter { it.budget.id != budgets.summaryBudgetId }
                            .sortedByDescending { it.progress }
                            .take(TOP_BUDGETS),
                        budgetPeriod = budgets.summaryPeriod,
                        budgetDaysLeft = budgets.summaryPeriod?.daysLeft() ?: 1,
                        recentTransactions = transactions,
                        pendingRecurring = pending.filter { it.isPending },
                        snoozedRecurring = pending.filter { it.isSnoozed },
                        goals = goalItems,
                        goalsExpanded = goalsExpanded,
                        isLoading = false
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun setGoalsExpanded(expanded: Boolean) {
        viewModelScope.launch { uiPreferences.setHomeGoalsExpanded(expanded) }
    }

    fun onDisplayCurrencyChange(code: String) {
        viewModelScope.launch { sessionManager.setDisplayCurrency(code) }
    }

    /** Registra la ocurrencia tal cual. Si ya se resolvió (doble toque), no hace nada. */
    fun registerRecurring(item: RecurringItem) {
        viewModelScope.launch {
            recurringRepository.registerOccurrenceAsIs(item.recurring.id, item.recurring.nextOccurrence)
        }
    }

    /** «Más tarde»: se vuelve a anunciar mañana. */
    fun snoozeRecurring(item: RecurringItem) {
        viewModelScope.launch {
            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            recurringRepository.snooze(item.recurring.id, tomorrow)
        }
    }

    /** «Omitir esta vez»: pasa a la siguiente ocurrencia sin registrar nada. */
    fun skipRecurring(item: RecurringItem) {
        viewModelScope.launch {
            recurringRepository.skipOccurrence(item.recurring.id, item.recurring.nextOccurrence)
        }
    }

    private companion object {
        const val RECENT_TRANSACTIONS = 5
        const val TOP_BUDGETS = 3
    }
}
