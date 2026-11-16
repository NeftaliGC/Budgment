package com.nintech.budgment.ui.goals

import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.GoalEntity
import com.nintech.budgment.data.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildGoalItemsTest {

    private val now = 1_800_000_000_000L
    private val savings = AccountEntity(id = "s", userId = "u", name = "Ahorro", accountType = AccountType.DEBIT, currency = "USD", balanceCache = 32_000)

    private fun goal(id: String, accountId: String? = null, current: Long = 0, target: Long = 50_000, date: Long? = null, currency: String? = "MXN") =
        GoalEntity(id = id, userId = "u", accountId = accountId, name = id, targetAmount = target, currentAmount = current, currency = currency, targetDate = date)

    @Test
    fun `la meta vinculada avanza con el saldo de la cuenta y usa su moneda`() {
        val item = buildGoalItems(listOf(goal("viaje", accountId = "s", current = 999)), listOf(savings), now).single()
        assertEquals(32_000, item.progress.saved)
        assertEquals("USD", item.currency)
        assertTrue(item.isLinked)
        assertFalse(item.accountMissing)
    }

    @Test
    fun `la meta manual avanza con sus aportes`() {
        val item = buildGoalItems(listOf(goal("fondo", current = 9_000)), listOf(savings), now).single()
        assertEquals(9_000, item.progress.saved)
        assertEquals("MXN", item.currency)
    }

    @Test
    fun `avisa si la cuenta vinculada se elimino`() {
        val deleted = savings.copy(deletedAt = now - 1)
        assertTrue(buildGoalItems(listOf(goal("viaje", accountId = "s")), listOf(deleted), now).single().accountMissing)
        assertTrue(buildGoalItems(listOf(goal("viaje", accountId = "otra")), listOf(savings), now).single().accountMissing)
    }

    @Test
    fun `las alcanzadas van al final y las de fecha mas cercana primero`() {
        val items = buildGoalItems(
            listOf(
                goal("lista", current = 50_000),
                goal("sin-fecha"),
                goal("lejana", date = now + 300 * 86_400_000L),
                goal("cercana", date = now + 30 * 86_400_000L)
            ),
            emptyList(),
            now
        )
        assertEquals(listOf("cercana", "lejana", "sin-fecha", "lista"), items.map { it.goal.id })
    }

    @Test
    fun `las metas eliminadas no aparecen`() {
        assertTrue(buildGoalItems(listOf(goal("x").copy(deletedAt = now)), emptyList(), now).isEmpty())
    }
}
