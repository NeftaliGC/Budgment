package com.nintech.budgment.ui.statistics

import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.data.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ComputeStatisticsTest {

    private fun at(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear(); set(year, month - 1, day, 12, 0)
    }.timeInMillis

    private val now = at(2026, 11, 15)
    private val range = StatsRange.of(StatsRangeMode.MONTH, 0, now)
    private val previous = StatsRange.of(StatsRangeMode.MONTH, 1, now)

    private val food = CategoryEntity(id = "food", userId = "u", name = "Comida", type = CategoryType.EXPENSE)
    private val rent = CategoryEntity(id = "rent", userId = "u", name = "Renta", type = CategoryType.EXPENSE)
    private val salary = CategoryEntity(id = "sal", userId = "u", name = "Sueldo", type = CategoryType.INCOME)

    private fun account(id: String, balance: Long, currency: String = "MXN", created: Long = at(2026, 1, 1)) =
        AccountEntity(id = id, userId = "u", name = id, accountType = AccountType.DEBIT, currency = currency, balanceCache = balance, createdAt = created)

    private fun tx(id: String, account: String, amount: Long, date: Long, category: String?) =
        TransactionEntity(id = id, userId = "u", accountId = account, categoryId = category, amount = amount, date = date)

    private fun compute(
        txs: List<TransactionEntity>,
        accounts: List<AccountEntity> = listOf(account("a", 0)),
        rates: Map<String, Double> = mapOf("MXN" to 1.0)
    ) = computeStatistics(txs, accounts, listOf(food, rent, salary), { rates[it] }, range, previous, now)

    @Test
    fun `separa ingresos y gastos del periodo y compara con el anterior`() {
        val data = compute(
            listOf(
                tx("1", "a", 2_000_000, at(2026, 11, 1), "sal"),
                tx("2", "a", -300_000, at(2026, 11, 3), "food"),
                tx("3", "a", -800_000, at(2026, 11, 5), "rent"),
                tx("4", "a", -1_000_000, at(2026, 10, 20), "rent")
            )
        )
        assertEquals(2_000_000, data.income)
        assertEquals(1_100_000, data.expense)
        assertEquals(900_000, data.net)
        assertEquals(1_000_000, data.previousExpense)
    }

    @Test
    fun `las transferencias no cuentan como ingreso ni gasto`() {
        val data = compute(
            listOf(
                tx("1", "a", -500_000, at(2026, 11, 2), null),
                tx("2", "b", 500_000, at(2026, 11, 2), null)
            ),
            accounts = listOf(account("a", 0), account("b", 0))
        )
        assertFalse(data.hasActivity)
        assertTrue(data.byCategory.isEmpty())
    }

    @Test
    fun `gasto por categoria ordenado y con su proporcion`() {
        val data = compute(
            listOf(
                tx("1", "a", -250_000, at(2026, 11, 2), "food"),
                tx("2", "a", -750_000, at(2026, 11, 3), "rent")
            )
        )
        assertEquals(listOf("rent", "food"), data.byCategory.map { it.categoryId })
        assertEquals(0.75f, data.byCategory.first().share, 0.001f)
    }

    @Test
    fun `convierte a la moneda de visualizacion y avisa si falta un tipo de cambio`() {
        val data = compute(
            listOf(
                tx("1", "usd", -10_000, at(2026, 11, 2), "food"),
                tx("2", "eur", -10_000, at(2026, 11, 2), "food")
            ),
            accounts = listOf(account("usd", 0, "USD"), account("eur", 0, "EUR")),
            rates = mapOf("USD" to 18.0)
        )
        assertEquals(180_000, data.expense)
        assertTrue(data.missingRate)
    }

    @Test
    fun `el saldo historico descuenta lo movido despues de cada cierre`() {
        // Saldo actual 1,500: entró 1,000 en noviembre y salieron 500 en octubre.
        val data = compute(
            listOf(
                tx("1", "a", -50_000, at(2026, 10, 10), "food"),
                tx("2", "a", 100_000, at(2026, 11, 1), "sal")
            ),
            accounts = listOf(account("a", 150_000))
        )
        val byMonth = data.balanceHistory.associate { it.monthStart to it.balance }
        assertEquals(150_000L, byMonth[range.start])
        assertEquals(50_000L, byMonth[previous.start])
        assertEquals(100_000L, byMonth[StatsRange.of(StatsRangeMode.MONTH, 2, now).start])
    }

    @Test
    fun `una cuenta creada despues no cuenta en meses anteriores`() {
        val data = compute(emptyList(), accounts = listOf(account("a", 70_000, created = at(2026, 11, 2))))
        assertEquals(listOf(0L, 0L, 0L, 0L, 0L, 70_000L), data.balanceHistory.map { it.balance })
    }

    @Test
    fun `modo anio grafica los doce meses del anio`() {
        val year = StatsRange.of(StatsRangeMode.YEAR, 0, now)
        val months = chartMonths(year)
        assertEquals(12, months.size)
        assertEquals(year.start, months.first().start)
        assertEquals(year.endExclusive, months.last().endExclusive)
    }
}
