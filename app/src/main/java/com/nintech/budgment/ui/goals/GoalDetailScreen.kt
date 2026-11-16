package com.nintech.budgment.ui.goals

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.GoalContributionEntity
import com.nintech.budgment.data.local.entity.GoalEntity
import com.nintech.budgment.data.model.goalProgress
import com.nintech.budgment.ui.common.RowActionsMenu
import com.nintech.budgment.ui.common.SurfaceSystemBars
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.common.formatDate
import com.nintech.budgment.ui.common.parseAmountToCents
import com.nintech.budgment.ui.common.rememberUndoableDelete
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.theme.tabular

@Composable
fun GoalDetailScreen(
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onTransferToAccount: (String) -> Unit,
    viewModel: GoalDetailViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value ?: GoalDetailUiState(isLoading = false)
    val snackbarHostState = remember { SnackbarHostState() }

    // Al eliminarla (desde aquí u otra pantalla) no queda nada que mostrar.
    LaunchedEffect(uiState.isGone) { if (uiState.isGone) onBackClick() }

    GoalDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onEditClick = onEditClick,
        onTransferToAccount = onTransferToAccount,
        onAddContribution = { amount, note, withdrawal -> viewModel?.addContribution(amount, note, withdrawal) },
        onDeleteContribution = rememberUndoableDelete(
            snackbarHostState = snackbarHostState,
            message = stringResource(R.string.goal_contribution_deleted),
            onDelete = { viewModel?.deleteContribution(it) },
            onRestore = { viewModel?.restoreContribution(it) }
        ),
        onDeleteGoal = { viewModel?.deleteGoal() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    uiState: GoalDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onTransferToAccount: (String) -> Unit,
    onAddContribution: (Long?, String, Boolean) -> ContributionError?,
    onDeleteContribution: (String) -> Unit,
    onDeleteGoal: () -> Unit
) {
    SurfaceSystemBars()
    var dialogWithdrawal by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val item = uiState.item

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.goal?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (item != null) {
                        IconButton(onClick = { onEditClick(item.goal.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.goal_edit))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.goal_delete))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (item == null) return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "card") { GoalCard(item = item, onClick = {}) }

            if (item.isLinked) {
                item(key = "linked") {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(
                            stringResource(
                                if (item.accountMissing) R.string.goal_detail_account_missing else R.string.goal_detail_linked_explanation,
                                item.account?.name.orEmpty()
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.accountMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!item.accountMissing && item.goal.accountId != null) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { onTransferToAccount(item.goal.accountId) }) {
                                Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.goal_detail_transfer))
                            }
                        }
                    }
                }
            } else {
                item(key = "actions") {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { dialogWithdrawal = false }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.goal_contribute))
                        }
                        OutlinedButton(
                            onClick = { dialogWithdrawal = true },
                            enabled = item.progress.saved > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.goal_withdraw))
                        }
                    }
                }
                item(key = "history-title") {
                    Text(
                        stringResource(R.string.goal_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp).semantics { heading() }
                    )
                }
                if (uiState.contributions.isEmpty()) {
                    item(key = "history-empty") {
                        Text(
                            stringResource(R.string.goal_history_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(uiState.contributions, key = { it.id }) { contribution ->
                    ContributionRow(contribution, item.currency, onDelete = { onDeleteContribution(contribution.id) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }
        }
    }

    dialogWithdrawal?.let { withdrawal ->
        ContributionDialog(
            isWithdrawal = withdrawal,
            currency = item?.currency.orEmpty(),
            maxWithdrawal = item?.progress?.saved ?: 0,
            onConfirm = { amount, note -> onAddContribution(amount, note, withdrawal) },
            onDismiss = { dialogWithdrawal = null }
        )
    }

    if (confirmDelete && item != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.goal_delete_confirm_title, item.goal.name)) },
            text = {
                Text(
                    stringResource(
                        if (item.isLinked) R.string.goal_delete_confirm_linked else R.string.goal_delete_confirm_manual
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDeleteGoal() }) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun ContributionRow(contribution: GoalContributionEntity, currency: String, onDelete: () -> Unit) {
    val isWithdrawal = contribution.amount < 0
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                contribution.note ?: stringResource(if (isWithdrawal) R.string.goal_withdrawal else R.string.goal_contribution),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                formatDate(contribution.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            (if (isWithdrawal) "−" else "+") + formatCents(kotlin.math.abs(contribution.amount), currency),
            style = MaterialTheme.typography.bodyLarge.tabular,
            color = if (isWithdrawal) MaterialTheme.colorScheme.error else BudgmentTheme.extraColors.success
        )
        RowActionsMenu(deleteLabel = stringResource(R.string.common_delete), onDeleteClick = onDelete)
    }
}

@Composable
private fun ContributionDialog(
    isWithdrawal: Boolean,
    currency: String,
    maxWithdrawal: Long,
    onConfirm: (Long?, String) -> ContributionError?,
    onDismiss: () -> Unit
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<ContributionError?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isWithdrawal) R.string.goal_withdraw_title else R.string.goal_contribute_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text(stringResource(R.string.goal_contribution_amount)) },
                    singleLine = true,
                    isError = error != null,
                    suffix = { Text(currency) },
                    supportingText = {
                        when (error) {
                            ContributionError.INVALID_AMOUNT -> Text(stringResource(R.string.budget_form_error_amount))
                            ContributionError.EXCEEDS_SAVED -> Text(stringResource(R.string.goal_error_exceeds, formatCents(maxWithdrawal, currency)))
                            null -> if (isWithdrawal) Text(stringResource(R.string.goal_withdraw_available, formatCents(maxWithdrawal, currency)))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.goal_contribution_note)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = onConfirm(parseAmountToCents(amountText), note)
                if (result == null) onDismiss() else error = result
            }) {
                Text(stringResource(if (isWithdrawal) R.string.goal_withdraw else R.string.goal_contribute))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GoalDetailScreenPreview() {
    val goal = GoalEntity(id = "g", userId = "u", name = "Fondo de emergencia", targetAmount = 3_600_000, currentAmount = 900_000, currency = "MXN", color = "#4CAF50")
    BudgmentTheme {
        GoalDetailScreen(
            uiState = GoalDetailUiState(
                item = GoalItem(goal, "MXN", goalProgress(900_000, 3_600_000, null)),
                contributions = listOf(
                    GoalContributionEntity("1", "g", 500_000, "Aguinaldo", System.currentTimeMillis()),
                    GoalContributionEntity("2", "g", 400_000, null, System.currentTimeMillis() - 86_400_000L * 30)
                ),
                isLoading = false
            ),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {}, onEditClick = {}, onTransferToAccount = {},
            onAddContribution = { _, _, _ -> null }, onDeleteContribution = {}, onDeleteGoal = {}
        )
    }
}
