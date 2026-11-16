package com.nintech.budgment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Aporte (positivo) o retiro (negativo) de una meta con aportes manuales, en la moneda de la
 * meta. No mueve dinero entre cuentas: es el registro de lo que el usuario apartó.
 */
@Entity(
    tableName = "goal_contributions",
    foreignKeys = [
        ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("goalId")]
)
data class GoalContributionEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    /** Centavos con signo: positivo = aporte, negativo = retiro. */
    val amount: Long,
    val note: String? = null,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis()
)
