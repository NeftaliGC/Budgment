package com.nintech.budgment.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nintech.budgment.R
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.ui.categories.CategoryIcon
import com.nintech.budgment.ui.categories.parseHexColor
import com.nintech.budgment.ui.common.ColorIconTile
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.common.formatShortDate
import com.nintech.budgment.ui.common.localDateAsUtcMidnight
import com.nintech.budgment.ui.common.startOfDay
import com.nintech.budgment.ui.common.withDateFromUtcMidnight

private enum class FilterSheet { KIND, ACCOUNT, CATEGORY, DATE }

/**
 * Búsqueda por texto y filtros por tipo, cuenta, categoría y fecha. Cada chip abre su lista;
 * los cambios se aplican al momento y el resultado se resume debajo.
 */
@Composable
fun TransactionFilterBar(
    uiState: TransactionsUiState,
    onQueryChange: (String) -> Unit,
    onKindsChange: (Set<TransactionKind>) -> Unit,
    onAccountsChange: (Set<String>) -> Unit,
    onCategoriesChange: (Set<String>) -> Unit,
    onDateChange: (DateFilter?) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filter = uiState.filter
    val focusManager = LocalFocusManager.current
    var openSheet by rememberSaveable { mutableStateOf<FilterSheet?>(null) }

    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = filter.query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.transactions_search_placeholder)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (filter.query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.transactions_search_clear))
                    }
                }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "kind") {
                FilterDropdownChip(
                    label = kindLabel(filter.kinds),
                    selected = filter.kinds.isNotEmpty(),
                    onClick = { openSheet = FilterSheet.KIND }
                )
            }
            item(key = "account") {
                FilterDropdownChip(
                    label = selectionLabel(filter.accountIds, uiState.accountOptions, R.string.transactions_filter_account),
                    selected = filter.accountIds.isNotEmpty(),
                    onClick = { openSheet = FilterSheet.ACCOUNT }
                )
            }
            item(key = "category") {
                FilterDropdownChip(
                    label = selectionLabel(filter.categoryIds, uiState.categoryOptions, R.string.transactions_filter_category),
                    selected = filter.categoryIds.isNotEmpty(),
                    onClick = { openSheet = FilterSheet.CATEGORY }
                )
            }
            item(key = "date") {
                FilterDropdownChip(
                    label = dateLabel(filter.date),
                    selected = filter.date != null,
                    onClick = { openSheet = FilterSheet.DATE }
                )
            }
            if (filter.isActive) {
                item(key = "clear") {
                    AssistChip(
                        onClick = onClear,
                        label = { Text(stringResource(R.string.transactions_filter_clear)) },
                        leadingIcon = { Icon(Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }

        uiState.summary?.let { summary -> SummaryLine(summary) }
    }

    when (openSheet) {
        FilterSheet.KIND -> FilterSheetScaffold(stringResource(R.string.transactions_filter_kind), onDismiss = { openSheet = null }) {
            TransactionKind.entries.forEach { kind ->
                CheckRow(
                    label = stringResource(kind.labelRes()),
                    checked = kind in filter.kinds,
                    onCheckedChange = { checked -> onKindsChange(if (checked) filter.kinds + kind else filter.kinds - kind) }
                )
            }
        }
        FilterSheet.ACCOUNT -> FilterSheetScaffold(
            stringResource(R.string.transactions_filter_account),
            onDismiss = { openSheet = null },
            onReset = if (filter.accountIds.isNotEmpty()) ({ onAccountsChange(emptySet()) }) else null
        ) {
            uiState.accountOptions.forEach { option ->
                CheckRow(
                    label = option.displayName(),
                    checked = option.id in filter.accountIds,
                    onCheckedChange = { checked ->
                        onAccountsChange(if (checked) filter.accountIds + option.id else filter.accountIds - option.id)
                    }
                )
            }
        }
        FilterSheet.CATEGORY -> FilterSheetScaffold(
            stringResource(R.string.transactions_filter_category),
            onDismiss = { openSheet = null },
            onReset = if (filter.categoryIds.isNotEmpty()) ({ onCategoriesChange(emptySet()) }) else null
        ) {
            uiState.categoryOptions.groupBy { it.categoryType }.forEach { (type, options) ->
                Text(
                    stringResource(if (type == CategoryType.INCOME) R.string.transactions_filter_kind_income else R.string.transactions_filter_kind_expense),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).semantics { heading() }
                )
                options.forEach { option ->
                    CheckRow(
                        label = option.displayName(),
                        checked = option.id in filter.categoryIds,
                        leading = {
                            ColorIconTile(
                                color = parseHexColor(option.color) ?: MaterialTheme.colorScheme.primary,
                                icon = CategoryIcon.resolveIcon(option.icon),
                                size = 28.dp,
                                cornerRadius = 8.dp
                            )
                        },
                        onCheckedChange = { checked ->
                            onCategoriesChange(if (checked) filter.categoryIds + option.id else filter.categoryIds - option.id)
                        }
                    )
                }
            }
        }
        FilterSheet.DATE -> DateFilterSheet(filter.date, onDateChange, onDismiss = { openSheet = null })
        null -> Unit
    }
}

@Composable
private fun FilterDropdownChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
    )
}

