package com.nintech.budgment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nintech.budgment.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Insert
    suspend fun insert(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT * FROM attachments WHERE transactionId = :transactionId ORDER BY createdAt")
    suspend fun getByTransaction(transactionId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments")
    suspend fun getAll(): List<AttachmentEntity>

    /** Movimientos que tienen al menos una foto. */
    @Query("SELECT DISTINCT transactionId FROM attachments")
    fun observeTransactionIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM attachments")
    fun observeCount(): Flow<Int>

    /** Fotos tomadas antes de [cutoff]. */
    @Query("SELECT * FROM attachments WHERE createdAt < :cutoff")
    suspend fun getOlderThan(cutoff: Long): List<AttachmentEntity>

    /** Fotos de movimientos eliminados antes de [cutoff]: ya no se pueden deshacer. */
    @Query(
        "SELECT a.* FROM attachments a JOIN transactions t ON t.id = a.transactionId " +
            "WHERE t.deletedAt IS NOT NULL AND t.deletedAt < :cutoff"
    )
    suspend fun getOfTransactionsDeletedBefore(cutoff: Long): List<AttachmentEntity>
}
