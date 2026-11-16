package com.nintech.budgment.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nintech.budgment.data.repository.CategoryRepository
import com.nintech.budgment.data.repository.RecurringRepository
import com.nintech.budgment.data.session.SessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Revisa una vez al día si hay movimientos recurrentes vencidos y, si los hay, lo anuncia con
 * una notificación. Nunca registra nada: eso lo decide el usuario desde la app.
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
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        // Solo hay usuario en segundo plano si eligió «Recordarme»; sin sesión no se avisa.
        val userId = deps.sessionManager().currentUserId.first() ?: return Result.success()

        val pending = deps.recurringRepository().getPending(userId)
        if (pending.isEmpty()) {
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
        private const val REMINDER_HOUR = 9

        /** Programa la revisión diaria hacia las 9:00. Si ya estaba programada, la conserva. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecurringReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilNextReminder(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        private fun delayUntilNextReminder(now: Long = System.currentTimeMillis()): Long {
            val next = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, REMINDER_HOUR); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) add(Calendar.DAY_OF_MONTH, 1)
            }
            return next.timeInMillis - now
        }
    }
}
