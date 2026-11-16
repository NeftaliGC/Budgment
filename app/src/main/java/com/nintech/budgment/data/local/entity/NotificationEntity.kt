package com.nintech.budgment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class NotificationType {
    /** Un movimiento recurrente vencido espera que el usuario lo registre. */
    RECURRING_DUE,
    /** Un presupuesto llegó a un umbral de su límite. */
    BUDGET_THRESHOLD
}

/**
 * Aviso guardado en la bandeja de Notificaciones. El texto se fija al crearlo: es lo que pasó en
 * ese momento, aunque luego cambie el recurrente o el presupuesto.
 */
@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index("userId"),
        // Un mismo hecho (p. ej. la ocurrencia del 1 de nov de «Renta») se avisa una sola vez.
        Index(value = ["userId", "dedupKey"], unique = true)
    ]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: NotificationType,
    /** Recurrente o presupuesto al que se refiere. */
    val refId: String?,
    /** Para recurrentes: la ocurrencia anunciada; sirve para saber si sigue pendiente. */
    val occurrence: Long? = null,
    val dedupKey: String,
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val readAt: Long? = null
)
