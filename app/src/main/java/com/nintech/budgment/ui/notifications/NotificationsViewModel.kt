package com.nintech.budgment.ui.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nintech.budgment.data.local.entity.NotificationEntity
import com.nintech.budgment.data.local.entity.NotificationType
import com.nintech.budgment.data.local.entity.RecurringTransactionEntity
import com.nintech.budgment.data.repository.NotificationRepository
import com.nintech.budgment.data.repository.RecurringRepository
import com.nintech.budgment.data.session.NotificationPreferences
import com.nintech.budgment.data.session.NotificationSettings
import com.nintech.budgment.data.session.SessionManager
import com.nintech.budgment.notifications.RecurringReminderWorker
import com.nintech.budgment.ui.common.dayTicks
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/** Qué se puede hacer todavía con un aviso. */
enum class InboxAction {
    /** El recurrente sigue esperando esa ocurrencia: se puede registrar. */
    REGISTER,
    /** Ya se registró u omitió, o el recurrente se pausó o eliminó. */
    RESOLVED,
    /** Abre Presupuestos. */
    OPEN_BUDGETS
}

data class InboxItem(
    val notification: NotificationEntity,
    /** No se había leído al abrir la pantalla. */
    val isNew: Boolean,
    val action: InboxAction
)

/** Avisos de un mismo día; [dayStart] es la medianoche local. */
data class InboxDay(val dayStart: Long, val items: List<InboxItem>)

data class NotificationsUiState(
    val days: List<InboxDay> = emptyList(),
    val today: Long = 0,
    val preferences: NotificationPreferences = NotificationPreferences(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository,
    private val recurringRepository: RecurringRepository,
    private val notificationSettings: NotificationSettings,
    private val sessionManager: SessionManager
) : ViewModel() {

    /** Ids sin leer al abrir la pantalla: conservan su marca aunque ya se hayan marcado leídos. */
    private val newIds = MutableStateFlow<Set<String>?>(null)
    private val deleted = mutableMapOf<String, NotificationEntity>()

    val uiState: StateFlow<NotificationsUiState> = sessionManager.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) return@flatMapLatest flowOf(NotificationsUiState(isLoading = false))
            combine(
                notificationRepository.observeInbox(userId),
                recurringRepository.observeRecurring(userId),
                notificationSettings.preferences,
                newIds.filterNotNull(),
                dayTicks()
            ) { inbox, recurrings, preferences, unreadAtOpen, now ->
                NotificationsUiState(
                    days = groupByDay(inbox.map { it.toItem(unreadAtOpen, recurrings.associateBy { r -> r.id }) }),
                    today = startOfDay(now),
                    preferences = preferences,
                    isLoading = false
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationsUiState())

    init {
        // Al abrir la bandeja se da todo por leído, pero lo nuevo sigue marcado mientras se ve.
        viewModelScope.launch {
            val userId = sessionManager.currentUserId.filterNotNull().first()
            notificationRepository.syncRecurring(userId)
            val inbox = notificationRepository.observeInbox(userId).first()
            newIds.value = inbox.filter { it.readAt == null }.map { it.id }.toSet()
            notificationRepository.markAllRead(userId)
        }
    }

    private fun NotificationEntity.toItem(
        unreadAtOpen: Set<String>,
        recurrings: Map<String, RecurringTransactionEntity>
    ): InboxItem {
        val action = when (type) {
            NotificationType.RECURRING_DUE -> {
                val recurring = refId?.let { recurrings[it] }
                if (recurring != null && recurring.active && recurring.nextOccurrence == occurrence) {
                    InboxAction.REGISTER
                } else {
                    InboxAction.RESOLVED
                }
            }
            NotificationType.BUDGET_THRESHOLD -> InboxAction.OPEN_BUDGETS
        }
        return InboxItem(this, isNew = id in unreadAtOpen, action = action)
    }

    fun setRecurringEnabled(enabled: Boolean) {
        viewModelScope.launch { notificationSettings.setRecurringEnabled(enabled) }
    }

    fun setBudgetsEnabled(enabled: Boolean) {
        viewModelScope.launch { notificationSettings.setBudgetsEnabled(enabled) }
    }

    fun setBudgetWarningPercent(percent: Int) {
        viewModelScope.launch { notificationSettings.setBudgetWarningPercent(percent) }
    }

    fun setReminderHour(hour: Int) {
        viewModelScope.launch {
            notificationSettings.setReminderHour(hour)
            RecurringReminderWorker.schedule(context, hour, replace = true)
        }
    }

    fun delete(id: String) {
        val item = uiState.value.days.flatMap { it.items }.firstOrNull { it.notification.id == id } ?: return
        deleted[id] = item.notification
        viewModelScope.launch { notificationRepository.delete(listOf(item.notification)) }
    }

    fun restore(id: String) {
        val notification = deleted.remove(id) ?: return
        viewModelScope.launch { notificationRepository.restore(listOf(notification)) }
    }
}

internal fun groupByDay(items: List<InboxItem>): List<InboxDay> =
    items
        .groupBy { startOfDay(it.notification.createdAt) }
        .toSortedMap(compareByDescending { it })
        .map { (day, dayItems) -> InboxDay(day, dayItems.sortedByDescending { it.notification.createdAt }) }

internal fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis
