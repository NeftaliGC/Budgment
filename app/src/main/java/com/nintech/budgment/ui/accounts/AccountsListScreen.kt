package com.nintech.budgment.ui.accounts

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.ui.categories.parseHexColor
import com.nintech.budgment.ui.common.BudgmentScaffold
import com.nintech.budgment.ui.common.ColorIconTile
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.theme.BudgmentTheme
import androidx.compose.ui.platform.LocalInspectionMode
import com.nintech.budgment.ui.common.RowActionsMenu
import androidx.compose.runtime.saveable.rememberSaveable
import com.nintech.budgment.ui.theme.tabular

@Composable
fun AccountsListScreen(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    viewModel: AccountsViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    if (viewModel != null) {
        val uiState by viewModel.uiState.collectAsState()

        AccountsListScreen(
            uiState = uiState,
            currentRoute = currentRoute,
            onNavigateToTab = onNavigateToTab,
            onAddAccountClick = onAddAccountClick,
            onAccountClick = onAccountClick,
            onDeleteAccount = viewModel::deleteAccount,
            onCurrencyChange = viewModel::onDisplayCurrencyChange
        )
    } else {
        AccountsListScreen(
            uiState = AccountsUiState(),
            currentRoute = currentRoute,
            onNavigateToTab = onNavigateToTab,
            onAddAccountClick = onAddAccountClick,
            onAccountClick = onAccountClick,
            onDeleteAccount = {},
            onCurrencyChange = {}
        )
    }
}

@Composable
fun AccountsListScreen(
    uiState: AccountsUiState,
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onCurrencyChange: (String) -> Unit
) {
    // Cuenta para la que se pidió eliminar; el diálogo decide si se puede.
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    uiState.accounts.firstOrNull { it.id == pendingDeleteId }?.let { account ->
        DeleteAccountDialog(
            account = account,
            onConfirm = {
                onDeleteAccount(account.id)
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null }
        )
    }

    BudgmentScaffold(
        currentRoute = currentRoute,
        onNavigateToTab = onNavigateToTab,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAccountClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.accounts_add_content_description))
            }
        }
    ) { innerPadding ->
        if (!uiState.isLoading && uiState.accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.accounts_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.accounts_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "summary") {
                    TotalSummaryHeader(
                        totalBalance = uiState.totalBalance,
                        primaryCurrency = uiState.primaryCurrency,
                        accountCount = uiState.accounts.size,
                        missingRateAccounts = uiState.missingRateAccounts,
                        onCurrencyChange = onCurrencyChange
                    )
                }
                AccountType.entries.forEach { type ->
                    val typeAccounts = uiState.accounts.filter { it.accountType == type }
                    if (typeAccounts.isNotEmpty()) {
                        item(key = "header_${type.name}") {
                            Text(
                                text = type.label(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                            )
                        }
                        items(typeAccounts, key = { it.id }) { account ->
                            AccountCard(
                                account = account,
                                onClick = { onAccountClick(account.id) },
                                onDeleteClick = { pendingDeleteId = account.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Una cuenta solo se puede eliminar con saldo en cero. Si tiene saldo se explica por qué no;
 * si no, se pide confirmación porque la acción no se puede deshacer desde la app.
 */
@Composable
private fun DeleteAccountDialog(account: AccountEntity, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (account.balanceCache != 0L) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.accounts_delete_blocked_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.accounts_delete_blocked_message,
                        account.name,
                        formatCents(account.balanceCache, account.currency)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_understood)) }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.accounts_delete_confirm_title, account.name)) },
            text = { Text(stringResource(R.string.accounts_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun TotalSummaryHeader(
    totalBalance: Long,
    primaryCurrency: String,
    accountCount: Int,
    missingRateAccounts: Int,
    onCurrencyChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.accounts_total_balance_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatCents(totalBalance, primaryCurrency),
                style = MaterialTheme.typography.displaySmall.tabular,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.accounts_count_format, accountCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (missingRateAccounts > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.accounts_missing_rates, missingRateAccounts),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SupportedCurrency.ALL) { currency ->
                    FilterChip(
                        selected = primaryCurrency == currency.code,
                        onClick = { onCurrencyChange(currency.code) },
                        label = { Text(currency.code) }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_currency_rate_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountCard(account: AccountEntity, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    val accentColor = parseHexColor(account.color)
        ?: parseHexColor(account.accountType.defaultColorHex())!!
    val icon = AccountIcon.resolveIcon(account.icon) ?: account.accountType.defaultIcon().icon
    val balanceColor = if (account.balanceCache < 0) {
        MaterialTheme.colorScheme.error
    } else {
        BudgmentTheme.extraColors.success
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorIconTile(color = accentColor, icon = icon)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                val creditInfo = if (account.accountType == AccountType.CREDIT && account.creditLimit != null) {
                    " · " + stringResource(R.string.accounts_credit_limit_format, formatCents(account.creditLimit, account.currency))
                } else {
                    ""
                }
                Text(
                    account.accountType.label() + creditInfo,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    formatCents(account.balanceCache, account.currency),
                    style = MaterialTheme.typography.titleMedium.tabular,
                    color = balanceColor
                )
            }
            RowActionsMenu(deleteLabel = stringResource(R.string.accounts_delete_content_description), onDeleteClick = onDeleteClick)
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountsListScreenPopulatedLightPreview() {
    BudgmentTheme {
        AccountsListScreen(
            uiState = AccountsUiState(
                accounts = listOf(
                    AccountEntity(
                        id = "1",
                        userId = "user_1",
                        name = "Cuenta de Débito",
                        accountType = AccountType.DEBIT,
                        currency = "MXN",
                        icon = AccountIcon.CARD.key,
                        color = AccountType.DEBIT.defaultColorHex(),
                        balanceCache = 1500000L
                    ),
                    AccountEntity(
                        id = "2",
                        userId = "user_1",
                        name = "Efectivo",
                        accountType = AccountType.CASH,
                        currency = "MXN",
                        icon = AccountIcon.WALLET.key,
                        color = AccountType.CASH.defaultColorHex(),
                        balanceCache = 25000L
                    ),
                    AccountEntity(
                        id = "3",
                        userId = "user_1",
                        name = "Tarjeta de Crédito",
                        accountType = AccountType.CREDIT,
                        currency = "MXN",
                        icon = AccountIcon.CARD.key,
                        color = AccountType.CREDIT.defaultColorHex(),
                        creditLimit = 1000000L,
                        balanceCache = -80000L
                    )
                ),
                totalBalance = 1445000L,
                primaryCurrency = "MXN",
                isLoading = false
            ),
            currentRoute = "accounts",
            onNavigateToTab = {},
            onAddAccountClick = {},
            onAccountClick = {},
            onDeleteAccount = {},
            onCurrencyChange = {}
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountsListScreenEmptyLightPreview() {
    BudgmentTheme {
        AccountsListScreen(
            uiState = AccountsUiState(
                accounts = emptyList(),
                isLoading = false
            ),
            currentRoute = "accounts",
            onNavigateToTab = {},
            onAddAccountClick = {},
            onAccountClick = {},
            onDeleteAccount = {},
            onCurrencyChange = {}
        )
    }
}