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
        val today = startOfDay(now)
        return ((endExclusive - today).toDouble() / DAY_MS).roundToLong().toInt().coerceAtLeast(1)
    }

    /** Mismo tipo de periodo (unidad e intervalo) que [other], sin importar las fechas. */
    fun sameKindAs(other: BudgetPeriod): Boolean = unit == other.unit && interval == other.interval

    companion object {
        private const val DAY_MS = 24 * 60 * 60 * 1000.0

        /**
         * Inicio del periodo de calendario que contiene [millis]: el día, el lunes de esa semana,
         * el día 1 del mes o el 1 de enero. Sirve de ancla al crear un presupuesto.
         */
        fun alignedStart(millis: Long, unit: PeriodUnit): Long = Calendar.getInstance().apply {
            timeInMillis = startOfDay(millis)
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

            // Estimación directa del número de periodos transcurridos; luego se corrige el borde.
            val baseCal = Calendar.getInstance().apply { timeInMillis = base }
            val nowCal = Calendar.getInstance().apply { timeInMillis = now }
            val elapsedUnits = when (unit) {
                PeriodUnit.DAY -> daysBetween(base, now)
                PeriodUnit.WEEK -> daysBetween(base, now) / 7
                PeriodUnit.MONTH ->
                    (nowCal.get(Calendar.YEAR) - baseCal.get(Calendar.YEAR)) * 12L +
                        (nowCal.get(Calendar.MONTH) - baseCal.get(Calendar.MONTH))
                PeriodUnit.YEAR -> (nowCal.get(Calendar.YEAR) - baseCal.get(Calendar.YEAR)).toLong()
            }
            var k = (elapsedUnits / step).toInt().coerceAtLeast(0)
            var period = nth(base, unit, step, k)
            while (now < period.start && k > 0) period = nth(base, unit, step, --k)
            while (now >= period.endExclusive) period = nth(base, unit, step, ++k)
            return period
        }

        private fun nth(base: Long, unit: PeriodUnit, step: Int, k: Int): BudgetPeriod {
            val start = addUnits(base, unit, step * k)
            return BudgetPeriod(start, addUnits(start, unit, step), unit, step)
        }

        private fun addUnits(millis: Long, unit: PeriodUnit, amount: Int): Long = Calendar.getInstance().apply {
            timeInMillis = millis
            when (unit) {
                PeriodUnit.DAY -> add(Calendar.DAY_OF_MONTH, amount)
                PeriodUnit.WEEK -> add(Calendar.DAY_OF_MONTH, amount * 7)
                PeriodUnit.MONTH -> add(Calendar.MONTH, amount)
                PeriodUnit.YEAR -> add(Calendar.YEAR, amount)
            }
        }.timeInMillis

        /** Días de calendario entre dos instantes; redondea para absorber los cambios de horario. */
        private fun daysBetween(from: Long, to: Long): Long =
            ((startOfDay(to) - startOfDay(from)) / DAY_MS).roundToLong()

        private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

/**
 * Periodo vigente del presupuesto. `startDate` es el ancla (inicio del primer periodo); si
 * falta, se ancla en la fecha de creación. `endDate` no se usa: los presupuestos se renuevan
 * indefinidamente.
 */
fun BudgetEntity.currentPeriod(now: Long = System.currentTimeMillis()): BudgetPeriod =
    BudgetPeriod.current(startDate ?: createdAt, periodUnit, periodInterval, now)
