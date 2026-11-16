package com.nintech.budgment.ui.goals

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.ui.categories.CategoryColorPalette
import com.nintech.budgment.ui.categories.parseHexColor
import com.nintech.budgment.ui.common.ColorPicker
import com.nintech.budgment.ui.common.IconPicker
import com.nintech.budgment.ui.common.SurfaceSystemBars
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.transactions.DateField
import java.util.Calendar

@Composable
fun GoalFormScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    onCreateAccountClick: () -> Unit,
    viewModel: GoalFormViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    val state = viewModel?.state?.collectAsState()?.value ?: GoalFormState(isLoading = false)
    GoalFormScreen(
        state = state,
        onBackClick = onBackClick,
        onCreateAccountClick = onCreateAccountClick,
        onNameChange = { viewModel?.onNameChange(it) },
        onTargetChange = { viewModel?.onTargetChange(it) },
        onSourceChange = { viewModel?.onSourceChange(it) },
        onAccountChange = { viewModel?.onAccountChange(it) },
        onCurrencyChange = { viewModel?.onCurrencyChange(it) },
        onInitialChange = { viewModel?.onInitialChange(it) },
        onTargetDateChange = { viewModel?.onTargetDateChange(it) },
        onIconChange = { viewModel?.onIconChange(it) },
        onColorChange = { viewModel?.onColorChange(it) },
        onSaveClick = { viewModel?.save(onSaved) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormScreen(
    state: GoalFormState,
    onBackClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onSourceChange: (GoalSource) -> Unit,
    onAccountChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onInitialChange: (String) -> Unit,
    onTargetDateChange: (Long?) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (String?) -> Unit,
    onSaveClick: () -> Unit
) {
    SurfaceSystemBars()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEditing) R.string.goal_form_title_edit else R.string.goal_form_title_new)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.goal_form_name_label)) },
                placeholder = { Text(stringResource(R.string.goal_form_name_placeholder)) },
                singleLine = true,
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.form_error_name_required)) }
                } else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // Primero cómo se ahorra: de eso depende la moneda del monto objetivo.
            Text(
                stringResource(R.string.goal_form_source_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            val sources = listOf(GoalSource.ACCOUNT, GoalSource.MANUAL)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                sources.forEachIndexed { index, source ->
                    SegmentedButton(
                        selected = state.source == source,
                        onClick = { onSourceChange(source) },
                        shape = SegmentedButtonDefaults.itemShape(index, sources.size)
                    ) {
                        Text(stringResource(if (source == GoalSource.ACCOUNT) R.string.goal_form_source_account else R.string.goal_form_source_manual))
                    }
                }
            }
            Text(
                stringResource(
                    if (state.source == GoalSource.ACCOUNT) R.string.goal_form_source_account_hint else R.string.goal_form_source_manual_hint
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            // Cambiar de modo en una meta guardada cambia de dónde sale el avance: se dice antes de guardar.
            if (state.isEditing && state.source == GoalSource.MANUAL) {
                Text(
                    stringResource(R.string.goal_form_switch_to_manual_hint, formatCents(state.savedManual, state.manualCurrency)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(16.dp))

            if (state.source == GoalSource.ACCOUNT) {
                if (state.accounts.isEmpty() && state.accountId == null) {
                    Text(stringResource(R.string.goal_form_no_accounts), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onCreateAccountClick, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text(stringResource(R.string.accounts_add_content_description))
                    }
                } else {
                    val selected = state.selectedAccount
                    OptionDropdown(
                        label = stringResource(R.string.goal_form_account_label),
                        value = selected?.name ?: if (state.accountId != null) stringResource(R.string.goal_source_account_missing) else "",
                        options = state.accounts.map { it.id to "${it.name} · ${it.currency}" },
                        onSelect = onAccountChange,
                        isError = state.accountError,
                        supporting = when {
                            state.accountError -> stringResource(R.string.goal_form_error_account)
                            selected != null -> stringResource(R.string.goal_form_account_balance, formatCents(selected.balanceCache, selected.currency))
                            else -> null
                        }
                    )
                }
            } else {
                OptionDropdown(
                    label = stringResource(R.string.account_form_currency_label),
                    value = state.manualCurrency,
                    options = SupportedCurrency.ALL.map { it.code to it.code },
                    onSelect = onCurrencyChange,
                    // Con aportes ya registrados, cambiar la moneda mezclaría cifras.
                    enabled = !(state.isEditing && state.savedManual != 0L)
                )
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.targetText,
                onValueChange = onTargetChange,
                label = { Text(stringResource(R.string.goal_form_target_label)) },
                singleLine = true,
                isError = state.targetError,
                suffix = { Text(state.currency) },
                supportingText = if (state.targetError) {
                    { Text(stringResource(R.string.budget_form_error_amount)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            if (state.source == GoalSource.MANUAL && !state.isEditing) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.initialText,
                    onValueChange = onInitialChange,
                    label = { Text(stringResource(R.string.goal_form_initial_label)) },
                    singleLine = true,
                    isError = state.initialError,
                    suffix = { Text(state.currency) },
                    supportingText = {
                        Text(stringResource(if (state.initialError) R.string.budget_form_error_amount else R.string.goal_form_initial_hint))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(16.dp))

            TargetDateField(state.targetDate, state.dateError, onTargetDateChange)
            Spacer(Modifier.height(20.dp))

            val accent = parseHexColor(state.color) ?: MaterialTheme.colorScheme.primary
            Text(
                stringResource(R.string.category_form_icon_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            IconPicker(
                items = GoalIcons,
                icon = { it.icon },
                label = { stringResource(it.labelRes) },
                isSelected = { state.icon == it.key },
                onClick = { onIconChange(it.key) },
                accentColor = accent
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.category_form_color_label),
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
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onSaveClick,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text(stringResource(R.string.goal_form_save))
            }
        }
    }
}

/** La fecha es opcional: sin ella la meta solo dice cuánto falta. */
@Composable
private fun TargetDateField(date: Long?, isError: Boolean, onChange: (Long?) -> Unit) {
    if (date == null) {
        Text(
            stringResource(R.string.goal_form_date_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = {
                // Propone dentro de un año; el usuario la ajusta en el calendario.
                onChange(Calendar.getInstance().apply {
                    add(Calendar.YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis)
            },
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) { Text(stringResource(R.string.goal_form_date_add)) }
    } else {
        DateField(dateMillis = date, onDateChange = onChange, label = stringResource(R.string.goal_form_date_label))
        if (isError) {
            Text(
                stringResource(R.string.goal_form_error_date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
        TextButton(onClick = { onChange(null) }, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Text(stringResource(R.string.goal_form_date_remove))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionDropdown(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
    isError: Boolean = false,
    supporting: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            isError = isError,
            label = { Text(label) },
            supportingText = supporting?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(key); expanded = false })
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GoalFormScreenPreview() {
    BudgmentTheme {
        GoalFormScreen(
            state = GoalFormState(name = "Viaje a Japón", targetText = "50000", isLoading = false),
            onBackClick = {}, onCreateAccountClick = {}, onNameChange = {}, onTargetChange = {}, onSourceChange = {},
            onAccountChange = {}, onCurrencyChange = {}, onInitialChange = {}, onTargetDateChange = {},
            onIconChange = {}, onColorChange = {}, onSaveClick = {}
        )
    }
}
