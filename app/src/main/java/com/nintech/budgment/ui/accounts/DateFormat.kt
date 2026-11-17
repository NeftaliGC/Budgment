package com.nintech.budgment.ui.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

fun formatDate(millis: Long): String =
    SimpleDateFormat(if (isSpanish()) "d MMM yyyy" else "MMM d, yyyy", appLocale()).format(Date(millis))

/** «1 dic» («Dec 1» en inglés): fecha corta sin año, para fechas cercanas como la renovación de un presupuesto. */
fun formatShortDate(millis: Long): String =
    SimpleDateFormat(if (isSpanish()) "d MMM" else "MMM d", appLocale()).format(Date(millis))

/** Medianoche local del día al que pertenece [millis]: sirve para agrupar por día. */
fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** "Hoy", "Ayer" o "lunes, 10 de noviembre" («Monday, November 10» en inglés); con año solo si no es el actual. */
fun relativeDayLabel(millis: Long, today: String, yesterday: String): String {
    val day = startOfDay(millis)
    val todayStart = startOfDay(System.currentTimeMillis())
    val yesterdayStart = Calendar.getInstance().apply {
        timeInMillis = todayStart
        add(Calendar.DAY_OF_MONTH, -1)
    }.timeInMillis
    return when (day) {
        todayStart -> today
        yesterdayStart -> yesterday
        else -> {
            val sameYear = Calendar.getInstance().apply { timeInMillis = day }.get(Calendar.YEAR) ==
                Calendar.getInstance().get(Calendar.YEAR)
            val locale = appLocale()
            val pattern = when {
                isSpanish() && sameYear -> "EEEE, d 'de' MMMM"
                isSpanish() -> "EEEE, d 'de' MMMM 'de' yyyy"
                sameYear -> "EEEE, MMMM d"
                else -> "EEEE, MMMM d, yyyy"
            }
            SimpleDateFormat(pattern, locale).format(Date(day))
                .replaceFirstChar { it.titlecase(locale) }
        }
    }
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
