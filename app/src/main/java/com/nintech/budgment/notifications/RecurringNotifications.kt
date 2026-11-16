package com.nintech.budgment.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nintech.budgment.MainActivity
import com.nintech.budgment.R

private const val CHANNEL_RECURRING = "recurring"
private const val NOTIFICATION_PENDING_RECURRING = 1001

/** En Android 13+ las notificaciones necesitan permiso explícito. */
fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

object RecurringNotifications {

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_RECURRING,
            context.getString(R.string.notification_channel_recurring),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.notification_channel_recurring_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Anuncia los movimientos recurrentes que esperan confirmación. Solo avisa: al tocarla se
     * abre Inicio, donde el usuario decide registrar, posponer u omitir cada uno.
     */
    fun showPending(context: Context, names: List<String>) {
        if (names.isEmpty() || needsNotificationPermission(context)) return
        ensureChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = context.resources.getQuantityString(R.plurals.notification_recurring_title, names.size, names.size)
        val text = names.joinToString(", ")

        val notification = NotificationCompat.Builder(context, CHANNEL_RECURRING)
            .setSmallIcon(R.drawable.ic_notification_repeat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notification_recurring_body, text)))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_PENDING_RECURRING, notification)
        } catch (_: SecurityException) {
            // El permiso se revocó entre la comprobación y el aviso: no hay nada que hacer.
        }
    }

    fun cancelPending(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_PENDING_RECURRING)
    }
}
