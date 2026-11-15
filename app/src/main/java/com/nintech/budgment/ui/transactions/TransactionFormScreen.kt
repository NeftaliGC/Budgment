package com.nintech.budgment.ui.transactions

import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.ui.categories.CategoryIcon
import com.nintech.budgment.ui.categories.label
import com.nintech.budgment.ui.categories.parseHexColor
import com.nintech.budgment.ui.common.ColorIconTile
import com.nintech.budgment.ui.common.SurfaceSystemBars
import com.nintech.budgment.ui.common.formatDate
import com.nintech.budgment.ui.common.isToday
import com.nintech.budgment.ui.common.localDateAsUtcMidnight
import com.nintech.budgment.ui.common.withDateFromUtcMidnight
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.theme.tabular

@Composable
fun TransactionFormScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onCreateCategoryClick: () -> Unit,
    viewModel: TransactionFormViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    if (viewModel != null) {
        val state by viewModel.state.collectAsState()

        LaunchedEffect(state.isSaved) {
            if (state.isSaved) onSaved()
        }

        TransactionFormScreen(
            state = state,
            onTypeChange = viewModel::onTypeChange,
            onAmountChange = viewModel::onAmountChange,
            onCategorySelected = viewModel::onCategorySelected,
            onAccountSelected = viewModel::onAccountSelected,
            onDateChange = viewModel::onDateChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onSaveClick = viewModel::save,
            onBackClick = onBackClick,
            onCreateAccountClick = onCreateAccountClick,
            onCreateCategoryClick = onCreateCategoryClick
        )
    } else {
        TransactionFormScreen(
            state = TransactionFormState(isLoading = false),
            onTypeChange = {},
            onAmountChange = {},
            onCategorySelected = {},
            onAccountSelected = {},
            onDateChange = {},
            onDescriptionChange = {},
            onSaveClick = {},
            onBackClick = onBackClick,
            onCreateAccountClick = onCreateAccountClick,
            onCreateCategoryClick = onCreateCategoryClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    state: TransactionFormState,
    onTypeChange: (CategoryType) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onAccountSelected: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onCreateCategoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SurfaceSystemBars()

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
        when {
            state.isLoading -> Unit

            // Sin cuentas no hay dónde registrar el movimiento: se ofrece crear una.
            state.accounts.isEmpty() -> NoAccountsMessage(
                onCreateAccountClick = onCreateAccountClick,
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                TypeSelector(type = state.type, onTypeChange = onTypeChange)

                Spacer(Modifier.height(16.dp))

                AmountField(
                    state = state,
                    onAmountChange = onAmountChange,
                    requestFocus = !state.isEditing
                )

                Spacer(Modifier.height(16.dp))

                CategorySelector(
                    state = state,
                    onCategorySelected = onCategorySelected,
                    onCreateCategoryClick = onCreateCategoryClick
                )

                Spacer(Modifier.height(16.dp))

                AccountField(state = state, onAccountSelected = onAccountSelected)

                Spacer(Modifier.height(16.dp))

                DateField(dateMillis = state.dateMillis, onDateChange = onDateChange)

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.transaction_form_description_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) {
                    Text(stringResource(R.string.transaction_form_save_button))
                }
            }
        }
    }
}

@Composable
private fun NoAccountsMessage(onCreateAccountClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.transaction_form_no_accounts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCreateAccountClick) {
                Text(stringResource(R.string.transaction_form_create_account))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSelector(type: CategoryType, onTypeChange: (CategoryType) -> Unit) {
    val types = CategoryType.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        types.forEachIndexed { index, option ->
            SegmentedButton(
                selected = type == option,
                onClick = { onTypeChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size)
            ) {
                Text(option.label())
            }
        }
    }
}

