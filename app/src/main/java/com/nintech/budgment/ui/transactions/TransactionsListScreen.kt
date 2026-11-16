package com.nintech.budgment.ui.transactions

import com.nintech.budgment.ui.common.relativeDayLabel
import com.nintech.budgment.ui.common.startOfDay
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import com.nintech.budgment.ui.common.EmptyState
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.ui.common.BudgmentScaffold
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.common.RowActionsMenu
import com.nintech.budgment.ui.common.rememberUndoableDelete
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EventRepeat

@Composable
fun TransactionsListScreen(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    onStatisticsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAddTransactionClick: () -> Unit,
    onCategoriesClick: () -> Unit = {},
    onTransactionClick: (String) -> Unit,
    onAddTransferClick: () -> Unit = {},
    onTransferClick: (String) -> Unit = {},
    onRecurringClick: () -> Unit = {},
    viewModel: TransactionsViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    if (viewModel != null) {
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        TransactionsListScreen(
            uiState = uiState,
            currentRoute = currentRoute,
            onNavigateToTab = onNavigateToTab,
            onSettingsClick = onSettingsClick,
            onStatisticsClick = onStatisticsClick,
            onNotificationsClick = onNotificationsClick,
            onAddTransactionClick = onAddTransactionClick,
            onCategoriesClick = onCategoriesClick,
            onTransactionClick = onTransactionClick,
            onDeleteTransaction = rememberUndoableDelete(
                snackbarHostState = snackbarHostState,
                message = stringResource(R.string.transactions_deleted_message),
                onDelete = viewModel::deleteTransaction,
                onRestore = viewModel::restoreTransaction
            ),
            onAddTransferClick = onAddTransferClick,
            onTransferClick = onTransferClick,
            onRecurringClick = onRecurringClick,
            onDeleteTransfer = rememberUndoableDelete(
                snackbarHostState = snackbarHostState,
                message = stringResource(R.string.transfer_deleted_message),
                onDelete = viewModel::deleteTransfer,
                onRestore = viewModel::restoreTransfer
            ),
            snackbarHostState = snackbarHostState
        )
    } else {
        TransactionsListScreen(
            uiState = TransactionsUiState(),
            currentRoute = currentRoute,
            onNavigateToTab = onNavigateToTab,
            onSettingsClick = onSettingsClick,
            onStatisticsClick = onStatisticsClick,
            onNotificationsClick = onNotificationsClick,
            onAddTransactionClick = onAddTransactionClick,
            onCategoriesClick = onCategoriesClick,
            onTransactionClick = onTransactionClick,
            onDeleteTransaction = {},
            onAddTransferClick = onAddTransferClick,
            onTransferClick = onTransferClick,
            onRecurringClick = onRecurringClick,
            onDeleteTransfer = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsListScreen(
    uiState: TransactionsUiState,
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    onStatisticsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAddTransactionClick: () -> Unit,
    onCategoriesClick: () -> Unit = {},
    onTransactionClick: (String) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onAddTransferClick: () -> Unit = {},
    onTransferClick: (String) -> Unit = {},
    onDeleteTransfer: (String) -> Unit = {},
    onRecurringClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    BudgmentScaffold(
        currentRoute = currentRoute,
        title = stringResource(R.string.transactions_title),
        onNavigateToTab = onNavigateToTab,
        onSettingsClick = onSettingsClick,
        onStatisticsClick = onStatisticsClick,
        onNotificationsClick = onNotificationsClick,
        topBarActions = {
            IconButton(onClick = onRecurringClick) {
                Icon(Icons.Filled.EventRepeat, contentDescription = stringResource(R.string.recurring_title))
            }
            IconButton(onClick = onAddTransferClick) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_transfer),
                    contentDescription = stringResource(R.string.transfer_add_content_description)
                )
            }
            // Las categorías sirven para clasificar movimientos: se administran desde aquí.
            IconButton(onClick = onCategoriesClick) {
                Icon(Icons.Filled.Category, contentDescription = stringResource(R.string.categories_title))
            }
        },
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
            EmptyState(
                title = stringResource(R.string.transactions_empty_title),
                message = stringResource(R.string.transactions_empty_subtitle),
                actionLabel = stringResource(R.string.transactions_add_content_description),
                onAction = onAddTransactionClick,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Ya vienen del más reciente al más antiguo; groupBy conserva ese orden.
                uiState.transactions
                    .groupBy { startOfDay(it.transaction.date) }
                    .forEach { (day, dayTransactions) ->
                stickyHeader(key = "day_$day") {
                    DayHeader(day)
                }
                items(dayTransactions, key = { it.transaction.id }) { item ->
                    val transfer = item.transfer
                    TransactionRow(
                        item = item,
                        onClick = {
                            if (transfer != null) onTransferClick(transfer.transferId)
                            else onTransactionClick(item.transaction.id)
                        },
                        showDate = false,
                        trailing = {
                            if (item.isReadOnly) {
                                // Conserva la alineación del monto con las filas que sí tienen menú.
                                Spacer(Modifier.width(48.dp))
                            } else {
                                RowActionsMenu(
                                    deleteLabel = stringResource(
                                        if (transfer != null) R.string.transfer_delete_content_description
                                        else R.string.transactions_delete_content_description
                                    ),
                                    onDeleteClick = {
                                        if (transfer != null) onDeleteTransfer(transfer.transferId)
                                        else onDeleteTransaction(item.transaction.id)
                                    }
                                )
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
                    }
            }
        }
    }
}

/** Encabezado de día; queda fijo arriba mientras se recorren sus movimientos. */
@Composable
private fun DayHeader(day: Long) {
    Text(
        text = relativeDayLabel(
            day,
            today = stringResource(R.string.common_today),
            yesterday = stringResource(R.string.common_yesterday)
        ),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, bottom = 4.dp)
            .semantics { heading() }
    )
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
