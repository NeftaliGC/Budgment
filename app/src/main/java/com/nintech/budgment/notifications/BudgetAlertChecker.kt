package com.nintech.budgment.notifications

import android.content.Context
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.NotificationEntity
import com.nintech.budgment.data.local.entity.NotificationType
import com.nintech.budgment.data.model.BUDGET_LIMIT_LEVEL
import com.nintech.budgment.data.model.BudgetScope
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.data.model.budgetAlertLevel
import com.nintech.budgment.data.model.currentPeriod
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.BudgetRepository
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.ExchangeRateRepository
import com.nintech.budgment.data.repository.NotificationRepository
import com.nintech.budgment.data.repository.TransactionRepository
import com.nintech.budgment.data.repository.UserRepository
import com.nintech.budgment.data.session.NotificationSettings
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.common.formatShortDate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Revisa los presupuestos y avisa cuando uno llega al porcentaje elegido o a su límite. Cada
 * umbral se avisa una sola vez por periodo: al renovarse el presupuesto puede volver a avisar.
 *
 * Las cifras van en la moneda por defecto del usuario, que es la de los límites.
 */
@Singleton
class BudgetAlertChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationSettings: NotificationSettings
) {
    // Varias pantallas pueden pedir la revisión a la vez: se hacen una tras otra.
    private val mutex = Mutex()

    suspend fun check(userId: String, now: Long = System.currentTimeMillis()) = mutex.withLock {
        val budgets = budgetRepository.observeBudgets(userId).first()
            .filter { it.deletedAt == null && it.amountLimit > 0 }
        if (budgets.isEmpty()) return@withLock

        val preferences = notificationSettings.preferences.first()
        val baseCurrency = userRepository.getUser(userId)?.defaultCurrency ?: SupportedCurrency.MXN.code
        val categories = categoryRepository.observeCategoriesIncludingDeleted(userId).first()
        val accountCurrency = accountRepository.observeAccountsIncludingDeleted(userId).first()
            .associate { it.id to it.currency }
        val expenseCategoryIds = categories.filter { it.type == CategoryType.EXPENSE }.map { it.id }.toSet()
        val links = budgetRepository.getCategoryIdsForBudgets(budgets.map { it.id })
        // Solo gastos con categoría: las transferencias no cuentan.
        val expenses = transactionRepository.observeTransactions(userId).first()
            .filter { it.amount < 0 && it.categoryId != null }

        budgets.forEach { budget ->
            val period = budget.currentPeriod(now)
            val relevant = if (budget.scope == BudgetScope.CATEGORY) links[budget.id].orEmpty().toSet() else expenseCategoryIds
            val spent = expenses
                .filter { it.categoryId in relevant && it.date in period }
                .sumOf { tx ->
                    val from = tx.currency ?: accountCurrency[tx.accountId] ?: SupportedCurrency.MXN.code
                    exchangeRateRepository.convert(-tx.amount, from, baseCurrency) ?: 0L
                }
            val level = budgetAlertLevel(spent, budget.amountLimit, preferences.budgetWarningPercent) ?: return@forEach

            val limitText = formatCents(budget.amountLimit, baseCurrency)
            val spentText = formatCents(spent, baseCurrency)
            val renewal = formatShortDate(period.endExclusive)
            val name = budget.name.ifBlank { context.getString(R.string.budget_alert_unnamed) }
            val (title, body) = when {
                level < BUDGET_LIMIT_LEVEL -> context.getString(R.string.budget_alert_warning_title, name, level) to
                    context.getString(R.string.budget_alert_warning_body, spentText, limitText, formatCents(budget.amountLimit - spent, baseCurrency), renewal)
                spent > budget.amountLimit -> context.getString(R.string.budget_alert_over_title, name) to
                    context.getString(R.string.budget_alert_over_body, spentText, limitText, formatCents(spent - budget.amountLimit, baseCurrency), renewal)
                else -> context.getString(R.string.budget_alert_limit_title, name) to
                    context.getString(R.string.budget_alert_limit_body, spentText, limitText, renewal)
            }
            val isNew = notificationRepository.insertIfNew(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = NotificationType.BUDGET_THRESHOLD,
                    refId = budget.id,
                    occurrence = period.start,
                    dedupKey = "budget:${budget.id}:${period.start}:$level",
                    title = title,
                    body = body,
                    createdAt = now
                )
            )
            if (isNew && preferences.budgetsEnabled) {
                BudgetNotifications.show(context, budget.id, title, body)
            }
        }
    }
}
