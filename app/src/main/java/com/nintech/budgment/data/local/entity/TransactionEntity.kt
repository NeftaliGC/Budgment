package com.nintech.budgment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = RecurringTransactionEntity::class, parentColumns = ["id"], childColumns = ["recurringId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [
        Index(value = ["userId", "date"]),
        Index("accountId"),
        Index("categoryId"),
        Index("recurringId")
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val accountId: String,
    val categoryId: String,
    val recurringId: String? = null,
    val amount: Long,               // centavos, con signo
    val currency: String? = null,   // null = usa la currency de la cuenta
    val exchangeRateMicros: Long? = null,
    val description: String? = null,
    val date: Long,                 // fecha efectiva de la transacción
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val deletedAt: Long? = null
)