package com.nintech.budgment.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val esMx = Locale("es", "MX")

/** «nov» para seis meses; con doce solo cabe la inicial: «N». */
fun monthLabel(monthStart: Long, compact: Boolean): String {
    val name = SimpleDateFormat("MMM", esMx).format(Date(monthStart)).trimEnd('.')
    return if (compact) name.take(1).uppercase(esMx) else name.replaceFirstChar { it.titlecase(esMx) }
}

/**
 * Barras agrupadas de ingresos y gastos por mes. El mes elegido se resalta en su etiqueta;
 * los demás quedan atenuados para dar contexto.
 */
@Composable
fun IncomeExpenseChart(
    flows: List<MonthFlow>,
    selectedMonthStart: Long?,
    incomeColor: Color,
    expenseColor: Color,
    description: String,
    modifier: Modifier = Modifier
) {
    val maxValue = flows.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1) ?: 1
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val compact = flows.size > 6

    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .semantics { contentDescription = description }
        ) {
            val slot = size.width / flows.size
            val barWidth = (slot * 0.3f).coerceAtMost(14.dp.toPx())
            val gap = 2.dp.toPx()
            val radius = CornerRadius(3.dp.toPx())

            drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            flows.forEachIndexed { index, flow ->
                val center = slot * index + slot / 2
                val dim = selectedMonthStart != null && flow.monthStart != selectedMonthStart
                val alpha = if (dim) 0.45f else 1f
                val incomeHeight = size.height * flow.income / maxValue
                val expenseHeight = size.height * flow.expense / maxValue
                if (incomeHeight > 0) {
                    drawRoundRect(
                        color = incomeColor.copy(alpha = alpha),
                        topLeft = Offset(center - gap / 2 - barWidth, size.height - incomeHeight),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = radius
                    )
                }
                if (expenseHeight > 0) {
                    drawRoundRect(
                        color = expenseColor.copy(alpha = alpha),
                        topLeft = Offset(center + gap / 2, size.height - expenseHeight),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = radius
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        MonthLabels(flows.map { it.monthStart }, selectedMonthStart, compact)
    }
}

/** Línea del saldo total al cierre de cada mes. Si cruza el cero, se marca la línea base. */
@Composable
fun BalanceLineChart(
    points: List<BalancePoint>,
    totalMonths: List<Long>,
    lineColor: Color,
    description: String,
    modifier: Modifier = Modifier
) {
    val zeroColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val pointFill = MaterialTheme.colorScheme.surface

    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = description }
        ) {
            if (points.isEmpty()) return@Canvas
            val values = points.map { it.balance }
            val max = maxOf(values.max(), 0L)
            val min = minOf(values.min(), 0L)
            val span = (max - min).coerceAtLeast(1)
            val padding = 6.dp.toPx()
            val usable = size.height - padding * 2
            val slot = size.width / totalMonths.size

            fun y(value: Long) = padding + usable * (max - value) / span
            fun x(monthStart: Long) = slot * totalMonths.indexOf(monthStart).coerceAtLeast(0) + slot / 2

            if (min < 0) {
                drawLine(
                    zeroColor, Offset(0f, y(0)), Offset(size.width, y(0)),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                )
            }
            val path = Path()
            points.forEachIndexed { i, point ->
                val p = Offset(x(point.monthStart), y(point.balance))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(
                path, lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            points.forEach { point ->
                val center = Offset(x(point.monthStart), y(point.balance))
                drawCircle(pointFill, radius = 4.dp.toPx(), center = center)
                drawCircle(lineColor, radius = 4.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
            }
        }
        Spacer(Modifier.height(6.dp))
        MonthLabels(totalMonths, selected = points.lastOrNull()?.monthStart, compact = totalMonths.size > 6)
    }
}

@Composable
private fun MonthLabels(months: List<Long>, selected: Long?, compact: Boolean) {
    Row(Modifier.fillMaxWidth()) {
        months.forEach { month ->
            val isSelected = month == selected
            Text(
                text = monthLabel(month, compact),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
