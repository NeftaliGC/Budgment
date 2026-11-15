package com.nintech.budgment.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.ui.categories.CategoryColorPalette
import com.nintech.budgment.ui.common.IconPicker
import com.nintech.budgment.ui.common.ColorPicker
import com.nintech.budgment.ui.common.SurfaceSystemBars

@Composable
fun AccountFormScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AccountFormViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    if (viewModel != null) {
        val state by viewModel.state.collectAsState()

        LaunchedEffect(state.isSaved) {
            if (state.isSaved) onSaved()
        }

        AccountFormScreenContent(
            state = state,
            onNameChange = viewModel::onNameChange,
            onAccountTypeChange = viewModel::onAccountTypeChange,
            onCurrencyChange = viewModel::onCurrencyChange,
            onIconChange = viewModel::onIconChange,
            onColorChange = viewModel::onColorChange,
            onCreditLimitChange = viewModel::onCreditLimitChange,
            onStatementDayChange = viewModel::onStatementDayChange,
            onPaymentDueDayChange = viewModel::onPaymentDueDayChange,
            onInitialBalanceChange = viewModel::onInitialBalanceChange,
            onSaveClick = viewModel::save,
            onBackClick = onBackClick
        )
    } else {
        AccountFormScreenContent(
            state = AccountFormState(),
            onNameChange = {},
            onAccountTypeChange = {},
            onCurrencyChange = {},
            onIconChange = {},
            onColorChange = {},
            onCreditLimitChange = {},
            onStatementDayChange = {},
            onPaymentDueDayChange = {},
            onInitialBalanceChange = {},
            onSaveClick = {},
            onBackClick = onBackClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormScreenContent(
    state: AccountFormState,
    onNameChange: (String) -> Unit,
    onAccountTypeChange: (AccountType) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onIconChange: (String?) -> Unit,
    onColorChange: (String?) -> Unit,
    onCreditLimitChange: (String) -> Unit,
    onStatementDayChange: (String) -> Unit,
    onPaymentDueDayChange: (String) -> Unit,
    onInitialBalanceChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var typeMenuExpanded by remember { mutableStateOf(false) }

    SurfaceSystemBars()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEditing) R.string.account_form_title_edit else R.string.account_form_title_new)) },
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
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.account_form_name_label)) },
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.form_error_name_required)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(expanded = typeMenuExpanded, onExpandedChange = { typeMenuExpanded = it }) {
                OutlinedTextField(
                    value = state.accountType.label(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.account_form_type_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                    AccountType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label()) },
                            onClick = { onAccountTypeChange(type); typeMenuExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            var currencyMenuExpanded by remember { mutableStateOf(false) }
            val currencies = buildList {
                addAll(SupportedCurrency.ALL.map { it.code })
                if (state.currency !in this) add(state.currency)
            }
            ExposedDropdownMenuBox(expanded = currencyMenuExpanded, onExpandedChange = { currencyMenuExpanded = it }) {
                OutlinedTextField(
                    value = state.currency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.account_form_currency_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = currencyMenuExpanded, onDismissRequest = { currencyMenuExpanded = false }) {
                    currencies.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(code) },
                            onClick = { onCurrencyChange(code); currencyMenuExpanded = false }
                        )
                    }
                }
            }

            if (!state.isEditing) {
                val isCredit = state.accountType == AccountType.CREDIT
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.initialBalanceText,
                    onValueChange = onInitialBalanceChange,
                    label = {
                        Text(
                            stringResource(
                                if (isCredit) R.string.account_form_initial_debt_label
                                else R.string.account_form_initial_balance_label
                            )
                        )
                    },
                    suffix = { Text(state.currency) },
                    supportingText = {
                        Text(
                            stringResource(
                                when {
                                    state.initialBalanceError -> R.string.account_form_initial_balance_error
                                    isCredit -> R.string.account_form_initial_debt_hint
                                    else -> R.string.account_form_initial_balance_hint
                                }
                            )
                        )
                    },
                    isError = state.initialBalanceError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(15.dp))

            Text(
                text = stringResource(R.string.category_form_icon_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            IconPicker(
                items = AccountIcon.entries,
                icon = { it.icon },
                label = { stringResource(it.labelRes) },
                isSelected = { state.icon == it.key },
                // Volver a tocar el icono elegido lo quita y se usa el automático según el tipo.
                onClick = { onIconChange(if (state.icon == it.key) null else it.key) },
                accentColor = MaterialTheme.colorScheme.tertiary
            )

            Spacer(Modifier.height(15.dp))

            Text(
                text = stringResource(R.string.category_form_color_label) + ": " + stringResource(R.string.account_form_color_auto_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            ColorPicker(
                colors = CategoryColorPalette,
                selectedHex = state.color,
                onSelect = onColorChange,
                defaultLabel = stringResource(R.string.category_form_color_default_content_description)
            )

            if (state.accountType == AccountType.CREDIT) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.creditLimitText,
                    onValueChange = onCreditLimitChange,
                    label = { Text(stringResource(R.string.account_form_credit_limit_label)) },
                    isError = state.creditLimitError,
                    supportingText = if (state.creditLimitError) {
                        { Text(stringResource(R.string.form_error_invalid_amount)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.statementDayText,
                    onValueChange = onStatementDayChange,
                    label = { Text(stringResource(R.string.account_form_statement_day_label)) },
                    isError = state.statementDayError,
                    supportingText = if (state.statementDayError) {
                        { Text(stringResource(R.string.account_form_error_day)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.paymentDueDayText,
                    onValueChange = onPaymentDueDayChange,
                    label = { Text(stringResource(R.string.account_form_payment_due_day_label)) },
                    isError = state.paymentDueDayError,
                    supportingText = if (state.paymentDueDayError) {
                        { Text(stringResource(R.string.account_form_error_day)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text(stringResource(R.string.account_form_save_button))
            }
        }
    }
}