package com.nintech.budgment.ui.recurring

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.local.entity.RecurringTransactionEntity
import com.nintech.budgment.data.model.RecurrenceFrequency
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.data.model.dueCount
import com.nintech.budgment.data.model.frequency
import com.nintech.budgment.data.model.isPending
import com.nintech.budgment.data.model.isRunning

@Composable
fun RecurrenceFrequency.label(): String = stringResource(
    when (this) {
        RecurrenceFrequency.DAILY -> R.string.recurring_frequency_daily
        RecurrenceFrequency.WEEKLY -> R.string.recurring_frequency_weekly
        RecurrenceFrequency.BIWEEKLY -> R.string.recurring_frequency_biweekly
        RecurrenceFrequency.MONTHLY -> R.string.recurring_frequency_monthly
        RecurrenceFrequency.BIMONTHLY -> R.string.recurring_frequency_bimonthly
        RecurrenceFrequency.QUARTERLY -> R.string.recurring_frequency_quarterly
        RecurrenceFrequency.SEMIANNUAL -> R.string.recurring_frequency_semiannual
        RecurrenceFrequency.YEARLY -> R.string.recurring_frequency_yearly
    }
)

/** Un recurrente con los datos de su cuenta y categoría, listo para mostrarse. */
data class RecurringItem(
    val recurring: RecurringTransactionEntity,
    /** Su nombre o, si no tiene, el de su categoría. */
    val name: String,
    val accountName: String,
    val categoryIcon: String? = null,
    val categoryColor: String? = null,
    val currency: String = SupportedCurrency.MXN.code,
    val frequency: RecurrenceFrequency? = recurring.frequency,
    /** Su cuenta o categoría ya no existe: no se puede registrar hasta editarlo. */
    val needsAttention: Boolean = false,
    /** Tiene una ocurrencia vencida esperando confirmación (y no está pospuesta). */
    val isPending: Boolean = false,
    /** Ocurrencias vencidas sin registrar, contando la actual. */
    val dueCount: Int = 0
) {
    val isExpense: Boolean get() = recurring.baseAmount < 0

    /** Ya venció y se puede registrar en cualquier momento, esté pospuesto o no. */
    val isDue: Boolean get() = dueCount > 0 && !needsAttention

    /** Venció pero el usuario eligió «Más tarde»: no se anuncia, aunque sigue registrable. */
    val isSnoozed: Boolean get() = isDue && !isPending
    val isPaused: Boolean get() = !recurring.active
    val isFinished: Boolean get() = recurring.active && !recurring.isRunning
}

/**
 * Une los recurrentes con sus cuentas y categorías. Las listas deben incluir las eliminadas
 * para poder mostrar el nombre y avisar que hay que corregirlos.
 */
fun buildRecurringItems(
    recurrings: List<RecurringTransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    now: Long
): List<RecurringItem> {
    val accountsById = accounts.associateBy { it.id }
    val categoriesById = categories.associateBy { it.id }
    return recurrings
        .filter { it.deletedAt == null }
        .map { recurring ->
            val account = accountsById[recurring.accountId]
            val category = categoriesById[recurring.categoryId]
            val needsAttention = account == null || account.deletedAt != null ||
                category == null || category.deletedAt != null
            RecurringItem(
                recurring = recurring,
                name = recurring.description ?: category?.name.orEmpty(),
                accountName = account?.name.orEmpty(),
                categoryIcon = category?.icon,
                categoryColor = category?.color,
                currency = recurring.currency ?: account?.currency ?: SupportedCurrency.MXN.code,
                needsAttention = needsAttention,
                isPending = !needsAttention && recurring.isPending(now),
                dueCount = if (needsAttention) 0 else recurring.dueCount(now)
            )
        }
        .sortedBy { it.recurring.nextOccurrence }
}
