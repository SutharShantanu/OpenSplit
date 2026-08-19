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
        ensureFirebaseInitialized()
        container = AppContainer(this)
        com.opensplit.service.NotificationHelper.createNotificationChannels(this)
        scheduleBackgroundWorkers()
    }

    private fun ensureFirebaseInitialized() {
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.fromResource(this)
                    ?: com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:873815654150:android:f9a25eabb9f8c370246887")
                        .setApiKey("AIzaSyCcWKsD6Lg5jr08Ed7B-fC5Fy4iJql6KeA")
                        .setProjectId("opensplit-fe83f")
                        .setGcmSenderId("873815654150")
                        .setStorageBucket("opensplit-fe83f.firebasestorage.app")
                        .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            }
            com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled = false
        } catch (e: Exception) {
            // Firebase init fallback
        }
    }

    private fun scheduleBackgroundWorkers() {
        try {
            val wm = WorkManager.getInstance(this)

            val recurringRequest = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(1, TimeUnit.DAYS).build()
            wm.enqueueUniquePeriodicWork(
                RecurringExpenseWorker.UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                recurringRequest
            )

            val debtReminderRequest = PeriodicWorkRequestBuilder<com.opensplit.data.work.DebtReminderWorker>(3, TimeUnit.DAYS).build()
            wm.enqueueUniquePeriodicWork(
                com.opensplit.data.work.DebtReminderWorker.UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                debtReminderRequest
            )
        } catch (e: Exception) {
            // WorkManager initialization may fail in test environments without full background services
        }
    }
}
