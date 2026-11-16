package com.nintech.budgment.ui.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nintech.budgment.R
import com.nintech.budgment.ui.categories.CategoryIcon
import com.nintech.budgment.ui.categories.parseHexColor
import com.nintech.budgment.ui.common.ColorIconTile
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.common.formatShortDate
import com.nintech.budgment.ui.common.isToday
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.theme.tabular

/**
 * Movimientos recurrentes que ya tocan y esperan confirmación. Nada se registra solo: por cada
 * uno el usuario elige registrarlo, dejarlo para más tarde u omitir esta vez.
 *
 * Los pospuestos («Más tarde») no se anuncian hasta mañana, pero quedan plegados al final para
 * que se puedan registrar en cualquier momento.
 */
@Composable
fun PendingRecurringCard(
    items: List<RecurringItem>,
    snoozed: List<RecurringItem>,
    onRegister: (RecurringItem) -> Unit,
    onReview: (RecurringItem) -> Unit,
    onSnooze: (RecurringItem) -> Unit,
    onSkip: (RecurringItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (items.isNotEmpty()) {
                Text(
                    text = pluralStringResource(R.plurals.recurring_pending_title, items.size, items.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.recurring_pending_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                PendingRow(
                    item = item,
                    onRegister = { onRegister(item) },
                    onReview = { onReview(item) },
                    onSnooze = { onSnooze(item) },
                    onSkip = { onSkip(item) }
                )
            }
            if (snoozed.isNotEmpty()) {
                SnoozedSection(
                    items = snoozed,
                    hasPendingAbove = items.isNotEmpty(),
                    onRegister = onRegister,
                    onReview = onReview,
                    onSkip = onSkip
                )
            }
        }
    }
}

/** Pospuestos, plegados: un toque los muestra con su botón Registrar. */
@Composable
private fun SnoozedSection(
    items: List<RecurringItem>,
    hasPendingAbove: Boolean,
    onRegister: (RecurringItem) -> Unit,
    onReview: (RecurringItem) -> Unit,
    onSkip: (RecurringItem) -> Unit
) {
    // Si solo hay pospuestos, se muestran abiertos: la tarjeta existe por ellos.
    var expanded by rememberSaveable { mutableStateOf(!hasPendingAbove) }

    if (hasPendingAbove) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            modifier = Modifier.padding(top = 12.dp)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(top = if (hasPendingAbove) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = pluralStringResource(R.plurals.recurring_snoozed_title, items.size, items.size),
            style = if (hasPendingAbove) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
            color = if (hasPendingAbove) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = stringResource(if (expanded) R.string.common_hide else R.string.common_show),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (expanded) {
        Text(
            text = stringResource(R.string.recurring_snoozed_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        items.forEach { item ->
            PendingRow(
                item = item,
                onRegister = { onRegister(item) },
                onReview = { onReview(item) },
                // Ya está pospuesto: no se ofrece «Más tarde» otra vez.
                onSnooze = null,
                onSkip = { onSkip(item) }
            )
        }
    }
}

@Composable
private fun PendingRow(
    item: RecurringItem,
    onRegister: () -> Unit,
    onReview: () -> Unit,
    onSnooze: (() -> Unit)?,
    onSkip: () -> Unit
) {
    val occurrence = item.recurring.nextOccurrence
    val whenText = if (isToday(occurrence)) {
        stringResource(R.string.recurring_due_today)
    } else {
        stringResource(R.string.recurring_due_since_format, formatShortDate(occurrence))
    }
    val amountColor = if (item.isExpense) MaterialTheme.colorScheme.error else BudgmentTheme.extraColors.success
    val amountText = (if (item.isExpense) "" else "+") + formatCents(item.recurring.baseAmount, item.currency)

    Column(Modifier.padding(top = 12.dp)) {
        // Tocar la fila abre el formulario prellenado, por si el monto o la fecha cambiaron.
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onReview),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorIconTile(
                color = parseHexColor(item.categoryColor) ?: MaterialTheme.colorScheme.primary,
                icon = CategoryIcon.resolveIcon(item.categoryIcon),
                size = 36.dp,
                cornerRadius = 10.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${item.accountName} · $whenText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.dueCount > 1) {
                    Text(
                        pluralStringResource(R.plurals.recurring_more_due, item.dueCount - 1, item.dueCount - 1),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(amountText, style = MaterialTheme.typography.titleMedium.tabular, color = amountColor)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoreActions(onReview = onReview, onSkip = onSkip)
            Spacer(Modifier.weight(1f))
            if (onSnooze != null) {
                TextButton(onClick = onSnooze) { Text(stringResource(R.string.recurring_snooze)) }
                Spacer(Modifier.width(8.dp))
            }
            FilledTonalButton(onClick = onRegister) { Text(stringResource(R.string.recurring_register)) }
        }
    }
}

@Composable
private fun MoreActions(onReview: () -> Unit, onSkip: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.common_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.recurring_review)) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = {
                    expanded = false
                    onReview()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.recurring_skip)) },
                leadingIcon = { Icon(Icons.Filled.SkipNext, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSkip()
                }
            )
        }
    }
}
