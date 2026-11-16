package com.nintech.budgment.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.BudgetEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.model.BudgetPeriod
import com.nintech.budgment.data.model.currentPeriod
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.data.repository.BudgetRepository
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetWithProgress(
    val budget: BudgetEntity,
    val spent: Long,
    val progress: Float,
    val missingRate: Boolean,
    /** Presupuesto por categoría cuyas categorías ya fueron eliminadas todas. */
    val allCategoriesDeleted: Boolean = false,
    /** Límite del presupuesto ya convertido a la moneda de visualización. */
    val limit: Long = budget.amountLimit,
    /** Periodo vigente: lo gastado solo cuenta dentro de él. */
    val period: BudgetPeriod = budget.currentPeriod()
)

data class BudgetsUiState(
    val global: List<BudgetWithProgress> = emptyList(),
    val category: List<BudgetWithProgress> = emptyList(),
    val budgetCategories: Map<String, List<CategoryEntity>> = emptyMap(),
    /** Límite del resumen: el del presupuesto global si hay, o la suma de los de categoría. */
    val totalAllocated: Long = 0,
    val totalSpent: Long = 0,
    /** Presupuesto global que da las cifras del resumen; null si se suman los de categoría. */
    val summaryBudgetId: String? = null,
    val categoryLimitsTotal: Long = 0,
    val primaryCurrency: String = SupportedCurrency.MXN.code,
    val missingRateCount: Int = 0,
    /** Periodo al que corresponde el resumen; null si no hay presupuestos. */
    val summaryPeriod: BudgetPeriod? = null,
    /** Hay presupuestos de otra periodicidad que no entran en el resumen. */
    val hasOtherPeriods: Boolean = false,
    val isLoading: Boolean = true
) {
    val hasGlobal: Boolean get() = summaryBudgetId != null

    /** Los topes por categoría suman más que el global: no es un error, pero suele ser un descuido. */
    val categoryLimitsExceedGlobal: Boolean get() = hasGlobal && categoryLimitsTotal > totalAllocated
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetOverviewSource: BudgetOverviewSource,
    private val budgetRepository: BudgetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<BudgetsUiState> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) flowOf(BudgetsUiState(isLoading = true)) else budgetOverviewSource.observe(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetsUiState())

    fun deleteBudget(id: String) {
        viewModelScope.launch { budgetRepository.deleteBudget(id) }
    }

    fun restoreBudget(id: String) {
        viewModelScope.launch { budgetRepository.restoreBudget(id) }
    }

    fun setDisplayCurrency(code: String) {
        viewModelScope.launch { sessionManager.setDisplayCurrency(code) }
    }
}
