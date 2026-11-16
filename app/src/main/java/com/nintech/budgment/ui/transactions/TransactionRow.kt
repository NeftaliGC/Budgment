package com.nintech.budgment.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nintech.budgment.R
import com.nintech.budgment.data.model.CategoryType
import com.nintech.budgment.ui.categories.CategoryIcon
import com.nintech.budgment.ui.categories.parseHexColor
import com.nintech.budgment.ui.common.ColorIconTile
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.common.formatDate
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.theme.tabular

/**
 * Fila de un movimiento. La comparten la lista de Movimientos y el resumen de Inicio.
 *
 * Los movimientos de una cuenta eliminada son de solo lectura: no reaccionan al toque.
 * [trailing] es el hueco para el menú de acciones; si no hay, el monto queda al borde.
 */
@Composable
fun TransactionRow(
    item: TransactionWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** En la lista agrupada por día la fecha ya está en el encabezado. */
    showDate: Boolean = true,
    trailing: @Composable (() -> Unit)? = null
) {
    val isExpense = item.categoryType == CategoryType.EXPENSE
    // Gasto en rojo e ingreso en verde, y además con signo: el color no es la única pista.
    val amountColor = if (isExpense) MaterialTheme.colorScheme.error else BudgmentTheme.extraColors.success
    val amountText = (if (isExpense) "" else "+") + formatCents(item.transaction.amount, item.currency)

    val categoryLabel = if (item.categoryDeleted) {
        stringResource(R.string.transactions_deleted_category_format, item.categoryName)
    } else {
        item.categoryName
    }
    val accountLabel = if (item.accountDeleted) {
        stringResource(R.string.transactions_deleted_account_format, item.accountName)
    } else {
        item.accountName
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !item.accountDeleted, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorIconTile(
            color = parseHexColor(item.categoryColor) ?: MaterialTheme.colorScheme.primary,
            icon = CategoryIcon.resolveIcon(item.categoryIcon),
            size = 36.dp,
            cornerRadius = 10.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = categoryLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (showDate) "$accountLabel · ${formatDate(item.transaction.date)}" else accountLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = amountText,
            style = MaterialTheme.typography.titleMedium.tabular,
            color = amountColor
        )
        trailing?.invoke()
    }
}
