package com.nintech.budgment.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.ui.categories.CategoryColorPalette
import com.nintech.budgment.ui.categories.parseHexColor

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

            Spacer(Modifier.height(15.dp))

            Text(
                text = stringResource(R.string.category_form_icon_label),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            AccountIcon.entries.chunked(6).forEach { rowIcons ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowIcons.forEach { item ->
                        val selected = state.icon == item.key
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onIconChange(if (selected) null else item.key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(15.dp))

            Text(
                text = stringResource(R.string.category_form_color_label) + ": " + stringResource(R.string.account_form_color_auto_hint),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            (listOf<String?>(null) + CategoryColorPalette).chunked(6).forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowColors.forEach { hex ->
                        val color = parseHexColor(hex)
                        val selected = state.color == hex
                        val isDefault = hex == null
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color ?: MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .border(
                                    if (selected) 3.dp else 1.dp,
                                    if (selected && !isDefault) color!! else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    CircleShape
                                )
                                .clickable { onColorChange(if (selected) null else hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDefault) {
                                Icon(
                                    imageVector = Icons.Filled.NotInterested,
                                    contentDescription = stringResource(R.string.category_form_color_default_content_description),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
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