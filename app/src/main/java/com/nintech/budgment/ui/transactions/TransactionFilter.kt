package com.nintech.budgment.ui.transactions

import com.nintech.budgment.data.model.CategoryType
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

enum class TransactionKind { EXPENSE, INCOME, TRANSFER }

/** Rangos de fecha que se ofrecen; [CUSTOM] usa las fechas elegidas en el calendario. */
enum class DatePreset { THIS_MONTH, LAST_MONTH, LAST_30_DAYS, THIS_YEAR, CUSTOM }

data class DateFilter(
    val preset: DatePreset,
    /** Solo en [DatePreset.CUSTOM]: medianoche del primer y del último día, ambos incluidos. */
    val customStart: Long? = null,
    val customEnd: Long? = null
) {
    /** Rango [inicio, fin) en milisegundos para la fecha [now]. */
    fun range(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        fun cal(block: Calendar.() -> Unit) = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            block()
        }.timeInMillis
        return when (preset) {
            DatePreset.THIS_MONTH -> cal { set(Calendar.DAY_OF_MONTH, 1) } to cal { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, 1) }
            DatePreset.LAST_MONTH -> cal { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, -1) } to cal { set(Calendar.DAY_OF_MONTH, 1) }
            DatePreset.LAST_30_DAYS -> cal { add(Calendar.DAY_OF_MONTH, -29) } to cal { add(Calendar.DAY_OF_MONTH, 1) }
            DatePreset.THIS_YEAR -> cal { set(Calendar.DAY_OF_YEAR, 1) } to cal { set(Calendar.DAY_OF_YEAR, 1); add(Calendar.YEAR, 1) }
            DatePreset.CUSTOM -> {
                val start = customStart ?: Long.MIN_VALUE
                val end = customEnd?.let { endDay ->
                    Calendar.getInstance().apply { timeInMillis = endDay; add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
                } ?: Long.MAX_VALUE
                start to end
            }
        }
    }
}

data class TransactionFilter(
    val query: String = "",
    val kinds: Set<TransactionKind> = emptySet(),
    val accountIds: Set<String> = emptySet(),
    val categoryIds: Set<String> = emptySet(),
    val date: DateFilter? = null
) {
    /** Filtros de los chips (sin contar el texto de búsqueda). */
    val activeCount: Int
        get() = listOf(kinds.isNotEmpty(), accountIds.isNotEmpty(), categoryIds.isNotEmpty(), date != null).count { it }

    val isActive: Boolean get() = query.isNotBlank() || activeCount > 0
}

fun TransactionWithDetails.kind(): TransactionKind = when {
    transfer != null -> TransactionKind.TRANSFER
    categoryType == CategoryType.INCOME -> TransactionKind.INCOME
    else -> TransactionKind.EXPENSE
}

/**
 * Aplica [filter] conservando el orden. Cada grupo de opciones suma (cualquiera de las cuentas
 * elegidas) y los grupos se combinan entre sí (cuenta Y categoría Y fecha).
 *
 * - Cuentas: una transferencia aparece si sale o llega a alguna de las elegidas.
 * - Categorías: las transferencias no tienen categoría, así que se ocultan al filtrar por una.
 * - Texto: busca en descripción, categoría, cuentas y monto, sin importar acentos ni mayúsculas.
 */
fun applyFilter(
    items: List<TransactionWithDetails>,
    filter: TransactionFilter,
    now: Long = System.currentTimeMillis()
): List<TransactionWithDetails> {
    if (!filter.isActive) return items
    val range = filter.date?.range(now)
    val terms = normalize(filter.query).split(' ').filter { it.isNotBlank() }

    return items.filter { item ->
        val tx = item.transaction
        (filter.kinds.isEmpty() || item.kind() in filter.kinds) &&
            (filter.accountIds.isEmpty() || tx.accountId in filter.accountIds || item.transfer?.toAccountId in filter.accountIds) &&
            (filter.categoryIds.isEmpty() || tx.categoryId in filter.categoryIds) &&
            (range == null || tx.date >= range.first && tx.date < range.second) &&
            (terms.isEmpty() || item.searchText().let { text -> terms.all { it in text } })
    }
}

/** Todo lo que se puede buscar de una fila, ya normalizado. */
private fun TransactionWithDetails.searchText(): String {
    val cents = abs(transaction.amount)
    // El monto se busca como se escribe: «350», «350.5», «350.50» o «1,200».
    val whole = cents / 100
    val fraction = "%02d".format(cents % 100)
    val grouped = String.format(Locale.US, "%,d", whole)
    val amountForms = listOf("$whole", "$whole.$fraction", grouped, "$grouped.$fraction")
    return normalize(
        listOfNotNull(
            transaction.description,
            categoryName,
            accountName,
            transfer?.toAccountName
        ).joinToString(" ") + " " + amountForms.joinToString(" ")
    )
}

/** Minúsculas y sin acentos: «Café» se encuentra escribiendo «cafe». */
internal fun normalize(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .trim()
