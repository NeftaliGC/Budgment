package com.nintech.budgment.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.nintech.budgment.R
import com.nintech.budgment.data.model.GoalStatus
import com.nintech.budgment.ui.goals.GoalCard
import com.nintech.budgment.ui.goals.GoalItem

/**
 * Metas de ahorro en Inicio, en una sección que se pliega. Plegada solo dice cuántas hay y
 * cuántas se alcanzaron; el estado se recuerda entre aperturas de la app.
 */
@Composable
fun GoalsSection(
    goals: List<GoalItem>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onGoalClick: (String) -> Unit,
    onAddGoalClick: () -> Unit
) {
    val reached = goals.count { it.progress.status == GoalStatus.REACHED }
    val stateLabel = stringResource(if (expanded) R.string.common_expanded else R.string.common_collapsed)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = stringResource(if (expanded) R.string.common_hide else R.string.common_show)) {
                onExpandedChange(!expanded)
            }
            .semantics { stateDescription = stateLabel },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.goals_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            if (goals.isNotEmpty()) {
                Text(
                    if (reached > 0) {
                        pluralStringResource(R.plurals.goals_summary_reached, goals.size, goals.size, reached)
                    } else {
                        pluralStringResource(R.plurals.goals_summary, goals.size, goals.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onAddGoalClick) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.goal_form_title_new))
        }
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    AnimatedVisibility(visible = expanded) {
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (goals.isEmpty()) {
                Text(
                    stringResource(R.string.goals_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onAddGoalClick, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text(stringResource(R.string.goals_create_first))
                }
            } else {
                goals.forEach { item ->
                    GoalCard(item = item, onClick = { onGoalClick(item.goal.id) })
                }
            }
        }
    }
}
