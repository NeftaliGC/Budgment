package com.nintech.budgment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nintech.budgment.data.model.PeriodUnit

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index("userId"),
        Index("accountId"),
        Index("nextOccurrence")
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val accountId: String,
    val categoryId: String,
    val baseAmount: Long,
    val currency: String? = null,
    val periodInterval: Int = 1,
    val periodUnit: PeriodUnit,
    val anchorDate: Long,
    val nextOccurrence: Long,
    val endDate: Long? = null,
    val active: Boolean = true,
    val notifyBeforeDays: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val deletedAt: Long? = null
)