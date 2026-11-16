package com.nintech.budgment.ui.statistics

import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.model.SupportedCurrency
import java.util.Calendar
import kotlin.math.roundToLong

enum class StatsRangeMode { MONTH, YEAR }

/** Periodo que se analiza: un mes o un año de calendario, de [start] (incluido) a [endExclusive]. */
data class StatsRange(val mode: StatsRangeMode, val start: Long, val endExclusive: Long) {
    operator fun contains(millis: Long): Boolean = millis >= start && millis < endExclusive

    companion object {
        /** El mes o año actual con [offset] = 0; 1 es el anterior, y así. */
        fun of(mode: StatsRangeMode, offset: Int, now: Long = System.currentTimeMillis()): StatsRange {
            val start = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_MONTH, 1)
                when (mode) {
                    StatsRangeMode.MONTH -> add(Calendar.MONTH, -offset)
                    StatsRangeMode.YEAR -> {
                        set(Calendar.MONTH, Calendar.JANUARY)
                        add(Calendar.YEAR, -offset)
                    }
                }
            }
            val end = (start.clone() as Calendar).apply {
                add(if (mode == StatsRangeMode.MONTH) Calendar.MONTH else Calendar.YEAR, 1)
            }
            return StatsRange(mode, start.timeInMillis, end.timeInMillis)
        }
    }
}

data class CategorySpending(
    val categoryId: String,
    val name: String,
    val icon: String?,
    val color: String?,
    val amount: Long,
    /** Fracción del gasto total del periodo (0–1). */
    val share: Float,
    val deleted: Boolean = false
)

/** Ingresos y gastos de un mes, ambos positivos. */
data class MonthFlow(val monthStart: Long, val income: Long, val expense: Long)

/** Saldo total de las cuentas al cierre de un mes (o ahora, si es el mes en curso). */
data class BalancePoint(val monthStart: Long, val balance: Long)

data class StatisticsData(
    val range: StatsRange,
    val income: Long = 0,
    val expense: Long = 0,
    val previousIncome: Long = 0,
    val previousExpense: Long = 0,
    val byCategory: List<CategorySpending> = emptyList(),
    /** Seis meses hasta el elegido, o los doce del año elegido. */
    val monthlyFlows: List<MonthFlow> = emptyList(),
    val balanceHistory: List<BalancePoint> = emptyList(),
    /** Algún monto no se pudo convertir por falta de tipo de cambio y quedó fuera. */
    val missingRate: Boolean = false
) {
    val net: Long get() = income - expense
    val hasActivity: Boolean get() = income != 0L || expense != 0L
}

/**
 * Calcula las estadísticas de [range] en una sola moneda. [rateTo] da el factor para convertir
 * de cada moneda a la de visualización (null si no hay tipo de cambio).
 *
 * Solo cuentan los movimientos con categoría: las transferencias mueven dinero entre cuentas
 * propias y no son ni ingreso ni gasto.
 */
fun computeStatistics(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    rateTo: (String) -> Double?,
    range: StatsRange,
    previousRange: StatsRange,
    now: Long = System.currentTimeMillis()
): StatisticsData {
    val accountsById = accounts.associateBy { it.id }
    val categoriesById = categories.associateBy { it.id }
    var missingRate = false

    fun currencyOf(tx: TransactionEntity) =
        tx.currency ?: accountsById[tx.accountId]?.currency ?: SupportedCurrency.MXN.code

    fun convert(cents: Long, currency: String): Long? {
        val rate = rateTo(currency)
        if (rate == null) {
            missingRate = true
            return null
        }
        return (cents * rate).roundToLong()
    }

    // Ingreso o gasto según el signo; las transferencias (sin categoría) se descartan.
    val categorized = transactions.filter { it.deletedAt == null && it.categoryId != null }
    val converted = categorized.mapNotNull { tx -> convert(tx.amount, currencyOf(tx))?.let { tx to it } }

    fun totals(r: StatsRange): Pair<Long, Long> {
        var income = 0L
        var expense = 0L
        converted.forEach { (tx, amount) ->
            if (tx.date in r) {
                if (amount >= 0) income += amount else expense += -amount
            }
        }
        return income to expense
    }

    val (income, expense) = totals(range)
    val (previousIncome, previousExpense) = totals(previousRange)

    val byCategory = converted
        .filter { (tx, amount) -> tx.date in range && amount < 0 }
        .groupBy({ it.first.categoryId!! }, { -it.second })
        .map { (categoryId, amounts) ->
            val category = categoriesById[categoryId]
            val total = amounts.sum()
            CategorySpending(
                categoryId = categoryId,
                name = category?.name.orEmpty(),
                icon = category?.icon,
                color = category?.color,
                amount = total,
                share = if (expense > 0) total.toFloat() / expense else 0f,
                deleted = category?.deletedAt != null
            )
        }
        .sortedByDescending { it.amount }

    val months = chartMonths(range)
    val monthlyFlows = months.map { month ->
        val (i, e) = totals(month)
        MonthFlow(month.start, i, e)
    }

    // Cierre de cada mes ya empezado; el mes en curso se mide a hoy.
    val balanceHistory = months
        .filter { it.start <= now }
        .map { month ->
            BalancePoint(month.start, totalBalanceAt(accounts, transactions, minOf(month.endExclusive - 1, now), ::convert))
        }

    return StatisticsData(
        range = range,
        income = income,
        expense = expense,
        previousIncome = previousIncome,
        previousExpense = previousExpense,
        byCategory = byCategory,
        monthlyFlows = monthlyFlows,
        balanceHistory = balanceHistory,
        missingRate = missingRate
    )
}

/** Meses que se grafican: los seis que terminan en el mes elegido, o los doce del año. */
fun chartMonths(range: StatsRange): List<StatsRange> {
    val count = if (range.mode == StatsRangeMode.MONTH) 6 else 12
    val firstMonth = Calendar.getInstance().apply {
        timeInMillis = range.start
        if (range.mode == StatsRangeMode.MONTH) add(Calendar.MONTH, -(count - 1))
    }
    return (0 until count).map { i ->
        val start = (firstMonth.clone() as Calendar).apply { add(Calendar.MONTH, i) }
        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        StatsRange(StatsRangeMode.MONTH, start.timeInMillis, end.timeInMillis)
    }
}

/**
 * Saldo total en el instante [t]: el saldo actual de cada cuenta menos lo que se movió después.
 * Solo cuentan las cuentas que existían en [t]. Usa el tipo de cambio de hoy.
 */
private fun totalBalanceAt(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    t: Long,
    convert: (Long, String) -> Long?
): Long {
    val laterByAccount = transactions
        .filter { it.deletedAt == null && it.date > t }
        .groupBy { it.accountId }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    return accounts
        .filter { it.createdAt <= t && (it.deletedAt == null || it.deletedAt > t) }
        .sumOf { account ->
            val balance = account.balanceCache - (laterByAccount[account.id] ?: 0L)
            convert(balance, account.currency) ?: 0L
        }
}
