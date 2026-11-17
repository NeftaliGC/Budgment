package com.nintech.budgment.ui.statistics

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.ui.categories.CategoryIcon
import com.nintech.budgment.ui.categories.parseHexColor
import com.nintech.budgment.ui.common.ColorIconTile
import com.nintech.budgment.ui.common.CurrencyMenu
import com.nintech.budgment.ui.common.SurfaceSystemBars
import com.nintech.budgment.ui.common.appLocale
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.theme.tabular
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit,
    viewModel: StatisticsViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value ?: StatisticsUiState(isLoading = false)
    StatisticsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onModeChange = { viewModel?.onModeChange(it) },
        onPrevious = { viewModel?.onPrevious() },
        onNext = { viewModel?.onNext() },
        onCurrencyChange = { viewModel?.onCurrencyChange(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    onBackClick: () -> Unit,
    onModeChange: (StatsRangeMode) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrencyChange: (String) -> Unit
) {
    SurfaceSystemBars()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = { CurrencyMenu(selectedCode = uiState.currency, onSelect = onCurrencyChange) }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) return@Scaffold
        val data = uiState.data
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PeriodSelector(uiState, onModeChange, onPrevious, onNext)
            SummaryCard(data, uiState.currency)
            if (data.missingRate) {
                Text(
                    stringResource(R.string.stats_missing_rate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            CategoryCard(data, uiState.currency)
            FlowsCard(data, uiState.currency)
            BalanceCard(data, uiState.currency)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    uiState: StatisticsUiState,
    onModeChange: (StatsRangeMode) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val modes = StatsRangeMode.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = uiState.mode == mode,
                onClick = { onModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
            ) {
                Text(stringResource(if (mode == StatsRangeMode.MONTH) R.string.stats_mode_month else R.string.stats_mode_year))
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.stats_previous))
        }
        Text(
            text = rangeLabel(uiState.data.range),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        IconButton(onClick = onNext, enabled = uiState.canGoForward) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.stats_next))
        }
    }
}

/** «Noviembre 2026» o «2026». */
private fun rangeLabel(range: StatsRange): String {
    val locale = appLocale()
    val pattern = if (range.mode == StatsRangeMode.MONTH) "MMMM yyyy" else "yyyy"
    return SimpleDateFormat(pattern, locale).format(Date(range.start)).replaceFirstChar { it.titlecase(locale) }
}

/** «en octubre» o «en 2025»: con qué se compara. */
private fun previousLabel(range: StatsRange): String {
    val locale = appLocale()
    val previous = StatsRange.of(range.mode, 1, now = range.start)
    val pattern = if (range.mode == StatsRangeMode.MONTH) "MMMM" else "yyyy"
    return SimpleDateFormat(pattern, locale).format(Date(previous.start))
}

