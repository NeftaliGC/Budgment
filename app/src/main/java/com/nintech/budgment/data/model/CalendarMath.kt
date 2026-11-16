package com.nintech.budgment.data.model

import java.util.Calendar
import kotlin.math.roundToLong

/** Cálculos de calendario en hora local, compartidos por periodos de presupuesto y recurrencias. */
internal object CalendarMath {

    const val DAY_MS = 24 * 60 * 60 * 1000.0

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /**
     * Suma [amount] unidades a [millis]. Al sumar meses o años, Calendar ajusta el día al último
     * del mes si no existe (31 ene + 1 mes = 28 feb); por eso las series se calculan siempre
     * desde el ancla y no encadenando resultados.
     */
    fun addUnits(millis: Long, unit: PeriodUnit, amount: Int): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        when (unit) {
            PeriodUnit.DAY -> add(Calendar.DAY_OF_MONTH, amount)
            PeriodUnit.WEEK -> add(Calendar.DAY_OF_MONTH, amount * 7)
            PeriodUnit.MONTH -> add(Calendar.MONTH, amount)
            PeriodUnit.YEAR -> add(Calendar.YEAR, amount)
        }
    }.timeInMillis

    /** Días de calendario entre dos instantes; redondea para absorber los cambios de horario. */
    fun daysBetween(from: Long, to: Long): Long =
        ((startOfDay(to) - startOfDay(from)) / DAY_MS).roundToLong()

    /** Unidades completas de calendario aproximadas entre [from] y [to]; sirve como estimación inicial. */
    fun unitsBetween(from: Long, to: Long, unit: PeriodUnit): Long {
        val a = Calendar.getInstance().apply { timeInMillis = from }
        val b = Calendar.getInstance().apply { timeInMillis = to }
        return when (unit) {
            PeriodUnit.DAY -> daysBetween(from, to)
            PeriodUnit.WEEK -> daysBetween(from, to) / 7
            PeriodUnit.MONTH ->
                (b.get(Calendar.YEAR) - a.get(Calendar.YEAR)) * 12L + (b.get(Calendar.MONTH) - a.get(Calendar.MONTH))
            PeriodUnit.YEAR -> (b.get(Calendar.YEAR) - a.get(Calendar.YEAR)).toLong()
        }
    }

    /**
     * Índice k ≥ 0 del primer término de la serie `addUnits(anchor, unit, step * k)` que es ≥ [target].
     */
    fun firstIndexAtOrAfter(anchor: Long, unit: PeriodUnit, step: Int, target: Long): Int {
        if (target <= anchor) return 0
        var k = (unitsBetween(anchor, target, unit) / step).toInt().coerceAtLeast(0)
        while (k > 0 && addUnits(anchor, unit, step * (k - 1)) >= target) k--
        while (addUnits(anchor, unit, step * k) < target) k++
        return k
    }
}
