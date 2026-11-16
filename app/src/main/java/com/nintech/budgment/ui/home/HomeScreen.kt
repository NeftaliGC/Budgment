package com.nintech.budgment.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.nintech.budgment.notifications.RecurringNotifications
import com.nintech.budgment.ui.recurring.PendingRecurringCard
import com.nintech.budgment.ui.recurring.RecurringItem
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.BudgetEntity
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.model.BudgetScope
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.data.model.PeriodUnit
import com.nintech.budgment.ui.budgets.BudgetWithProgress
import com.nintech.budgment.ui.budgets.homeCardTitle
import com.nintech.budgment.ui.common.BudgmentScaffold
import com.nintech.budgment.ui.common.CurrencyMenu
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.navigation.BudgmentDestination
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.theme.tabular
import com.nintech.budgment.ui.transactions.TransactionRow
import com.nintech.budgment.ui.transactions.TransactionWithDetails
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.res.pluralStringResource

@Composable
fun HomeScreen(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onStatisticsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAddAccountClick: () -> Unit,
    onAddBudgetClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onTransferClick: (String) -> Unit = {},
    onReviewRecurring: (String) -> Unit = {},
    viewModel: HomeViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    val uiState = if (viewModel != null) viewModel.uiState.collectAsState().value else HomeUiState()

    HomeScreenContent(
        currentRoute = currentRoute,
        uiState = uiState,
        onNavigateToTab = onNavigateToTab,
        onSettingsClick = onSettingsClick,
        onStatisticsClick = onStatisticsClick,
        onNotificationsClick = onNotificationsClick,
        onAddAccountClick = onAddAccountClick,
        onAddBudgetClick = onAddBudgetClick,
        onAddTransactionClick = onAddTransactionClick,
        onTransactionClick = onTransactionClick,
        onTransferClick = onTransferClick,
        onCurrencyChange = { viewModel?.onDisplayCurrencyChange(it) },
        onRegisterRecurring = { viewModel?.registerRecurring(it) },
        onReviewRecurring = { onReviewRecurring(it.recurring.id) },
        onSnoozeRecurring = { viewModel?.snoozeRecurring(it) },
        onSkipRecurring = { viewModel?.skipRecurring(it) }
    )
}

/**
 * Inicio responde cómo voy: arriba el saldo de las cuentas, luego el presupuesto del mes
 * (cuánto queda y cuánto se puede gastar por día), los presupuestos más avanzados y los
 * últimos movimientos.
 */
