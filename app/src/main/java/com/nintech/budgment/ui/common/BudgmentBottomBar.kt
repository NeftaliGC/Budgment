package com.nintech.budgment.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nintech.budgment.R
import com.nintech.budgment.ui.navigation.BudgmentDestination
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.theme.ChromeBackground
import com.nintech.budgment.ui.theme.ChromeContent

@Composable
fun BudgmentBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.tertiary,
        selectedTextColor = MaterialTheme.colorScheme.tertiary,
        indicatorColor = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.6f),
        unselectedIconColor = ChromeContent,
        unselectedTextColor = ChromeContent
    )

    NavigationBar(
        containerColor = ChromeBackground,
        contentColor = ChromeContent
    ) {
        NavigationBarItem(
            selected = currentRoute == BudgmentDestination.Home.route,
            onClick = { onNavigate(BudgmentDestination.Home.route) },
            icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.bottom_nav_home)) },
            label = { Text(stringResource(R.string.bottom_nav_home)) },
            alwaysShowLabel = true,
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == BudgmentDestination.Accounts.route,
            onClick = { onNavigate(BudgmentDestination.Accounts.route) },
            icon = { Icon(painter = painterResource(id = R.drawable.ic_wallet), contentDescription = stringResource(R.string.bottom_nav_accounts)) },
            label = { Text(stringResource(R.string.bottom_nav_accounts)) },
            alwaysShowLabel = true,
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == BudgmentDestination.Transactions.route,
            onClick = { onNavigate(BudgmentDestination.Transactions.route) },
            icon = { Icon(painter = painterResource(id = R.drawable.ic_transacciones), contentDescription = stringResource(R.string.bottom_nav_transactions)) },
            label = { Text(stringResource(R.string.bottom_nav_transactions)) },
            alwaysShowLabel = true,
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == BudgmentDestination.Budgets.route,
            onClick = { onNavigate(BudgmentDestination.Budgets.route) },
            icon = { Icon(painter = painterResource(id = R.drawable.ic_calendar), contentDescription = stringResource(R.string.bottom_nav_budgets)) },
            label = { Text(stringResource(R.string.bottom_nav_budgets)) },
            alwaysShowLabel = true,
            colors = itemColors
        )
    }
}

@Preview(showBackground = true, name = "BottomBar - Claro")
@Composable
private fun BudgmentBottomBarPreview() {
    BudgmentTheme(darkTheme = false) {
        BudgmentBottomBar(currentRoute = BudgmentDestination.Home.route, onNavigate = {})
    }
}

@Preview(showBackground = true, name = "BottomBar - Oscuro")
@Composable
private fun BudgmentBottomBarDarkPreview() {
    BudgmentTheme(darkTheme = true) {
        BudgmentBottomBar(currentRoute = BudgmentDestination.Home.route, onNavigate = {})
    }
}