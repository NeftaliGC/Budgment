package com.nintech.budgment.data.repository

import androidx.room.withTransaction
import com.nintech.budgment.data.local.BudgmentDatabase
import com.nintech.budgment.data.local.dao.GoalContributionDao
import com.nintech.budgment.data.local.dao.GoalDao
import com.nintech.budgment.data.local.entity.GoalContributionEntity
import com.nintech.budgment.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/** Datos editables de una meta; los mismos al crear y al editar. */
data class GoalInput(
    val name: String,
    val targetAmount: Long,
    val currency: String,
    /** Null = aportes manuales. */
    val accountId: String?,
    val targetDate: Long?,
    val icon: String?,
    val color: String?
)

interface GoalRepository {
    fun observeGoals(userId: String): Flow<List<GoalEntity>>
    fun observeGoal(id: String): Flow<GoalEntity?>
    fun observeContributions(goalId: String): Flow<List<GoalContributionEntity>>
    suspend fun getGoal(id: String): GoalEntity?

    /** [initialAmount] es lo que ya se tenía apartado; solo aplica a metas con aportes manuales. */
    suspend fun createGoal(userId: String, input: GoalInput, initialAmount: Long = 0): String
    suspend fun updateGoal(goal: GoalEntity, input: GoalInput)
    suspend fun deleteGoal(id: String)
    suspend fun restoreGoal(id: String)

    /** [amount] positivo aporta, negativo retira. */
    suspend fun addContribution(goalId: String, amount: Long, note: String?, date: Long = System.currentTimeMillis())
    suspend fun deleteContribution(contribution: GoalContributionEntity)
    suspend fun restoreContribution(contribution: GoalContributionEntity)
}

class GoalRepositoryImpl @Inject constructor(
    private val database: BudgmentDatabase,
    private val goalDao: GoalDao,
    private val contributionDao: GoalContributionDao
) : GoalRepository {

    override fun observeGoals(userId: String): Flow<List<GoalEntity>> = goalDao.observeByUser(userId)

    override fun observeGoal(id: String): Flow<GoalEntity?> = goalDao.observeById(id)

    override fun observeContributions(goalId: String): Flow<List<GoalContributionEntity>> =
        contributionDao.observeByGoal(goalId)

    override suspend fun getGoal(id: String): GoalEntity? = goalDao.getById(id)

    override suspend fun createGoal(userId: String, input: GoalInput, initialAmount: Long): String {
        val id = UUID.randomUUID().toString()
        database.withTransaction {
            goalDao.insert(
                GoalEntity(
                    id = id,
                    userId = userId,
                    accountId = input.accountId,
                    name = input.name,
                    targetAmount = input.targetAmount,
                    currency = input.currency,
                    targetDate = input.targetDate,
                    icon = input.icon,
                    color = input.color
                )
            )
            // Lo ya ahorrado queda como primer aporte para que el historial cuadre con el total.
            if (input.accountId == null && initialAmount > 0) insertContribution(id, initialAmount, null, System.currentTimeMillis())
        }
        return id
    }

    override suspend fun updateGoal(goal: GoalEntity, input: GoalInput) {
        goalDao.update(
            goal.copy(
                accountId = input.accountId,
                name = input.name,
                targetAmount = input.targetAmount,
                currency = input.currency,
                targetDate = input.targetDate,
                icon = input.icon,
                color = input.color,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteGoal(id: String) = goalDao.softDelete(id)

    override suspend fun restoreGoal(id: String) = goalDao.restore(id)

    override suspend fun addContribution(goalId: String, amount: Long, note: String?, date: Long) {
        require(amount != 0L)
        database.withTransaction { insertContribution(goalId, amount, note, date) }
    }

    private suspend fun insertContribution(goalId: String, amount: Long, note: String?, date: Long) {
        contributionDao.insert(
            GoalContributionEntity(id = UUID.randomUUID().toString(), goalId = goalId, amount = amount, note = note, date = date)
        )
        goalDao.addToCurrent(goalId, amount)
    }

    override suspend fun deleteContribution(contribution: GoalContributionEntity) {
        database.withTransaction {
            contributionDao.delete(contribution)
            goalDao.addToCurrent(contribution.goalId, -contribution.amount)
        }
    }

    override suspend fun restoreContribution(contribution: GoalContributionEntity) {
        database.withTransaction {
            contributionDao.insert(contribution)
            goalDao.addToCurrent(contribution.goalId, contribution.amount)
        }
    }
}
