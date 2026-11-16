package com.nintech.budgment

import android.app.Application
import com.nintech.budgment.data.attachments.AttachmentRepository
import com.nintech.budgment.data.session.NotificationSettings
import com.nintech.budgment.notifications.BudgetNotifications
import com.nintech.budgment.notifications.RecurringNotifications
import com.nintech.budgment.notifications.RecurringReminderWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BudgmentApp : Application() {

    @Inject
    lateinit var notificationSettings: NotificationSettings

    @Inject
    lateinit var attachmentRepository: AttachmentRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        RecurringNotifications.ensureChannel(this)
        BudgetNotifications.ensureChannel(this)
        appScope.launch {
            val hour = notificationSettings.preferences.first().reminderHour
            RecurringReminderWorker.schedule(this@BudgmentApp, hour)
        }
        // También al abrir la app: la revisión diaria puede tardar si el teléfono ahorra batería,
        // y tras restaurar un respaldo sin fotos hay que quitar las que ya no existen.
        appScope.launch { runCatching { attachmentRepository.cleanUp() } }
    }
}
