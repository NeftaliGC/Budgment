package com.nintech.budgment.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.nintech.budgment.data.local.entity.GoalContributionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalContributionDao {

    @Insert
    suspend fun insert(contribution: GoalContributionEntity)

    @Delete
    suspend fun delete(contribution: GoalContributionEntity)

    @Query("SELECT * FROM goal_contributions WHERE id = :id")
    suspend fun getById(id: String): GoalContributionEntity?

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY date DESC, createdAt DESC")
    fun observeByGoal(goalId: String): Flow<List<GoalContributionEntity>>
}
