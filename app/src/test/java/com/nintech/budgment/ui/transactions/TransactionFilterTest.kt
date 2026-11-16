package com.nintech.budgment.ui.transactions

import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TransactionFilterTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long = Calendar.getInstance().apply {
        clear(); set(year, month - 1, day, hour, 0)
    }.timeInMillis

    private val now = at(2026, 11, 15)

    private fun item(
        id: String,
        amount: Long,
        date: Long = now,
        account: String = "deb",
        accountName: String = "Débito",
        category: String? = "food",
        categoryName: String = "Café y comida",
        type: CategoryType? = if (amount < 0) CategoryType.EXPENSE else CategoryType.INCOME,
        description: String? = null,
        transferTo: String? = null,
        currency: String = "MXN"
    ) = TransactionWithDetails(
        transaction = TransactionEntity(id = id, userId = "u", accountId = account, categoryId = category, amount = amount, date = date, description = description),
        accountName = accountName,
        categoryName = if (transferTo != null) "" else categoryName,
        categoryType = if (transferTo != null) null else type,
        currency = currency,
        transfer = transferTo?.let { TransferDetails("t$id", it, "Ahorro", amountIn = -amount, currencyIn = currency) }
    )

    private val coffee = item("1", -4_550, description = "Latte en Starbucks")
    private val salary = item("2", 2_500_000, category = "sal", categoryName = "Sueldo", date = at(2026, 10, 30))
    private val rent = item("3", -800_000, category = "rent", categoryName = "Renta", account = "cred", accountName = "Crédito", date = at(2026, 1, 3))
    private val transfer = item("4", -100_000, category = null, transferTo = "sav")
    private val all = listOf(coffee, salary, rent, transfer)

    private fun ids(filter: TransactionFilter) = applyFilter(all, filter, now).map { it.transaction.id }

    @Test
    fun `sin filtros devuelve todo`() {
        assertEquals(listOf("1", "2", "3", "4"), ids(TransactionFilter()))
    }

    @Test
    fun `la busqueda ignora acentos y mayusculas`() {
        assertEquals(listOf("1"), ids(TransactionFilter(query = "CAFE")))
        assertEquals(listOf("3"), ids(TransactionFilter(query = "credito")))
        assertEquals(listOf("1"), ids(TransactionFilter(query = "latte starbucks")))
    }

    @Test
    fun `la busqueda encuentra montos como se escriben`() {
        assertEquals(listOf("1"), ids(TransactionFilter(query = "45.50")))
        assertEquals(listOf("2"), ids(TransactionFilter(query = "25,000")))
        assertEquals(listOf("3"), ids(TransactionFilter(query = "8000")))
    }

    @Test
    fun `la busqueda encuentra la cuenta destino de una transferencia`() {
        assertEquals(listOf("4"), ids(TransactionFilter(query = "ahorro")))
    }

    @Test
    fun `filtra por tipo`() {
        assertEquals(listOf("1", "3"), ids(TransactionFilter(kinds = setOf(TransactionKind.EXPENSE))))
        assertEquals(listOf("2", "4"), ids(TransactionFilter(kinds = setOf(TransactionKind.INCOME, TransactionKind.TRANSFER))))
    }

    @Test
    fun `una transferencia aparece en la cuenta de origen y en la de destino`() {
        assertEquals(listOf("4"), ids(TransactionFilter(accountIds = setOf("sav"))))
        assertEquals(listOf("1", "2", "4"), ids(TransactionFilter(accountIds = setOf("deb"))))
    }

    @Test
    fun `filtrar por categoria oculta las transferencias`() {
        assertEquals(listOf("1", "3"), ids(TransactionFilter(categoryIds = setOf("food", "rent"))))
    }

    @Test
    fun `los grupos de filtros se combinan`() {
        val filter = TransactionFilter(kinds = setOf(TransactionKind.EXPENSE), accountIds = setOf("deb"))
        assertEquals(listOf("1"), ids(filter))
    }

    @Test
    fun `rangos de fecha`() {
        assertEquals(listOf("1", "4"), ids(TransactionFilter(date = DateFilter(DatePreset.THIS_MONTH))))
        assertEquals(listOf("2"), ids(TransactionFilter(date = DateFilter(DatePreset.LAST_MONTH))))
        assertEquals(listOf("1", "2", "4"), ids(TransactionFilter(date = DateFilter(DatePreset.LAST_30_DAYS))))
        assertEquals(listOf("1", "2", "3", "4"), ids(TransactionFilter(date = DateFilter(DatePreset.THIS_YEAR))))
    }

    @Test
    fun `el rango personalizado incluye el ultimo dia completo`() {
        val day = at(2026, 1, 3, hour = 0)
        val filter = TransactionFilter(date = DateFilter(DatePreset.CUSTOM, customStart = day, customEnd = day))
        assertEquals(listOf("3"), ids(filter))
    }

    @Test
    fun `el resumen suma por separado y no mezcla monedas`() {
        val summary = summarize(listOf(coffee, salary, transfer))
        assertEquals(3, summary.count)
        assertEquals(2_500_000L, summary.income)
        assertEquals(4_550L, summary.expense)

        val mixed = summarize(listOf(coffee, item("9", -100, currency = "USD")))
        assertNull(mixed.currency)
        assertNull(mixed.income)
    }

    @Test
    fun `cuenta los filtros activos sin contar la busqueda`() {
        val filter = TransactionFilter(query = "x", kinds = setOf(TransactionKind.INCOME), date = DateFilter(DatePreset.THIS_YEAR))
        assertEquals(2, filter.activeCount)
        assertTrue(filter.isActive)
        assertFalse(TransactionFilter(query = "  ").isActive)
    }
}
