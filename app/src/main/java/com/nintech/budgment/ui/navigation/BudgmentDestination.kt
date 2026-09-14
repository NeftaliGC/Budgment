package com.nintech.budgment.ui.navigation

sealed class BudgmentDestination(val route: String) {
    object Login : BudgmentDestination("login")
    object Register : BudgmentDestination("register")
    object Home : BudgmentDestination("home") // placeholder — hasta que exista el MVP real (cuentas, etc.)
}
