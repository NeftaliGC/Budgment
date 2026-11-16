package com.nintech.budgment.ui.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.AccountEntity
import com.nintech.budgment.data.local.entity.GoalEntity
import com.nintech.budgment.data.model.GoalProgress
import com.nintech.budgment.data.model.GoalStatus
import com.nintech.budgment.data.model.SupportedCurrency
import com.nintech.budgment.data.model.goalProgress
import com.nintech.budgment.ui.categories.CategoryIcon
import com.nintech.budgment.ui.categories.parseHexColor
import com.nintech.budgment.ui.common.ColorIconTile
import com.nintech.budgment.ui.common.formatCents
import com.nintech.budgment.ui.common.formatShortDate
import com.nintech.budgment.ui.theme.BudgmentTheme
import com.nintech.budgment.ui.theme.tabular
import kotlin.math.roundToInt

/** Íconos que tienen sentido para una meta; se reutilizan los de categorías. */
val GoalIcons = listOf(
    CategoryIcon.SAVINGS, CategoryIcon.TRAVEL, CategoryIcon.HOME, CategoryIcon.TRANSPORT,
    CategoryIcon.SCHOOL, CategoryIcon.HEALTH, CategoryIcon.PHONE, CategoryIcon.GAMES,
    CategoryIcon.PETS, CategoryIcon.ENTERTAINMENT, CategoryIcon.INVESTMENT, CategoryIcon.BANK
)

data class GoalItem(
    val goal: GoalEntity,
    val currency: String,
    val progress: GoalProgress,
    /** Cuenta vinculada (aunque esté eliminada); null en metas con aportes manuales. */
    val account: AccountEntity? = null
) {
    val isLinked: Boolean get() = goal.accountId != null

    /** La cuenta vinculada se eliminó o no existe: el avance ya no se actualiza. */
    val accountMissing: Boolean get() = isLinked && (account == null || account.deletedAt != null)
}

/**
 * Arma el avance de cada meta: el saldo de la cuenta vinculada o la suma de los aportes.
 * Primero las que siguen en curso (las de fecha más cercana arriba) y al final las alcanzadas.
 */
fun buildGoalItems(goals: List<GoalEntity>, accounts: List<AccountEntity>, now: Long = System.currentTimeMillis()): List<GoalItem> {
    val accountsById = accounts.associateBy { it.id }
    return goals
        .filter { it.deletedAt == null }
        .map { goal ->
            val account = goal.accountId?.let { accountsById[it] }
            val saved = if (goal.accountId != null) account?.balanceCache ?: 0L else goal.currentAmount
            GoalItem(
                goal = goal,
                currency = account?.currency ?: goal.currency ?: SupportedCurrency.MXN.code,
                progress = goalProgress(saved, goal.targetAmount, goal.targetDate, now),
                account = account
            )
        }
        .sortedWith(
            compareBy<GoalItem> { it.progress.status == GoalStatus.REACHED }
                .thenBy { it.goal.targetDate ?: Long.MAX_VALUE }
                .thenBy { it.goal.name.lowercase() }
        )
}

@Composable
fun GoalItem.accentColor(): Color = parseHexColor(goal.color) ?: MaterialTheme.colorScheme.primary

/** Qué falta y cómo llegar: «Faltan $18,000 · aparta $3,000 al mes hasta el 15 jun». */
@Composable
fun GoalItem.planText(): String {
    val remaining = formatCents(progress.remaining, currency)
    return when (progress.status) {
        GoalStatus.REACHED -> stringResource(R.string.goal_status_reached)
        GoalStatus.NO_DATE -> stringResource(R.string.goal_status_remaining, remaining)
        GoalStatus.OVERDUE -> stringResource(R.string.goal_status_overdue, formatShortDate(goal.targetDate ?: 0), remaining)
        GoalStatus.ON_TRACK -> stringResource(
            R.string.goal_status_on_track,
            remaining,
            formatCents(progress.monthlyNeeded ?: 0, currency),
            formatShortDate(goal.targetDate ?: 0)
        )
    }
}

/** «Saldo de Ahorro» o «Aportes manuales». */
@Composable
fun GoalItem.sourceText(): String = when {
    accountMissing -> stringResource(R.string.goal_source_account_missing)
    isLinked -> stringResource(R.string.goal_source_account, account?.name.orEmpty())
    else -> stringResource(R.string.goal_source_manual)
}

/** Tarjeta de una meta: nombre, barra de avance, cifras y qué falta. */
@Composable
fun GoalCard(item: GoalItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = item.accentColor()
    val percent = (item.progress.fraction * 100).roundToInt()
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorIconTile(
                    color = color,
                    icon = CategoryIcon.resolveIcon(item.goal.icon) ?: CategoryIcon.SAVINGS.icon,
                    size = 36.dp,
                    cornerRadius = 10.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.goal.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        item.sourceText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.accountMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    stringResource(R.string.stats_percent_format, percent),
                    style = MaterialTheme.typography.labelLarge.tabular,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { item.progress.fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (item.progress.status == GoalStatus.REACHED) BudgmentTheme.extraColors.success else color,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    R.string.goal_amount_of_format,
                    formatCents(item.progress.saved, item.currency),
                    formatCents(item.progress.target, item.currency)
                ),
                style = MaterialTheme.typography.bodyMedium.tabular,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                item.planText(),
                style = MaterialTheme.typography.bodySmall,
                color = when (item.progress.status) {
                    GoalStatus.OVERDUE -> MaterialTheme.colorScheme.error
                    GoalStatus.REACHED -> BudgmentTheme.extraColors.success
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
