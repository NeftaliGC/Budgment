package com.nintech.budgment.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import android.text.format.Formatter
import com.nintech.budgment.R
import com.nintech.budgment.data.attachments.AttachmentUsage
import com.nintech.budgment.data.backup.BackupCrypto
import com.nintech.budgment.data.backup.BackupError
import com.nintech.budgment.data.backup.RestorePreview
import com.nintech.budgment.ui.common.formatDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Tipo genérico: «.budgment» no tiene un tipo propio y así ningún proveedor le cambia la extensión.
private const val BACKUP_MIME_TYPE = "application/octet-stream"

/** Crear un respaldo en un archivo cifrado y restaurarlo. */
@Composable
fun BackupSection(viewModel: BackupViewModel = hiltViewModel()) {
    val step by viewModel.step.collectAsState()
    val unlocked by viewModel.unlocked.collectAsState()

    val createFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
        viewModel::onExportFilePicked
    )
    val openFile = rememberRestoreFilePicker(viewModel)
    val fileName = stringResource(
        R.string.backup_file_name,
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    )

    BackupSectionContent(
        busy = step != BackupStep.Idle,
        onCreateClick = viewModel::onCreateClick,
        onRestoreClick = openFile
    )

    // Al volver del selector de archivos la app puede quedar bloqueada: los diálogos esperan.
    if (unlocked) {
        BackupDialogs(
            step = step,
            viewModel = viewModel,
            onExportPasswordChosen = { password, includePhotos ->
                viewModel.onExportPasswordChosen(password, includePhotos)
                createFile.launch(fileName)
            }
        )
    }
}

/**
 * Restaurar desde la pantalla de inicio de sesión: en una instalación nueva no hay ningún
 * usuario con el que entrar a Configuración. Aquí no aplica el bloqueo de la app.
 */
@Composable
fun RestoreBackupButton(modifier: Modifier = Modifier, viewModel: BackupViewModel = hiltViewModel()) {
    val step by viewModel.step.collectAsState()
    val openFile = rememberRestoreFilePicker(viewModel)

    TextButton(onClick = openFile, enabled = step == BackupStep.Idle, modifier = modifier) {
        Text(stringResource(R.string.settings_backup_restore))
    }
    BackupDialogs(step = step, viewModel = viewModel, onExportPasswordChosen = { _, _ -> })
}

@Composable
private fun rememberRestoreFilePicker(viewModel: BackupViewModel): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
        viewModel::onRestoreFilePicked
    )
    // Cualquier tipo: los proveedores (Drive, Descargas…) no se ponen de acuerdo en el de un
    // archivo «.budgment»; si no es un respaldo, se avisa al abrirlo.
    return { launcher.launch(arrayOf("*/*")) }
}

@Composable
private fun BackupDialogs(
    step: BackupStep,
    viewModel: BackupViewModel,
    onExportPasswordChosen: (password: String, includePhotos: Boolean) -> Unit
) {
    when (step) {
        BackupStep.Idle -> Unit
        is BackupStep.ExportPassword -> NewPasswordDialog(
            photos = step.photos,
            onConfirm = onExportPasswordChosen,
            onDismiss = viewModel::dismiss
        )
        BackupStep.Exporting -> ProgressDialog(stringResource(R.string.backup_exporting))
        BackupStep.ExportDone -> MessageDialog(
            title = stringResource(R.string.backup_export_done_title),
            text = stringResource(R.string.backup_export_done_text),
            onDismiss = viewModel::dismiss
        )
        BackupStep.ExportFailed -> MessageDialog(
            title = stringResource(R.string.backup_export_failed_title),
            text = stringResource(R.string.backup_export_failed_text),
            onDismiss = viewModel::dismiss
        )
        is BackupStep.RestorePassword -> RestorePasswordDialog(
            step = step,
            onConfirm = viewModel::onRestorePasswordEntered,
            onDismiss = viewModel::dismiss
        )
        is BackupStep.RestoreConfirm -> RestoreConfirmDialog(
            preview = step.preview,
            onConfirm = viewModel::onRestoreConfirmed,
            onDismiss = viewModel::dismiss
        )
        BackupStep.Restoring -> ProgressDialog(stringResource(R.string.backup_restoring))
        is BackupStep.RestoreFailed -> MessageDialog(
            title = stringResource(R.string.backup_restore_failed_title),
            text = stringResource(
                when (step.error) {
                    BackupError.NOT_A_BACKUP -> R.string.backup_error_not_a_backup
                    BackupError.NEWER_VERSION -> R.string.backup_error_newer_version
                    BackupError.WRONG_PASSWORD -> R.string.backup_restore_wrong_password
                    BackupError.CORRUPT -> R.string.backup_error_corrupt
                    null -> R.string.backup_error_read
                }
            ),
            onDismiss = viewModel::dismiss
        )
    }
}

