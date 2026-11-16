package com.nintech.budgment.ui.budgets

import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.data.repository.UserRepository
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
import com.nintech.budgment.data.model.BudgetPeriod
import com.nintech.budgment.data.model.RecurrenceFrequency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.nintech.budgment.ui.common.parseAmountToCents
import com.nintech.budgment.ui.common.centsToInputText

data class BudgetFormState(
    val name: String = "",
    val amountLimitText: String = "",
    val scope: BudgetScope = BudgetScope.GLOBAL,
    /** Cada cuánto se renueva. */
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    /** Al editar: ancla y frecuencia guardadas, para mostrar el periodo real mientras no cambie. */
    val savedAnchor: Long? = null,
    val savedFrequency: RecurrenceFrequency? = null,
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    /** Los límites siempre están en la moneda por defecto del usuario. */
    val currency: String = SupportedCurrency.MXN.code,
    /** Otro presupuesto global del usuario; si existe, este no puede ser global también. */
    val existingGlobal: BudgetEntity? = null,
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val nameError: Boolean = false,
    val amountError: Boolean = false,
    val categoriesError: Boolean = false
) {
    val globalTaken: Boolean get() = scope == BudgetScope.GLOBAL && existingGlobal != null

    /** Periodo vigente con la frecuencia elegida; si cambia al editar, empieza en el periodo actual. */
    fun periodPreview(now: Long = System.currentTimeMillis()): BudgetPeriod {
        val anchor = savedAnchor?.takeIf { frequency == savedFrequency }
            ?: BudgetPeriod.alignedStart(now, frequency.unit)
        return BudgetPeriod.current(anchor, frequency.unit, frequency.interval, now)
    }
}

@HiltViewModel
class BudgetFormViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
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
            // Solo puede haber un global: es el techo del mes y el resumen depende de él.
            val existingGlobal = budgetRepository.observeBudgets(userId).first()
                .filter { it.scope == BudgetScope.GLOBAL && it.id != budgetId }
                .minByOrNull { it.createdAt }
            _state.value = _state.value.copy(
                expenseCategories = categories,
                currency = userRepository.getUser(userId)?.defaultCurrency ?: SupportedCurrency.MXN.code,
                existingGlobal = existingGlobal,
                // Uno nuevo con un global ya creado solo tiene sentido por categoría.
                scope = if (budgetId == null && existingGlobal != null) BudgetScope.CATEGORY else _state.value.scope,
                isLoading = false
            )

            budgetId?.let { id ->
                budgetRepository.getBudget(id)?.let { budget ->
                    existingBudget = budget
                    val selected = budgetRepository.getCategoryIds(id).toSet()
                    _state.value = _state.value.copy(
                        name = budget.name,
                        amountLimitText = centsToInputText(budget.amountLimit),
                        scope = budget.scope,
                        frequency = RecurrenceFrequency.of(budget.periodUnit, budget.periodInterval)
                            ?: RecurrenceFrequency.MONTHLY,
                        savedAnchor = budget.startDate,
                        savedFrequency = RecurrenceFrequency.of(budget.periodUnit, budget.periodInterval),
                        selectedCategoryIds = selected
                    )
                }
            }
        }
    }

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v, nameError = false) }
    fun onAmountChange(v: String) { _state.value = _state.value.copy(amountLimitText = v, amountError = false) }
    fun onFrequencyChange(v: RecurrenceFrequency) { _state.value = _state.value.copy(frequency = v) }
    fun onScopeChange(v: BudgetScope) { _state.value = _state.value.copy(scope = v, categoriesError = false) }
    fun onCategoryToggle(categoryId: String) {
        val current = _state.value.selectedCategoryIds
        _state.value = _state.value.copy(selectedCategoryIds = if (categoryId in current) current - categoryId else current + categoryId, categoriesError = false)
    }

    fun save() {
        val s = _state.value
        val cents = parseAmountToCents(s.amountLimitText)
        val name = s.name.trim()
        val nameError = name.isBlank()
        val amountError = cents == null || cents <= 0
        val categoriesError = s.scope == BudgetScope.CATEGORY && s.selectedCategoryIds.isEmpty()
        if (s.globalTaken) return
        if (nameError || amountError || categoriesError || cents == null) {
            _state.value = s.copy(nameError = nameError, amountError = amountError, categoriesError = categoriesError)
            return
        }
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            existingBudget?.let { budget ->
                // Si cambia la periodicidad, el ancla se reinicia al periodo actual de la nueva
                // unidad; si no, se conserva para no mover el historial de periodos.
                val anchor = budget.startDate?.takeIf {
                    budget.periodUnit == s.frequency.unit && budget.periodInterval == s.frequency.interval
                } ?: BudgetPeriod.alignedStart(now, s.frequency.unit)
                budgetRepository.updateBudget(budget, name, cents, s.scope, s.selectedCategoryIds.toList(), s.frequency, anchor)
            } ?: run {
                val userId = sessionManager.currentUserId.filterNotNull().first()
                val anchor = BudgetPeriod.alignedStart(now, s.frequency.unit)
                budgetRepository.createBudget(userId, name, cents, s.scope, s.selectedCategoryIds.toList(), s.frequency, anchor)
            }
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}