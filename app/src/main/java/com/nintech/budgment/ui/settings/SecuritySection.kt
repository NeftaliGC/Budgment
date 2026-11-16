package com.nintech.budgment.ui.settings

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.nintech.budgment.R
import com.nintech.budgment.data.session.AppLockConfig
import com.nintech.budgment.security.AppLockPolicy
import com.nintech.budgment.ui.lock.BiometricAvailability
import com.nintech.budgment.ui.lock.PinSetupMode
import com.nintech.budgment.ui.lock.authenticateWithBiometrics
import com.nintech.budgment.ui.lock.biometricAvailability
import com.nintech.budgment.ui.lock.findFragmentActivity

/** Bloqueo con PIN, huella o rostro y cuándo volver a pedirlo. */
@Composable
fun SecuritySection(
    config: AppLockConfig,
    onPinSetupClick: (PinSetupMode) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onTimeoutChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val preview = LocalInspectionMode.current
    // Se vuelve a revisar al regresar: el usuario pudo registrar su huella en los ajustes.
    var biometrics by remember {
        mutableStateOf(if (preview) BiometricAvailability.AVAILABLE else biometricAvailability(context))
    }
    if (!preview) {
        LifecycleResumeEffect(Unit) {
            biometrics = biometricAvailability(context)
            onPauseOrDispose {}
        }
    }
    val promptTitle = stringResource(R.string.biometric_confirm_title)
    val promptNegative = stringResource(R.string.common_cancel)

    Column(Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = stringResource(R.string.settings_security_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        SettingsSwitchRow(
            title = stringResource(R.string.settings_lock),
            description = stringResource(
                if (config.enabled) R.string.settings_lock_description_on else R.string.settings_lock_description_off
            ),
            checked = config.enabled,
            onCheckedChange = { on -> onPinSetupClick(if (on) PinSetupMode.CREATE else PinSetupMode.DISABLE) }
        )
    }

    if (!config.enabled) return

    Column(Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = stringResource(R.string.settings_change_pin),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) { onPinSetupClick(PinSetupMode.CHANGE) }
                .padding(vertical = 12.dp)
        )
        if (biometrics != BiometricAvailability.UNAVAILABLE) {
            val available = biometrics == BiometricAvailability.AVAILABLE
            SettingsSwitchRow(
                title = stringResource(R.string.settings_biometric),
                description = stringResource(
                    if (available) R.string.settings_biometric_description else R.string.settings_biometric_not_enrolled
                ),
                checked = config.biometricEnabled && available,
                enabled = available,
                onCheckedChange = { on ->
                    if (!on) {
                        onBiometricChange(false)
                    } else {
                        // Se confirma una vez que el sensor reconoce al usuario antes de activarlo.
                        context.findFragmentActivity()?.authenticateWithBiometrics(
                            title = promptTitle,
                            negativeButton = promptNegative,
                            onSuccess = { onBiometricChange(true) }
                        )
                    }
                }
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings_lock_timeout),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Column(Modifier.selectableGroup()) {
        AppLockPolicy.TIMEOUT_OPTIONS_SECONDS.forEach { seconds ->
            val selected = seconds == config.timeoutSeconds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onTimeoutChange(seconds) })
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected, onClick = null)
                Spacer(Modifier.width(16.dp))
                Text(timeoutLabel(seconds), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.settings_lock_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

@Composable
private fun timeoutLabel(seconds: Int): String = when (seconds) {
    0 -> stringResource(R.string.settings_lock_timeout_immediately)
    in 1..59 -> pluralStringResource(R.plurals.settings_lock_timeout_seconds, seconds, seconds)
    else -> stringResource(R.string.settings_lock_timeout_minutes, seconds / 60)
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        // El estado lo comunica la fila; el Switch no recibe clics propios para no duplicar el foco.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
