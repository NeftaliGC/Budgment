package com.nintech.budgment.ui.budgets

import com.nintech.budgment.ui.common.EmptyState
import androidx.compose.ui.res.pluralStringResource
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.BudgetEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.model.BudgetScope
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.data.model.BudgetPeriod
import com.nintech.budgment.data.model.PeriodUnit
import com.nintech.budgment.ui.categories.CategoryIcon
import com.nintech.budgment.ui.categories.parseHexColor
import com.nintech.budgment.ui.common.BudgmentScaffold
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.common.formatShortDate
import com.nintech.budgment.ui.theme.BudgmentTheme
import kotlin.math.roundToInt
import com.nintech.budgment.ui.common.RowActionsMenu
import com.nintech.budgment.ui.common.rememberUndoableDelete
import com.nintech.budgment.ui.theme.tabular
import com.nintech.budgment.ui.common.CurrencyMenu

@Composable
fun BudgetsListScreen(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    onAddBudgetClick: () -> Unit,
    onBudgetClick: (String) -> Unit,
    viewModel: BudgetsViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    if (viewModel != null) {
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        BudgetsListScreen(
            uiState = uiState,
            currentRoute = currentRoute,
            onNavigateToTab = onNavigateToTab,
            onSettingsClick = onSettingsClick,
            onAddBudgetClick = onAddBudgetClick,
            onBudgetClick = onBudgetClick,
            onDeleteBudget = rememberUndoableDelete(
                snackbarHostState = snackbarHostState,
                message = stringResource(R.string.budgets_deleted_message),
                onDelete = viewModel::deleteBudget,
                onRestore = viewModel::restoreBudget
            ),
            snackbarHostState = snackbarHostState,
            onCurrencyChange = viewModel::setDisplayCurrency,
            modifier = Modifier
        )
    } else {
        BudgetsListScreen(
            uiState = BudgetsUiState(),
            currentRoute = currentRoute,
            onNavigateToTab = onNavigateToTab,
            onSettingsClick = onSettingsClick,
            onAddBudgetClick = onAddBudgetClick,
            onBudgetClick = onBudgetClick,
            onDeleteBudget = {},
            onCurrencyChange = {},
            modifier = Modifier
        )
    }
}

