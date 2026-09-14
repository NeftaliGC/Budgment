package com.nintech.budgment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nintech.budgment.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert
    suspend fun insert(tag: TagEntity)

    @Query("SELECT * FROM tags WHERE userId = :userId ORDER BY name")
    fun observeByUser(userId: String): Flow<List<TagEntity>>
}