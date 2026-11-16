package com.nintech.budgment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nintech.budgment.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("DELETE FROM transfers WHERE userId = :userId")
    suspend fun deleteTransfersOf(userId: String)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteTransactionsOf(userId: String)

    @Query("DELETE FROM recurring_transactions WHERE userId = :userId")
    suspend fun deleteRecurringOf(userId: String)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserRow(userId: String)

    /**
     * Borra al usuario con todo lo suyo, sin tocar a los demás usuarios.
     *
     * El orden importa: transferencias, movimientos y recurrentes impiden borrar las cuentas y
     * categorías que usan, así que se quitan primero. Lo demás (cuentas, categorías, presupuestos,
     * metas, avisos) se borra en cascada con el usuario.
     */
    @Transaction
    suspend fun deleteWithData(userId: String) {
        deleteTransfersOf(userId)
        deleteTransactionsOf(userId)
        deleteRecurringOf(userId)
        deleteUserRow(userId)
    }
}