@Composable
private fun StatsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SummaryCard(data: StatisticsData, currency: String) {
    val success = BudgmentTheme.extraColors.success
    val error = MaterialTheme.colorScheme.error
    StatsCard(stringResource(R.string.stats_summary_title)) {
        Row(Modifier.fillMaxWidth()) {
            SummaryFigure(
                icon = R.drawable.ic_up_stats,
                label = stringResource(R.string.stats_income),
                value = formatCents(data.income, currency),
                color = success,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            SummaryFigure(
                icon = R.drawable.ic_down_stats,
                label = stringResource(R.string.stats_expense),
                value = formatCents(data.expense, currency),
                color = error,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.stats_net),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                (if (data.net > 0) "+" else "") + formatCents(data.net, currency),
                style = MaterialTheme.typography.titleLarge.tabular,
                color = if (data.net < 0) error else success
            )
        }
        comparisonText(data)?.let { text ->
            Spacer(Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** «Gastaste 12 % más que en octubre». Sin gasto previo no hay con qué comparar. */
@Composable
private fun comparisonText(data: StatisticsData): String? {
    if (data.previousExpense <= 0) return null
    val previous = previousLabel(data.range)
    val change = (data.expense - data.previousExpense).toDouble() / data.previousExpense
    val percent = abs(change * 100).roundToInt()
    return when {
        percent == 0 -> stringResource(R.string.stats_compare_same_format, previous)
        change > 0 -> stringResource(R.string.stats_compare_more_format, percent, previous)
        else -> stringResource(R.string.stats_compare_less_format, percent, previous)
    }
}

@Composable
private fun SummaryFigure(icon: Int, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.tabular,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryCard(data: StatisticsData, currency: String) {
    StatsCard(stringResource(R.string.stats_by_category_title)) {
        if (data.byCategory.isEmpty()) {
            Text(
                stringResource(R.string.stats_no_expenses),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@StatsCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            data.byCategory.forEach { item ->
                val color = parseHexColor(item.color) ?: MaterialTheme.colorScheme.primary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorIconTile(color = color, icon = CategoryIcon.resolveIcon(item.icon), size = 32.dp, cornerRadius = 8.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (item.deleted) stringResource(R.string.transactions_deleted_category_format, item.name) else item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                formatCents(item.amount, currency),
                                style = MaterialTheme.typography.bodyMedium.tabular,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { item.share },
                                modifier = Modifier.weight(1f).height(6.dp),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                drawStopIndicator = {}
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.stats_percent_format, (item.share * 100).roundToInt()),
                                style = MaterialTheme.typography.labelSmall.tabular,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowsCard(data: StatisticsData, currency: String) {
    val success = BudgmentTheme.extraColors.success
    val error = MaterialTheme.colorScheme.error
    val description = data.monthlyFlows.joinToString("; ") {
        flowDescription(it, currency)
    }
    StatsCard(stringResource(R.string.stats_flows_title)) {
        IncomeExpenseChart(
            flows = data.monthlyFlows,
            // En modo año se ven los doce meses por igual.
            selectedMonthStart = if (data.range.mode == StatsRangeMode.MONTH) data.range.start else null,
            incomeColor = success,
            expenseColor = error,
            description = description
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(success, stringResource(R.string.stats_income))
            LegendItem(error, stringResource(R.string.stats_expense))
        }
    }
}

/** Descripción accesible de un mes de la gráfica: «nov: ingresos $X, gastos $Y». */
private fun flowDescription(flow: MonthFlow, currency: String): String =
    "${monthLabel(flow.monthStart, compact = false)}: +${formatCents(flow.income, currency)}, -${formatCents(flow.expense, currency)}"

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BalanceCard(data: StatisticsData, currency: String) {
    val points = data.balanceHistory
    StatsCard(stringResource(R.string.stats_balance_title)) {
        points.lastOrNull()?.let { last ->
            Text(
                formatCents(last.balance, currency),
                style = MaterialTheme.typography.headlineSmall.tabular,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.stats_balance_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        }
        BalanceLineChart(
            points = points,
            totalMonths = data.monthlyFlows.map { it.monthStart },
            lineColor = MaterialTheme.colorScheme.primary,
            description = points.joinToString("; ") {
                "${monthLabel(it.monthStart, compact = false)}: ${formatCents(it.balance, currency)}"
            }
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatisticsScreenPreview() {
    val range = StatsRange.of(StatsRangeMode.MONTH, 0)
    val months = chartMonths(range)
    BudgmentTheme {
        StatisticsScreen(
            uiState = StatisticsUiState(
                data = StatisticsData(
                    range = range,
                    income = 2_500_000,
                    expense = 1_830_000,
                    previousExpense = 1_600_000,
                    byCategory = listOf(
                        CategorySpending("1", "Renta", "home", "#7B1FA2", 800_000, 0.44f),
                        CategorySpending("2", "Supermercado", "grocery", "#4CAF50", 520_000, 0.28f),
                        CategorySpending("3", "Transporte", "transport", "#2196F3", 310_000, 0.17f)
                    ),
                    monthlyFlows = months.mapIndexed { i, m -> MonthFlow(m.start, 2_400_000L + i * 50_000, 1_500_000L + i * 70_000) },
                    balanceHistory = months.mapIndexed { i, m -> BalancePoint(m.start, 3_000_000L + i * 400_000) }
                ),
                isLoading = false
            ),
            onBackClick = {},
            onModeChange = {},
            onPrevious = {},
            onNext = {},
            onCurrencyChange = {}
        )
    }
}
