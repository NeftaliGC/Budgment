package com.nintech.budgment.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.data.model.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun SectionHeader(title: String, description: String? = null) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    if (description != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Apariencia

/** Tema claro, oscuro o el del teléfono. */
@Composable
fun AppearanceSection(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        SectionHeader(stringResource(R.string.settings_appearance_title))
    }
    Spacer(Modifier.height(8.dp))
    Column(Modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            val selected = mode == themeMode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onThemeModeChange(mode) })
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected, onClick = null)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(
                        when (mode) {
                            ThemeMode.SYSTEM -> R.string.settings_theme_system
                            ThemeMode.LIGHT -> R.string.settings_theme_light
                            ThemeMode.DARK -> R.string.settings_theme_dark
                        }
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// Exportar

/** Exportar los movimientos a un archivo CSV. */
@Composable
fun ExportSection(viewModel: ExportViewModel = hiltViewModel()) {
    val step by viewModel.step.collectAsState()
    val labels = CsvLabels(
        header = listOf(
            stringResource(R.string.csv_column_date),
            stringResource(R.string.csv_column_type),
            stringResource(R.string.csv_column_category),
            stringResource(R.string.csv_column_account),
            stringResource(R.string.csv_column_to_account),
            stringResource(R.string.csv_column_amount),
            stringResource(R.string.csv_column_currency),
            stringResource(R.string.csv_column_note)
        ),
        expense = stringResource(R.string.csv_type_expense),
        income = stringResource(R.string.csv_type_income),
        transfer = stringResource(R.string.csv_type_transfer)
    )
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        viewModel.onFilePicked(uri, labels)
    }
    val fileName = stringResource(
        R.string.export_file_name,
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    )

    ExportSectionContent(busy = step == ExportStep.Working, onExportClick = { createFile.launch(fileName) })

    when (val current = step) {
        is ExportStep.Done -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.export_done_title)) },
            text = { Text(pluralStringResource(R.plurals.export_done_text, current.count, current.count)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.common_understood)) }
            }
        )
        ExportStep.Failed -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.export_failed_title)) },
            text = { Text(stringResource(R.string.backup_export_failed_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.common_understood)) }
            }
        )
        else -> Unit
    }
}

@Composable
fun ExportSectionContent(busy: Boolean, onExportClick: () -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        SectionHeader(
            title = stringResource(R.string.settings_export_title),
            description = stringResource(R.string.settings_export_description)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onExportClick, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_export_button))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_export_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Cuenta

/** Cambiar la contraseña y eliminar la cuenta. */
@Composable
fun AccountSection(viewModel: AccountSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    AccountSectionContent(
        onChangePasswordClick = { viewModel.open(AccountDialog.CHANGE_PASSWORD) },
        onDeleteUserClick = { viewModel.open(AccountDialog.DELETE_USER) }
    )

    when (state.dialog) {
        AccountDialog.NONE -> Unit
        AccountDialog.CHANGE_PASSWORD -> ChangePasswordDialog(
            state = state,
            onEdited = viewModel::onPasswordEdited,
            onConfirm = viewModel::changePassword,
            onDismiss = viewModel::dismiss
        )
        AccountDialog.PASSWORD_CHANGED -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.account_password_changed_title)) },
            text = { Text(stringResource(R.string.account_password_changed_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.common_understood)) }
            }
        )
        AccountDialog.DELETE_USER -> DeleteUserDialog(
            state = state,
            onEdited = viewModel::onPasswordEdited,
            onConfirm = viewModel::deleteUser,
            onDismiss = viewModel::dismiss
        )
    }
}

@Composable
fun AccountSectionContent(onChangePasswordClick: () -> Unit, onDeleteUserClick: () -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        SectionHeader(stringResource(R.string.settings_account_title))
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_change_password),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onChangePasswordClick)
                .padding(vertical = 12.dp)
        )
        Text(
            text = stringResource(R.string.settings_delete_user),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onDeleteUserClick)
                .padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun PasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    error: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ChangePasswordDialog(
    state: AccountSettingsState,
    onEdited: () -> Unit,
    onConfirm: (current: String, new: String) -> Unit,
    onDismiss: () -> Unit
) {
    var current by rememberSaveable { mutableStateOf("") }
    var new by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    // El error de que no coinciden aparece al intentar guardar, no mientras se escribe.
    var submitted by rememberSaveable { mutableStateOf(false) }
    val mismatch = new != confirmation

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_change_password)) },
        text = {
            Column {
                PasswordInput(
                    value = current,
                    onValueChange = { current = it; onEdited() },
                    label = stringResource(R.string.account_current_password),
                    enabled = !state.working,
                    error = if (state.wrongPassword) stringResource(R.string.lock_recovery_wrong) else null
                )
                Spacer(Modifier.height(8.dp))
                PasswordInput(
                    value = new,
                    onValueChange = { new = it },
                    label = stringResource(R.string.account_new_password),
                    enabled = !state.working
                )
                Spacer(Modifier.height(8.dp))
                PasswordInput(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = stringResource(R.string.account_confirm_new_password),
                    enabled = !state.working,
                    error = when {
                        submitted && mismatch -> stringResource(R.string.auth_error_passwords_do_not_match)
                        state.failed -> stringResource(R.string.account_error_generic)
                        else -> null
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    submitted = true
                    if (!mismatch) onConfirm(current, new)
                },
                enabled = !state.working && current.isNotEmpty() && new.isNotBlank()
            ) {
                Text(stringResource(R.string.account_change_password_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.working) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun DeleteUserDialog(
    state: AccountSettingsState,
    onEdited: () -> Unit,
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_delete_title)) },
        text = {
            Column {
                Text(stringResource(R.string.account_delete_text))
                Spacer(Modifier.height(16.dp))
                PasswordInput(
                    value = password,
                    onValueChange = { password = it; onEdited() },
                    label = stringResource(R.string.lock_recovery_password),
                    enabled = !state.working,
                    error = when {
                        state.wrongPassword -> stringResource(R.string.lock_recovery_wrong)
                        state.failed -> stringResource(R.string.account_error_generic)
                        else -> null
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }, enabled = !state.working && password.isNotEmpty()) {
                Text(stringResource(R.string.account_delete_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.working) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
