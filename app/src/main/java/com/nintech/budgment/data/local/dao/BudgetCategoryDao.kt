package com.nintech.budgment.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.nintech.budgment.data.local.entity.BudgetCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetCategoryDao {

    @Insert
    suspend fun insert(budgetCategory: BudgetCategoryEntity)

    @Delete
    suspend fun delete(budgetCategory: BudgetCategoryEntity)

    @Query("SELECT * FROM budget_categories WHERE budgetId = :budgetId")
    fun observeByBudget(budgetId: String): Flow<List<BudgetCategoryEntity>>
}