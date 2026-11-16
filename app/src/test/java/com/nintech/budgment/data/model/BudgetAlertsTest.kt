package com.nintech.budgment.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetAlertsTest {

    @Test
    fun `bajo el aviso no hay alerta`() {
        assertNull(budgetAlertLevel(spent = 79_99, limit = 100_00, warningPercent = 80))
    }

    @Test
    fun `al llegar exacto al porcentaje avisa`() {
        assertEquals(80, budgetAlertLevel(spent = 80_00, limit = 100_00, warningPercent = 80))
    }

    @Test
    fun `respeta el porcentaje elegido`() {
        assertNull(budgetAlertLevel(spent = 80_00, limit = 100_00, warningPercent = 90))
        assertEquals(50, budgetAlertLevel(spent = 55_00, limit = 100_00, warningPercent = 50))
    }

    @Test
    fun `al llegar o pasar el limite solo avisa el limite`() {
        assertEquals(BUDGET_LIMIT_LEVEL, budgetAlertLevel(spent = 100_00, limit = 100_00, warningPercent = 80))
        assertEquals(BUDGET_LIMIT_LEVEL, budgetAlertLevel(spent = 250_00, limit = 100_00, warningPercent = 80))
    }

    @Test
    fun `sin limite no avisa`() {
        assertNull(budgetAlertLevel(spent = 10_00, limit = 0, warningPercent = 80))
    }

    @Test
    fun `montos grandes no se desbordan`() {
        assertEquals(80, budgetAlertLevel(spent = 8_000_000_000_00, limit = 10_000_000_000_00, warningPercent = 80))
    }
}
