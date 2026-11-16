package com.nintech.budgment.ui.recurring

import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.CategoryEntity
import com.nintech.budgment.data.local.entity.RecurringTransactionEntity
import com.nintech.budgment.data.model.AccountType
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.data.model.PeriodUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRecurringItemsTest {

    private val hour = 60 * 60 * 1000L
    private val now = 1_000 * hour
    private val account = AccountEntity(id = "a", userId = "u", name = "Débito", accountType = AccountType.DEBIT, currency = "MXN")
    private val category = CategoryEntity(id = "c", userId = "u", name = "Renta", type = CategoryType.EXPENSE)
    private val due = RecurringTransactionEntity(
        id = "r", userId = "u", accountId = "a", categoryId = "c", baseAmount = -800_000,
        periodUnit = PeriodUnit.MONTH, anchorDate = now - 2 * hour, nextOccurrence = now - 2 * hour
    )

    private fun build(r: RecurringTransactionEntity) =
        buildRecurringItems(listOf(r), listOf(account), listOf(category), now).single()

    @Test
    fun `vencido sin posponer se anuncia y se puede registrar`() {
        val item = build(due)
        assertTrue(item.isPending)
        assertTrue(item.isDue)
        assertFalse(item.isSnoozed)
    }

    @Test
    fun `pospuesto no se anuncia pero sigue registrable`() {
        val item = build(due.copy(snoozedUntil = now + 10 * hour))
        assertFalse(item.isPending)
        assertTrue(item.isDue)
        assertTrue(item.isSnoozed)
    }

    @Test
    fun `al pasar la hora del posponer vuelve a anunciarse`() {
        val item = build(due.copy(snoozedUntil = now - hour))
        assertTrue(item.isPending)
        assertFalse(item.isSnoozed)
    }

    @Test
    fun `sin categoria no se puede registrar hasta corregirlo`() {
        val item = buildRecurringItems(listOf(due), listOf(account), emptyList(), now).single()
        assertTrue(item.needsAttention)
        assertFalse(item.isDue)
    }

    @Test
    fun `si aun no vence no esta pendiente`() {
        val item = build(due.copy(nextOccurrence = now + 24 * hour))
        assertFalse(item.isDue)
        assertEquals(0, item.dueCount)
    }
}
