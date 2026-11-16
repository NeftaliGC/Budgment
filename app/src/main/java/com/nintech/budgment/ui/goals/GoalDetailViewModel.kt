package com.nintech.budgment.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.GoalContributionEntity
import com.nintech.budgment.data.repository.AccountRepository
import com.nintech.budgment.data.repository.GoalRepository
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.ui.common.dayTicks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalDetailUiState(
    val item: GoalItem? = null,
    val contributions: List<GoalContributionEntity> = emptyList(),
    /** La meta ya no existe (se eliminó): la pantalla se cierra. */
    val isGone: Boolean = false,
    val isLoading: Boolean = true
)

/** Resultado de validar un aporte o retiro. */
enum class ContributionError { INVALID_AMOUNT, EXCEEDS_SAVED }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val goalId: String = checkNotNull(savedStateHandle.get<String>(ARG_GOAL_ID))
    private val deletedContributions = mutableMapOf<String, GoalContributionEntity>()

    val uiState: StateFlow<GoalDetailUiState> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) return@flatMapLatest flowOf(GoalDetailUiState(isLoading = false))
            combine(
                goalRepository.observeGoal(goalId),
                goalRepository.observeContributions(goalId),
                accountRepository.observeAccountsIncludingDeleted(userId),
                dayTicks()
            ) { goal, contributions, accounts, now ->
                if (goal == null || goal.deletedAt != null) {
                    GoalDetailUiState(isGone = true, isLoading = false)
                } else {
                    GoalDetailUiState(
                        item = buildGoalItems(listOf(goal), accounts, now).single(),
                        contributions = contributions,
                        isLoading = false
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalDetailUiState())

    /**
     * Registra un aporte ([isWithdrawal] = false) o un retiro. No se puede retirar más de lo
     * ahorrado. Devuelve el error, o null si se guardó.
     */
    fun addContribution(amount: Long?, note: String, isWithdrawal: Boolean): ContributionError? {
        if (amount == null || amount <= 0) return ContributionError.INVALID_AMOUNT
        val saved = uiState.value.item?.progress?.saved ?: 0
        if (isWithdrawal && amount > saved) return ContributionError.EXCEEDS_SAVED
        viewModelScope.launch {
            goalRepository.addContribution(goalId, if (isWithdrawal) -amount else amount, note.trim().ifBlank { null })
        }
        return null
    }

    fun deleteContribution(id: String) {
        val contribution = uiState.value.contributions.firstOrNull { it.id == id } ?: return
        deletedContributions[id] = contribution
        viewModelScope.launch { goalRepository.deleteContribution(contribution) }
    }

    fun restoreContribution(id: String) {
        val contribution = deletedContributions.remove(id) ?: return
        viewModelScope.launch { goalRepository.restoreContribution(contribution) }
    }

    fun deleteGoal() {
        viewModelScope.launch { goalRepository.deleteGoal(goalId) }
    }

    companion object {
        const val ARG_GOAL_ID = "goalId"
    }
}
