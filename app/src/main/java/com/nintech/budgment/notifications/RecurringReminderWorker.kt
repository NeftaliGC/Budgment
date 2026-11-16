package com.nintech.budgment.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.NotificationRepository
import com.nintech.budgment.data.repository.RecurringRepository
import com.nintech.budgment.data.session.NotificationSettings
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Revisa una vez al día, a la hora elegida en Notificaciones, si hay movimientos recurrentes
 * vencidos: los agrega a la bandeja y, si el usuario lo permite, los anuncia con una
 * notificación. Nunca registra nada: eso lo decide el usuario desde la app.
 */
class RecurringReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun sessionManager(): SessionManager
        fun recurringRepository(): RecurringRepository
        fun categoryRepository(): CategoryRepository
        fun notificationRepository(): NotificationRepository
        fun notificationSettings(): NotificationSettings
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        // Solo hay usuario en segundo plano si eligió «Recordarme»; sin sesión no se avisa.
        val userId = deps.sessionManager().currentUserId.first() ?: return Result.success()

        deps.notificationRepository().syncRecurring(userId)

        val pending = deps.recurringRepository().getPending(userId)
        if (pending.isEmpty() || !deps.notificationSettings().preferences.first().recurringEnabled) {
            RecurringNotifications.cancelPending(applicationContext)
            return Result.success()
        }
        val names = pending.map { recurring ->
            recurring.description ?: deps.categoryRepository().getCategory(recurring.categoryId)?.name.orEmpty()
        }.filter { it.isNotBlank() }
        RecurringNotifications.showPending(applicationContext, names)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "recurring_reminder"
        /**
         * Programa la revisión diaria a las [hour]:00. Con [replace] = false conserva la que ya
         * estuviera programada; con true la reemplaza (al cambiar la hora).
         */
        fun schedule(context: Context, hour: Int, replace: Boolean = false) {
            val request = PeriodicWorkRequestBuilder<RecurringReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilNextReminder(hour), TimeUnit.MILLISECONDS)
                .build()
            val policy = if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, policy, request)
        }

        private fun delayUntilNextReminder(hour: Int, now: Long = System.currentTimeMillis()): Long {
            val next = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) add(Calendar.DAY_OF_MONTH, 1)
            }
            return next.timeInMillis - now
        }
    }
}
