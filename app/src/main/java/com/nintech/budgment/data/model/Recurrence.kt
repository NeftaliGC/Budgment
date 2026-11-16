package com.nintech.budgment.data.model

import com.nintech.budgment.data.local.entity.RecurringTransactionEntity
import java.util.Calendar

/**
 * Frecuencias que ofrece la app, para recurrentes y para la renovación de presupuestos.
 * El modelo admite cualquier unidad e intervalo; esta lista es lo que se puede elegir.
 */
enum class RecurrenceFrequency(val unit: PeriodUnit, val interval: Int) {
    DAILY(PeriodUnit.DAY, 1),
    WEEKLY(PeriodUnit.WEEK, 1),
    BIWEEKLY(PeriodUnit.WEEK, 2),
    MONTHLY(PeriodUnit.MONTH, 1),
    BIMONTHLY(PeriodUnit.MONTH, 2),
    QUARTERLY(PeriodUnit.MONTH, 3),
    SEMIANNUAL(PeriodUnit.MONTH, 6),
    YEARLY(PeriodUnit.YEAR, 1);

    companion object {
        fun of(unit: PeriodUnit, interval: Int): RecurrenceFrequency? =
            entries.firstOrNull { it.unit == unit && it.interval == interval }
    }
}

/**
 * Serie de fechas de un movimiento recurrente: `ancla + k · intervalo` (k = 0, 1, 2…), siempre
 * a medianoche local. Se calcula desde el ancla para que un cargo del día 31 vuelva al 31
 * después de pasar por febrero.
 */
object Recurrence {

    /** Primera ocurrencia en o después de [target]. */
    fun occurrenceAtOrAfter(anchor: Long, unit: PeriodUnit, interval: Int, target: Long): Long {
        val base = CalendarMath.startOfDay(anchor)
        val step = interval.coerceAtLeast(1)
        val k = CalendarMath.firstIndexAtOrAfter(base, unit, step, target)
        return CalendarMath.addUnits(base, unit, step * k)
    }

    /** La ocurrencia siguiente a [occurrence]. */
    fun occurrenceAfter(anchor: Long, unit: PeriodUnit, interval: Int, occurrence: Long): Long =
        occurrenceAtOrAfter(anchor, unit, interval, occurrence + 1)

    /**
     * Primera ocurrencia que aún no ha pasado: hoy o después. Al crear un recurrente con fecha de
     * inicio pasada no se generan pendientes atrasados: el ancla solo fija el día de la serie.
     */
    fun firstUpcoming(anchor: Long, unit: PeriodUnit, interval: Int, now: Long = System.currentTimeMillis()): Long =
        occurrenceAtOrAfter(anchor, unit, interval, CalendarMath.startOfDay(now))

    /** Cuántas ocurrencias hay desde [next] hasta [now], ambas incluidas (con un tope). */
    fun countDue(anchor: Long, unit: PeriodUnit, interval: Int, next: Long, now: Long, cap: Int = 99): Int {
        var count = 0
        var occurrence = next
        while (occurrence <= now && count < cap) {
            count++
            occurrence = occurrenceAfter(anchor, unit, interval, occurrence)
        }
        return count
    }

    /**
     * Fecha del movimiento al registrar una ocurrencia: ahora si vence hoy; si está atrasada,
     * el mediodía de su día, para que caiga en la fecha que le corresponde.
     */
    fun transactionDateFor(occurrence: Long, now: Long = System.currentTimeMillis()): Long {
        if (CalendarMath.startOfDay(now) == CalendarMath.startOfDay(occurrence)) return now
        return Calendar.getInstance().apply {
            timeInMillis = CalendarMath.startOfDay(occurrence)
            set(Calendar.HOUR_OF_DAY, 12)
        }.timeInMillis
    }
}

/** El recurrente sigue en curso: activo, sin borrar y sin haber pasado su fecha de fin. */
val RecurringTransactionEntity.isRunning: Boolean
    get() = active && deletedAt == null && (endDate == null || nextOccurrence <= endDate)

/** Tiene una ocurrencia vencida que espera confirmación y no está pospuesta. */
fun RecurringTransactionEntity.isPending(now: Long = System.currentTimeMillis()): Boolean =
    isRunning && nextOccurrence <= now && (snoozedUntil == null || snoozedUntil <= now)

/** Ocurrencias vencidas sin registrar (la actual y las atrasadas). */
fun RecurringTransactionEntity.dueCount(now: Long = System.currentTimeMillis()): Int {
    if (!isRunning || nextOccurrence > now) return 0
    val limit = endDate?.let { minOf(it, now) } ?: now
    return Recurrence.countDue(anchorDate, periodUnit, periodInterval, nextOccurrence, limit)
}

val RecurringTransactionEntity.frequency: RecurrenceFrequency?
    get() = RecurrenceFrequency.of(periodUnit, periodInterval)
