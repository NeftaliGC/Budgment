package com.nintech.budgment.data.model

import com.nintech.budgment.data.local.entity.BudgetEntity
import java.util.Calendar
import kotlin.math.roundToLong

/**
 * Un periodo concreto de un presupuesto: de [start] (incluido) a [endExclusive] (excluido),
 * en hora local.
 */
data class BudgetPeriod(
    val start: Long,
    val endExclusive: Long,
    val unit: PeriodUnit,
    val interval: Int = 1
) {
    operator fun contains(millis: Long): Boolean = millis >= start && millis < endExclusive

    /** Días que faltan para que se renueve, contando hoy. Nunca menos de 1. */
    fun daysLeft(now: Long = System.currentTimeMillis()): Int {
        val today = CalendarMath.startOfDay(now)
        return ((endExclusive - today).toDouble() / CalendarMath.DAY_MS).roundToLong().toInt().coerceAtLeast(1)
    }

    /** La frecuencia de la lista que corresponde a este periodo, si es una de ellas. */
    val frequency: RecurrenceFrequency? get() = RecurrenceFrequency.of(unit, interval)

    /** Mismo tipo de periodo (unidad e intervalo) que [other], sin importar las fechas. */
    fun sameKindAs(other: BudgetPeriod): Boolean = unit == other.unit && interval == other.interval

    companion object {
        /**
         * Inicio del periodo de calendario que contiene [millis]: el día, el lunes de esa semana,
         * el día 1 del mes o el 1 de enero. Sirve de ancla al crear un presupuesto.
         */
        fun alignedStart(millis: Long, unit: PeriodUnit): Long = Calendar.getInstance().apply {
            timeInMillis = CalendarMath.startOfDay(millis)
            when (unit) {
                PeriodUnit.DAY -> Unit
                PeriodUnit.WEEK -> {
                    // Semana de lunes a domingo, sin depender del primer día que use el idioma.
                    val sinceMonday = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                    add(Calendar.DAY_OF_MONTH, -sinceMonday)
                }
                PeriodUnit.MONTH -> set(Calendar.DAY_OF_MONTH, 1)
                PeriodUnit.YEAR -> set(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis

        /**
         * Periodo vigente en [now] para un presupuesto anclado en [anchor] que se repite cada
         * [interval] [unit]. Si [now] es anterior al ancla, devuelve el primer periodo.
         */
        fun current(anchor: Long, unit: PeriodUnit, interval: Int, now: Long = System.currentTimeMillis()): BudgetPeriod {
            val step = interval.coerceAtLeast(1)
            val base = alignedStart(anchor, unit)
            if (now < base) return nth(base, unit, step, 0)

            // Índice del periodo que contiene now: el último cuyo inicio es ≤ now.
            val firstAfter = CalendarMath.firstIndexAtOrAfter(base, unit, step, now + 1)
            return nth(base, unit, step, (firstAfter - 1).coerceAtLeast(0))
        }

        private fun nth(base: Long, unit: PeriodUnit, step: Int, k: Int): BudgetPeriod {
            val start = CalendarMath.addUnits(base, unit, step * k)
            return BudgetPeriod(start, CalendarMath.addUnits(start, unit, step), unit, step)
        }
    }
}

/**
 * Periodo vigente del presupuesto. `startDate` es el ancla (inicio del primer periodo); si
 * falta, se ancla en la fecha de creación. `endDate` no se usa: los presupuestos se renuevan
 * indefinidamente.
 */
fun BudgetEntity.currentPeriod(now: Long = System.currentTimeMillis()): BudgetPeriod =
    BudgetPeriod.current(startDate ?: createdAt, periodUnit, periodInterval, now)
