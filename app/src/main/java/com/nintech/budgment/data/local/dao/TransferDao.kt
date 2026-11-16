package com.nintech.budgment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nintech.budgment.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    @Insert
    suspend fun insert(transfer: TransferEntity)

    @Query("SELECT * FROM transfers WHERE id = :id")
    suspend fun getById(id: String): TransferEntity?

    /** La transferencia a la que pertenece un movimiento, sea su salida o su entrada. */
    @Query("SELECT * FROM transfers WHERE fromTransactionId = :transactionId OR toTransactionId = :transactionId")
    suspend fun getByTransactionId(transactionId: String): TransferEntity?

    @Query("UPDATE transfers SET fromAccountId = :fromAccountId, toAccountId = :toAccountId WHERE id = :id")
    suspend fun updateAccounts(id: String, fromAccountId: String, toAccountId: String)

    @Query("SELECT * FROM transfers WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeByUser(userId: String): Flow<List<TransferEntity>>
}
