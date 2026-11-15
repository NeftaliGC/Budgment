package com.nintech.budgment.ui.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.model.BudgetScope
import com.nintech.budgment.ui.common.SurfaceSystemBars

@Composable
fun BudgetFormScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: BudgetFormViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    if (viewModel != null) {
        val state by viewModel.state.collectAsState()

        LaunchedEffect(state.isSaved) {
            if (state.isSaved) onSaved()
        }

        BudgetFormScreenContent(
            state = state,
            onNameChange = viewModel::onNameChange,
            onAmountChange = viewModel::onAmountChange,
            onScopeChange = viewModel::onScopeChange,
            onCategoryToggle = viewModel::onCategoryToggle,
            onSaveClick = viewModel::save,
            onBackClick = onBackClick
        )
    } else {
        BudgetFormScreenContent(
            state = BudgetFormState(),
            onNameChange = {},
            onAmountChange = {},
            onScopeChange = {},
            onCategoryToggle = {},
            onSaveClick = {},
            onBackClick = onBackClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormScreenContent(
    state: BudgetFormState,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onScopeChange: (BudgetScope) -> Unit,
    onCategoryToggle: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scopeMenuExpanded by remember { mutableStateOf(false) }

    SurfaceSystemBars()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEditing) R.string.budget_form_title_edit else R.string.budget_form_title_new)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.budget_form_name_label)) },
                singleLine = true,
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.form_error_name_required)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.amountLimitText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.budget_form_amount_label)) },
                singleLine = true,
                isError = state.amountError,
                supportingText = if (state.amountError) {
                    { Text(stringResource(R.string.budget_form_error_amount)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(expanded = scopeMenuExpanded, onExpandedChange = { scopeMenuExpanded = it }) {
                OutlinedTextField(
                    value = state.scope.label(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.budget_form_scope_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scopeMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = scopeMenuExpanded, onDismissRequest = { scopeMenuExpanded = false }) {
                    BudgetScope.entries.forEach { scope ->
                        DropdownMenuItem(text = { Text(scope.label()) }, onClick = { onScopeChange(scope); scopeMenuExpanded = false })
                    }
                }
            }

            if (state.scope == BudgetScope.CATEGORY) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.budget_form_categories_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.categoriesError) {
                    Text(
                        stringResource(R.string.budget_form_error_categories_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (!state.isLoading && state.expenseCategories.isEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.budget_form_no_expense_categories), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                } else {
                    state.expenseCategories.forEach { category ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = category.id in state.selectedCategoryIds, onCheckedChange = { onCategoryToggle(category.id) })
                            Text(category.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(onClick = onSaveClick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.budget_form_save_button))
            }
        }
    }
}