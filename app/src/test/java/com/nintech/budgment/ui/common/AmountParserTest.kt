package com.nintech.budgment.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountParserTest {

    @Test
    fun `entero y decimales con punto`() {
        assertEquals(200000L, parseAmountToCents("2000"))
        assertEquals(35000L, parseAmountToCents("350"))
        assertEquals(1999L, parseAmountToCents("19.99"))
        assertEquals(50L, parseAmountToCents("0.5"))
        assertEquals(50L, parseAmountToCents(".5"))
    }

    @Test
    fun `la coma seguida de tres digitos separa miles`() {
        assertEquals(200000L, parseAmountToCents("2,000"))
        assertEquals(100000000L, parseAmountToCents("1,000,000"))
        assertEquals(123456789L, parseAmountToCents("1,234,567.89"))
        assertEquals(200050L, parseAmountToCents("2,000.5"))
    }

    @Test
    fun `la coma seguida de uno o dos digitos es decimal`() {
        assertEquals(1999L, parseAmountToCents("19,99"))
        assertEquals(1950L, parseAmountToCents("19,5"))
        assertEquals(200L, parseAmountToCents("2,00"))
    }

    @Test
    fun `formato con punto de miles y coma decimal`() {
        assertEquals(123456L, parseAmountToCents("1.234,56"))
        assertEquals(200050L, parseAmountToCents("2.000,5"))
    }

    @Test
    fun `ignora espacios y simbolo de moneda`() {
        assertEquals(150050L, parseAmountToCents("$ 1,500.50"))
        assertEquals(200000L, parseAmountToCents(" 2,000 "))
    }

    @Test
    fun `no pierde centavos por redondeo binario`() {
        assertEquals(1999L, parseAmountToCents("19.99"))
        assertEquals(110L, parseAmountToCents("1.10"))
    }

    @Test
    fun `rechaza texto que no es un monto`() {
        assertNull(parseAmountToCents(""))
        assertNull(parseAmountToCents("abc"))
        assertNull(parseAmountToCents("-5"))
        assertNull(parseAmountToCents("1.2.3"))
        assertNull(parseAmountToCents("12,34,5"))
    }

    @Test
    fun `centavos a texto editable`() {
        assertEquals("350", centsToInputText(35000L))
        assertEquals("350.5", centsToInputText(35050L))
        assertEquals("19.99", centsToInputText(1999L))
        assertEquals("0", centsToInputText(0L))
    }
}
