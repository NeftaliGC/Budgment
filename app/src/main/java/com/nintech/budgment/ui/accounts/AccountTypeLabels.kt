package com.nintech.budgment.ui.accounts

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nintech.budgment.R
import com.nintech.budgment.data.model.AccountType

@Composable
fun AccountType.label(): String = when (this) {
    AccountType.CASH -> stringResource(R.string.account_type_cash)
    AccountType.DEBIT -> stringResource(R.string.account_type_debit)
    AccountType.CREDIT -> stringResource(R.string.account_type_credit)
    AccountType.SAVINGS -> stringResource(R.string.account_type_savings)
    AccountType.INVESTMENT -> stringResource(R.string.account_type_investment)
}
