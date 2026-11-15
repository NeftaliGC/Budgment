package com.nintech.budgment.ui.common

import java.math.RoundingMode

/**
 * Convierte un monto escrito por el usuario a centavos.
 * Usa decimales exactos: con Double, 19.99 * 100 da 1998.99… y se guardaría un centavo de menos.
 *
 * Acepta separadores de miles y coma decimal, porque el teclado numérico permite ambos:
 * "2,000" -> 2000.00, "1,234,567.89" -> 1234567.89, "1.234,56" -> 1234.56, "19,99" -> 19.99.
 *
 * @return los centavos, o null si el texto no es un monto válido mayor o igual a cero.
 */
fun parseAmountToCents(text: String): Long? {
    val amount = normalizeAmountText(text)?.toBigDecimalOrNull() ?: return null
    if (amount.signum() < 0) return null
    return runCatching {
        amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }.getOrNull()
}

/**
 * Centavos a texto editable para un campo de monto: 35000 -> "350", 35050 -> "350.5", 1999 -> "19.99".
 */
fun centsToInputText(cents: Long): String =
    java.math.BigDecimal.valueOf(cents, 2).stripTrailingZeros().toPlainString()

/**
 * Deja el texto con punto como único separador decimal y sin separadores de miles.
 *
 * - Con punto y coma a la vez, el que aparece al final es el decimal y el otro agrupa miles.
 * - Con un solo tipo de separador repetido ("1,234,567"), agrupa miles.
 * - Con una sola coma seguida de exactamente tres dígitos ("2,000"), agrupa miles.
 * - Con una sola coma en cualquier otro caso ("19,99", "19,5"), es el decimal.
 * - Un solo punto siempre es el decimal.
 *
 * @return el texto normalizado, o null si los separadores no forman un número reconocible.
 */
private fun normalizeAmountText(text: String): String? {
    val clean = text.filterNot { it.isWhitespace() || it == '$' }
    if (clean.isEmpty()) return null
    if (clean.any { !it.isDigit() && it != '.' && it != ',' }) return null

    val commas = clean.count { it == ',' }
    val dots = clean.count { it == '.' }

    val decimalSeparator: Char? = when {
        commas > 0 && dots > 0 -> if (clean.lastIndexOf(',') > clean.lastIndexOf('.')) ',' else '.'
        dots == 1 -> '.'
        dots > 1 -> null
        commas == 1 -> if (clean.length - clean.indexOf(',') - 1 == 3) null else ','
        else -> null
    }
    // El separador decimal solo puede aparecer una vez.
    if (decimalSeparator != null && clean.count { it == decimalSeparator } > 1) return null

    val integerPart = if (decimalSeparator != null) clean.substringBeforeLast(decimalSeparator) else clean
    val decimalPart = if (decimalSeparator != null) clean.substringAfterLast(decimalSeparator) else ""

    // Lo que queda en la parte entera son separadores de miles: deben ir en grupos de tres dígitos.
    val groups = integerPart.split(',', '.')
    if (groups.size > 1) {
        val validGrouping = groups.first().length in 1..3 && groups.drop(1).all { it.length == 3 }
        if (!validGrouping) return null
    }
    if (decimalPart.any { !it.isDigit() }) return null

    val digits = groups.joinToString("")
    return if (decimalPart.isEmpty()) digits.ifEmpty { "0" } else "${digits.ifEmpty { "0" }}.$decimalPart"
}
