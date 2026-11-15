package com.nintech.budgment.ui.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale("es", "MX")).format(Date(millis))

fun currentMonthRange(): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val start = cal.timeInMillis
    cal.add(Calendar.MONTH, 1)
    cal.add(Calendar.MILLISECOND, -1)
    return start to cal.timeInMillis
}

fun isToday(millis: Long): Boolean {
    val today = Calendar.getInstance()
    val other = Calendar.getInstance().apply { timeInMillis = millis }
    return today.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

/**
 * El selector de fecha de Material trabaja con la medianoche UTC del día elegido.
 * Devuelve ese valor para el día local al que pertenece [localMillis].
 */
fun localDateAsUtcMidnight(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/**
 * Aplica el día elegido en el selector ([utcMidnight]) a [originalMillis], conservando su hora local.
 * Así los movimientos del mismo día mantienen su orden.
 */
fun withDateFromUtcMidnight(originalMillis: Long, utcMidnight: Long): Long {
    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMidnight }
    return Calendar.getInstance().apply {
        timeInMillis = originalMillis
        set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
