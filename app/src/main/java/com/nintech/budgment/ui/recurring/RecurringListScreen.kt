package com.nintech.budgment.ui.recurring

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.RecurringTransactionEntity
import com.nintech.budgment.data.model.PeriodUnit
import com.nintech.budgment.ui.categories.CategoryIcon
import com.nintech.budgment.ui.categories.parseHexColor
import com.nintech.budgment.ui.common.ColorIconTile
import com.nintech.budgment.ui.common.EmptyState
import com.nintech.budgment.ui.common.SurfaceSystemBars
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.common.formatShortDate
import com.nintech.budgment.ui.common.rememberUndoableDelete
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.theme.tabular

@Composable
fun RecurringListScreen(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onRecurringClick: (String) -> Unit,
    onRegisterClick: (String) -> Unit = {},
    viewModel: RecurringListViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    if (viewModel != null) {
        val uiState by viewModel.uiState.collectAsState()
        RecurringListScreen(
            uiState = uiState,
            onBackClick = onBackClick,
            onAddClick = onAddClick,
            onRecurringClick = onRecurringClick,
            onSetActive = viewModel::setActive,
            onRegister = onRegisterClick,
            onSkip = viewModel::skip,
            onDelete = rememberUndoableDelete(
                snackbarHostState = snackbarHostState,
                message = stringResource(R.string.recurring_deleted_message),
                onDelete = viewModel::delete,
                onRestore = viewModel::restore
            ),
            snackbarHostState = snackbarHostState
        )
    } else {
        RecurringListScreen(
            uiState = RecurringListUiState(isLoading = false),
            onBackClick = onBackClick,
            onAddClick = onAddClick,
            onRecurringClick = onRecurringClick,
            onSetActive = { _, _ -> },
            onRegister = onRegisterClick,
            onSkip = {},
            onDelete = {},
            snackbarHostState = snackbarHostState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    uiState: RecurringListUiState,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onRecurringClick: (String) -> Unit,
    onSetActive: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onRegister: (String) -> Unit = {},
    onSkip: (RecurringItem) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    SurfaceSystemBars()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recurring_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.recurring_add_content_description))
            }
        }
    ) { innerPadding ->
        if (!uiState.isLoading && uiState.items.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.recurring_empty_title),
                message = stringResource(R.string.recurring_empty_subtitle),
                actionLabel = stringResource(R.string.recurring_add_content_description),
                onAction = onAddClick,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                item(key = "explainer") {
                    Text(
                        stringResource(R.string.recurring_list_explainer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(uiState.items, key = { it.recurring.id }) { item ->
                    RecurringRow(
                        item = item,
                        onClick = { onRecurringClick(item.recurring.id) },
                        onSetActive = { onSetActive(item.recurring.id, it) },
                        onDelete = { onDelete(item.recurring.id) },
                        onRegister = { onRegister(item.recurring.id) },
                        onSkip = { onSkip(item) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }
        }
    }
}

@Composable
private fun RecurringRow(
    item: RecurringItem,
    onClick: () -> Unit,
    onSetActive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onRegister: () -> Unit,
    onSkip: () -> Unit
) {
    val dimmed = item.isPaused || item.isFinished
    val amountColor = when {
        dimmed -> MaterialTheme.colorScheme.onSurfaceVariant
        item.isExpense -> MaterialTheme.colorScheme.error
        else -> BudgmentTheme.extraColors.success
    }
    val amountText = (if (item.isExpense) "" else "+") + formatCents(item.recurring.baseAmount, item.currency)

    // Estado en palabras: el color no es la única pista.
    val status = when {
        item.needsAttention -> stringResource(R.string.recurring_status_needs_attention)
        item.isPaused -> stringResource(R.string.recurring_status_paused)
        item.isFinished -> stringResource(R.string.recurring_status_finished)
        item.isSnoozed -> stringResource(R.string.recurring_status_snoozed)
        item.dueCount > 0 -> stringResource(R.string.recurring_status_pending)
        else -> stringResource(R.string.recurring_status_next_format, formatShortDate(item.recurring.nextOccurrence))
    }
    val statusColor = when {
        item.needsAttention || (item.isDue && !item.isPaused) -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val frequencyLabel = item.frequency?.label().orEmpty()

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorIconTile(
            color = parseHexColor(item.categoryColor) ?: MaterialTheme.colorScheme.primary,
            icon = CategoryIcon.resolveIcon(item.categoryIcon),
            size = 36.dp,
            cornerRadius = 10.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOf(frequencyLabel, item.accountName).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(status, style = MaterialTheme.typography.bodySmall, color = statusColor)
            // Lo vencido se puede registrar desde aquí en cualquier momento, aunque esté pospuesto.
            if (item.isDue && !item.isPaused) {
                FilledTonalButton(
                    onClick = onRegister,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(stringResource(R.string.recurring_register))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(amountText, style = MaterialTheme.typography.titleMedium.tabular, color = amountColor)
        RecurringActions(
            paused = item.isPaused,
            canSkip = item.isDue && !item.isPaused,
            onSetActive = onSetActive,
            onSkip = onSkip,
            onDelete = onDelete
        )
    }
}

@Composable
private fun RecurringActions(
    paused: Boolean,
    canSkip: Boolean,
    onSetActive: (Boolean) -> Unit,
    onSkip: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.common_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (canSkip) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.recurring_skip)) },
                    leadingIcon = { Icon(Icons.Filled.SkipNext, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onSkip()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(if (paused) R.string.recurring_resume else R.string.recurring_pause)) },
                leadingIcon = {
                    Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onSetActive(paused)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.recurring_delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

private fun sample(id: String, name: String, amount: Long, next: Long, active: Boolean = true) = RecurringItem(
    recurring = RecurringTransactionEntity(
        id = id, userId = "u", accountId = "a", categoryId = "c", baseAmount = amount, description = name,
        periodUnit = PeriodUnit.MONTH, anchorDate = next, nextOccurrence = next, active = active
    ),
    name = name,
    accountName = "Débito",
    currency = "MXN"
)

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RecurringListScreenPreview() {
    val day = 24 * 60 * 60 * 1000L
    val now = System.currentTimeMillis()
    BudgmentTheme {
        RecurringListScreen(
            uiState = RecurringListUiState(
                items = listOf(
                    sample("1", "Renta", -800_000, now - day).copy(dueCount = 1, isPending = true),
                    sample("2", "Nómina", 2_500_000, now + 10 * day),
                    sample("3", "Gimnasio", -59_900, now + 3 * day, active = false)
                ),
                isLoading = false
            ),
            onBackClick = {},
            onAddClick = {},
            onRecurringClick = {},
            onSetActive = { _, _ -> },
            onDelete = {}
        )
    }
}
