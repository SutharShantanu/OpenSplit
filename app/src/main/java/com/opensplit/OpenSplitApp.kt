package com.opensplit

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.opensplit.data.work.RecurringExpenseWorker
import com.opensplit.di.AppContainer
import java.util.concurrent.TimeUnit

class OpenSplitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        scheduleRecurringExpenses()
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
