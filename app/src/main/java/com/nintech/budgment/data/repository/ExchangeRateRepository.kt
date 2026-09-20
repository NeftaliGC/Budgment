package com.nintech.budgment.data.repository

import com.nintech.budgment.data.local.dao.ExchangeRateDao
import com.nintech.budgment.data.local.entity.ExchangeRateEntity
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.data.remote.ExchangeRateApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

interface ExchangeRateRepository {
    suspend fun convert(cents: Long, from: String, to: String): Long?
    suspend fun getRateMicros(from: String, to: String): Long?
}

@Singleton
class ExchangeRateRepositoryImpl @Inject constructor(
    private val exchangeRateDao: ExchangeRateDao,
    private val exchangeRateApi: ExchangeRateApi
) : ExchangeRateRepository {

    private val mutex = Mutex()
    private var lastRefreshAttempt: Long = 0

    override suspend fun convert(cents: Long, from: String, to: String): Long? {
        val fromCode = SupportedCurrency.fromCode(from) ?: return null
        val toCode = SupportedCurrency.fromCode(to) ?: return null
        if (fromCode == toCode) return cents
        val rateMicros = getRateMicros(fromCode.code, toCode.code) ?: return null
        return (cents.toDouble() * rateMicros / ONE_UNIT_MICROS).roundToLong()
    }

    override suspend fun getRateMicros(from: String, to: String): Long? {
        val fromCode = SupportedCurrency.fromCode(from) ?: return null
        val toCode = SupportedCurrency.fromCode(to) ?: return null
        if (fromCode == toCode) return ONE_UNIT_MICROS
        refreshIfNeeded()
        exchangeRateDao.getLatestRate(fromCode.code, toCode.code)?.let { return it.rateMicros }
        // Respaldo por pivote USD si el par directo aún no está cacheado.
        val usdToTarget = exchangeRateDao.getLatestRate(SupportedCurrency.USD.code, toCode.code) ?: return null
        val usdToSource = exchangeRateDao.getLatestRate(SupportedCurrency.USD.code, fromCode.code) ?: return null
        if (usdToSource.rateMicros == 0L) return null
        return (usdToTarget.rateMicros * ONE_UNIT_MICROS) / usdToSource.rateMicros
    }

    private suspend fun refreshIfNeeded() {
        mutex.withLock {
            if (System.currentTimeMillis() - lastRefreshAttempt < REFRESH_ATTEMPT_MIN_MS) return
            val latest = exchangeRateDao.getLatestRate(SupportedCurrency.USD.code, SupportedCurrency.MXN.code)
            if (latest != null && System.currentTimeMillis() - latest.date <= REFRESH_INTERVAL_MS) return
            lastRefreshAttempt = System.currentTimeMillis()
            try {
                refreshRates()
            } catch (_: Exception) {
                // Sin red: se conserva la caché vigente; la primera vez quedarán tasas nulas.
            }
        }
    }

    private suspend fun refreshRates() {
        val codes = SupportedCurrency.ALL.map { it.code }
        val symbols = codes.filter { it != SupportedCurrency.USD.code }.joinToString(",")
        val response = exchangeRateApi.getLatestRates(from = SupportedCurrency.USD.code, symbols = symbols)
        val usdToOther = mapOf(SupportedCurrency.USD.code to 1.0) + codes
            .filter { it != SupportedCurrency.USD.code }
            .map { it to (response.rates[it] ?: 0.0) }
        val now = System.currentTimeMillis()
        val pairs = buildList {
            for (from in codes) {
                for (to in codes) {
                    if (from == to) continue
                    val rate = if (from == SupportedCurrency.USD.code) {
                        usdToOther[to]
                    } else if (to == SupportedCurrency.USD.code) {
                        usdToOther[from]?.let { 1.0 / it }
                    } else {
                        val toUsd = usdToOther[to]
                        val fromUsd = usdToOther[from]
                        if (toUsd != null && fromUsd != null && fromUsd != 0.0) toUsd / fromUsd else 0.0
                    }
                    if (rate == null || rate <= 0.0) continue
                    add(
                        ExchangeRateEntity(
                            id = UUID.randomUUID().toString(),
                            fromCurrency = from,
                            toCurrency = to,
                            rateMicros = (rate * ONE_UNIT_MICROS).roundToLong(),
                            date = now
                        )
                    )
                }
            }
        }
        pairs.forEach { exchangeRateDao.insert(it) }
    }

    private companion object {
        const val ONE_UNIT_MICROS = 1_000_000L
        const val REFRESH_INTERVAL_MS = 6 * 60 * 60 * 1000L
        const val REFRESH_ATTEMPT_MIN_MS = 60 * 1000L
    }
}