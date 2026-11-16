package com.nintech.budgment.ui.budgets

import com.nintech.budgment.data.local.entity.BudgetEntity
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.model.BudgetPeriod
import com.nintech.budgment.data.model.BudgetScope
import com.nintech.budgment.data.model.PeriodUnit
import com.nintech.budgment.data.model.currentPeriod
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.BudgetRepository
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.ExchangeRateRepository
import com.nintech.budgment.data.repository.TransactionRepository
import com.nintech.budgment.data.repository.UserRepository
import com.nintech.budgment.data.session.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.util.Calendar
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
        // El periodo vigente depende de la fecha: se recalcula a medianoche aunque no cambien
        // los datos, para que el presupuesto se renueve con la app abierta.
        val context = combine(currencies, dayTicks()) { currencyPair, now -> currencyPair to now }
        val overview = combine(
            budgetRepository.observeBudgets(userId),
            transactionRepository.observeTransactions(userId),
            // Incluye las eliminadas: lo ya gastado en ellas sigue contando en los presupuestos.
            categoryRepository.observeCategoriesIncludingDeleted(userId),
            accountRepository.observeAccountsIncludingDeleted(userId),
            context
        ) { budgets, transactions, categories, accounts, (currencyPair, now) ->
            val (displayCurrency, baseCurrency) = currencyPair
            val expenseCategoryIds = categories
                .filter { it.type == CategoryType.EXPENSE }
                .map { it.id }
                .toSet()
            val linksByBudget = budgetRepository.getCategoryIdsForBudgets(budgets.map { it.id })
            // Solo las activas se muestran como etiquetas en la tarjeta.
            val categoriesById = categories.filter { it.deletedAt == null }.associateBy { it.id }
            val accountCurrencyById = accounts.associate { it.id to it.currency }
            // Solo los gastos con categoría: las transferencias (sin categoría) nunca cuentan.
            val expenses = transactions.filter { it.amount < 0 && it.categoryId != null }

            /** Suma lo gastado en la moneda de visualización; el segundo valor indica si faltó alguna tasa. */
            suspend fun sumConverted(items: List<TransactionEntity>): Pair<Long, Boolean> {
                var missing = false
                var total = 0L
                items.forEach { transaction ->
                    val from = transaction.currency
                        ?: accountCurrencyById[transaction.accountId]
                        ?: SupportedCurrency.MXN.code
                    val converted = exchangeRateRepository.convert(-transaction.amount, from, displayCurrency)
                    if (converted == null) missing = true else total += converted
                }
                return total to missing
            }

            fun relevantCategories(budget: BudgetEntity): Set<String> =
                if (budget.scope == BudgetScope.CATEGORY) linksByBudget[budget.id]?.toSet().orEmpty()
                else expenseCategoryIds

            val withProgress = budgets.map { budget ->
                val period = budget.currentPeriod(now)
                val relevant = relevantCategories(budget)
                val (spent, spentMissingRate) = sumConverted(
                    expenses.filter { it.categoryId in relevant && it.date in period }
                )
                // El límite se convierte igual que lo gastado; si no, al cambiar de moneda se
                // compararían cifras en monedas distintas.
                val convertedLimit = exchangeRateRepository.convert(budget.amountLimit, baseCurrency, displayCurrency)
                val limit = convertedLimit ?: budget.amountLimit
                val progress = if (limit > 0) spent.toFloat() / limit else 0f
                val linkedIds = linksByBudget[budget.id].orEmpty()
                val allCategoriesDeleted = budget.scope == BudgetScope.CATEGORY &&
                    linkedIds.isNotEmpty() && linkedIds.none { it in categoriesById }
                BudgetWithProgress(
                    budget = budget,
                    spent = spent,
                    progress = progress,
                    missingRate = spentMissingRate || convertedLimit == null,
                    allCategoriesDeleted = allCategoriesDeleted,
                    limit = limit,
                    period = period
                )
            }

            // El global es el techo del periodo y los de categoría son topes dentro de él: sumar
            // sus límites contaría el mismo dinero dos veces. Si hay global, el resumen es ese; si
            // no, se suman los de categoría, que entonces sí son independientes entre sí.
            // Solo puede crearse un global; si quedaran varios de antes, manda el más antiguo.
            // Cifras de distinta periodicidad no se suman: el resumen usa una sola.
            val summaryGlobal = withProgress
                .filter { it.budget.scope == BudgetScope.GLOBAL }
                .minByOrNull { it.budget.createdAt }
            val categoryBudgets = withProgress.filter { it.budget.scope == BudgetScope.CATEGORY }

            val summaryPeriod: BudgetPeriod?
            val summaryCategoryBudgets: List<BudgetWithProgress>
            if (summaryGlobal != null) {
                summaryPeriod = summaryGlobal.period
                summaryCategoryBudgets = categoryBudgets.filter { it.period.sameKindAs(summaryGlobal.period) }
            } else {
                // Sin global, manda la periodicidad más usada; a igualdad, la mensual.
                summaryCategoryBudgets = categoryBudgets
                    .groupBy { it.period.unit to it.period.interval }
                    .values
                    .maxWithOrNull(
                        compareBy<List<BudgetWithProgress>> { it.size }
                            .thenBy { it.first().period.unit == PeriodUnit.MONTH }
                    )
                    .orEmpty()
                summaryPeriod = summaryCategoryBudgets.firstOrNull()?.period
            }
            val categoryLimitsTotal = summaryCategoryBudgets.sumOf { it.limit }
            val hasOtherPeriods = summaryCategoryBudgets.size < categoryBudgets.size

            val totalAllocated: Long
            val totalSpent: Long
            if (summaryGlobal != null) {
                totalAllocated = summaryGlobal.limit
                totalSpent = summaryGlobal.spent
            } else {
                // Lo gastado se cuenta una sola vez aunque una categoría esté en dos presupuestos.
                totalAllocated = categoryLimitsTotal
                totalSpent = sumConverted(
                    expenses.filter { tx ->
                        summaryCategoryBudgets.any { item ->
                            tx.categoryId in relevantCategories(item.budget) && tx.date in item.period
                        }
                    }
                ).first
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
                summaryPeriod = summaryPeriod,
                hasOtherPeriods = hasOtherPeriods,
                isLoading = false
            )
        }
        emitAll(overview)
    }
}

/** Emite la hora actual al suscribirse y de nuevo justo después de cada medianoche local. */
private fun dayTicks(): Flow<Long> = flow {
    while (true) {
        val now = System.currentTimeMillis()
        emit(now)
        val nextMidnight = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        delay(nextMidnight - now + 1_000)
    }
}
