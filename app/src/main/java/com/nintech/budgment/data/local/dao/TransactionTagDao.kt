package com.nintech.budgment.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.nintech.budgment.data.local.entity.TransactionTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTagDao {

    @Insert
    suspend fun insert(transactionTag: TransactionTagEntity)

    @Delete
    suspend fun delete(transactionTag: TransactionTagEntity)

    @Query("SELECT * FROM transaction_tags WHERE transactionId = :transactionId")
    fun observeByTransaction(transactionId: String): Flow<List<TransactionTagEntity>>
}