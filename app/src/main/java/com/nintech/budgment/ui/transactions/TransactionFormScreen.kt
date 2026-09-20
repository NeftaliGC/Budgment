package com.nintech.budgment.ui.transactions

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import androidx.compose.ui.platform.LocalInspectionMode
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.ui.categories.label
import com.nintech.budgment.ui.theme.BudgmentTheme

@Composable
fun TransactionFormScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TransactionFormViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    if (viewModel != null) {
        val state by viewModel.state.collectAsState()

        LaunchedEffect(state.isSaved) {
            if (state.isSaved) onSaved()
        }

        TransactionFormScreen(
            state = state,
            onAccountSelected = viewModel::onAccountSelected,
            onCategorySelected = viewModel::onCategorySelected,
            onAmountChange = viewModel::onAmountChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onSaveClick = viewModel::save,
            onBackClick = onBackClick
        )
    } else {
        TransactionFormScreen(
            state = TransactionFormState(),
            onAccountSelected = {},
            onCategorySelected = {},
            onAmountChange = {},
            onDescriptionChange = {},
            onSaveClick = {},
            onBackClick = onBackClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    state: TransactionFormState,
    onAccountSelected: (AccountEntity) -> Unit,
    onCategorySelected: (CategoryEntity) -> Unit,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEditing) R.string.transaction_form_title_edit else R.string.transaction_form_title_new)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            when {
                !state.isLoading && state.accounts.isEmpty() -> {
                    Text(stringResource(R.string.transaction_form_no_accounts), color = MaterialTheme.colorScheme.onBackground)
                }
                !state.isLoading && state.categories.isEmpty() -> {
                    Text(stringResource(R.string.transaction_form_no_categories), color = MaterialTheme.colorScheme.onBackground)
                }
                else -> {
                    ExposedDropdownMenuBox(expanded = accountMenuExpanded, onExpandedChange = { accountMenuExpanded = it }) {
                        OutlinedTextField(
                            value = state.accountName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.transaction_form_account_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = accountMenuExpanded, onDismissRequest = { accountMenuExpanded = false }) {
                            state.accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name) },
                                    onClick = { onAccountSelected(account); accountMenuExpanded = false }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = it }) {
                        OutlinedTextField(
                            value = if (state.categoryName.isNotBlank()) "${state.categoryName} (${state.categoryType?.label() ?: ""})" else "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.transaction_form_category_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                            state.categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text("${category.name} (${category.type.label()})") },
                                    onClick = { onCategorySelected(category); categoryMenuExpanded = false }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.amountText,
                        onValueChange = onAmountChange,
                        label = { Text(stringResource(R.string.transaction_form_amount_label)) },
                        singleLine = true,
                        isError = state.errorMessage != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.description,
                        onValueChange = onDescriptionChange,
                        label = { Text(stringResource(R.string.transaction_form_description_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    state.errorMessage?.let { error ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                if (error == TransactionFormError.INVALID_AMOUNT) R.string.transaction_form_error_invalid_amount
                                else R.string.transaction_form_error_missing_fields
                            ),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    Button(
                        onClick = onSaveClick,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(stringResource(R.string.transaction_form_save_button))
                    }
                }
            }
        }
    }
}

private val sampleAccounts = listOf(
    AccountEntity(
        id = "acc_1",
        userId = "user_1",
        name = "Cuenta Principal",
        accountType = AccountType.DEBIT,
        currency = "MXN"
    ),
    AccountEntity(
        id = "acc_2",
        userId = "user_1",
        name = "Tarjeta de Crédito",
        accountType = AccountType.CREDIT,
        currency = "MXN"
    )
)

private val sampleCategories = listOf(
    CategoryEntity(
        id = "cat_1",
        userId = "user_1",
        name = "Supermercado",
        type = CategoryType.EXPENSE
    ),
    CategoryEntity(
        id = "cat_2",
        userId = "user_1",
        name = "Sueldo",
        type = CategoryType.INCOME
    )
)

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransactionFormScreenPreview() {
    BudgmentTheme {
        TransactionFormScreen(
            state = TransactionFormState(
                accountId = "acc_1",
                accountName = "Cuenta Principal",
                categoryId = "cat_1",
                categoryName = "Supermercado",
                categoryType = CategoryType.EXPENSE,
                amountText = "350.00",
                description = "Compras de la semana",
                accounts = sampleAccounts,
                categories = sampleCategories,
                isEditing = false,
                isLoading = false
            ),
            onAccountSelected = {},
            onCategorySelected = {},
            onAmountChange = {},
            onDescriptionChange = {},
            onSaveClick = {},
            onBackClick = {}
        )
    }
}