package com.nintech.budgment.data.attachments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AttachmentPolicyTest {

    private fun date(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, 0)
        }.timeInMillis

    @Test
    fun `el plazo se cuenta en meses de calendario`() {
        assertEquals(date(2026, 8, 16), AttachmentPolicy.retentionCutoff(date(2026, 11, 16), 3))
        assertEquals(date(2025, 11, 16), AttachmentPolicy.retentionCutoff(date(2026, 11, 16), 12))
        // Cruza el cambio de año.
        assertEquals(date(2025, 11, 10), AttachmentPolicy.retentionCutoff(date(2026, 2, 10), 3))
    }

    @Test
    fun `si el dia no existe en el mes de corte se usa el ultimo`() {
        // 31 de mayo menos 3 meses: febrero no tiene 31.
        assertEquals(date(2026, 2, 28), AttachmentPolicy.retentionCutoff(date(2026, 5, 31), 3))
    }

    @Test
    fun `conservar siempre no tiene fecha de corte`() {
        assertNull(AttachmentPolicy.retentionCutoff(date(2026, 11, 16), AttachmentPolicy.KEEP_FOREVER))
    }

    @Test
    fun `el plazo por defecto es una de las opciones`() {
        assertTrue(AttachmentPolicy.DEFAULT_RETENTION_MONTHS in AttachmentPolicy.RETENTION_OPTIONS_MONTHS)
    }

    @Test
    fun `una foto grande se reduce conservando la proporcion`() {
        assertEquals(1600 to 1200, AttachmentPolicy.scaledSize(4000, 3000))
        assertEquals(1200 to 1600, AttachmentPolicy.scaledSize(3000, 4000))
        assertEquals(1600 to 1, AttachmentPolicy.scaledSize(20000, 10)) // nunca queda en cero
    }

    @Test
    fun `una foto pequena no se agranda`() {
        assertEquals(800 to 600, AttachmentPolicy.scaledSize(800, 600))
        assertEquals(1600 to 1600, AttachmentPolicy.scaledSize(1600, 1600))
    }

    @Test
    fun `al decodificar no se baja del tamano final`() {
        assertEquals(1, AttachmentPolicy.sampleSize(1600, 1200))
        assertEquals(1, AttachmentPolicy.sampleSize(3000, 2000)) // a la mitad quedaría en 1500
        assertEquals(2, AttachmentPolicy.sampleSize(4000, 3000))
        assertEquals(4, AttachmentPolicy.sampleSize(8000, 6000))
        assertEquals(1, AttachmentPolicy.sampleSize(100, 100))
    }
}
