package com.nintech.budgment.ui.transfers

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.ui.accounts.AccountIcon
import com.nintech.budgment.ui.accounts.defaultColorHex
import com.nintech.budgment.ui.accounts.defaultIcon
import com.nintech.budgment.ui.categories.parseHexColor
import com.nintech.budgment.ui.common.ColorIconTile
import com.nintech.budgment.ui.common.SurfaceSystemBars
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.theme.tabular
import com.nintech.budgment.ui.transactions.AmountError
import com.nintech.budgment.ui.transactions.DateField

@Composable
fun TransferFormScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    onCreateAccountClick: () -> Unit,
    viewModel: TransferFormViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    if (viewModel != null) {
        val state by viewModel.state.collectAsState()

        LaunchedEffect(state.isSaved) {
            if (state.isSaved) onSaved()
        }

        TransferFormScreen(
            state = state,
            onFromSelected = viewModel::onFromSelected,
            onToSelected = viewModel::onToSelected,
            onSwap = viewModel::onSwap,
            onAmountChange = viewModel::onAmountChange,
            onAmountInChange = viewModel::onAmountInChange,
            onDateChange = viewModel::onDateChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onSaveClick = viewModel::save,
            onBackClick = onBackClick,
            onCreateAccountClick = onCreateAccountClick
        )
    } else {
        TransferFormScreen(
            state = TransferFormState(isLoading = false),
            onFromSelected = {},
            onToSelected = {},
            onSwap = {},
            onAmountChange = {},
            onAmountInChange = {},
            onDateChange = {},
            onDescriptionChange = {},
            onSaveClick = {},
            onBackClick = onBackClick,
            onCreateAccountClick = onCreateAccountClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFormScreen(
    state: TransferFormState,
    onFromSelected: (String) -> Unit,
    onToSelected: (String) -> Unit,
    onSwap: () -> Unit,
    onAmountChange: (String) -> Unit,
    onAmountInChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SurfaceSystemBars()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isEditing) R.string.transfer_form_title_edit else R.string.transfer_form_title_new))
                },
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

            // Hacen falta dos cuentas: se ofrece crear la que falta.
            state.accounts.size < 2 -> NotEnoughAccountsMessage(
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
                AccountDropdown(
                    label = stringResource(R.string.transfer_form_from_label),
                    accounts = state.accounts,
                    selected = state.fromAccount,
                    onSelected = onFromSelected,
                    supportingText = state.fromBalanceAfter?.let { after ->
                        stringResource(
                            R.string.transfer_form_balance_after_format,
                            formatCents(after, state.fromAccount?.currency.orEmpty())
                        )
                    }
                )

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FilledTonalIconButton(onClick = onSwap) {
                        Icon(
                            Icons.Filled.SwapVert,
                            contentDescription = stringResource(R.string.transfer_form_swap_content_description)
                        )
                    }
                }

                AccountDropdown(
                    label = stringResource(R.string.transfer_form_to_label),
                    accounts = state.accounts,
                    selected = state.toAccount,
                    onSelected = onToSelected,
                    errorText = if (state.sameAccountError) stringResource(R.string.transfer_form_error_same_account) else null
                )

                Spacer(Modifier.height(16.dp))

                AmountInput(
                    value = state.amountText,
                    onValueChange = onAmountChange,
                    label = stringResource(
                        if (state.currenciesDiffer) R.string.transfer_form_amount_label else R.string.transaction_form_amount_label
                    ),
                    currency = state.fromAccount?.currency,
                    error = state.amountError,
                    requestFocus = !state.isEditing
                )

                // Entre monedas distintas lo que llega no es lo que sale: se sugiere y se puede ajustar.
                if (state.currenciesDiffer) {
                    Spacer(Modifier.height(16.dp))
                    AmountInput(
                        value = state.amountInText,
                        onValueChange = onAmountInChange,
                        label = stringResource(R.string.transfer_form_amount_in_label),
                        currency = state.toAccount?.currency,
                        error = state.amountInError,
                        hint = stringResource(
                            if (state.rateMissing) R.string.transfer_form_amount_in_no_rate
                            else R.string.transfer_form_amount_in_hint
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))

                DateField(dateMillis = state.dateMillis, onDateChange = onDateChange)

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.transaction_form_description_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.transaction_form_save_button else R.string.transfer_form_save_button
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun NotEnoughAccountsMessage(onCreateAccountClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.transfer_form_not_enough_accounts),
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
private fun AccountDropdown(
    label: String,
    accounts: List<AccountEntity>,
    selected: AccountEntity?,
    onSelected: (String) -> Unit,
    supportingText: String? = null,
    errorText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = selected?.let { { AccountTile(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = (errorText ?: supportingText)?.let { { Text(it) } },
            isError = errorText != null,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    leadingIcon = { AccountTile(account) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(account.name, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                formatCents(account.balanceCache, account.currency),
                                style = MaterialTheme.typography.bodyMedium.tabular,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelected(account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AccountTile(account: AccountEntity) {
    ColorIconTile(
        color = parseHexColor(account.color) ?: parseHexColor(account.accountType.defaultColorHex())!!,
        icon = AccountIcon.resolveIcon(account.icon) ?: account.accountType.defaultIcon().icon,
        size = 24.dp,
        cornerRadius = 6.dp
    )
}

@Composable
private fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    currency: String?,
    error: AmountError?,
    hint: String? = null,
    requestFocus: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    // Al crear, el teclado aparece directo en el monto: es lo primero que se quiere escribir.
    LaunchedEffect(requestFocus) {
        if (requestFocus) focusRequester.requestFocus()
    }

    val errorText = error?.let {
        stringResource(
            if (it == AmountError.REQUIRED) R.string.transaction_form_error_amount_required
            else R.string.transaction_form_error_invalid_amount
        )
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        textStyle = MaterialTheme.typography.headlineSmall.tabular,
        suffix = currency?.let { { Text(it) } },
        supportingText = (errorText ?: hint)?.let { { Text(it) } },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
    )
}

private val sampleAccounts = listOf(
    AccountEntity(
        id = "acc_1", userId = "user_1", name = "Cuenta Principal",
        accountType = AccountType.DEBIT, currency = "MXN", balanceCache = 1_250_000
    ),
    AccountEntity(
        id = "acc_2", userId = "user_1", name = "Ahorro en dólares",
        accountType = AccountType.SAVINGS, currency = "USD", balanceCache = 50_000
    ),
    AccountEntity(
        id = "acc_3", userId = "user_1", name = "Efectivo",
        accountType = AccountType.CASH, currency = "MXN", balanceCache = 80_000
    )
)

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransferFormScreenPreview() {
    BudgmentTheme {
        TransferFormScreen(
            state = TransferFormState(
                fromAccountId = "acc_1",
                toAccountId = "acc_2",
                amountText = "2000.00",
                amountInText = "100.00",
                accounts = sampleAccounts,
                isEditing = true,
                isLoading = false
            ),
            onFromSelected = {},
            onToSelected = {},
            onSwap = {},
            onAmountChange = {},
            onAmountInChange = {},
            onDateChange = {},
            onDescriptionChange = {},
            onSaveClick = {},
            onBackClick = {},
            onCreateAccountClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransferFormScreenNotEnoughAccountsPreview() {
    BudgmentTheme {
        TransferFormScreen(
            state = TransferFormState(accounts = sampleAccounts.take(1), isLoading = false),
            onFromSelected = {},
            onToSelected = {},
            onSwap = {},
            onAmountChange = {},
            onAmountInChange = {},
            onDateChange = {},
            onDescriptionChange = {},
            onSaveClick = {},
            onBackClick = {},
            onCreateAccountClick = {}
        )
    }
}