/** «12 movimientos · Ingresos $X · Gastos $Y»; sin totales si hay varias monedas. */
@Composable
private fun SummaryLine(summary: FilterSummary) {
    val count = pluralStringResource(R.plurals.transactions_filter_count, summary.count, summary.count)
    val text = if (summary.currency != null && summary.count > 0) {
        stringResource(
            R.string.transactions_filter_summary,
            count,
            formatCents(summary.income ?: 0, summary.currency),
            formatCents(summary.expense ?: 0, summary.currency)
        )
    } else count
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp)
    )
}

private fun TransactionKind.labelRes(): Int = when (this) {
    TransactionKind.EXPENSE -> R.string.transactions_filter_kind_expense
    TransactionKind.INCOME -> R.string.transactions_filter_kind_income
    TransactionKind.TRANSFER -> R.string.transactions_filter_kind_transfer
}

@Composable
private fun kindLabel(kinds: Set<TransactionKind>): String = when (kinds.size) {
    0 -> stringResource(R.string.transactions_filter_kind)
    1 -> stringResource(kinds.single().labelRes())
    else -> stringResource(R.string.transactions_filter_multiple, stringResource(R.string.transactions_filter_kind), kinds.size)
}

/** «Cuenta», «Débito» o «Débito +2». */
@Composable
private fun selectionLabel(selected: Set<String>, options: List<FilterOption>, emptyLabel: Int): String {
    if (selected.isEmpty()) return stringResource(emptyLabel)
    val first = options.firstOrNull { it.id in selected }?.name ?: return stringResource(emptyLabel)
    return if (selected.size == 1) first else stringResource(R.string.transactions_filter_first_plus, first, selected.size - 1)
}

@Composable
private fun dateLabel(date: DateFilter?): String = when (date?.preset) {
    null -> stringResource(R.string.transactions_filter_date)
    DatePreset.CUSTOM -> stringResource(
        R.string.transactions_filter_date_range,
        formatShortDate(date.customStart ?: 0),
        formatShortDate(date.customEnd ?: 0)
    )
    else -> stringResource(date.preset.labelRes())
}

private fun DatePreset.labelRes(): Int = when (this) {
    DatePreset.THIS_MONTH -> R.string.transactions_filter_date_this_month
    DatePreset.LAST_MONTH -> R.string.transactions_filter_date_last_month
    DatePreset.LAST_30_DAYS -> R.string.transactions_filter_date_last_30
    DatePreset.THIS_YEAR -> R.string.transactions_filter_date_this_year
    DatePreset.CUSTOM -> R.string.transactions_filter_date_custom
}

@Composable
private fun FilterOption.displayName(): String =
    if (deleted) stringResource(R.string.transactions_filter_deleted_format, name) else name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheetScaffold(
    title: String,
    onDismiss: () -> Unit,
    onReset: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
                if (onReset != null) {
                    TextButton(onClick = onReset) { Text(stringResource(R.string.transactions_filter_all)) }
                }
            }
            // Listas largas (muchas categorías) se desplazan dentro de la hoja.
            LazyColumn(Modifier.heightIn(max = 480.dp).selectableGroup()) {
                item { content() }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                Text(stringResource(R.string.transactions_filter_done))
            }
        }
    }
}

@Composable
private fun CheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leading: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFilterSheet(current: DateFilter?, onChange: (DateFilter?) -> Unit, onDismiss: () -> Unit) {
    var pickingRange by rememberSaveable { mutableStateOf(false) }

    FilterSheetScaffold(stringResource(R.string.transactions_filter_date), onDismiss = onDismiss) {
        Column(Modifier.selectableGroup()) {
            RadioRow(stringResource(R.string.transactions_filter_date_any), selected = current == null) { onChange(null) }
            listOf(DatePreset.THIS_MONTH, DatePreset.LAST_MONTH, DatePreset.LAST_30_DAYS, DatePreset.THIS_YEAR).forEach { preset ->
                RadioRow(stringResource(preset.labelRes()), selected = current?.preset == preset) { onChange(DateFilter(preset)) }
            }
            RadioRow(
                label = if (current?.preset == DatePreset.CUSTOM) dateLabel(current) else stringResource(R.string.transactions_filter_date_custom),
                selected = current?.preset == DatePreset.CUSTOM
            ) { pickingRange = true }
        }
    }

    if (pickingRange) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = current?.customStart?.let(::localDateAsUtcMidnight),
            initialSelectedEndDateMillis = current?.customEnd?.let(::localDateAsUtcMidnight)
        )
        DatePickerDialog(
            onDismissRequest = { pickingRange = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedStartDateMillis != null,
                    onClick = {
                        val today = startOfDay(System.currentTimeMillis())
                        val start = pickerState.selectedStartDateMillis?.let { withDateFromUtcMidnight(today, it) }
                        // Un solo día elegido filtra ese día.
                        val end = pickerState.selectedEndDateMillis?.let { withDateFromUtcMidnight(today, it) } ?: start
                        onChange(DateFilter(DatePreset.CUSTOM, start, end))
                        pickingRange = false
                    }
                ) { Text(stringResource(R.string.transactions_filter_done)) }
            },
            dismissButton = { TextButton(onClick = { pickingRange = false }) { Text(stringResource(R.string.common_cancel)) } }
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.height(500.dp))
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
