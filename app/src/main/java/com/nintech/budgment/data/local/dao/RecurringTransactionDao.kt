package com.nintech.budgment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nintech.budgment.data.local.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Insert
    suspend fun insert(recurring: RecurringTransactionEntity)

    @Update
    suspend fun update(recurring: RecurringTransactionEntity)

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: String): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId AND deletedAt IS NULL ORDER BY nextOccurrence")
    fun observeByUser(userId: String): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE active = 1 AND deletedAt IS NULL AND nextOccurrence <= :upToDate")
    suspend fun getDueRecurrences(upToDate: Long): List<RecurringTransactionEntity>

    @Query("UPDATE recurring_transactions SET nextOccurrence = :nextOccurrence, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNextOccurrence(id: String, nextOccurrence: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE recurring_transactions SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long = System.currentTimeMillis())
}