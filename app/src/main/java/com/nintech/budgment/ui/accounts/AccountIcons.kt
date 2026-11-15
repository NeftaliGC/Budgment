package com.nintech.budgment.ui.accounts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.vector.ImageVector
import com.nintech.budgment.data.model.AccountType
import androidx.annotation.StringRes
import com.nintech.budgment.R

enum class AccountIcon(val key: String, @StringRes val labelRes: Int) {
    WALLET("wallet", R.string.icon_wallet),
    CARD("card", R.string.icon_card),
    CASH("cash", R.string.icon_cash),
    PAYMENTS("payments", R.string.icon_payments),
    SAVINGS("savings", R.string.icon_savings),
    BANK("bank", R.string.icon_bank),
    INVESTMENT("investment", R.string.icon_investment);

    val icon: ImageVector
        get() = try {
            when (this) {
                WALLET -> Icons.Filled.AccountBalanceWallet
                CARD -> Icons.Filled.CreditCard
                CASH -> Icons.Filled.Paid
                PAYMENTS -> Icons.Filled.Payments
                SAVINGS -> Icons.Filled.Savings
                BANK -> Icons.Filled.AccountBalance
                INVESTMENT -> Icons.AutoMirrored.Filled.TrendingUp
            }
        } catch (_: Throwable) {
            Icons.Filled.Payments
        }

    companion object {
        fun resolveIcon(key: String?): ImageVector? =
            entries.firstOrNull { it.key == key }?.icon
    }
}

fun AccountType.defaultIcon(): AccountIcon = when (this) {
    AccountType.CASH -> AccountIcon.WALLET
    AccountType.DEBIT -> AccountIcon.CARD
    AccountType.CREDIT -> AccountIcon.CARD
    AccountType.SAVINGS -> AccountIcon.SAVINGS
    AccountType.INVESTMENT -> AccountIcon.INVESTMENT
}

fun AccountType.defaultColorHex(): String = when (this) {
    AccountType.CASH -> "#00897B"
    AccountType.DEBIT -> "#1976D2"
    AccountType.CREDIT -> "#E64A19"
    AccountType.SAVINGS -> "#7B1FA2"
    AccountType.INVESTMENT -> "#388E3C"
}