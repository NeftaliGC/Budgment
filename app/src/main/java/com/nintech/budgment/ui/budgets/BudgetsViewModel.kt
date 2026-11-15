package com.nintech.budgment.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.BudgetEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.model.BudgetScope
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.BudgetRepository
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.ExchangeRateRepository
import com.nintech.budgment.data.repository.TransactionRepository
import com.nintech.budgment.data.repository.UserRepository
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetWithProgress(
    val budget: BudgetEntity,
    val spent: Long,
    val progress: Float,
    val missingRate: Boolean,
    /** Presupuesto por categoría cuyas categorías ya fueron eliminadas todas. */
    val allCategoriesDeleted: Boolean = false
)

data class BudgetsUiState(
    val global: List<BudgetWithProgress> = emptyList(),
    val category: List<BudgetWithProgress> = emptyList(),
    val budgetCategories: Map<String, List<CategoryEntity>> = emptyMap(),
    val totalAllocated: Long = 0,
    val totalSpent: Long = 0,
    val primaryCurrency: String = SupportedCurrency.MXN.code,
    val missingRateCount: Int = 0,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<BudgetsUiState> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(BudgetsUiState(isLoading = true))
            } else {
                val defaultCurrency = userRepository.getUser(userId)?.defaultCurrency
                    ?: SupportedCurrency.MXN.code
                combine(
                    budgetRepository.observeBudgets(userId),
                    transactionRepository.observeTransactions(userId),
                    // Incluye las eliminadas: lo ya gastado en ellas sigue contando en los presupuestos.
                    categoryRepository.observeCategoriesIncludingDeleted(userId),
                    accountRepository.observeAccountsIncludingDeleted(userId),
                    sessionManager.displayCurrency
                ) { budgets, transactions, categories, accounts, preferredCurrency ->
                    val displayCurrency = preferredCurrency ?: defaultCurrency
                    val expenseCategoryIds = categories
                        .filter { it.type == CategoryType.EXPENSE }
                        .map { it.id }
                        .toSet()
                    val linksByBudget = budgetRepository.getCategoryIdsForBudgets(budgets.map { it.id })
                    // Solo las activas se muestran como etiquetas en la tarjeta.
                    val categoriesById = categories.filter { it.deletedAt == null }.associateBy { it.id }
                    val accountCurrencyById = accounts.associate { it.id to it.currency }

                    val coveredByAny = mutableSetOf<String>()
                    budgets.forEach { budget ->
                        val relevant = if (budget.scope == BudgetScope.CATEGORY) {
                            linksByBudget[budget.id]?.toSet().orEmpty()
                        } else {
                            expenseCategoryIds
                        }
                        coveredByAny += relevant
                    }

                    val withProgress = budgets.map { budget ->
                        val relevant = if (budget.scope == BudgetScope.CATEGORY) {
                            linksByBudget[budget.id]?.toSet().orEmpty()
                        } else {
                            expenseCategoryIds
                        }
                        var missingRate = false
                        val spent = transactions
                            .filter { it.categoryId in relevant && it.amount < 0 }
                            .filter { budget.startDate == null || it.date >= budget.startDate }
                            .filter { budget.endDate == null || it.date <= budget.endDate }
                            .mapNotNull { transaction ->
                                val from = transaction.currency
                                    ?: accountCurrencyById[transaction.accountId]
                                    ?: SupportedCurrency.MXN.code
                                val converted = exchangeRateRepository.convert(-transaction.amount, from, displayCurrency)
                                if (converted == null) {
                                    missingRate = true
                                    null
                                } else {
                                    converted
                                }
                            }
                            .sum()
                        val progress = if (budget.amountLimit > 0) {
                            spent.toFloat() / budget.amountLimit
                        } else {
                            0f
                        }
                        val linkedIds = linksByBudget[budget.id].orEmpty()
                        val allCategoriesDeleted = budget.scope == BudgetScope.CATEGORY &&
                            linkedIds.isNotEmpty() && linkedIds.none { it in categoriesById }
                        BudgetWithProgress(budget, spent, progress, missingRate, allCategoriesDeleted)
                    }

                    val rangeStart = budgets.mapNotNull { it.startDate }.minOrNull()
                    val rangeEnd = budgets.mapNotNull { it.endDate }.maxOrNull()
                    val totalSpent = transactions
                        .filter { it.categoryId in coveredByAny && it.amount < 0 }
                        .filter { rangeStart == null || it.date >= rangeStart }
                        .filter { rangeEnd == null || it.date <= rangeEnd }
                        .mapNotNull { transaction ->
                            val from = transaction.currency
                                ?: accountCurrencyById[transaction.accountId]
                                ?: SupportedCurrency.MXN.code
                            exchangeRateRepository.convert(-transaction.amount, from, displayCurrency)
                        }
                        .sum()

                    val sorted = withProgress.sortedWith(
                        compareByDescending<BudgetWithProgress> { it.progress }
                            .thenBy { it.budget.name.lowercase() }
                    )
                    val budgetCategories = budgets.associate { budget ->
                        budget.id to (linksByBudget[budget.id].orEmpty().mapNotNull { categoriesById[it] })
                    }

                    BudgetsUiState(
                        global = sorted.filter { it.budget.scope == BudgetScope.GLOBAL },
                        category = sorted.filter { it.budget.scope == BudgetScope.CATEGORY },
                        budgetCategories = budgetCategories,
                        totalAllocated = budgets.sumOf { it.amountLimit },
                        totalSpent = totalSpent,
                        primaryCurrency = displayCurrency,
                        missingRateCount = sorted.count { it.missingRate },
                        isLoading = false
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetsUiState())

    fun deleteBudget(id: String) {
        viewModelScope.launch { budgetRepository.deleteBudget(id) }
    }

    fun restoreBudget(id: String) {
        viewModelScope.launch { budgetRepository.restoreBudget(id) }
    }

    fun setDisplayCurrency(code: String) {
        viewModelScope.launch { sessionManager.setDisplayCurrency(code) }
    }
}