package com.nintech.budgment.ui.categories

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nintech.budgment.R
import com.nintech.budgment.data.model.CategoryType

@Composable
fun CategoryType.label(): String = when (this) {
    CategoryType.EXPENSE -> stringResource(R.string.category_type_expense)
    CategoryType.INCOME -> stringResource(R.string.category_type_income)
}