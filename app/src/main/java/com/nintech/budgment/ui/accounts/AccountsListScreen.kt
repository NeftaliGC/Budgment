package com.nintech.budgment.ui.accounts

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.ui.common.BudgmentScaffold
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.theme.BudgmentTheme
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun AccountsListScreen(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AccountsListScreen(
        uiState = uiState,
        currentRoute = currentRoute,
        onNavigateToTab = onNavigateToTab,
        onAddAccountClick = onAddAccountClick,
        onAccountClick = onAccountClick,
        onDeleteAccount = viewModel::deleteAccount
    )
}

@Composable
fun AccountsListScreen(
    uiState: AccountsUiState,
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onDeleteAccount: (String) -> Unit
) {
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
                        fontSize = 18.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.accounts_empty_subtitle),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
                items(uiState.accounts, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        onClick = { onAccountClick(account.id) },
                        onDeleteClick = { onDeleteAccount(account.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCard(account: AccountEntity, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(account.accountType.label(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.accounts_balance_format, formatCents(account.balanceCache, account.currency)),
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.accounts_delete_content_description),
                    tint = MaterialTheme.colorScheme.error
                )
            }
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
                        balanceCache = 1500000L
                    ),
                    AccountEntity(
                        id = "2",
                        userId = "user_1",
                        name = "Efectivo",
                        accountType = AccountType.CASH,
                        currency = "MXN",
                        balanceCache = 25000L
                    )
                ),
                isLoading = false
            ),
            currentRoute = "accounts",
            onNavigateToTab = {},
            onAddAccountClick = {},
            onAccountClick = {},
            onDeleteAccount = {}
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
            onDeleteAccount = {}
        )
    }
}
