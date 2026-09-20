package com.nintech.budgment.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.ui.accounts.label
import com.nintech.budgment.ui.common.BudgmentScaffold
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.navigation.BudgmentDestination
import com.nintech.budgment.ui.theme.BudgmentTheme

@Composable
fun HomeScreen(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onAddCategoryClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onSeeAllCategoriesClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreenContent(
        currentRoute = currentRoute,
        uiState = uiState,
        onNavigateToTab = onNavigateToTab,
        onAddAccountClick = onAddAccountClick,
        onAccountClick = onAccountClick,
        onAddCategoryClick = onAddCategoryClick,
        onCategoryClick = onCategoryClick,
        onSeeAllCategoriesClick = onSeeAllCategoriesClick,
        onAddTransactionClick = onAddTransactionClick
    )
}

@Composable
fun HomeScreenContent(
    currentRoute: String,
    uiState: HomeUiState,
    onNavigateToTab: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onAccountClick: (String) -> Unit,
    onAddCategoryClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onSeeAllCategoriesClick: () -> Unit,
    onAddTransactionClick: () -> Unit
) {
    BudgmentScaffold(currentRoute = currentRoute, onNavigateToTab = onNavigateToTab) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Saldo disponible
            Text(
                text = stringResource(R.string.home_total_balance_title),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatCents(uiState.totalBalance, uiState.primaryCurrency),
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(32.dp))

            // Acciones rápidas
            Text(
                text = stringResource(R.string.home_quick_actions_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))
            Row {
                QuickActionItem(
                    label = stringResource(R.string.home_action_new_transaction),
                    icon = { Icon(painter = painterResource(R.drawable.ic_transacciones), contentDescription = null) },
                    onClick = onAddTransactionClick
                )
                Spacer(Modifier.width(8.dp))
                QuickActionItem(
                    label = stringResource(R.string.home_action_new_account),
                    icon = { Icon(painter = painterResource(R.drawable.ic_wallet), contentDescription = null) },
                    onClick = onAddAccountClick
                )
                Spacer(Modifier.width(8.dp))
                QuickActionItem(
                    label = stringResource(R.string.home_action_new_category),
                    icon = { Icon(painter = painterResource(R.drawable.ic_category), contentDescription = null) },
                    onClick = onAddCategoryClick
                )
            }

            Spacer(Modifier.height(32.dp))

            // Mis cuentas
            SectionHeader(
                title = stringResource(R.string.home_accounts_title),
                showSeeAll = uiState.accounts.isNotEmpty(),
                onSeeAllClick = { onNavigateToTab(BudgmentDestination.Accounts.route) }
            )
            Spacer(Modifier.height(8.dp))
            if (!uiState.isLoading && uiState.accounts.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_no_accounts),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            } else {
                Column {
                    uiState.accounts.forEach { account ->
                        AccountRow(account = account, onClick = { onAccountClick(account.id) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Mis categorías
            SectionHeader(
                title = stringResource(R.string.home_categories_title),
                showSeeAll = uiState.categories.isNotEmpty(),
                onSeeAllClick = onSeeAllCategoriesClick
            )
            Spacer(Modifier.height(8.dp))
            if (!uiState.isLoading && uiState.categories.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_no_categories),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.categories, key = { it.id }) { category ->
                        CategoryChip(category = category, onClick = { onCategoryClick(category.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, showSeeAll: Boolean, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        if (showSeeAll) {
            Text(
                text = stringResource(R.string.home_see_all),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.clickable(onClick = onSeeAllClick)
            )
        }
    }
}

@Composable
private fun QuickActionItem(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun AccountRow(account: AccountEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(account.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(2.dp))
            Text(account.accountType.label(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        Text(
            text = formatCents(account.balanceCache, account.currency),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun CategoryChip(category: CategoryEntity, onClick: () -> Unit) {
    val accentColor = if (category.type == CategoryType.EXPENSE) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, accentColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(category.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenPreview() {
    val sampleAccounts = listOf(
        AccountEntity(
            id = "1",
            userId = "user_1",
            name = "Efectivo",
            accountType = AccountType.CASH,
            currency = "MXN",
            balanceCache = 150000 // 1,500.00
        ),
        AccountEntity(
            id = "2",
            userId = "user_1",
            name = "Tarjeta de Crédito",
            accountType = AccountType.CREDIT,
            currency = "MXN",
            balanceCache = 50000 // -500.00
        )
    )

    val sampleCategories = listOf(
        CategoryEntity(
            id = "1",
            userId = "user_1",
            name = "Comida",
            type = CategoryType.EXPENSE
        ),
        CategoryEntity(
            id = "2",
            userId = "user_1",
            name = "Sueldo",
            type = CategoryType.INCOME
        )
    )

    BudgmentTheme {
        HomeScreenContent(
            currentRoute = BudgmentDestination.Home.route,
            uiState = HomeUiState(
                totalBalance = 100000,
                primaryCurrency = "MXN",
                accounts = sampleAccounts,
                categories = sampleCategories,
                isLoading = false
            ),
            onNavigateToTab = {},
            onAddAccountClick = {},
            onAccountClick = {},
            onAddCategoryClick = {},
            onCategoryClick = {},
            onSeeAllCategoriesClick = {},
            onAddTransactionClick = {}
        )
    }
}
