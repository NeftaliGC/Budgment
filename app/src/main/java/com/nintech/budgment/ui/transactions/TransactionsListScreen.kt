package com.nintech.budgment.ui.transactions

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.ui.common.BudgmentScaffold
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.common.formatDate
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.common.RowActionsMenu
import com.nintech.budgment.ui.common.rememberUndoableDelete
import com.nintech.budgment.ui.theme.tabular

@Composable
fun TransactionsListScreen(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    viewModel: TransactionsViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    if (viewModel != null) {
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        TransactionsListScreen(
            uiState = uiState,
            currentRoute = currentRoute,
            onNavigateToTab = onNavigateToTab,
            onAddTransactionClick = onAddTransactionClick,
            onTransactionClick = onTransactionClick,
            onDeleteTransaction = rememberUndoableDelete(
                snackbarHostState = snackbarHostState,
                message = stringResource(R.string.transactions_deleted_message),
                onDelete = viewModel::deleteTransaction,
                onRestore = viewModel::restoreTransaction
            ),
            snackbarHostState = snackbarHostState
        )
    } else {
        TransactionsListScreen(
            uiState = TransactionsUiState(),
            currentRoute = currentRoute,
            onNavigateToTab = onNavigateToTab,
            onAddTransactionClick = onAddTransactionClick,
            onTransactionClick = onTransactionClick,
            onDeleteTransaction = {}
        )
    }
}

@Composable
fun TransactionsListScreen(
    uiState: TransactionsUiState,
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    BudgmentScaffold(
        currentRoute = currentRoute,
        onNavigateToTab = onNavigateToTab,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.transactions_add_content_description))
            }
        }
    ) { innerPadding ->
        if (!uiState.isLoading && uiState.transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.transactions_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.transactions_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(uiState.transactions, key = { it.transaction.id }) { item ->
                    TransactionRow(
                        item = item,
                        onClick = { onTransactionClick(item.transaction.id) },
                        onDeleteClick = { onDeleteTransaction(item.transaction.id) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(item: TransactionWithDetails, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    val amountColor = if (item.categoryType == CategoryType.EXPENSE) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    val categoryLabel = if (item.categoryDeleted) {
        stringResource(R.string.transactions_deleted_category_format, item.categoryName)
    } else {
        item.categoryName
    }
    val accountLabel = if (item.accountDeleted) {
        stringResource(R.string.transactions_deleted_account_format, item.accountName)
    } else {
        item.accountName
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.accountDeleted, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(categoryLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$accountLabel · ${formatDate(item.transaction.date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatCents(item.transaction.amount, item.transaction.currency ?: ""),
            style = MaterialTheme.typography.titleMedium.tabular,
            color = amountColor
        )
        if (item.accountDeleted) {
            // Conserva la alineación del monto con las filas que sí tienen menú.
            Spacer(Modifier.width(48.dp))
        } else {
            RowActionsMenu(deleteLabel = stringResource(R.string.transactions_delete_content_description), onDeleteClick = onDeleteClick)
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransactionsListScreenPreview() {
    BudgmentTheme {
        TransactionsListScreen(
            uiState = TransactionsUiState(
                transactions = listOf(
                    TransactionWithDetails(
                        transaction = TransactionEntity(
                            id = "1",
                            userId = "user_1",
                            accountId = "acc_1",
                            categoryId = "cat_1",
                            amount = -35000L,
                            currency = "MXN",
                            date = System.currentTimeMillis()
                        ),
                        accountName = "Cuenta Principal",
                        categoryName = "Supermercado",
                        categoryType = CategoryType.EXPENSE
                    ),
                    TransactionWithDetails(
                        transaction = TransactionEntity(
                            id = "2",
                            userId = "user_1",
                            accountId = "acc_1",
                            categoryId = "cat_2",
                            amount = 2500000L,
                            currency = "MXN",
                            date = System.currentTimeMillis() - 86400000L
                        ),
                        accountName = "Cuenta Principal",
                        categoryName = "Sueldo",
                        categoryType = CategoryType.INCOME
                    )
                ),
                isLoading = false
            ),
            currentRoute = "transactions",
            onNavigateToTab = {},
            onAddTransactionClick = {},
            onTransactionClick = {},
            onDeleteTransaction = {}
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransactionsListScreenEmptyPreview() {
    BudgmentTheme {
        TransactionsListScreen(
            uiState = TransactionsUiState(
                transactions = emptyList(),
                isLoading = false
            ),
            currentRoute = "transactions",
            onNavigateToTab = {},
            onAddTransactionClick = {},
            onTransactionClick = {},
            onDeleteTransaction = {}
        )
    }
}
