package com.nintech.budgment.ui.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nintech.budgment.R
import com.nintech.budgment.data.local.entity.NotificationEntity
import com.nintech.budgment.data.local.entity.NotificationType
import com.nintech.budgment.data.session.NotificationPreferences
import com.nintech.budgment.ui.common.SurfaceSystemBars
import com.nintech.budgment.ui.common.formatShortDate
import com.nintech.budgment.ui.common.rememberUndoableDelete
import com.nintech.budgment.ui.theme.BudgmentTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    onRegisterRecurring: (String) -> Unit,
    onOpenBudgets: () -> Unit,
    viewModel: NotificationsViewModel? = if (LocalInspectionMode.current) null else hiltViewModel()
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value ?: NotificationsUiState(isLoading = false)
    val snackbarHostState = remember { SnackbarHostState() }
    NotificationsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onRegisterRecurring = onRegisterRecurring,
        onOpenBudgets = onOpenBudgets,
        onDelete = rememberUndoableDelete(
            snackbarHostState = snackbarHostState,
            message = stringResource(R.string.inbox_deleted),
            onDelete = { viewModel?.delete(it) },
            onRestore = { viewModel?.restore(it) }
        ),
        onRecurringEnabledChange = { viewModel?.setRecurringEnabled(it) },
        onBudgetsEnabledChange = { viewModel?.setBudgetsEnabled(it) },
        onReminderHourChange = { viewModel?.setReminderHour(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    uiState: NotificationsUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onRegisterRecurring: (String) -> Unit,
    onOpenBudgets: () -> Unit,
    onDelete: (String) -> Unit,
    onRecurringEnabledChange: (Boolean) -> Unit,
    onBudgetsEnabledChange: (Boolean) -> Unit,
    onReminderHourChange: (Int) -> Unit
) {
    SurfaceSystemBars()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val systemEnabled = rememberSystemNotificationsEnabled()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.inbox_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.inbox_settings_title))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (!systemEnabled) {
                item(key = "system-off") {
                    SystemNotificationsOffBanner(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
            if (uiState.days.isEmpty()) {
                item(key = "empty") { EmptyInbox() }
            }
            uiState.days.forEach { day ->
                item(key = "day-${day.dayStart}") {
                    Text(
                        text = dayLabel(day.dayStart, uiState.today),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                            .semantics { heading() }
                    )
                }
                items(day.items, key = { it.notification.id }) { item ->
                    DismissibleInboxRow(
                        item = item,
                        onClick = {
                            when (item.action) {
                                InboxAction.REGISTER -> item.notification.refId?.let(onRegisterRecurring)
                                InboxAction.OPEN_BUDGETS -> onOpenBudgets()
                                InboxAction.RESOLVED -> Unit
                            }
                        },
                        onDelete = { onDelete(item.notification.id) }
                    )
                }
            }
        }
    }

    if (showSettings) {
        NotificationSettingsSheet(
            preferences = uiState.preferences,
            systemEnabled = systemEnabled,
            onDismiss = { showSettings = false },
            onRecurringEnabledChange = onRecurringEnabledChange,
            onBudgetsEnabledChange = onBudgetsEnabledChange,
            onReminderHourChange = onReminderHourChange
        )
    }
}

@Composable
private fun dayLabel(dayStart: Long, today: Long): String {
    val dayMillis = 24 * 60 * 60 * 1000L
    return when {
        dayStart == today -> stringResource(R.string.common_today)
        // Con cambio de horario un día puede durar 23 o 25 horas.
        today - dayStart in (dayMillis - 2 * 3_600_000)..(dayMillis + 2 * 3_600_000) -> stringResource(R.string.common_yesterday)
        else -> formatShortDate(dayStart)
    }
}

private fun timeLabel(millis: Long): String = SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(millis))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleInboxRow(item: InboxItem, onClick: () -> Unit, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onDelete()
            value != SwipeToDismissBoxValue.Settled
        }
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        InboxRow(item, onClick, onDelete)
    }
}

