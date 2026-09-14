package com.nintech.budgment.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exchange_rates",
    indices = [Index(value = ["fromCurrency", "toCurrency", "date"], unique = true)]
)
data class ExchangeRateEntity(
    @PrimaryKey val id: String,
    val fromCurrency: String,
    val toCurrency: String,
    val rateMicros: Long,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis()
)