@Composable
fun BackupSectionContent(
    busy: Boolean,
    onCreateClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = stringResource(R.string.settings_backup_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_backup_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCreateClick, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_backup_create))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRestoreClick, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_backup_restore))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_backup_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onVisibleChange: ((Boolean) -> Unit)?,
    error: String?,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        trailingIcon = onVisibleChange?.let { toggle ->
            {
                TextButton(onClick = { toggle(!visible) }) {
                    Text(
                        text = stringResource(
                            if (visible) R.string.common_password_hide else R.string.common_password_show
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun NewPasswordDialog(
    photos: AttachmentUsage,
    onConfirm: (password: String, includePhotos: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var includePhotos by rememberSaveable { mutableStateOf(true) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }
    // Los errores aparecen al intentar continuar, no mientras se escribe.
    var submitted by rememberSaveable { mutableStateOf(false) }

    val tooShort = password.length < BackupCrypto.MIN_PASSWORD_LENGTH
    val mismatch = password != confirmation

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_password_title)) },
        text = {
            Column {
                Text(stringResource(R.string.backup_password_text))
                Spacer(Modifier.height(16.dp))
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.backup_password_label),
                    visible = visible,
                    onVisibleChange = { visible = it },
                    error = if (submitted && tooShort) {
                        stringResource(R.string.backup_password_too_short, BackupCrypto.MIN_PASSWORD_LENGTH)
                    } else null,
                    imeAction = ImeAction.Next
                )
                Spacer(Modifier.height(8.dp))
                PasswordField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = stringResource(R.string.backup_password_confirm_label),
                    visible = visible,
                    onVisibleChange = null,
                    error = if (submitted && !tooShort && mismatch) {
                        stringResource(R.string.auth_error_passwords_do_not_match)
                    } else null
                )
                // Sin fotos el respaldo pesa mucho menos; la opción solo aparece si hay alguna.
                if (photos.count > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(value = includePhotos, role = Role.Checkbox, onValueChange = { includePhotos = it })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = includePhotos, onCheckedChange = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(
                                R.string.backup_include_photos,
                                Formatter.formatShortFileSize(LocalContext.current, photos.bytes)
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                submitted = true
                if (!tooShort && !mismatch) onConfirm(password, includePhotos && photos.count > 0)
            }) {
                Text(stringResource(R.string.backup_password_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun RestorePasswordDialog(
    step: BackupStep.RestorePassword,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        // Mientras se descifra no se cierra tocando fuera.
        onDismissRequest = { if (!step.checking) onDismiss() },
        title = { Text(stringResource(R.string.backup_restore_title)) },
        text = {
            Column {
                Text(stringResource(R.string.backup_restore_password_text, formatDate(step.createdAt)))
                Spacer(Modifier.height(16.dp))
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.backup_password_label),
                    visible = visible,
                    onVisibleChange = { visible = it },
                    error = if (step.wrongPassword) stringResource(R.string.backup_restore_wrong_password) else null,
                    enabled = !step.checking
                )
                if (step.checking) {
                    Spacer(Modifier.height(12.dp))
                    ProgressRow(stringResource(R.string.backup_restore_checking))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }, enabled = !step.checking && password.isNotEmpty()) {
                Text(stringResource(R.string.backup_restore_open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !step.checking) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun RestoreConfirmDialog(preview: RestorePreview, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
        text = {
            Text(
                stringResource(
                    R.string.backup_restore_confirm_text,
                    formatDate(preview.createdAt),
                    pluralStringResource(R.plurals.backup_accounts_count, preview.accounts, preview.accounts),
                    pluralStringResource(R.plurals.backup_transactions_count, preview.transactions, preview.transactions),
                    pluralStringResource(R.plurals.backup_photos_count, preview.photos, preview.photos)
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.backup_restore_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun MessageDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_understood)) }
        }
    )
}

/** No se puede cerrar: termina solo cuando acaba el trabajo. */
@Composable
private fun ProgressDialog(text: String) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        text = { ProgressRow(text) },
        confirmButton = {}
    )
}

@Composable
private fun ProgressRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
