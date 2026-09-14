package com.nintech.budgment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nintech.budgment.data.local.entity.ExchangeRateEntity

@Dao
interface ExchangeRateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rate: ExchangeRateEntity)

    @Query("SELECT * FROM exchange_rates WHERE fromCurrency = :from AND toCurrency = :to ORDER BY date DESC LIMIT 1")
    suspend fun getLatestRate(from: String, to: String): ExchangeRateEntity?
}