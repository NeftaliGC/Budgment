package com.nintech.budgment.ui.budgets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.BudgetEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.model.BudgetScope
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.data.repository.BudgetRepository
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.ui.common.currentMonthRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetFormState(
    val name: String = "",
    val amountLimitText: String = "",
    val scope: BudgetScope = BudgetScope.GLOBAL,
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: Boolean = false
)

@HiltViewModel
class BudgetFormViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val budgetId: String? = savedStateHandle.get<String>("budgetId")?.ifBlank { null }
    private var existingBudget: BudgetEntity? = null

    private val _state = MutableStateFlow(BudgetFormState(isEditing = budgetId != null))
    val state: StateFlow<BudgetFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = sessionManager.currentUserId.filterNotNull().first()
            val categories = categoryRepository.observeCategories(userId).first().filter { it.type == CategoryType.EXPENSE }
            _state.value = _state.value.copy(expenseCategories = categories, isLoading = false)

            budgetId?.let { id ->
                budgetRepository.getBudget(id)?.let { budget ->
                    existingBudget = budget
                    val selected = budgetRepository.getCategoryIds(id).toSet()
                    _state.value = _state.value.copy(
                        name = budget.name,
                        amountLimitText = (budget.amountLimit / 100.0).toString(),
                        scope = budget.scope,
                        selectedCategoryIds = selected
                    )
                }
            }
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, errorMessage = false) }
    fun onAmountChange(v: String) { _state.value = _state.value.copy(amountLimitText = v, errorMessage = false) }
    fun onScopeChange(v: BudgetScope) { _state.value = _state.value.copy(scope = v) }
    fun onCategoryToggle(categoryId: String) {
        val current = _state.value.selectedCategoryIds
        _state.value = _state.value.copy(selectedCategoryIds = if (categoryId in current) current - categoryId else current + categoryId)
    }

    fun save() {
        val s = _state.value
        val limit = s.amountLimitText.toDoubleOrNull()
        if (s.name.isBlank() || limit == null || limit <= 0 || (s.scope == BudgetScope.CATEGORY && s.selectedCategoryIds.isEmpty())) {
            _state.value = s.copy(errorMessage = true)
            return
        }
        val cents = (limit * 100).toLong()
        val (start, end) = currentMonthRange()

        viewModelScope.launch {
            existingBudget?.let { budget ->
                budgetRepository.updateBudget(budget, s.name, cents, s.scope, s.selectedCategoryIds.toList(), start, end)
            } ?: run {
                val userId = sessionManager.currentUserId.filterNotNull().first()
                budgetRepository.createBudget(userId, s.name, cents, s.scope, s.selectedCategoryIds.toList(), start, end)
            }
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}