@Composable
fun BudgetsListScreen(
    uiState: BudgetsUiState,
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    onAddBudgetClick: () -> Unit,
    onBudgetClick: (String) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    BudgmentScaffold(
        currentRoute = currentRoute,
        title = stringResource(R.string.budgets_title),
        onNavigateToTab = onNavigateToTab,
        onSettingsClick = onSettingsClick,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBudgetClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.budgets_add_content_description))
            }
        }
    ) { innerPadding ->
        if (!uiState.isLoading && uiState.global.isEmpty() && uiState.category.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.budgets_empty_title),
                message = stringResource(R.string.budgets_empty_subtitle),
                actionLabel = stringResource(R.string.budgets_add_content_description),
                onAction = onAddBudgetClick,
                modifier = modifier.background(MaterialTheme.colorScheme.background).padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "summary") {
                    BudgetSummaryHeader(
                        totalAllocated = uiState.totalAllocated,
                        totalSpent = uiState.totalSpent,
                        hasGlobal = uiState.hasGlobal,
                        categoryLimitsTotal = uiState.categoryLimitsTotal,
                        categoryLimitsExceedGlobal = uiState.categoryLimitsExceedGlobal,
                        primaryCurrency = uiState.primaryCurrency,
                        missingRateCount = uiState.missingRateCount,
                        period = uiState.summaryPeriod,
                        hasOtherPeriods = uiState.hasOtherPeriods,
                        onCurrencyChange = onCurrencyChange
                    )
                }
                if (uiState.global.isNotEmpty()) {
                    item(key = "section_global") {
                        BudgetSectionHeader(BudgetScope.GLOBAL.label())
                    }
                    items(uiState.global, key = { it.budget.id }) { item ->
                        BudgetCard(
                            item = item,
                            currency = uiState.primaryCurrency,
                            linkedCategories = uiState.budgetCategories[item.budget.id].orEmpty(),
                            onClick = { onBudgetClick(item.budget.id) },
                            onDeleteClick = { onDeleteBudget(item.budget.id) }
                        )
                    }
                }
                if (uiState.category.isNotEmpty()) {
                    item(key = "section_category") {
                        // Con un global, los de categoría son topes dentro de él, no dinero aparte.
                        BudgetSectionHeader(
                            if (uiState.hasGlobal) {
                                stringResource(R.string.budgets_section_category_within_global)
                            } else {
                                BudgetScope.CATEGORY.label()
                            }
                        )
                    }
                    items(uiState.category, key = { it.budget.id }) { item ->
                        BudgetCard(
                            item = item,
                            currency = uiState.primaryCurrency,
                            linkedCategories = uiState.budgetCategories[item.budget.id].orEmpty(),
                            onClick = { onBudgetClick(item.budget.id) },
                            onDeleteClick = { onDeleteBudget(item.budget.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
    )
}

@Composable
private fun BudgetSummaryHeader(
    totalAllocated: Long,
    totalSpent: Long,
    hasGlobal: Boolean,
    categoryLimitsTotal: Long,
    categoryLimitsExceedGlobal: Boolean,
    primaryCurrency: String,
    missingRateCount: Int,
    period: BudgetPeriod?,
    hasOtherPeriods: Boolean,
    onCurrencyChange: (String) -> Unit
) {
    val remaining = totalAllocated - totalSpent
    val remainingColor = if (remaining < 0) {
        MaterialTheme.colorScheme.error
    } else {
        BudgmentTheme.extraColors.success
    }
    val progress = if (totalAllocated > 0) {
        (totalSpent.toFloat() / totalAllocated).coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    period?.summaryTitle() ?: stringResource(R.string.budgets_summary_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CurrencyMenu(selectedCode = primaryCurrency, onSelect = onCurrencyChange)
            }
            // Dice de dónde salen las cifras: sin global, no es todo lo gastado en el mes.
            Text(
                stringResource(
                    if (hasGlobal) R.string.budgets_summary_source_global else R.string.budgets_summary_source_categories
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            // Una fila por cifra: con montos largos o texto grande, tres columnas recortaban el dinero.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SummaryValue(
                    label = stringResource(R.string.budgets_summary_allocated),
                    value = formatCents(totalAllocated, primaryCurrency)
                )
                SummaryValue(
                    label = stringResource(R.string.budgets_summary_spent),
                    value = formatCents(totalSpent, primaryCurrency)
                )
                SummaryValue(
                    label = stringResource(R.string.budgets_summary_remaining),
                    value = formatCents(remaining, primaryCurrency),
                    color = remainingColor
                )
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            if (categoryLimitsExceedGlobal) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(
                        R.string.budgets_category_limits_exceed_global,
                        formatCents(categoryLimitsTotal, primaryCurrency),
                        formatCents(totalAllocated, primaryCurrency)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hasOtherPeriods) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.budgets_summary_other_periods),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (missingRateCount > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    pluralStringResource(R.plurals.budgets_missing_rates, missingRateCount, missingRateCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.tabular,
            color = color ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BudgetCard(
    item: BudgetWithProgress,
    currency: String,
    linkedCategories: List<CategoryEntity>,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val overLimit = item.progress > 1f
    val accentColor = if (item.budget.scope == BudgetScope.CATEGORY) {
        linkedCategories.firstNotNullOfOrNull { parseHexColor(it.color) } ?: MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val remaining = item.limit - item.spent
    val percent = (item.progress * 100).roundToInt().coerceAtLeast(0)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.budget.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.budgets_percent_format, percent),
                        style = MaterialTheme.typography.labelMedium,
                        // El color de la categoría queda solo en la barra: como texto no garantiza contraste.
                        color = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(
                        R.string.budgets_renews_format,
                        item.period.label(),
                        formatShortDate(item.period.endExclusive)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { item.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (overLimit) MaterialTheme.colorScheme.error else accentColor,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(6.dp))
                // Dos líneas: en una sola fila los dos montos no caben en pantallas angostas.
                Text(
                    stringResource(
                        R.string.budgets_spent_format,
                        formatCents(item.spent, currency),
                        formatCents(item.limit, currency)
                    ),
                    style = MaterialTheme.typography.bodyMedium.tabular,
                    color = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(
                        if (overLimit) R.string.budgets_exceeded_format else R.string.budgets_remaining_format,
                        formatCents(if (overLimit) -remaining else remaining, currency)
                    ),
                    style = MaterialTheme.typography.labelLarge.tabular,
                    color = if (overLimit) MaterialTheme.colorScheme.error else BudgmentTheme.extraColors.success
                )
                if (item.allCategoriesDeleted) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.budgets_categories_deleted_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.budget.scope == BudgetScope.CATEGORY && linkedCategories.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        linkedCategories.take(3).forEach { category ->
                            CategoryMiniChip(category)
                        }
                    }
                }
            }
            RowActionsMenu(deleteLabel = stringResource(R.string.budgets_delete_content_description), onDeleteClick = onDeleteClick)
        }
    }
}

@Composable
private fun CategoryMiniChip(category: CategoryEntity) {
    val accent = parseHexColor(category.color) ?: MaterialTheme.colorScheme.primary
    val icon = CategoryIcon.resolveIcon(category.icon)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            category.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BudgetsListScreenPopulatedLightPreview() {
    BudgmentTheme {
        BudgetsListScreen(
            uiState = BudgetsUiState(
                global = listOf(
                    BudgetWithProgress(
                        budget = BudgetEntity(
                            id = "1",
                            userId = "user_1",
                            name = "Gastos del mes",
                            amountLimit = 2000000L,
                            scope = BudgetScope.GLOBAL,
                            periodUnit = PeriodUnit.MONTH,
                            startDate = 1000L,
                            endDate = 2000L,
                            periodInterval = 1
                        ),
                        spent = 1450000L,
                        progress = 0.725f,
                        missingRate = false
                    )
                ),
                category = listOf(
                    BudgetWithProgress(
                        budget = BudgetEntity(
                            id = "2",
                            userId = "user_1",
                            name = "Entretenimiento",
                            amountLimit = 500000L,
                            scope = BudgetScope.CATEGORY,
                            periodUnit = PeriodUnit.MONTH,
                            startDate = 1000L,
                            endDate = 2000L,
                            periodInterval = 1
                        ),
                        spent = 780000L,
                        progress = 1.56f,
                        missingRate = false
                    ),
                    BudgetWithProgress(
                        budget = BudgetEntity(
                            id = "3",
                            userId = "user_1",
                            name = "Alimentación",
                            amountLimit = 400000L,
                            scope = BudgetScope.CATEGORY,
                            periodUnit = PeriodUnit.MONTH,
                            startDate = 1000L,
                            endDate = 2000L,
                            periodInterval = 1
                        ),
                        spent = 230000L,
                        progress = 0.575f,
                        missingRate = false
                    )
                ),
                budgetCategories = mapOf(
                    "3" to listOf(
                        CategoryEntity(
                            id = "c1",
                            userId = "user_1",
                            name = "Comida",
                            type = CategoryType.EXPENSE,
                            icon = "restaurant",
                            color = "#FFC107"
                        ),
                        CategoryEntity(
                            id = "c2",
                            userId = "user_1",
                            name = "Bebidas",
                            type = CategoryType.EXPENSE,
                            icon = "fastfood",
                            color = "#E91E63"
                        )
                    )
                ),
                totalAllocated = 2000000L,
                totalSpent = 1450000L,
                summaryBudgetId = "1",
                categoryLimitsTotal = 900000L,
                primaryCurrency = "MXN",
                isLoading = false
            ),
            currentRoute = "budgets",
            onNavigateToTab = {},
            onAddBudgetClick = {},
            onBudgetClick = {},
            onDeleteBudget = {},
            onCurrencyChange = {}
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BudgetsListScreenEmptyLightPreview() {
    BudgmentTheme {
        BudgetsListScreen(
            uiState = BudgetsUiState(isLoading = false),
            currentRoute = "budgets",
            onNavigateToTab = {},
            onAddBudgetClick = {},
            onBudgetClick = {},
            onDeleteBudget = {},
            onCurrencyChange = {}
        )
    }
}