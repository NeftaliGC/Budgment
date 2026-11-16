package com.nintech.budgment.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.nintech.budgment.data.model.BUDGET_WARNING_OPTIONS
import com.nintech.budgment.data.model.DEFAULT_BUDGET_WARNING_PERCENT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val NOTIFY_RECURRING_KEY = booleanPreferencesKey("notify_recurring")
private val NOTIFY_BUDGETS_KEY = booleanPreferencesKey("notify_budgets")
private val REMINDER_HOUR_KEY = intPreferencesKey("reminder_hour")
private val BUDGET_WARNING_PERCENT_KEY = intPreferencesKey("budget_warning_percent")

/**
 * Qué avisos llegan como notificación del teléfono y a qué hora se revisa cada día. La bandeja de
 * la app registra todos los avisos sin importar estas preferencias.
 *
 * Son preferencias del dispositivo: no se borran al cerrar sesión.
 */
data class NotificationPreferences(
    val recurringEnabled: Boolean = true,
    val budgetsEnabled: Boolean = true,
    val reminderHour: Int = DEFAULT_REMINDER_HOUR,
    /** Aviso previo para todos los presupuestos; el del 100 % llega siempre. */
    val budgetWarningPercent: Int = DEFAULT_BUDGET_WARNING_PERCENT
) {
    companion object {
        const val DEFAULT_REMINDER_HOUR = 9
    }
}

interface NotificationSettings {
    val preferences: Flow<NotificationPreferences>
    suspend fun setRecurringEnabled(enabled: Boolean)
    suspend fun setBudgetsEnabled(enabled: Boolean)
    suspend fun setReminderHour(hour: Int)
    suspend fun setBudgetWarningPercent(percent: Int)
}

@Singleton
class NotificationSettingsImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : NotificationSettings {

    override val preferences: Flow<NotificationPreferences> = dataStore.data.map {
        NotificationPreferences(
            recurringEnabled = it[NOTIFY_RECURRING_KEY] ?: true,
            budgetsEnabled = it[NOTIFY_BUDGETS_KEY] ?: true,
            reminderHour = it[REMINDER_HOUR_KEY] ?: NotificationPreferences.DEFAULT_REMINDER_HOUR,
            budgetWarningPercent = it[BUDGET_WARNING_PERCENT_KEY]?.takeIf { p -> p in BUDGET_WARNING_OPTIONS }
                ?: DEFAULT_BUDGET_WARNING_PERCENT
        )
    }

    override suspend fun setRecurringEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFY_RECURRING_KEY] = enabled }
    }

    override suspend fun setBudgetsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFY_BUDGETS_KEY] = enabled }
    }

    override suspend fun setBudgetWarningPercent(percent: Int) {
        require(percent in BUDGET_WARNING_OPTIONS)
        dataStore.edit { it[BUDGET_WARNING_PERCENT_KEY] = percent }
    }

    override suspend fun setReminderHour(hour: Int) {
        dataStore.edit { it[REMINDER_HOUR_KEY] = hour.coerceIn(0, 23) }
    }
}
