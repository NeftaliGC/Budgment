package com.nintech.budgment.data.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BudgetPeriodTest {

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        // Zona con horario de verano para cubrir días de 23 y 25 horas.
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, hour, 0)
    }.timeInMillis

    @Test
    fun `mensual creado en noviembre se renueva en diciembre`() {
        val anchor = BudgetPeriod.alignedStart(at(2026, 11, 15), PeriodUnit.MONTH)
        val period = BudgetPeriod.current(anchor, PeriodUnit.MONTH, 1, now = at(2026, 12, 3))

        assertEquals(at(2026, 12, 1, hour = 0), period.start)
        assertEquals(at(2027, 1, 1, hour = 0), period.endExclusive)
        assertFalse(at(2026, 11, 30) in period)
        assertTrue(at(2026, 12, 31, hour = 23) in period)
    }

    @Test
    fun `el ultimo instante del periodo pertenece a el y el primero del siguiente no`() {
        val anchor = BudgetPeriod.alignedStart(at(2026, 1, 1), PeriodUnit.MONTH)
        val period = BudgetPeriod.current(anchor, PeriodUnit.MONTH, 1, now = at(2026, 2, 28, hour = 23))

        assertEquals(at(2026, 2, 1, hour = 0), period.start)
        assertEquals(at(2026, 3, 1, hour = 0), period.endExclusive)
        assertFalse(period.endExclusive in period)
    }

    @Test
    fun `semanal empieza en lunes`() {
        // 15 nov 2026 es domingo: su semana empieza el lunes 9.
        assertEquals(at(2026, 11, 9, hour = 0), BudgetPeriod.alignedStart(at(2026, 11, 15), PeriodUnit.WEEK))
        // Y un lunes es su propio inicio.
        assertEquals(at(2026, 11, 16, hour = 0), BudgetPeriod.alignedStart(at(2026, 11, 16, hour = 8), PeriodUnit.WEEK))
    }

    @Test
    fun `semanal cruza el cambio de horario sin desfasarse`() {
        val anchor = BudgetPeriod.alignedStart(at(2026, 10, 26), PeriodUnit.WEEK)
        // El 1 nov 2026 termina el horario de verano en Nueva York.
        val period = BudgetPeriod.current(anchor, PeriodUnit.WEEK, 1, now = at(2026, 11, 4))

        assertEquals(at(2026, 11, 2, hour = 0), period.start)
        assertEquals(at(2026, 11, 9, hour = 0), period.endExclusive)
    }

    @Test
    fun `anual se renueva el 1 de enero`() {
        val anchor = BudgetPeriod.alignedStart(at(2024, 6, 10), PeriodUnit.YEAR)
        val period = BudgetPeriod.current(anchor, PeriodUnit.YEAR, 1, now = at(2026, 3, 1))

        assertEquals(at(2026, 1, 1, hour = 0), period.start)
        assertEquals(at(2027, 1, 1, hour = 0), period.endExclusive)
    }

    @Test
    fun `intervalo de dos meses cuenta desde el ancla`() {
        val anchor = BudgetPeriod.alignedStart(at(2026, 1, 20), PeriodUnit.MONTH)
        val period = BudgetPeriod.current(anchor, PeriodUnit.MONTH, 2, now = at(2026, 4, 10))

        assertEquals(at(2026, 3, 1, hour = 0), period.start)
        assertEquals(at(2026, 5, 1, hour = 0), period.endExclusive)
    }

    @Test
    fun `antes del ancla devuelve el primer periodo`() {
        val anchor = at(2026, 12, 1, hour = 0)
        val period = BudgetPeriod.current(anchor, PeriodUnit.MONTH, 1, now = at(2026, 11, 20))

        assertEquals(anchor, period.start)
    }

    @Test
    fun `dias restantes cuentan hoy`() {
        val period = BudgetPeriod.current(at(2026, 11, 1, hour = 0), PeriodUnit.MONTH, 1, now = at(2026, 11, 30))

        assertEquals(1, period.daysLeft(now = at(2026, 11, 30, hour = 22)))
        assertEquals(30, period.daysLeft(now = at(2026, 11, 1, hour = 9)))
    }

    @Test
    fun `diario dura un dia y se renueva a medianoche`() {
        val anchor = BudgetPeriod.alignedStart(at(2026, 11, 1), PeriodUnit.DAY)
        val period = BudgetPeriod.current(anchor, PeriodUnit.DAY, 1, now = at(2026, 11, 15, hour = 18))

        assertEquals(at(2026, 11, 15, hour = 0), period.start)
        assertEquals(at(2026, 11, 16, hour = 0), period.endExclusive)
        assertEquals(1, period.daysLeft(now = at(2026, 11, 15, hour = 18)))
    }

    @Test
    fun `trimestral cuenta tres meses desde el mes de creacion`() {
        val anchor = BudgetPeriod.alignedStart(at(2026, 2, 10), PeriodUnit.MONTH)
        val period = BudgetPeriod.current(anchor, PeriodUnit.MONTH, 3, now = at(2026, 11, 15))

        assertEquals(at(2026, 11, 1, hour = 0), period.start)
        assertEquals(at(2027, 2, 1, hour = 0), period.endExclusive)
        assertEquals(RecurrenceFrequency.QUARTERLY, period.frequency)
    }
}
