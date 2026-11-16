package com.nintech.budgment.ui.settings

import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.ui.transactions.TransactionWithDetails
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Textos del archivo, para que el cálculo no dependa de los recursos de Android. */
data class CsvLabels(
    /** Fecha, tipo, categoría, cuenta, cuenta destino, monto, moneda y nota, en ese orden. */
    val header: List<String>,
    val expense: String,
    val income: String,
    val transfer: String
)

/**
 * Los movimientos como CSV para una hoja de cálculo: una fila por movimiento, en el orden recibido.
 *
 * - El monto lleva signo (gasto negativo) y punto decimal, sin símbolo ni separador de miles, para
 *   que se pueda sumar. Una transferencia es una sola fila con lo que salió de la cuenta de origen.
 * - Empieza con la marca BOM para que Excel lea bien los acentos.
 * - Un texto que empiece como una fórmula (=, +, -, @) se antecede de un apóstrofo: así una nota
 *   no puede ejecutarse como fórmula al abrir el archivo.
 */
fun buildTransactionsCsv(
    items: List<TransactionWithDetails>,
    labels: CsvLabels,
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { this.timeZone = timeZone }
    val out = StringBuilder("\uFEFF")
    out.appendRow(labels.header.map(::text))
    items.forEach { item ->
        val transfer = item.transfer
        out.appendRow(
            listOf(
                dateFormat.format(Date(item.transaction.date)),
                text(
                    when {
                        transfer != null -> labels.transfer
                        item.categoryType == CategoryType.INCOME -> labels.income
                        else -> labels.expense
                    }
                ),
                text(item.categoryName),
                text(item.accountName),
                text(transfer?.toAccountName.orEmpty()),
                BigDecimal.valueOf(item.transaction.amount, 2).toPlainString(),
                text(item.currency),
                text(item.transaction.description.orEmpty())
            )
        )
    }
    return out.toString()
}

private fun StringBuilder.appendRow(cells: List<String>) {
    append(cells.joinToString(",")).append("\r\n")
}

private val FORMULA_START = setOf('=', '+', '-', '@', '\t', '\r')

private fun text(value: String): String {
    val safe = if (value.firstOrNull() in FORMULA_START) "'$value" else value
    return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + safe.replace("\"", "\"\"") + "\""
    } else safe
}
