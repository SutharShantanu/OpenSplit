package com.opensplit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.opensplit.data.work.RecurringExpenseWorker
import com.opensplit.di.AppContainer
import com.opensplit.service.OpenSplitMessagingService
import java.util.concurrent.TimeUnit

class OpenSplitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
        scheduleRecurringExpenses()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OpenSplitMessagingService.CHANNEL_ID,
                "Activity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "New expenses, settlements, and reminders" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun scheduleRecurringExpenses() {
        val request = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RecurringExpenseWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
