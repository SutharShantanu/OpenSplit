package com.opensplit.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.opensplit.MainActivity
import com.opensplit.R

object NotificationHelper {

    const val CHANNEL_ACTIVITY = "opensplit_activity"
    const val CHANNEL_REMINDERS = "opensplit_reminders"
    const val CHANNEL_RECURRING = "opensplit_recurring"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return

            val activityChannel = NotificationChannel(
                CHANNEL_ACTIVITY,
                "Activity & Expenses",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new expenses, group invites, and activity updates."
            }

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Settlement Reminders & Nudges",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for unsettled group balances and payment requests."
            }

            val recurringChannel = NotificationChannel(
                CHANNEL_RECURRING,
                "Recurring Expense Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when recurring bills (rent, WiFi, subscriptions) are posted."
            }

            manager.createNotificationChannel(activityChannel)
            manager.createNotificationChannel(remindersChannel)
            manager.createNotificationChannel(recurringChannel)
        }
    }

    fun sendNotification(
        context: Context,
        channelId: String,
        title: String,
        body: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(
                if (channelId == CHANNEL_REMINDERS) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }

    fun sendDebtReminderNotification(context: Context, totalOwedFormatted: String, groupCount: Int) {
        val title = "OpenSplit Settlement Reminder 💸"
        val body = "You have $totalOwedFormatted outstanding across $groupCount group${if (groupCount > 1) "s" else ""}. Tap to settle up!"
        sendNotification(context, CHANNEL_REMINDERS, title, body, notificationId = 1001)
    }

    fun sendRecurringExpenseNotification(context: Context, description: String, amountFormatted: String) {
        val title = "Recurring Bill Posted 🔄"
        val body = "'$description' ($amountFormatted) was automatically added to your group."
        sendNotification(context, CHANNEL_RECURRING, title, body, notificationId = System.currentTimeMillis().toInt())
    }
}
