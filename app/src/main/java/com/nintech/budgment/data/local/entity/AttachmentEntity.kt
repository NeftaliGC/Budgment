package com.nintech.budgment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("transactionId")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
)