@Composable
fun HomeScreenContent(
    currentRoute: String,
    uiState: HomeUiState,
    onNavigateToTab: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onStatisticsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAddAccountClick: () -> Unit,
    onAddBudgetClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onTransferClick: (String) -> Unit = {},
    onCurrencyChange: (String) -> Unit,
    onRegisterRecurring: (RecurringItem) -> Unit = {},
    onReviewRecurring: (RecurringItem) -> Unit = {},
    onSnoozeRecurring: (RecurringItem) -> Unit = {},
    onSkipRecurring: (RecurringItem) -> Unit = {}
) {
    // Si el usuario ya ve los pendientes en la app, la notificación que los anunciaba sobra.
    val context = LocalContext.current
    val hasPending = uiState.pendingRecurring.isNotEmpty()
    val hasDue = hasPending || uiState.snoozedRecurring.isNotEmpty()
    LaunchedEffect(hasPending) {
        if (hasPending) RecurringNotifications.cancelPending(context)
    }

    BudgmentScaffold(
        currentRoute = currentRoute,
        onNavigateToTab = onNavigateToTab,
        onSettingsClick = onSettingsClick,
        onStatisticsClick = onStatisticsClick,
        onNotificationsClick = onNotificationsClick,
        floatingActionButton = {
            // Registrar un movimiento es la acción principal de la app; sin cuentas no aplica.
            if (uiState.hasAccounts) {
                ExtendedFloatingActionButton(
                    onClick = onAddTransactionClick,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.home_action_new_transaction)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            when {
                uiState.isLoading -> Unit

                !uiState.hasAccounts -> GetStartedCard(onAddAccountClick = onAddAccountClick)

                else -> {
                    // Lo que espera una decisión va primero.
                    if (hasDue) {
                        PendingRecurringCard(
                            items = uiState.pendingRecurring,
                            snoozed = uiState.snoozedRecurring,
                            onRegister = onRegisterRecurring,
                            onReview = onReviewRecurring,
                            onSnooze = onSnoozeRecurring,
                            onSkip = onSkipRecurring
                        )
                        Spacer(Modifier.height(24.dp))
                    }

                    MonthSummary(
                        uiState = uiState,
                        onCurrencyChange = onCurrencyChange,
                        onAddBudgetClick = onAddBudgetClick,
                        onBudgetsClick = { onNavigateToTab(BudgmentDestination.Budgets.route) }
                    )

                    if (uiState.topBudgets.isNotEmpty()) {
                        Spacer(Modifier.height(32.dp))
                        SectionHeader(
                            title = stringResource(R.string.bottom_nav_budgets),
                            onSeeAllClick = { onNavigateToTab(BudgmentDestination.Budgets.route) }
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            uiState.topBudgets.forEach { item ->
                                BudgetProgressRow(
                                    item = item,
                                    onClick = { onNavigateToTab(BudgmentDestination.Budgets.route) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                    SectionHeader(
                        title = stringResource(R.string.home_recent_transactions_title),
                        onSeeAllClick = if (uiState.recentTransactions.isNotEmpty()) {
                            { onNavigateToTab(BudgmentDestination.Transactions.route) }
                        } else {
                            null
                        }
                    )
                    if (uiState.recentTransactions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_no_transactions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.recentTransactions.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                            TransactionRow(
                                item = item,
                                onClick = {
                                    val transfer = item.transfer
                                    if (transfer != null) onTransferClick(transfer.transferId)
                                    else onTransactionClick(item.transaction.id)
                                }
                            )
                        }
                    }

                    // Deja libre el último renglón por encima del botón flotante.
                    Spacer(Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
private fun GetStartedCard(onAddAccountClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_get_started_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_get_started_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAddAccountClick) {
                Text(stringResource(R.string.accounts_add_content_description))
            }
        }
    }
}

@Composable
private fun MonthSummary(
    uiState: HomeUiState,
    onCurrencyChange: (String) -> Unit,
    onAddBudgetClick: () -> Unit,
    onBudgetsClick: () -> Unit
) {
    val currency = uiState.primaryCurrency

    // 1. Saldo: el dinero que hay en las cuentas.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.accounts_total_balance_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CurrencyMenu(selectedCode = currency, onSelect = onCurrencyChange)
    }
    Text(
        text = formatCents(uiState.totalBalance, currency),
        style = MaterialTheme.typography.displaySmall.tabular,
        color = MaterialTheme.colorScheme.onBackground
    )
    if (uiState.missingRateAccounts > 0) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = pluralStringResource(R.plurals.missing_rate_accounts, uiState.missingRateAccounts, uiState.missingRateAccounts),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    Spacer(Modifier.height(20.dp))

    // 2. Presupuesto del mes: cuánto se ha gastado y cuánto queda.
    if (uiState.hasBudgets) {
        BudgetMonthCard(uiState = uiState, onClick = onBudgetsClick)
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.home_no_budgets_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onAddBudgetClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text(stringResource(R.string.home_action_new_budget))
                }
            }
        }
    }
}

/**
 * Resumen del presupuesto del mes en tres datos, cada uno con su etiqueta: cuánto queda,
 * cuánto se ha gastado del total y cuánto se puede gastar por día hasta que se renueve.
 */
@Composable
private fun BudgetMonthCard(uiState: HomeUiState, onClick: () -> Unit) {
    val currency = uiState.primaryCurrency
    val overBudget = uiState.budgetRemaining < 0
    val percent = (uiState.budgetSpentFraction * 100).roundToInt().coerceAtLeast(0)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.budgetFromGlobal) {
                        uiState.budgetPeriod?.homeCardTitle() ?: stringResource(R.string.home_budget_card_title)
                    } else {
                        stringResource(R.string.home_budget_card_title_categories)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.budgets_percent_format, percent),
                    style = MaterialTheme.typography.labelLarge.tabular,
                    color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (overBudget) R.string.budgets_exceeded_format else R.string.budgets_remaining_format,
                    formatCents(abs(uiState.budgetRemaining), currency)
                ),
                style = MaterialTheme.typography.headlineSmall.tabular,
                color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { uiState.budgetSpentFraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.home_budget_spent_format,
                    formatCents(uiState.budgetSpent, currency),
                    formatCents(uiState.budgetAllocated, currency)
                ),
                style = MaterialTheme.typography.bodyMedium.tabular,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!overBudget) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.home_budget_per_day,
                        uiState.budgetDaysLeft,
                        uiState.budgetDaysLeft,
                        formatCents(uiState.budgetPerDayLeft, currency)
                    ),
                    style = MaterialTheme.typography.bodyMedium.tabular,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAllClick: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onSeeAllClick != null) {
            TextButton(
                onClick = onSeeAllClick,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text(text = stringResource(R.string.home_see_all), style = MaterialTheme.typography.labelLarge)
            }
        } else {
            // Mantiene la misma altura que un encabezado con botón.
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun BudgetProgressRow(item: BudgetWithProgress, onClick: () -> Unit) {
    val overLimit = item.progress > 1f
    val percent = (item.progress * 100).roundToInt().coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.budget.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.budgets_percent_format, percent),
                style = MaterialTheme.typography.labelLarge.tabular,
                color = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { item.progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
    }
}

