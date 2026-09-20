package com.nintech.budgment.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class FrankfurterResponse(
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)

interface ExchangeRateApi {
    @GET("latest")
    suspend fun getLatestRates(
        @Query("from") from: String,
        @Query("symbols") symbols: String
    ): FrankfurterResponse
}