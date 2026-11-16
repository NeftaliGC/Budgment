package com.nintech.budgment.data.model

/** Porcentajes que se pueden elegir para el aviso previo al límite. */
val BUDGET_WARNING_OPTIONS = listOf(50, 60, 70, 75, 80, 85, 90, 95)

const val DEFAULT_BUDGET_WARNING_PERCENT = 80

/** Nivel que representa haber llegado al límite. */
const val BUDGET_LIMIT_LEVEL = 100

/**
 * Umbral más alto que alcanzó lo gastado: [BUDGET_LIMIT_LEVEL] si llegó al límite,
 * [warningPercent] si pasó del aviso previo, o null si ninguno.
 *
 * Solo se avisa el más alto: si un gasto salta del 50 % al 110 %, llega un único aviso de límite.
 */
fun budgetAlertLevel(spent: Long, limit: Long, warningPercent: Int): Int? {
    if (limit <= 0) return null
    return when {
        spent >= limit -> BUDGET_LIMIT_LEVEL
        spent * 100 >= limit * warningPercent -> warningPercent
        else -> null
    }
}
