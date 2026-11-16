package com.nintech.budgment.ui.budgets

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
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Calcula el estado de los presupuestos del usuario (gastado, progreso, totales) en la moneda
 * de visualización. Lo comparten la pestaña Presupuestos y el resumen de Inicio para que las
 * cifras sean siempre las mismas.
 */
class BudgetOverviewSource @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) {
    fun observe(userId: String): Flow<BudgetsUiState> = flow {
        // Par (moneda de visualización, moneda base). Los presupuestos no guardan moneda propia:
        // su límite está en la moneda por defecto del usuario, que es la base.
        val currencies = combine(
            sessionManager.displayCurrency,
            userRepository.observeUser(userId)
        ) { preferredCurrency, user ->
            val baseCurrency = user?.defaultCurrency ?: SupportedCurrency.MXN.code
            (preferredCurrency ?: baseCurrency) to baseCurrency
        }
        val overview = combine(
            budgetRepository.observeBudgets(userId),
            transactionRepository.observeTransactions(userId),
            // Incluye las eliminadas: lo ya gastado en ellas sigue contando en los presupuestos.
            categoryRepository.observeCategoriesIncludingDeleted(userId),
            accountRepository.observeAccountsIncludingDeleted(userId),
            currencies
        ) { budgets, transactions, categories, accounts, (displayCurrency, baseCurrency) ->
            val expenseCategoryIds = categories
                .filter { it.type == CategoryType.EXPENSE }
                .map { it.id }
                .toSet()
            val linksByBudget = budgetRepository.getCategoryIdsForBudgets(budgets.map { it.id })
            // Solo las activas se muestran como etiquetas en la tarjeta.
            val categoriesById = categories.filter { it.deletedAt == null }.associateBy { it.id }
            val accountCurrencyById = accounts.associate { it.id to it.currency }

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
                // El límite se convierte igual que lo gastado; si no, al cambiar de moneda se
                // compararían cifras en monedas distintas.
                val convertedLimit = exchangeRateRepository.convert(budget.amountLimit, baseCurrency, displayCurrency)
                if (convertedLimit == null) missingRate = true
                val limit = convertedLimit ?: budget.amountLimit
                val progress = if (limit > 0) spent.toFloat() / limit else 0f
                val linkedIds = linksByBudget[budget.id].orEmpty()
                val allCategoriesDeleted = budget.scope == BudgetScope.CATEGORY &&
                    linkedIds.isNotEmpty() && linkedIds.none { it in categoriesById }
                BudgetWithProgress(
                    budget = budget,
                    spent = spent,
                    progress = progress,
                    missingRate = missingRate,
                    allCategoriesDeleted = allCategoriesDeleted,
                    limit = limit
                )
            }

            // El global es el techo del mes y los de categoría son topes dentro de él: sumar sus
            // límites contaría el mismo dinero dos veces. Si hay global, el resumen es ese; si
            // no, se suman los de categoría, que entonces sí son independientes entre sí.
            // Solo puede crearse un global; si quedaran varios de antes, manda el más antiguo.
            val summaryGlobal = withProgress
                .filter { it.budget.scope == BudgetScope.GLOBAL }
                .minByOrNull { it.budget.createdAt }
            val categoryBudgets = withProgress.filter { it.budget.scope == BudgetScope.CATEGORY }
            val categoryLimitsTotal = categoryBudgets.sumOf { it.limit }

            val totalAllocated: Long
            val totalSpent: Long
            if (summaryGlobal != null) {
                totalAllocated = summaryGlobal.limit
                totalSpent = summaryGlobal.spent
            } else {
                // Lo gastado se cuenta una sola vez aunque una categoría esté en dos presupuestos.
                val covered = categoryBudgets
                    .flatMap { linksByBudget[it.budget.id].orEmpty() }
                    .toSet()
                val rangeStart = categoryBudgets.mapNotNull { it.budget.startDate }.minOrNull()
                val rangeEnd = categoryBudgets.mapNotNull { it.budget.endDate }.maxOrNull()
                totalAllocated = categoryLimitsTotal
                totalSpent = transactions
                    .filter { it.categoryId in covered && it.amount < 0 }
                    .filter { rangeStart == null || it.date >= rangeStart }
                    .filter { rangeEnd == null || it.date <= rangeEnd }
                    .mapNotNull { transaction ->
                        val from = transaction.currency
                            ?: accountCurrencyById[transaction.accountId]
                            ?: SupportedCurrency.MXN.code
                        exchangeRateRepository.convert(-transaction.amount, from, displayCurrency)
                    }
                    .sum()
            }

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
                totalAllocated = totalAllocated,
                totalSpent = totalSpent,
                summaryBudgetId = summaryGlobal?.budget?.id,
                categoryLimitsTotal = categoryLimitsTotal,
                primaryCurrency = displayCurrency,
                missingRateCount = sorted.count { it.missingRate },
                isLoading = false
            )
        }
        emitAll(overview)
    }
}
