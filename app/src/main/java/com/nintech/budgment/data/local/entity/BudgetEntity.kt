package com.nintech.budgment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nintech.budgment.data.model.BudgetScope
import com.nintech.budgment.data.model.PeriodUnit

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("userId")]
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val amountLimit: Long,
    val scope: BudgetScope,
    val periodInterval: Int = 1,
    val periodUnit: PeriodUnit,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val rollover: Boolean = false,
    val notifyThresholdPercent: Int? = null,
    val autoCreateRecurring: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val deletedAt: Long? = null
)