package com.nintech.budgment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nintech.budgment.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    /** Ignora el aviso si ya existe uno con la misma [NotificationEntity.dedupKey]. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeByUser(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND readAt IS NULL")
    fun observeUnreadCount(userId: String): Flow<Int>

    @Query("UPDATE notifications SET readAt = :readAt WHERE userId = :userId AND readAt IS NULL")
    suspend fun markAllRead(userId: String, readAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM notifications WHERE userId = :userId AND createdAt < :before")
    suspend fun deleteOlderThan(userId: String, before: Long)
}
