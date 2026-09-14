package com.nintech.budgment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("userId"), Index("accountId")]
)
data class GoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val accountId: String? = null,
    val name: String,
    val targetAmount: Long,
    val currentAmount: Long = 0,
    val targetDate: Long? = null,
    val icon: String? = null,
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val deletedAt: Long? = null
)