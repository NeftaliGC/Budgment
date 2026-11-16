package com.nintech.budgment.ui.budgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nintech.budgment.R
import com.nintech.budgment.data.model.BudgetScope
import com.nintech.budgment.data.model.PeriodUnit

@Composable
fun BudgetScope.label(): String = when (this) {
    BudgetScope.GLOBAL -> stringResource(R.string.budget_scope_global)
    BudgetScope.CATEGORY -> stringResource(R.string.budget_scope_category)
}

/** Periodicidades que se ofrecen en el formulario; DAY existe en el modelo pero no tiene uso práctico. */
val selectablePeriodUnits = listOf(PeriodUnit.WEEK, PeriodUnit.MONTH, PeriodUnit.YEAR)

@Composable
fun PeriodUnit.label(): String = stringResource(
    when (this) {
        PeriodUnit.DAY -> R.string.period_unit_day
        PeriodUnit.WEEK -> R.string.period_unit_week
        PeriodUnit.MONTH -> R.string.period_unit_month
        PeriodUnit.YEAR -> R.string.period_unit_year
    }
)

/** «Resumen de la semana / del mes / del año». */
@Composable
fun PeriodUnit.summaryTitle(): String = stringResource(
    when (this) {
        PeriodUnit.DAY -> R.string.budgets_summary_title_day
        PeriodUnit.WEEK -> R.string.budgets_summary_title_week
        PeriodUnit.MONTH -> R.string.budgets_summary_title
        PeriodUnit.YEAR -> R.string.budgets_summary_title_year
    }
)

/** «Presupuesto de hoy / esta semana / este mes / este año». */
@Composable
fun PeriodUnit.homeCardTitle(): String = stringResource(
    when (this) {
        PeriodUnit.DAY -> R.string.home_budget_card_title_day
        PeriodUnit.WEEK -> R.string.home_budget_card_title_week
        PeriodUnit.MONTH -> R.string.home_budget_card_title
        PeriodUnit.YEAR -> R.string.home_budget_card_title_year
    }
)
