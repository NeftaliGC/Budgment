package com.nintech.budgment.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nintech.budgment.ui.accounts.AccountFormScreen
import com.nintech.budgment.ui.accounts.AccountsListScreen
import com.nintech.budgment.ui.budgets.BudgetFormScreen
import com.nintech.budgment.ui.budgets.BudgetsListScreen
import com.nintech.budgment.ui.categories.CategoriesListScreen
import com.nintech.budgment.ui.categories.CategoryFormScreen
import com.nintech.budgment.ui.home.HomeScreen
import com.nintech.budgment.ui.login.LoginScreen
import com.nintech.budgment.ui.register.RegisterScreen
import com.nintech.budgment.ui.transactions.TransactionFormScreen
import com.nintech.budgment.ui.transactions.TransactionsListScreen

@Composable
fun BudgmentNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = BudgmentDestination.Login.route
) {
    // Navegación entre pestañas del bottom nav: conserva el estado de cada pestaña
    // al cambiar entre ellas, en vez de recrearlas desde cero cada vez.
    val onNavigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(BudgmentDestination.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(BudgmentDestination.Home.route) {
                        popUpTo(BudgmentDestination.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRegisterClick = { navController.navigate(BudgmentDestination.Register.route) }
            )
        }

        composable(BudgmentDestination.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(BudgmentDestination.Home.route) {
                        popUpTo(BudgmentDestination.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBackClick = { navController.popBackStack() },
                onLoginClick = { navController.popBackStack() }
            )
        }

        composable(BudgmentDestination.Home.route) {
            HomeScreen(
                currentRoute = BudgmentDestination.Home.route,
                onNavigateToTab = onNavigateToTab,
                onAddAccountClick = { navController.navigate(BudgmentDestination.AccountForm.createRoute()) },
                onAccountClick = { id -> navController.navigate(BudgmentDestination.AccountForm.createRoute(id)) },
                onAddCategoryClick = { navController.navigate(BudgmentDestination.CategoryForm.createRoute()) },
                onCategoryClick = { id -> navController.navigate(BudgmentDestination.CategoryForm.createRoute(id)) },
                onSeeAllCategoriesClick = { navController.navigate(BudgmentDestination.Categories.route) },
                onAddTransactionClick = { navController.navigate(BudgmentDestination.TransactionForm.createRoute()) },
                onAddBudgetClick = { navController.navigate(BudgmentDestination.BudgetForm.createRoute()) }
            )
        }

        composable(BudgmentDestination.Categories.route) {
            CategoriesListScreen(
                onBackClick = { navController.popBackStack() },
                onAddCategoryClick = { navController.navigate(BudgmentDestination.CategoryForm.createRoute()) },
                onCategoryClick = { id -> navController.navigate(BudgmentDestination.CategoryForm.createRoute(id)) }
            )
        }

        composable(
            route = BudgmentDestination.CategoryForm.route,
            arguments = listOf(
                navArgument(BudgmentDestination.CategoryForm.ARG_CATEGORY_ID) {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            CategoryFormScreen(
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(BudgmentDestination.Accounts.route) {
            AccountsListScreen(
                currentRoute = BudgmentDestination.Accounts.route,
                onNavigateToTab = onNavigateToTab,
                onAddAccountClick = { navController.navigate(BudgmentDestination.AccountForm.createRoute()) },
                onAccountClick = { id -> navController.navigate(BudgmentDestination.AccountForm.createRoute(id)) }
            )
        }

        composable(
            route = BudgmentDestination.AccountForm.route,
            arguments = listOf(
                navArgument(BudgmentDestination.AccountForm.ARG_ACCOUNT_ID) {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            AccountFormScreen(
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(BudgmentDestination.Transactions.route) {
            TransactionsListScreen(
                currentRoute = BudgmentDestination.Transactions.route,
                onNavigateToTab = onNavigateToTab,
                onAddTransactionClick = { navController.navigate(BudgmentDestination.TransactionForm.createRoute()) },
                onTransactionClick = { id -> navController.navigate(BudgmentDestination.TransactionForm.createRoute(id)) }
            )
        }

        composable(BudgmentDestination.Budgets.route) {
            BudgetsListScreen(
                currentRoute = BudgmentDestination.Budgets.route,
                onNavigateToTab = onNavigateToTab,
                onAddBudgetClick = { navController.navigate(BudgmentDestination.BudgetForm.createRoute()) },
                onBudgetClick = { id -> navController.navigate(BudgmentDestination.BudgetForm.createRoute(id)) }
            )
        }

        composable(
            route = BudgmentDestination.BudgetForm.route,
            arguments = listOf(
                navArgument(BudgmentDestination.BudgetForm.ARG_BUDGET_ID) {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            BudgetFormScreen(
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = BudgmentDestination.TransactionForm.route,
            arguments = listOf(
                navArgument(BudgmentDestination.TransactionForm.ARG_TRANSACTION_ID) {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            TransactionFormScreen(
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onCreateAccountClick = { navController.navigate(BudgmentDestination.AccountForm.createRoute()) },
                onCreateCategoryClick = { navController.navigate(BudgmentDestination.CategoryForm.createRoute()) }
            )
        }
    }
}