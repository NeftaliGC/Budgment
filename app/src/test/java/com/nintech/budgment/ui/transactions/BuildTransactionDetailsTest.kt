package com.nintech.budgment.ui.transactions

import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.local.entity.TransactionEntity
import com.nintech.budgment.data.local.entity.TransferEntity
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.data.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildTransactionDetailsTest {

    private val mxn = AccountEntity(id = "mxn", userId = "u", name = "Débito", accountType = AccountType.DEBIT, currency = "MXN")
    private val usd = AccountEntity(id = "usd", userId = "u", name = "Ahorro", accountType = AccountType.SAVINGS, currency = "USD")
    private val food = CategoryEntity(id = "food", userId = "u", name = "Comida", type = CategoryType.EXPENSE)

    private val expense = TransactionEntity(id = "t1", userId = "u", accountId = "mxn", categoryId = "food", amount = -5_000, date = 1)
    private val out = TransactionEntity(id = "out", userId = "u", accountId = "mxn", categoryId = null, amount = -200_000, date = 2)
    private val incoming = TransactionEntity(id = "in", userId = "u", accountId = "usd", categoryId = null, amount = 10_000, date = 2)
    private val transfer = TransferEntity(
        id = "tr", userId = "u", fromAccountId = "mxn", toAccountId = "usd",
        fromTransactionId = "out", toTransactionId = "in"
    )

    @Test
    fun `una transferencia aparece una sola vez, representada por su salida`() {
        val details = buildTransactionDetails(listOf(expense, out, incoming), listOf(mxn, usd), listOf(food), listOf(transfer))

        assertEquals(listOf("out", "t1"), details.map { it.transaction.id })
        val row = details.first()
        assertEquals("tr", row.transfer?.transferId)
        assertEquals("Débito", row.accountName)
        assertEquals("Ahorro", row.transfer?.toAccountName)
        assertEquals(10_000L, row.transfer?.amountIn)
        assertEquals("USD", row.transfer?.currencyIn)
        assertEquals("MXN", row.currency)
        assertNull(row.categoryType)
    }

    @Test
    fun `los movimientos normales no llevan datos de transferencia`() {
        val details = buildTransactionDetails(listOf(expense), listOf(mxn), listOf(food), listOf(transfer))

        assertNull(details.single().transfer)
        assertEquals(CategoryType.EXPENSE, details.single().categoryType)
    }

    @Test
    fun `un movimiento sin categoria ni transferencia se omite`() {
        val details = buildTransactionDetails(listOf(out, incoming), listOf(mxn, usd), listOf(food), emptyList())

        assertTrue(details.isEmpty())
    }

    @Test
    fun `es de solo lectura si la cuenta destino fue eliminada`() {
        val deletedUsd = usd.copy(deletedAt = 99)
        val row = buildTransactionDetails(listOf(out, incoming), listOf(mxn, deletedUsd), emptyList(), listOf(transfer)).single()

        assertFalse(row.accountDeleted)
        assertTrue(row.isReadOnly)
    }
}
