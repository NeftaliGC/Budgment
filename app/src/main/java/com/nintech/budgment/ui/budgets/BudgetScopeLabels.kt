package com.nintech.budgment.ui.budgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nintech.budgment.R
import com.nintech.budgment.data.model.BudgetScope

@Composable
fun BudgetScope.label(): String = when (this) {
    BudgetScope.GLOBAL -> stringResource(R.string.budget_scope_global)
    BudgetScope.CATEGORY -> stringResource(R.string.budget_scope_category)
}