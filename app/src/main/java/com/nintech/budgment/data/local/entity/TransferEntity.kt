package com.nintech.budgment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transfers",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["fromAccountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["toAccountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["fromTransactionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["toTransactionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index("userId"),
        Index("fromAccountId"),
        Index("toAccountId"),
        Index(value = ["fromTransactionId"], unique = true),
        Index(value = ["toTransactionId"], unique = true)
    ]
)
data class TransferEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val fromAccountId: String,
    val toAccountId: String,
    val fromTransactionId: String,
    val toTransactionId: String,
    val createdAt: Long = System.currentTimeMillis()
)