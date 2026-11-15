package com.nintech.budgment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nintech.budgment.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE userId = :userId AND deletedAt IS NULL ORDER BY name")
    fun observeAccounts(userId: String): Flow<List<AccountEntity>>

    /** Incluye las cuentas eliminadas: sirve para mostrar el historial de movimientos. */
    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY name")
    fun observeAllAccounts(userId: String): Flow<List<AccountEntity>>

    @Query("UPDATE accounts SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE accounts SET balanceCache = :newBalance, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBalanceCache(id: String, newBalance: Long, updatedAt: Long = System.currentTimeMillis())

}