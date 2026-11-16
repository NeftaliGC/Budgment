package com.nintech.budgment.ui.lock

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nintech.budgment.R
import com.nintech.budgment.security.AppLockPolicy
import com.nintech.budgment.ui.common.SurfaceSystemBars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(
    onBackClick: () -> Unit,
    onDone: () -> Unit,
    viewModel: PinSetupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(state.isDone) {
        if (!state.isDone) return@LaunchedEffect
        val message = when (state.mode) {
            PinSetupMode.CREATE -> R.string.pin_setup_done_create
            PinSetupMode.CHANGE -> R.string.pin_setup_done_change
            PinSetupMode.DISABLE -> R.string.pin_setup_done_disable
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        onDone()
    }

    SurfaceSystemBars()
    val title = when (state.mode) {
        PinSetupMode.CREATE -> R.string.pin_setup_title_create
        PinSetupMode.CHANGE -> R.string.pin_setup_title_change
        PinSetupMode.DISABLE -> R.string.pin_setup_title_disable
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val prompt = when (state.step) {
                PinSetupStep.CURRENT -> R.string.pin_setup_enter_current
                PinSetupStep.NEW -> R.string.pin_setup_enter_new
                PinSetupStep.CONFIRM -> R.string.pin_setup_confirm
            }
            Text(
                text = stringResource(prompt),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            if (state.step == PinSetupStep.NEW) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.pin_setup_enter_new_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(24.dp))
            val slots = if (state.step == PinSetupStep.NEW) {
                (state.entered.length + 1).coerceIn(AppLockPolicy.MIN_PIN_LENGTH, AppLockPolicy.MAX_PIN_LENGTH)
            } else {
                state.expectedLength
            }
            PinDots(
                slots = slots,
                filled = state.entered.length,
                color = MaterialTheme.colorScheme.onSurface,
                shakeKey = state.errorCount
            )
            Spacer(Modifier.height(16.dp))
            val error = when (val e = state.error) {
                is PinSetupError.WrongCurrent -> if (e.attemptsLeft in 1..3) {
                    pluralStringResource(R.plurals.lock_wrong_pin_attempts, e.attemptsLeft, e.attemptsLeft)
                } else stringResource(R.string.lock_wrong_pin)
                is PinSetupError.LockedOut -> stringResource(R.string.pin_setup_locked_out)
                PinSetupError.Mismatch -> stringResource(R.string.pin_setup_mismatch)
                null -> ""
            }
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
            Spacer(Modifier.height(8.dp))
            PinPad(
                onDigit = viewModel::onDigit,
                onDelete = viewModel::onDelete,
                contentColor = MaterialTheme.colorScheme.onSurface,
                enabled = !state.isWorking && state.error !is PinSetupError.LockedOut
            )
            Spacer(Modifier.height(16.dp))
            if (state.step == PinSetupStep.NEW) {
                FilledTonalButton(
                    onClick = viewModel::onContinue,
                    enabled = AppLockPolicy.isValidPin(state.entered),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.pin_setup_continue))
                }
            }
        }
    }
}
