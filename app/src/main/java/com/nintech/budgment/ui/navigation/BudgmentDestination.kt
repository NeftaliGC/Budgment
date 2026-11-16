package com.nintech.budgment.ui.navigation

import com.nintech.budgment.ui.lock.PinSetupMode
import com.nintech.budgment.ui.transactions.TransactionFormKind
import com.nintech.budgment.ui.transactions.TransactionFormViewModel

sealed class BudgmentDestination(val route: String) {
    object Login : BudgmentDestination("login")
    object Register : BudgmentDestination("register")
    object Home : BudgmentDestination("home")
    object Settings : BudgmentDestination("settings")
    object Statistics : BudgmentDestination("statistics")
    object Notifications : BudgmentDestination("notifications")
    object PinSetup : BudgmentDestination("pin_setup/{mode}") {
        fun createRoute(mode: PinSetupMode) = "pin_setup/${mode.name.lowercase()}"
    }
    object Accounts : BudgmentDestination("accounts")
    object AccountForm : BudgmentDestination("account_form?accountId={accountId}") {
        const val ARG_ACCOUNT_ID = "accountId"
        fun createRoute(accountId: String? = null) = "account_form?accountId=${accountId ?: ""}"
    }

    object Categories : BudgmentDestination("categories")
    object CategoryForm : BudgmentDestination("category_form?categoryId={categoryId}") {
        const val ARG_CATEGORY_ID = "categoryId"
        fun createRoute(categoryId: String? = null) = "category_form?categoryId=${categoryId ?: ""}"
    }

    object Transactions : BudgmentDestination("transactions")
    /** Formulario de movimiento; también registra ocurrencias y edita recurrentes (ver [TransactionFormKind]). */
    object TransactionForm : BudgmentDestination(
        "transaction_form?transactionId={transactionId}&recurringId={recurringId}&mode={mode}"
    ) {
        const val ARG_TRANSACTION_ID = TransactionFormViewModel.ARG_TRANSACTION_ID
        const val ARG_RECURRING_ID = TransactionFormViewModel.ARG_RECURRING_ID
        const val ARG_MODE = TransactionFormViewModel.ARG_MODE
        fun createRoute(transactionId: String? = null) = "transaction_form?transactionId=${transactionId ?: ""}"

        /** Revisar y registrar la ocurrencia vencida de un recurrente. */
        fun createOccurrenceRoute(recurringId: String) =
            "transaction_form?recurringId=$recurringId&mode=${TransactionFormViewModel.MODE_OCCURRENCE}"

        /** Crear (sin id) o editar un recurrente. */
        fun createRecurringRoute(recurringId: String? = null) =
            "transaction_form?recurringId=${recurringId ?: ""}&mode=${TransactionFormViewModel.MODE_TEMPLATE}"
    }

    object Recurring : BudgmentDestination("recurring")

    object TransferForm : BudgmentDestination("transfer_form?transferId={transferId}&toAccountId={toAccountId}") {
        const val ARG_TRANSFER_ID = "transferId"
        const val ARG_TO_ACCOUNT_ID = "toAccountId"
        fun createRoute(transferId: String? = null) = "transfer_form?transferId=${transferId ?: ""}"

        /** Nueva transferencia con la cuenta destino ya elegida (p. ej. la de una meta). */
        fun createToAccountRoute(toAccountId: String) = "transfer_form?toAccountId=$toAccountId"
    }

    object GoalForm : BudgmentDestination("goal_form?goalId={goalId}") {
        fun createRoute(goalId: String? = null) = "goal_form?goalId=${goalId ?: ""}"
    }

    object GoalDetail : BudgmentDestination("goal_detail/{goalId}") {
        fun createRoute(goalId: String) = "goal_detail/$goalId"
    }

    object Budgets : BudgmentDestination("budgets")
    object BudgetForm : BudgmentDestination("budget_form?budgetId={budgetId}") {
        const val ARG_BUDGET_ID = "budgetId"
        fun createRoute(budgetId: String? = null) = "budget_form?budgetId=${budgetId ?: ""}"
    }
}