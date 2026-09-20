package com.nintech.budgment.ui.transactions

import android.content.res.Configuration
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.ui.common.BudgmentScaffold
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.common.formatDate
import com.nintech.budgment.ui.theme.BudgmentTheme

@Composable
fun TransactionsListScreen(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    TransactionsListScreen(
        uiState = uiState,
        currentRoute = currentRoute,
        onNavigateToTab = onNavigateToTab,
        onAddTransactionClick = onAddTransactionClick,
        onTransactionClick = onTransactionClick,
        onDeleteTransaction = viewModel::deleteTransaction
    )
}

@Composable
fun TransactionsListScreen(
    uiState: TransactionsUiState,
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onDeleteTransaction: (String) -> Unit
) {
    BudgmentScaffold(
        currentRoute = currentRoute,
        onNavigateToTab = onNavigateToTab,
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
                        fontSize = 18.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.transactions_empty_subtitle),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
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

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.categoryName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${item.accountName} · ${formatDate(item.transaction.date)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Text(
            text = formatCents(item.transaction.amount, item.transaction.currency ?: ""),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = amountColor
        )
        IconButton(onClick = onDeleteClick) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.transactions_delete_content_description),
                tint = MaterialTheme.colorScheme.error
            )
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
