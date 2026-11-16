package com.nintech.budgment

import android.app.Application
import com.nintech.budgment.notifications.RecurringNotifications
import com.nintech.budgment.notifications.RecurringReminderWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BudgmentApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RecurringNotifications.ensureChannel(this)
        RecurringReminderWorker.schedule(this)
    }
}
