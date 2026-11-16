package com.nintech.budgment.ui.budgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nintech.budgment.R
import com.nintech.budgment.data.model.BudgetScope
import com.nintech.budgment.data.model.BudgetPeriod
import com.nintech.budgment.data.model.PeriodUnit
import com.nintech.budgment.data.model.RecurrenceFrequency

@Composable
fun BudgetScope.label(): String = when (this) {
    BudgetScope.GLOBAL -> stringResource(R.string.budget_scope_global)
    BudgetScope.CATEGORY -> stringResource(R.string.budget_scope_category)
}

/** Nombre de la periodicidad de un presupuesto: «Diario», «Mensual», «Trimestral»… */
@Composable
fun RecurrenceFrequency.budgetLabel(): String = stringResource(
    when (this) {
        RecurrenceFrequency.DAILY -> R.string.budget_frequency_daily
        RecurrenceFrequency.WEEKLY -> R.string.budget_frequency_weekly
        RecurrenceFrequency.BIWEEKLY -> R.string.budget_frequency_biweekly
        RecurrenceFrequency.MONTHLY -> R.string.budget_frequency_monthly
        RecurrenceFrequency.BIMONTHLY -> R.string.budget_frequency_bimonthly
        RecurrenceFrequency.QUARTERLY -> R.string.budget_frequency_quarterly
        RecurrenceFrequency.SEMIANNUAL -> R.string.budget_frequency_semiannual
        RecurrenceFrequency.YEARLY -> R.string.budget_frequency_yearly
    }
)

/** Nombre de la periodicidad; si no es una de la lista, se dice solo «Periódico». */
@Composable
fun BudgetPeriod.label(): String = frequency?.budgetLabel() ?: stringResource(R.string.budget_frequency_custom)

/** «Resumen de hoy / de la semana / del mes / del año»; con intervalos de varias unidades, «del periodo». */
@Composable
fun BudgetPeriod.summaryTitle(): String = stringResource(
    when {
        interval != 1 -> R.string.budgets_summary_title_period
        unit == PeriodUnit.DAY -> R.string.budgets_summary_title_day
        unit == PeriodUnit.WEEK -> R.string.budgets_summary_title_week
        unit == PeriodUnit.YEAR -> R.string.budgets_summary_title_year
        else -> R.string.budgets_summary_title
    }
)

/** «Presupuesto de hoy / esta semana / este mes / este año»; con intervalos de varias unidades, «de este periodo». */
@Composable
fun BudgetPeriod.homeCardTitle(): String = stringResource(
    when {
        interval != 1 -> R.string.home_budget_card_title_period
        unit == PeriodUnit.DAY -> R.string.home_budget_card_title_day
        unit == PeriodUnit.WEEK -> R.string.home_budget_card_title_week
        unit == PeriodUnit.YEAR -> R.string.home_budget_card_title_year
        else -> R.string.home_budget_card_title
    }
)
