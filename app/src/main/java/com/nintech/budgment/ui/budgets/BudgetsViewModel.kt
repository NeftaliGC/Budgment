package com.nintech.budgment.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.BudgetEntity
import com.nintech.budgment.data.model.BudgetScope
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.data.repository.BudgetRepository
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.TransactionRepository
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetWithProgress(val budget: BudgetEntity, val spent: Long, val progress: Float)

data class BudgetsUiState(
    val budgets: List<BudgetWithProgress> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<BudgetsUiState> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(BudgetsUiState(isLoading = true))
            } else {
                combine(
                    budgetRepository.observeBudgets(userId),
                    transactionRepository.observeTransactions(userId),
                    categoryRepository.observeCategories(userId)
                ) { budgets, transactions, categories ->
                    val expenseCategoryIds = categories.filter { it.type == CategoryType.EXPENSE }.map { it.id }.toSet()
                    val linksByBudget = budgetRepository.getCategoryIdsForBudgets(budgets.map { it.id })

                    val withProgress = budgets.map { budget ->
                        val relevantCategoryIds = if (budget.scope == BudgetScope.CATEGORY) {
                            linksByBudget[budget.id]?.toSet() ?: emptySet()
                        } else {
                            expenseCategoryIds
                        }
                        val spent = transactions
                            .filter { it.categoryId in relevantCategoryIds && it.amount < 0 }
                            .filter { budget.startDate == null || it.date >= budget.startDate }
                            .filter { budget.endDate == null || it.date <= budget.endDate }
                            .sumOf { -it.amount }
                        val progress = if (budget.amountLimit > 0) spent.toFloat() / budget.amountLimit else 0f
                        BudgetWithProgress(budget, spent, progress)
                    }
                    BudgetsUiState(budgets = withProgress, isLoading = false)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetsUiState())

    fun deleteBudget(id: String) {
        viewModelScope.launch { budgetRepository.deleteBudget(id) }
    }
}