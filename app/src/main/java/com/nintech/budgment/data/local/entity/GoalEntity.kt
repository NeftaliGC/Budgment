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
    /** Meta vinculada a una cuenta: su avance es el saldo de la cuenta. Null = aportes manuales. */
    val accountId: String? = null,
    val name: String,
    /** Centavos, en [currency]. */
    val targetAmount: Long,
    /** Suma de los aportes manuales (derivado); no se usa si la meta está vinculada a una cuenta. */
    val currentAmount: Long = 0,
    /** Moneda de la meta; en las vinculadas es la de la cuenta. */
    val currency: String? = null,
    val targetDate: Long? = null,
    val icon: String? = null,
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val deletedAt: Long? = null
)