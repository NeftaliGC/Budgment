package com.nintech.budgment.ui.budgets

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import androidx.compose.ui.platform.LocalInspectionMode
import com.nintech.budgment.ui.common.BudgmentScaffold
import com.nintech.budgment.ui.common.formatCents

@Composable
fun BudgetsListScreen(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onAddBudgetClick: () -> Unit,
    onBudgetClick: (String) -> Unit,
    viewModel: BudgetsViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    val uiState = if (viewModel != null) viewModel.uiState.collectAsState().value else BudgetsUiState()

    BudgmentScaffold(
        currentRoute = currentRoute,
        onNavigateToTab = onNavigateToTab,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBudgetClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.budgets_add_content_description))
            }
        }
    ) { innerPadding ->
        if (!uiState.isLoading && uiState.budgets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.budgets_empty_title), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.budgets_empty_subtitle), fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.budgets, key = { it.budget.id }) { item ->
                    BudgetRow(item = item, onClick = { onBudgetClick(item.budget.id) }, onDeleteClick = { viewModel?.deleteBudget(item.budget.id) })
                }
            }
        }
    }
}

@Composable
private fun BudgetRow(item: BudgetWithProgress, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    val overLimit = item.progress > 1f
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(item.budget.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.budgets_period_current_month), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.budgets_delete_content_description), tint = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { item.progress.coerceAtMost(1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.budgets_spent_format, formatCents(item.spent, "MXN"), formatCents(item.budget.amountLimit, "MXN")),
            fontSize = 13.sp,
            color = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}