@Composable
private fun InboxRow(item: InboxItem, onClick: () -> Unit, onDelete: () -> Unit) {
    val notification = item.notification
    val deleteLabel = stringResource(R.string.common_delete)
    val icon: ImageVector? = if (notification.type == NotificationType.RECURRING_DUE) Icons.Filled.EventRepeat else null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = item.action != InboxAction.RESOLVED, onClick = onClick)
            .semantics {
                customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
            } else {
                Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    timeLabel(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.isNew) {
                    val newLabel = stringResource(R.string.inbox_new)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .semantics { contentDescription = newLabel }
                    )
                }
            }
            Text(
                notification.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (item.action) {
                InboxAction.REGISTER -> FilledTonalButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) { Text(stringResource(R.string.recurring_register)) }
                InboxAction.OPEN_BUDGETS -> TextButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) { Text(stringResource(R.string.inbox_open_budgets)) }
                InboxAction.RESOLVED -> if (notification.type == NotificationType.RECURRING_DUE) {
                    Text(
                        stringResource(R.string.inbox_recurring_resolved),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyInbox() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.NotificationsNone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.inbox_empty_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.inbox_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Se vuelve a leer al regresar a la app: el usuario pudo cambiarlo en los ajustes del sistema. */
@Composable
private fun rememberSystemNotificationsEnabled(): Boolean {
    val context = LocalContext.current
    if (LocalInspectionMode.current) return true
    var enabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

private fun openSystemNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

/**
 * Pide el permiso la primera vez (Android 13+). Si ya se negó, Android no vuelve a mostrar el
 * diálogo: se abren los ajustes del sistema para activarlas a mano.
 */
@Composable
private fun rememberEnableNotifications(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) openSystemNotificationSettings(context)
    }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openSystemNotificationSettings(context)
        }
    }
}

@Composable
private fun SystemNotificationsOffBanner(modifier: Modifier = Modifier) {
    val enable = rememberEnableNotifications()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.inbox_system_off),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = enable) { Text(stringResource(R.string.inbox_system_enable)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsSheet(
    preferences: NotificationPreferences,
    systemEnabled: Boolean,
    onDismiss: () -> Unit,
    onRecurringEnabledChange: (Boolean) -> Unit,
    onBudgetsEnabledChange: (Boolean) -> Unit,
    onReminderHourChange: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                stringResource(R.string.inbox_settings_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(R.string.inbox_settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!systemEnabled) {
                SystemNotificationsOffBanner(Modifier.padding(vertical = 8.dp))
            }
            SwitchRow(
                title = stringResource(R.string.inbox_settings_recurring),
                description = stringResource(R.string.inbox_settings_recurring_description),
                checked = preferences.recurringEnabled,
                enabled = systemEnabled,
                onCheckedChange = onRecurringEnabledChange
            )
            SwitchRow(
                title = stringResource(R.string.inbox_settings_budgets),
                description = stringResource(R.string.inbox_settings_budgets_description),
                checked = preferences.budgetsEnabled,
                enabled = systemEnabled,
                onCheckedChange = onBudgetsEnabledChange
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ReminderHourField(preferences.reminderHour, onReminderHourChange)
            Text(
                stringResource(R.string.inbox_settings_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        // El estado lo comunica la fila; el Switch no recibe clics propios para no duplicar el foco.
        Switch(checked = checked && enabled, onCheckedChange = null, enabled = enabled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderHourField(hour: Int, onChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = hourLabel(hour),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.inbox_settings_hour)) },
            supportingText = { Text(stringResource(R.string.inbox_settings_hour_description)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            REMINDER_HOURS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(hourLabel(option)) },
                    onClick = {
                        expanded = false
                        if (option != hour) onChange(option)
                    }
                )
            }
        }
    }
}

private val REMINDER_HOURS = 6..22

private fun hourLabel(hour: Int): String = "%d:00".format(hour)

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NotificationsScreenPreview() {
    val now = System.currentTimeMillis()
    val today = startOfDay(now)
    fun entity(id: String, title: String, body: String, at: Long, type: NotificationType = NotificationType.RECURRING_DUE) =
        NotificationEntity(id = id, userId = "u", type = type, refId = "r$id", dedupKey = id, title = title, body = body, createdAt = at)
    BudgmentTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(
                days = groupByDay(
                    listOf(
                        InboxItem(entity("1", "Renta por registrar", "Gasto de $8,000.00 · Débito · 15 nov", today + 9 * 3_600_000), true, InboxAction.REGISTER),
                        InboxItem(entity("2", "Comida llegó al 80 %", "Llevas $3,200.00 de $4,000.00", today + 8 * 3_600_000, NotificationType.BUDGET_THRESHOLD), true, InboxAction.OPEN_BUDGETS),
                        InboxItem(entity("3", "Netflix por registrar", "Gasto de $219.00 · Crédito · 14 nov", today - 15 * 3_600_000), false, InboxAction.RESOLVED)
                    )
                ),
                today = today,
                isLoading = false
            ),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onRegisterRecurring = {},
            onOpenBudgets = {},
            onDelete = {},
            onRecurringEnabledChange = {},
            onBudgetsEnabledChange = {},
            onReminderHourChange = {}
        )
    }
}
