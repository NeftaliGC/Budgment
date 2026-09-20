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
            onCreditLimitChange = viewModel::onCreditLimitChange,
            onStatementDayChange = viewModel::onStatementDayChange,
            onPaymentDueDayChange = viewModel::onPaymentDueDayChange,
            onSaveClick = viewModel::save,
            onBackClick = onBackClick
        )
    } else {
        AccountFormScreenContent(
            state = AccountFormState(),
            onNameChange = {},
            onAccountTypeChange = {},
            onCurrencyChange = {},
            onCreditLimitChange = {},
            onStatementDayChange = {},
            onPaymentDueDayChange = {},
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
    onCreditLimitChange: (String) -> Unit,
    onStatementDayChange: (String) -> Unit,
    onPaymentDueDayChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var typeMenuExpanded by remember { mutableStateOf(false) }

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

            if (state.accountType == AccountType.CREDIT) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.creditLimitText,
                    onValueChange = onCreditLimitChange,
                    label = { Text(stringResource(R.string.account_form_credit_limit_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.statementDayText,
                    onValueChange = onStatementDayChange,
                    label = { Text(stringResource(R.string.account_form_statement_day_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.paymentDueDayText,
                    onValueChange = onPaymentDueDayChange,
                    label = { Text(stringResource(R.string.account_form_payment_due_day_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(R.string.account_form_save_button))
            }
        }
    }
}