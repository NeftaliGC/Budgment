package com.nintech.budgment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nintech.budgment.data.model.AccountType

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)

data class AccountEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val accountType: AccountType,
    val currency: String,
    val icon: String? = null,
    val color: String? = null,
    val creditLimit: Long? = null,      // centavos; solo si accountType = CREDIT
    val statementDay: Int? = null,      // solo crédito
    val paymentDueDay: Int? = null,     // solo crédito
    val balanceCache: Long = 0,         // centavos, derivado
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val deletedAt: Long? = null
)
