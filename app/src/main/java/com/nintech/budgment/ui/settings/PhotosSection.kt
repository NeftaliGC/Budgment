package com.nintech.budgment.ui.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.attachments.AttachmentPolicy

/** Cuánto tiempo se conservan las fotos de recibos y cuánto ocupan ahora. */
@Composable
fun PhotosSection(viewModel: PhotosSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    PhotosSectionContent(state = state, onRetentionSelected = viewModel::onRetentionSelected)

    if (state.pendingMonths != null) {
        AlertDialog(
            onDismissRequest = viewModel::onDeletionDismissed,
            title = { Text(stringResource(R.string.settings_photos_confirm_title)) },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.settings_photos_confirm_text, state.pendingDeletions, state.pendingDeletions
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onDeletionConfirmed) {
                    Text(stringResource(R.string.settings_photos_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeletionDismissed) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
fun PhotosSectionContent(state: PhotosSettingsState, onRetentionSelected: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = stringResource(R.string.settings_photos_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_photos_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings_photos_retention),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Column(Modifier.selectableGroup()) {
        AttachmentPolicy.RETENTION_OPTIONS_MONTHS.forEach { months ->
            val selected = months == state.retentionMonths
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onRetentionSelected(months) })
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected, onClick = null)
                Spacer(Modifier.width(16.dp))
                Text(retentionLabel(months), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = if (state.usage.count == 0) {
            stringResource(R.string.settings_photos_usage_none)
        } else {
            pluralStringResource(
                R.plurals.settings_photos_usage,
                state.usage.count,
                state.usage.count,
                Formatter.formatShortFileSize(LocalContext.current, state.usage.bytes)
            )
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

@Composable
private fun retentionLabel(months: Int): String = when (months) {
    AttachmentPolicy.KEEP_FOREVER -> stringResource(R.string.settings_photos_forever)
    12 -> stringResource(R.string.settings_photos_one_year)
    else -> pluralStringResource(R.plurals.settings_photos_months, months, months)
}
