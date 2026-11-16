package com.nintech.budgment.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class GoalProgressTest {

    private fun at(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear(); set(year, month - 1, day, 12, 0)
    }.timeInMillis

    private val now = at(2026, 11, 15)

    @Test
    fun `sin fecha solo dice cuanto falta`() {
        val p = goalProgress(saved = 9_000, target = 36_000, targetDate = null, now = now)
        assertEquals(GoalStatus.NO_DATE, p.status)
        assertEquals(27_000, p.remaining)
        assertEquals(0.25f, p.fraction, 0.0001f)
        assertNull(p.monthlyNeeded)
    }

    @Test
    fun `con fecha reparte lo que falta entre los meses que quedan`() {
        // Del 15 nov al 15 jun hay 7 meses.
        val p = goalProgress(saved = 32_000, target = 53_000, targetDate = at(2027, 6, 15), now = now)
        assertEquals(GoalStatus.ON_TRACK, p.status)
        assertEquals(3_000L, p.monthlyNeeded)
    }

    @Test
    fun `el aporte mensual se redondea hacia arriba`() {
        val p = goalProgress(saved = 0, target = 100, targetDate = at(2027, 2, 15), now = now)
        assertEquals(34L, p.monthlyNeeded) // 100 / 3 meses
    }

    @Test
    fun `un mes parcial cuenta como mes completo`() {
        assertEquals(8, monthsUntil(now, at(2027, 6, 20)))
        assertEquals(7, monthsUntil(now, at(2027, 6, 10)))
        assertEquals(1, monthsUntil(now, at(2026, 11, 20)))
    }

    @Test
    fun `al llegar al objetivo se da por alcanzada aunque la fecha haya pasado`() {
        val p = goalProgress(saved = 50_000, target = 50_000, targetDate = at(2026, 1, 1), now = now)
        assertEquals(GoalStatus.REACHED, p.status)
        assertEquals(0, p.remaining)
        assertEquals(1f, p.fraction, 0f)
    }

    @Test
    fun `fecha pasada sin llegar queda vencida`() {
        assertEquals(GoalStatus.OVERDUE, goalProgress(10, 100, at(2026, 11, 14), now).status)
    }

    @Test
    fun `la fecha de hoy aun cuenta como a tiempo`() {
        val p = goalProgress(10, 100, at(2026, 11, 15), now)
        assertEquals(GoalStatus.ON_TRACK, p.status)
        assertEquals(90L, p.monthlyNeeded)
    }

    @Test
    fun `saldo negativo no da avance negativo`() {
        assertEquals(0f, goalProgress(-500, 1_000, null, now).fraction, 0f)
    }
}
