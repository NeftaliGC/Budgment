package com.nintech.budgment.data.repository

import com.nintech.budgment.data.local.dao.BudgetCategoryDao
import com.nintech.budgment.data.local.dao.BudgetDao
import com.nintech.budgment.data.local.entity.BudgetCategoryEntity
import com.nintech.budgment.data.local.entity.BudgetEntity
import com.nintech.budgment.data.model.BudgetScope
import com.nintech.budgment.data.model.PeriodUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

interface BudgetRepository {
    fun observeBudgets(userId: String): Flow<List<BudgetEntity>>
    suspend fun getBudget(id: String): BudgetEntity?
    suspend fun getCategoryIds(budgetId: String): List<String>
    suspend fun getCategoryIdsForBudgets(budgetIds: List<String>): Map<String, List<String>>
    /** [anchor] es el inicio del primer periodo; desde ahí se renueva cada [periodUnit]. */
    suspend fun createBudget(userId: String, name: String, amountLimit: Long, scope: BudgetScope, categoryIds: List<String>, periodUnit: PeriodUnit, anchor: Long)
    suspend fun updateBudget(budget: BudgetEntity, name: String, amountLimit: Long, scope: BudgetScope, categoryIds: List<String>, periodUnit: PeriodUnit, anchor: Long)
    suspend fun deleteBudget(id: String)
    suspend fun restoreBudget(id: String)
}

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val budgetCategoryDao: BudgetCategoryDao
) : BudgetRepository {

    override fun observeBudgets(userId: String): Flow<List<BudgetEntity>> = budgetDao.observeByUser(userId)

    override suspend fun getBudget(id: String): BudgetEntity? = budgetDao.getById(id)

    override suspend fun getCategoryIds(budgetId: String): List<String> =
        budgetCategoryDao.observeByBudget(budgetId).first().map { it.categoryId }

    override suspend fun getCategoryIdsForBudgets(budgetIds: List<String>): Map<String, List<String>> =
        budgetCategoryDao.getForBudgets(budgetIds).groupBy({ it.budgetId }, { it.categoryId })

    override suspend fun createBudget(
        userId: String, name: String, amountLimit: Long, scope: BudgetScope, categoryIds: List<String>, periodUnit: PeriodUnit, anchor: Long
    ) {
        val budget = BudgetEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = name,
            amountLimit = amountLimit,
            scope = scope,
            periodInterval = 1,
            periodUnit = periodUnit,
            startDate = anchor,
            // Sin fin: se renueva cada periodo.
            endDate = null
        )
        budgetDao.insert(budget)
        if (scope == BudgetScope.CATEGORY) {
            categoryIds.forEach { catId -> budgetCategoryDao.insert(BudgetCategoryEntity(budgetId = budget.id, categoryId = catId)) }
        }
    }

    override suspend fun updateBudget(
        budget: BudgetEntity, name: String, amountLimit: Long, scope: BudgetScope, categoryIds: List<String>, periodUnit: PeriodUnit, anchor: Long
    ) {
        budgetDao.update(
            budget.copy(
                name = name, amountLimit = amountLimit, scope = scope,
                periodUnit = periodUnit, startDate = anchor, endDate = null,
                updatedAt = System.currentTimeMillis()
            )
        )
        budgetCategoryDao.observeByBudget(budget.id).first().forEach { budgetCategoryDao.delete(it) }
        if (scope == BudgetScope.CATEGORY) {
            categoryIds.forEach { catId -> budgetCategoryDao.insert(BudgetCategoryEntity(budgetId = budget.id, categoryId = catId)) }
        }
    }

    override suspend fun deleteBudget(id: String) {
        budgetDao.softDelete(id)
    }

    override suspend fun restoreBudget(id: String) {
        budgetDao.restore(id)
    }
}