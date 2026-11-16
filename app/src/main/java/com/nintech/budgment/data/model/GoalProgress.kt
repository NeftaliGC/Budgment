package com.nintech.budgment.data.model

import java.util.Calendar

enum class GoalStatus {
    /** Ya se juntó el monto objetivo. */
    REACHED,
    /** Tiene fecha y aún hay tiempo: se sabe cuánto apartar al mes. */
    ON_TRACK,
    /** La fecha ya pasó sin llegar al objetivo. */
    OVERDUE,
    /** Sin fecha objetivo. */
    NO_DATE
}

data class GoalProgress(
    val saved: Long,
    val target: Long,
    val status: GoalStatus,
    /** Cuánto apartar cada mes para llegar a tiempo; solo en [GoalStatus.ON_TRACK]. */
    val monthlyNeeded: Long? = null
) {
    val remaining: Long get() = (target - saved).coerceAtLeast(0)

    /** Avance de 0 a 1; lo retirado por debajo de cero cuenta como cero. */
    val fraction: Float get() = if (target > 0) (saved.toFloat() / target).coerceIn(0f, 1f) else 0f
}

/**
 * Calcula el avance de una meta. Con fecha objetivo, reparte lo que falta entre los meses que
 * quedan (contando el mes en curso como parcial, redondeado hacia arriba).
 */
fun goalProgress(saved: Long, target: Long, targetDate: Long?, now: Long = System.currentTimeMillis()): GoalProgress {
    if (target > 0 && saved >= target) return GoalProgress(saved, target, GoalStatus.REACHED)
    if (targetDate == null) return GoalProgress(saved, target, GoalStatus.NO_DATE)
    if (targetDate < CalendarMath.startOfDay(now)) return GoalProgress(saved, target, GoalStatus.OVERDUE)

    val remaining = (target - saved).coerceAtLeast(0)
    val months = monthsUntil(now, targetDate)
    // Redondeo hacia arriba: apartar un centavo de más es mejor que quedarse corto.
    val monthly = (remaining + months - 1) / months
    return GoalProgress(saved, target, GoalStatus.ON_TRACK, monthlyNeeded = monthly)
}

/** Meses que quedan hasta [target], redondeando hacia arriba; al menos 1. */
fun monthsUntil(now: Long, target: Long): Int {
    val from = Calendar.getInstance().apply { timeInMillis = now }
    val to = Calendar.getInstance().apply { timeInMillis = target }
    var months = (to.get(Calendar.YEAR) - from.get(Calendar.YEAR)) * 12 +
        (to.get(Calendar.MONTH) - from.get(Calendar.MONTH))
    if (to.get(Calendar.DAY_OF_MONTH) > from.get(Calendar.DAY_OF_MONTH)) months += 1
    return months.coerceAtLeast(1)
}