private fun sampleBudget(id: String, name: String, limit: Long, spent: Long) = BudgetWithProgress(
    budget = BudgetEntity(
        id = id,
        userId = "user_1",
        name = name,
        amountLimit = limit,
        scope = BudgetScope.GLOBAL,
        periodUnit = PeriodUnit.MONTH,
        startDate = 1000L,
        endDate = 2000L,
        periodInterval = 1
    ),
    spent = spent,
    progress = spent.toFloat() / limit,
    missingRate = false
)

private fun sampleTransaction(id: String, category: String, type: CategoryType, amount: Long) = TransactionWithDetails(
    transaction = TransactionEntity(
        id = id,
        userId = "user_1",
        accountId = "acc_1",
        categoryId = "cat_$id",
        amount = amount,
        date = System.currentTimeMillis()
    ),
    accountName = "Cuenta Principal",
    categoryName = category,
    categoryType = type,
    currency = "MXN"
)

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenPreview() {
    BudgmentTheme {
        HomeScreenContent(
            currentRoute = BudgmentDestination.Home.route,
            uiState = HomeUiState(
                primaryCurrency = "MXN",
                hasAccounts = true,
                totalBalance = 1445000,
                budgetAllocated = 2000000,
                budgetSpent = 1450000,
                budgetFromGlobal = true,
                topBudgets = listOf(
                    sampleBudget("1", "Entretenimiento", 500000, 780000),
                    sampleBudget("3", "Alimentación", 400000, 230000)
                ),
                budgetDaysLeft = 16,
                recentTransactions = listOf(
                    sampleTransaction("1", "Supermercado", CategoryType.EXPENSE, -35000),
                    sampleTransaction("2", "Sueldo", CategoryType.INCOME, 2500000)
                ),
                isLoading = false
            ),
            onNavigateToTab = {},
            onSettingsClick = {},
            onAddAccountClick = {},
            onAddBudgetClick = {},
            onAddTransactionClick = {},
            onTransactionClick = {},
            onCurrencyChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenNoBudgetsPreview() {
    BudgmentTheme {
        HomeScreenContent(
            currentRoute = BudgmentDestination.Home.route,
            uiState = HomeUiState(hasAccounts = true, totalBalance = 1445000, isLoading = false),
            onNavigateToTab = {},
            onSettingsClick = {},
            onAddAccountClick = {},
            onAddBudgetClick = {},
            onAddTransactionClick = {},
            onTransactionClick = {},
            onCurrencyChange = {}
        )
    }
}
