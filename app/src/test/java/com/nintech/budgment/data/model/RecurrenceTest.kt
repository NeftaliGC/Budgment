package com.nintech.budgment.data.model

import com.nintech.budgment.data.local.entity.RecurringTransactionEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class RecurrenceTest {

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun day(year: Int, month: Int, day: Int, hour: Int = 0): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, hour, 0)
    }.timeInMillis

    private fun recurring(
        anchor: Long, next: Long, unit: PeriodUnit = PeriodUnit.MONTH, interval: Int = 1,
        active: Boolean = true, snoozedUntil: Long? = null, endDate: Long? = null
    ) = RecurringTransactionEntity(
        id = "r", userId = "u", accountId = "a", categoryId = "c", baseAmount = -100,
        periodUnit = unit, periodInterval = interval, anchorDate = anchor, nextOccurrence = next,
        active = active, snoozedUntil = snoozedUntil, endDate = endDate
    )

    @Test
    fun `un cargo del 31 vuelve al 31 despues de febrero`() {
        val anchor = day(2026, 1, 31)
        val feb = Recurrence.occurrenceAfter(anchor, PeriodUnit.MONTH, 1, anchor)
        val mar = Recurrence.occurrenceAfter(anchor, PeriodUnit.MONTH, 1, feb)

        assertEquals(day(2026, 2, 28), feb)
        assertEquals(day(2026, 3, 31), mar)
    }

    @Test
    fun `cada dos semanas avanza catorce dias`() {
        val anchor = day(2026, 11, 2)
        assertEquals(day(2026, 11, 16), Recurrence.occurrenceAfter(anchor, PeriodUnit.WEEK, 2, anchor))
    }

    @Test
    fun `al crear con fecha pasada no quedan atrasados`() {
        // Renta desde el 1 de enero, creada el 15 de noviembre: lo próximo es el 1 de diciembre.
        val next = Recurrence.firstUpcoming(day(2026, 1, 1), PeriodUnit.MONTH, 1, now = day(2026, 11, 15, hour = 10))
        assertEquals(day(2026, 12, 1), next)
    }

    @Test
    fun `si toca hoy la primera ocurrencia es hoy`() {
        val next = Recurrence.firstUpcoming(day(2026, 1, 15), PeriodUnit.MONTH, 1, now = day(2026, 11, 15, hour = 10))
        assertEquals(day(2026, 11, 15), next)
    }

    @Test
    fun `cuenta las ocurrencias atrasadas`() {
        val anchor = day(2026, 8, 1)
        val count = Recurrence.countDue(anchor, PeriodUnit.MONTH, 1, next = day(2026, 9, 1), now = day(2026, 11, 15))
        assertEquals(3, count) // 1 sep, 1 oct y 1 nov
    }

    @Test
    fun `esta pendiente cuando vence y no esta pospuesto ni en pausa`() {
        val now = day(2026, 11, 15, hour = 10)
        val due = recurring(anchor = day(2026, 1, 15), next = day(2026, 11, 15))

        assertTrue(due.isPending(now))
        assertFalse(due.copy(nextOccurrence = day(2026, 12, 15)).isPending(now))
        assertFalse(due.copy(active = false).isPending(now))
        assertFalse(due.copy(snoozedUntil = day(2026, 11, 16)).isPending(now))
        assertTrue(due.copy(snoozedUntil = day(2026, 11, 15, hour = 9)).isPending(now))
        assertFalse(due.copy(endDate = day(2026, 11, 1)).isPending(now))
    }

    @Test
    fun `la fecha del movimiento es ahora si toca hoy y mediodia si esta atrasado`() {
        val now = day(2026, 11, 15, hour = 18)
        assertEquals(now, Recurrence.transactionDateFor(day(2026, 11, 15), now))
        assertEquals(day(2026, 11, 1, hour = 12), Recurrence.transactionDateFor(day(2026, 11, 1), now))
    }

    @Test
    fun `dueCount respeta la fecha de fin`() {
        val r = recurring(anchor = day(2026, 8, 1), next = day(2026, 9, 1), endDate = day(2026, 10, 15))
        assertEquals(2, r.dueCount(now = day(2026, 11, 15)))
    }

    @Test
    fun `diario cuenta cada dia atrasado`() {
        val anchor = day(2026, 11, 10)
        assertEquals(day(2026, 11, 11), Recurrence.occurrenceAfter(anchor, PeriodUnit.DAY, 1, anchor))
        assertEquals(3, Recurrence.countDue(anchor, PeriodUnit.DAY, 1, next = day(2026, 11, 13), now = day(2026, 11, 15, hour = 9)))
    }

    @Test
    fun `semestral salta seis meses conservando el dia`() {
        val anchor = day(2026, 3, 31)
        assertEquals(day(2026, 9, 30), Recurrence.occurrenceAfter(anchor, PeriodUnit.MONTH, 6, anchor))
        assertEquals(day(2027, 3, 31), Recurrence.occurrenceAfter(anchor, PeriodUnit.MONTH, 6, day(2026, 9, 30)))
    }

    @Test
    fun `cada frecuencia se reconoce por unidad e intervalo`() {
        RecurrenceFrequency.entries.forEach { assertEquals(it, RecurrenceFrequency.of(it.unit, it.interval)) }
    }
}
