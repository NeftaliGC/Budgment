package com.nintech.budgment.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nintech.budgment.R
import com.nintech.budgment.ui.login.LoginScreen
import com.nintech.budgment.ui.register.RegisterScreen

@Composable
fun BudgmentNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = BudgmentDestination.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(BudgmentDestination.Login.route) {
            LoginScreen(
                onLoginClick = { _, _ ->
                    navController.navigate(BudgmentDestination.Home.route) {
                        popUpTo(BudgmentDestination.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRegisterClick = {
                    navController.navigate(BudgmentDestination.Register.route)
                }
            )
        }

        composable(BudgmentDestination.Register.route) {
            RegisterScreen(
                onRegisterClick = { _, _, _, _ ->
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
            // Placeholder temporal — aquí conectamos el MVP real más adelante
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.home_placeholder_title),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
