package com.nintech.budgment.ui.settings

import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.ui.transactions.TransactionWithDetails
import com.nintech.budgment.ui.transactions.TransferDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class TransactionsCsvTest {

    private val labels = CsvLabels(
        header = listOf("Fecha", "Tipo", "Categoría", "Cuenta", "Cuenta destino", "Monto", "Moneda", "Nota"),
        expense = "Gasto", income = "Ingreso", transfer = "Transferencia"
    )
    private val utc = TimeZone.getTimeZone("UTC")

    // 2026-11-16 12:00 UTC
    private val date = 1_794_830_400_000L

    private fun item(
        amount: Long, type: CategoryType? = CategoryType.EXPENSE, category: String = "Comida",
        account: String = "Efectivo", note: String? = null, currency: String = "MXN",
        transfer: TransferDetails? = null
    ) = TransactionWithDetails(
        transaction = TransactionEntity(
            id = "t", userId = "u", accountId = "a", categoryId = "c", amount = amount, description = note, date = date
        ),
        accountName = account, categoryName = category, categoryType = type, currency = currency, transfer = transfer
    )

    private fun rows(vararg items: TransactionWithDetails) =
        buildTransactionsCsv(items.toList(), labels, utc).removePrefix("\uFEFF").trimEnd().split("\r\n")

    @Test
    fun `empieza con la marca BOM y el encabezado`() {
        val csv = buildTransactionsCsv(emptyList(), labels, utc)
        assertTrue(csv.startsWith("\uFEFF"))
        assertEquals("Fecha,Tipo,Categoría,Cuenta,Cuenta destino,Monto,Moneda,Nota\r\n", csv.removePrefix("\uFEFF"))
    }

    @Test
    fun `gasto e ingreso llevan signo y dos decimales`() {
        val rows = rows(item(-12_550, note = "Tacos"), item(2_000_000, CategoryType.INCOME, "Sueldo"), item(-5))
        assertEquals("2026-11-16,Gasto,Comida,Efectivo,,-125.50,MXN,Tacos", rows[1])
        assertEquals("2026-11-16,Ingreso,Sueldo,Efectivo,,20000.00,MXN,", rows[2])
        assertEquals("2026-11-16,Gasto,Comida,Efectivo,,-0.05,MXN,", rows[3])
    }

    @Test
    fun `una transferencia es una fila con origen y destino`() {
        val transfer = TransferDetails("x", "b", "Ahorro", amountIn = 50_000, currencyIn = "MXN")
        val row = rows(item(-50_000, type = null, category = "", account = "Débito", transfer = transfer))[1]
        assertEquals("2026-11-16,Transferencia,,Débito,Ahorro,-500.00,MXN,", row)
    }

    @Test
    fun `comas comillas y saltos de linea se escapan`() {
        val row = rows(item(-100, note = "Pan, leche y \"huevos\"\nde la tienda"))[1]
        assertTrue(row, row.endsWith(",\"Pan, leche y \"\"huevos\"\"\nde la tienda\""))
    }

    @Test
    fun `una nota que parece formula no se ejecuta`() {
        assertTrue(rows(item(-100, note = "=HYPERLINK(\"http://x\")"))[1].endsWith(",\"'=HYPERLINK(\"\"http://x\"\")\""))
        assertTrue(rows(item(-100, note = "-10% descuento"))[1].endsWith(",'-10% descuento"))
        assertTrue(rows(item(-100, account = "@casa"))[1].contains(",'@casa,"))
    }

    @Test
    fun `la fecha usa la zona horaria indicada`() {
        val csv = buildTransactionsCsv(listOf(item(-100)), labels, TimeZone.getTimeZone("GMT+14:00"))
        assertTrue(csv.contains("2026-11-17,"))
    }
}
