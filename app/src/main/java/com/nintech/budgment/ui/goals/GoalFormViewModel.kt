package com.nintech.budgment.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.GoalEntity
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.GoalInput
import com.nintech.budgment.data.repository.GoalRepository
import com.nintech.budgment.data.repository.UserRepository
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.ui.common.centsToInputText
import com.nintech.budgment.ui.common.parseAmountToCents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class GoalSource { ACCOUNT, MANUAL }

data class GoalFormState(
    val isEditing: Boolean = false,
    val name: String = "",
    val targetText: String = "",
    val source: GoalSource = GoalSource.MANUAL,
    val accountId: String? = null,
    /** Moneda de las metas con aportes manuales. */
    val manualCurrency: String = SupportedCurrency.MXN.code,
    /** Lo que ya se tenía apartado; solo al crear una meta manual. */
    val initialText: String = "",
    val targetDate: Long? = null,
    val icon: String? = DEFAULT_GOAL_ICON,
    val color: String? = null,
    val accounts: List<AccountEntity> = emptyList(),
    /** Suma de los aportes de la meta que se edita (para explicar el cambio de modo). */
    val savedManual: Long = 0,
    val nameError: Boolean = false,
    val targetError: Boolean = false,
    val accountError: Boolean = false,
    val initialError: Boolean = false,
    val dateError: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
) {
    val selectedAccount: AccountEntity? get() = accounts.firstOrNull { it.id == accountId }

    /** Las vinculadas usan la moneda de la cuenta. */
    val currency: String get() = if (source == GoalSource.ACCOUNT) selectedAccount?.currency ?: manualCurrency else manualCurrency
}

const val DEFAULT_GOAL_ICON = "savings"

@HiltViewModel
class GoalFormViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val goalId: String? = savedStateHandle.get<String>(ARG_GOAL_ID)?.ifBlank { null }
    private var original: GoalEntity? = null

    private val _state = MutableStateFlow(GoalFormState(isEditing = goalId != null))
    val state: StateFlow<GoalFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = sessionManager.currentUserId.filterNotNull().first()
            val defaultCurrency = userRepository.getUser(userId)?.defaultCurrency ?: SupportedCurrency.MXN.code
            val goal = goalId?.let { goalRepository.getGoal(it) }
            original = goal
            _state.update { s ->
                if (goal == null) {
                    s.copy(manualCurrency = defaultCurrency)
                } else {
                    s.copy(
                        name = goal.name,
                        targetText = centsToInputText(goal.targetAmount),
                        source = if (goal.accountId != null) GoalSource.ACCOUNT else GoalSource.MANUAL,
                        accountId = goal.accountId,
                        manualCurrency = goal.currency ?: defaultCurrency,
                        targetDate = goal.targetDate,
                        icon = goal.icon,
                        color = goal.color,
                        savedManual = goal.currentAmount
                    )
                }
            }
            accountRepository.observeAccounts(userId).collect { accounts ->
                _state.update { s ->
                    // Una cuenta eliminada no se ofrece, pero si era la vinculada se conserva elegida.
                    val keep = s.accountId?.takeIf { id -> accounts.any { it.id == id } || id == original?.accountId }
                    s.copy(accounts = accounts, accountId = keep, isLoading = false)
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = false) }
    fun onTargetChange(value: String) = _state.update { it.copy(targetText = value, targetError = false) }
    fun onInitialChange(value: String) = _state.update { it.copy(initialText = value, initialError = false) }
    fun onSourceChange(source: GoalSource) = _state.update {
        it.copy(source = source, accountId = it.accountId ?: it.accounts.firstOrNull()?.id, accountError = false)
    }
    fun onAccountChange(id: String) = _state.update { it.copy(accountId = id, accountError = false) }
    fun onCurrencyChange(code: String) = _state.update { it.copy(manualCurrency = code) }
    fun onTargetDateChange(date: Long?) = _state.update { it.copy(targetDate = date, dateError = false) }
    fun onIconChange(icon: String) = _state.update { it.copy(icon = icon) }
    fun onColorChange(color: String?) = _state.update { it.copy(color = color) }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (s.isSaving) return
        val target = parseAmountToCents(s.targetText)?.takeIf { it > 0 }
        val initial = if (s.initialText.isBlank()) 0L else parseAmountToCents(s.initialText)
        val linked = s.source == GoalSource.ACCOUNT
        val today = startOfToday()
        val errors = s.copy(
            nameError = s.name.isBlank(),
            targetError = target == null,
            accountError = linked && s.accountId == null,
            initialError = !linked && !s.isEditing && (initial == null || initial < 0),
            // La fecha objetivo tiene que quedar por delante; la de una meta ya guardada se respeta.
            dateError = s.targetDate != null && s.targetDate < today && s.targetDate != original?.targetDate
        )
        if (errors.nameError || errors.targetError || errors.accountError || errors.initialError || errors.dateError) {
            _state.value = errors
            return
        }
        _state.update { it.copy(isSaving = true) }
        val input = GoalInput(
            name = s.name.trim(),
            targetAmount = target!!,
            currency = s.currency,
            accountId = if (linked) s.accountId else null,
            targetDate = s.targetDate,
            icon = s.icon,
            color = s.color
        )
        viewModelScope.launch {
            val existing = original
            if (existing != null) {
                goalRepository.updateGoal(existing, input)
            } else {
                val userId = sessionManager.currentUserId.filterNotNull().first()
                goalRepository.createGoal(userId, input, initialAmount = initial ?: 0)
            }
            onSaved()
        }
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        const val ARG_GOAL_ID = "goalId"
    }
}
