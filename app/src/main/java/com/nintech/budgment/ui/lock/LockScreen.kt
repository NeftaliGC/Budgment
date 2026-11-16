package com.nintech.budgment.ui.lock

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nintech.budgment.R
import com.nintech.budgment.ui.theme.ChromeBackground
import com.nintech.budgment.ui.theme.ChromeContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Se dibuja encima de toda la app mientras está bloqueada. Tapa el contenido, bloquea los toques
 * y el botón Atrás manda la app al fondo en vez de saltarse el bloqueo.
 */
@Composable
fun LockScreen(viewModel: LockViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val biometricAvailable = remember { biometricAvailability(context) == BiometricAvailability.AVAILABLE }
    val canUseBiometric = state.biometricEnabled && biometricAvailable && activity != null

    val promptTitle = stringResource(R.string.biometric_prompt_title)
    val promptNegative = stringResource(R.string.biometric_prompt_use_pin)
    val showBiometricPrompt = {
        activity?.authenticateWithBiometrics(promptTitle, promptNegative, onSuccess = viewModel::onBiometricSuccess)
    }

    BackHandler { (context as? Activity ?: activity)?.moveTaskToBack(true) }
    LockSystemBars()
    AutoBiometricPrompt(enabled = canUseBiometric, show = { showBiometricPrompt() })

    var showRecovery by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ChromeBackground,
        contentColor = ChromeContent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_mark),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.lock_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(24.dp))
            PinDots(
                slots = state.pinLength,
                filled = state.entered.length,
                color = ChromeContent,
                shakeKey = state.errorCount
            )
            Spacer(Modifier.height(16.dp))
            LockMessageText(state)
            Spacer(Modifier.height(16.dp))
            PinPad(
                onDigit = viewModel::onDigit,
                onDelete = viewModel::onDelete,
                contentColor = ChromeContent,
                enabled = !state.isChecking,
                extraKey = if (canUseBiometric) {
                    {
                        IconButton(
                            onClick = { showBiometricPrompt() },
                            modifier = Modifier.size(KEY_SIZE),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = ChromeContent)
                        ) {
                            Icon(
                                Icons.Filled.Fingerprint,
                                contentDescription = stringResource(R.string.pin_key_biometric),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else null
            )
            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = { showRecovery = true },
                colors = ButtonDefaults.textButtonColors(contentColor = ChromeContent)
            ) {
                Text(stringResource(R.string.lock_forgot_pin))
            }
        }
    }

    if (showRecovery) {
        RecoveryDialog(
            onRecover = viewModel::recoverWithPassword,
            onDismiss = { showRecovery = false },
            onRecovered = {
                showRecovery = false
                Toast.makeText(context, R.string.lock_recovered_message, Toast.LENGTH_LONG).show()
            }
        )
    }
}

/** Error, intentos restantes o la cuenta atrás de la espera. Altura fija para que nada salte. */
@Composable
private fun LockMessageText(state: LockUiState) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lockedOut = now < state.lockoutUntil
    LaunchedEffect(state.lockoutUntil) {
        now = System.currentTimeMillis()
        while (now < state.lockoutUntil) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    val text = when {
        lockedOut -> {
            val seconds = ((state.lockoutUntil - now + 999) / 1000).toInt()
            stringResource(R.string.lock_locked_out, "%d:%02d".format(seconds / 60, seconds % 60))
        }
        state.message is LockMessage.WrongPin && state.message.attemptsLeft in 1..3 ->
            pluralStringResource(R.plurals.lock_wrong_pin_attempts, state.message.attemptsLeft, state.message.attemptsLeft)
        state.message is LockMessage.WrongPin -> stringResource(R.string.lock_wrong_pin)
        else -> ""
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    )
}

/**
 * Abre la huella o el rostro sola una vez cada vez que la app vuelve a primer plano bloqueada.
 * Si el usuario la cierra, queda el botón para reintentarlo.
 */
@Composable
private fun AutoBiometricPrompt(enabled: Boolean, show: () -> Unit) {
    if (!enabled) return
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentShow by rememberUpdatedState(show)
    DisposableEffect(lifecycleOwner) {
        var shown = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (!shown) {
                    shown = true
                    currentShow()
                }
                Lifecycle.Event.ON_STOP -> shown = false
                else -> Unit
            }
        }
        // Si ya está en primer plano, el observador recibe ON_RESUME al registrarse.
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * El bloqueo usa el verde del chrome, con iconos claros. Al quitarlo se devuelven los iconos que
 * tenía la pantalla de abajo, que no se entera de que estuvo tapada.
 */
@Composable
private fun LockSystemBars() {
    val view = LocalView.current
    if (view.isInEditMode) return
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(view, lifecycleOwner) {
        val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val previousStatus = controller.isAppearanceLightStatusBars
        val previousNavigation = controller.isAppearanceLightNavigationBars
        fun apply() {
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
        apply()
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) apply() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.isAppearanceLightStatusBars = previousStatus
            controller.isAppearanceLightNavigationBars = previousNavigation
        }
    }
}

@Composable
private fun RecoveryDialog(
    onRecover: suspend (String) -> Boolean,
    onDismiss: () -> Unit,
    onRecovered: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val submit = {
        if (!checking && password.isNotBlank()) {
            checking = true
            scope.launch {
                val ok = onRecover(password)
                checking = false
                if (ok) onRecovered() else wrong = true
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lock_recovery_title)) },
        text = {
            Column {
                Text(stringResource(R.string.lock_recovery_text))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; wrong = false },
                    label = { Text(stringResource(R.string.lock_recovery_password)) },
                    singleLine = true,
                    isError = wrong,
                    supportingText = if (wrong) {
                        { Text(stringResource(R.string.lock_recovery_wrong)) }
                    } else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }, enabled = !checking && password.isNotBlank()) {
                Text(stringResource(R.string.lock_recovery_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