@Composable
private fun AmountField(state: TransactionFormState, onAmountChange: (String) -> Unit, requestFocus: Boolean) {
    val focusRequester = remember { FocusRequester() }

    // Al crear, el teclado aparece directo en el monto: es lo primero que se quiere escribir.
    LaunchedEffect(requestFocus) {
        if (requestFocus) focusRequester.requestFocus()
    }

    OutlinedTextField(
        value = state.amountText,
        onValueChange = onAmountChange,
        label = { Text(stringResource(R.string.transaction_form_amount_label)) },
        textStyle = MaterialTheme.typography.headlineSmall.tabular,
        suffix = { state.selectedAccount?.let { Text(it.currency) } },
        supportingText = state.amountError?.let { error ->
            {
                Text(
                    stringResource(
                        if (error == AmountError.REQUIRED) R.string.transaction_form_error_amount_required
                        else R.string.transaction_form_error_invalid_amount
                    )
                )
            }
        },
        isError = state.amountError != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelector(
    state: TransactionFormState,
    onCategorySelected: (String) -> Unit,
    onCreateCategoryClick: () -> Unit
) {
    val categories = state.categoriesOfType

    Text(
        text = stringResource(R.string.transaction_form_category_label),
        style = MaterialTheme.typography.bodyMedium,
        color = if (state.categoryError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))

    if (categories.isEmpty()) {
        Text(
            text = stringResource(
                if (state.type == CategoryType.EXPENSE) R.string.transaction_form_no_expense_categories
                else R.string.transaction_form_no_income_categories
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                val selected = category.id == state.categoryId
                FilterChip(
                    selected = selected,
                    onClick = { onCategorySelected(category.id) },
                    label = { Text(category.name) },
                    leadingIcon = {
                        if (selected) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        } else {
                            ColorIconTile(
                                color = parseHexColor(category.color) ?: MaterialTheme.colorScheme.primary,
                                icon = CategoryIcon.resolveIcon(category.icon),
                                size = 18.dp,
                                cornerRadius = 5.dp
                            )
                        }
                    }
                )
            }
        }
    }

    if (state.categoryError && categories.isNotEmpty()) {
        Text(
            text = stringResource(R.string.transaction_form_error_category_required),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    TextButton(onClick = onCreateCategoryClick) {
        Text(stringResource(R.string.transaction_form_create_category))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountField(state: TransactionFormState, onAccountSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = state.selectedAccount?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.transaction_form_account_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = if (state.accountError) {
                { Text(stringResource(R.string.transaction_form_error_account_required)) }
            } else {
                null
            },
            isError = state.accountError,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = {
                        onAccountSelected(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(dateMillis: Long, onDateChange: (Long) -> Unit) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    // Un campo de solo lectura no recibe onClick: se abre el calendario al soltar el toque.
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) showPicker = true
        }
    }

    OutlinedTextField(
        value = if (isToday(dateMillis)) stringResource(R.string.common_today) else formatDate(dateMillis),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.transaction_form_date_label)) },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = stringResource(R.string.transaction_form_date_pick_content_description)
                )
            }
        },
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth()
    )

    if (showPicker) {
        // El calendario trabaja con medianoche UTC; se convierte desde y hacia la fecha local.
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = localDateAsUtcMidnight(dateMillis))
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onDateChange(withDateFromUtcMidnight(dateMillis, it)) }
                        showPicker = false
                    }
                ) { Text(stringResource(R.string.common_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
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
        type = CategoryType.EXPENSE,
        icon = "grocery",
        color = "#4CAF50"
    ),
    CategoryEntity(
        id = "cat_2",
        userId = "user_1",
        name = "Transporte",
        type = CategoryType.EXPENSE,
        icon = "transport",
        color = "#2196F3"
    ),
    CategoryEntity(
        id = "cat_3",
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
                type = CategoryType.EXPENSE,
                amountText = "350.00",
                categoryId = "cat_1",
                accountId = "acc_1",
                description = "Compras de la semana",
                accounts = sampleAccounts,
                categories = sampleCategories,
                isEditing = true,
                isLoading = false
            ),
            onTypeChange = {},
            onAmountChange = {},
            onCategorySelected = {},
            onAccountSelected = {},
            onDateChange = {},
            onDescriptionChange = {},
            onSaveClick = {},
            onBackClick = {},
            onCreateAccountClick = {},
            onCreateCategoryClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionFormScreenErrorsPreview() {
    BudgmentTheme {
        TransactionFormScreen(
            state = TransactionFormState(
                accounts = sampleAccounts,
                accountId = "acc_1",
                categories = sampleCategories,
                amountError = AmountError.REQUIRED,
                categoryError = true,
                isEditing = true,
                isLoading = false
            ),
            onTypeChange = {},
            onAmountChange = {},
            onCategorySelected = {},
            onAccountSelected = {},
            onDateChange = {},
            onDescriptionChange = {},
            onSaveClick = {},
            onBackClick = {},
            onCreateAccountClick = {},
            onCreateCategoryClick = {}
        )
    }
}
