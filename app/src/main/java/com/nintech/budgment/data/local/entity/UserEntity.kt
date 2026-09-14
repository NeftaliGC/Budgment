package com.nintech.budgment.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)

data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val username: String,
    val email: String? = null,
    val passwordHash: String,
    val defaultCurrency: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null
)
