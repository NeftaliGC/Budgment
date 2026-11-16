package com.nintech.budgment.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockPolicyTest {

    @Test
    fun `PIN valido tiene de 4 a 6 digitos`() {
        assertTrue(AppLockPolicy.isValidPin("0000"))
        assertTrue(AppLockPolicy.isValidPin("123456"))
        assertFalse(AppLockPolicy.isValidPin("123"))
        assertFalse(AppLockPolicy.isValidPin("1234567"))
        assertFalse(AppLockPolicy.isValidPin("12a4"))
        assertFalse(AppLockPolicy.isValidPin("١٢٣٤")) // dígitos de otra escritura
    }

    @Test
    fun `sin espera durante los primeros cuatro fallos`() {
        (0..4).forEach { assertEquals(0L, AppLockPolicy.lockoutMillis(it)) }
    }

    @Test
    fun `la espera empieza en 30 s y se duplica hasta 15 min`() {
        assertEquals(30_000L, AppLockPolicy.lockoutMillis(5))
        assertEquals(60_000L, AppLockPolicy.lockoutMillis(6))
        assertEquals(120_000L, AppLockPolicy.lockoutMillis(7))
        assertEquals(480_000L, AppLockPolicy.lockoutMillis(9))
        assertEquals(900_000L, AppLockPolicy.lockoutMillis(10))
        assertEquals(900_000L, AppLockPolicy.lockoutMillis(1_000))
    }

    @Test
    fun `con tiempo cero se bloquea siempre al volver`() {
        assertTrue(AppLockPolicy.shouldRelock(elapsedMs = 0, timeoutSeconds = 0))
        assertTrue(AppLockPolicy.shouldRelock(elapsedMs = 500, timeoutSeconds = 0))
    }

    @Test
    fun `se respeta el tiempo de gracia configurado`() {
        assertFalse(AppLockPolicy.shouldRelock(elapsedMs = 29_999, timeoutSeconds = 30))
        assertTrue(AppLockPolicy.shouldRelock(elapsedMs = 30_000, timeoutSeconds = 30))
        assertFalse(AppLockPolicy.shouldRelock(elapsedMs = 299_000, timeoutSeconds = 300))
    }

    @Test
    fun `un tiempo negativo se trata como bloqueo`() {
        assertTrue(AppLockPolicy.shouldRelock(elapsedMs = -1, timeoutSeconds = 300))
    }
}
