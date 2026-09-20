package com.nintech.budgment.ui.navigation

sealed class BudgmentDestination(val route: String) {
    object Login : BudgmentDestination("login")
    object Register : BudgmentDestination("register")
    object Home : BudgmentDestination("home")
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
    object TransactionForm : BudgmentDestination("transaction_form?transactionId={transactionId}") {
        const val ARG_TRANSACTION_ID = "transactionId"
        fun createRoute(transactionId: String? = null) = "transaction_form?transactionId=${transactionId ?: ""}"
    }
}