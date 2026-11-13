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
import com.opensplit.OpenSplitApp
import com.opensplit.R
import com.opensplit.domain.model.NotificationType
import com.opensplit.widget.OpenSplitBalanceWidgetProvider
import kotlinx.coroutines.flow.first

/**
 * Every notification the app shows goes through [show]: one channel per kind of event (so
 * people can silence one kind in system settings), the monochrome status-bar icon, the
 * in-app switches, and per-group stacking.
 */
object NotificationHelper {

    const val CHANNEL_ACTIVITY = "opensplit_activity"
    const val CHANNEL_PAYMENTS = "opensplit_payments"
    const val CHANNEL_FRIENDS = "opensplit_friends"
    const val CHANNEL_REMINDERS = "opensplit_reminders"
    const val CHANNEL_RECURRING = "opensplit_recurring"

    /** Tap action that opens a group; read by MainDashboard like the widget actions. */
    const val ACTION_OPEN_GROUP_PREFIX = "group:"

    private const val ID_DEBT_REMINDER = 1001

    fun channelFor(type: NotificationType?): String = when (type) {
        NotificationType.PAYMENT -> CHANNEL_PAYMENTS
        NotificationType.FRIEND_REQUEST -> CHANNEL_FRIENDS
        NotificationType.REMINDER -> CHANNEL_REMINDERS
        NotificationType.RECURRING -> CHANNEL_RECURRING
        else -> CHANNEL_ACTIVITY
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        fun channel(id: String, name: String, importance: Int, desc: String) =
            NotificationChannel(id, name, importance).apply { description = desc }
        manager.createNotificationChannels(
            listOf(
                channel(CHANNEL_ACTIVITY, "Expenses & groups", NotificationManager.IMPORTANCE_DEFAULT,
                    "New or changed expenses, and people joining or leaving groups."),
                channel(CHANNEL_PAYMENTS, "Payments", NotificationManager.IMPORTANCE_HIGH,
                    "A settlement is recorded with you."),
                channel(CHANNEL_FRIENDS, "Friend requests", NotificationManager.IMPORTANCE_DEFAULT,
                    "Someone wants to split expenses with you."),
                channel(CHANNEL_REMINDERS, "Settle-up reminders", NotificationManager.IMPORTANCE_HIGH,
                    "Reminders for balances you owe, and nudges from friends."),
                channel(CHANNEL_RECURRING, "Recurring bills", NotificationManager.IMPORTANCE_DEFAULT,
                    "A recurring bill (rent, WiFi, subscriptions) was posted.")
            )
        )
    }

    /** The in-app switches: master toggle, per-type toggles, muted groups. */
    suspend fun isAllowed(context: Context, type: NotificationType?, groupId: String?): Boolean {
        val prefs = (context.applicationContext as? OpenSplitApp)?.container?.userPreferencesRepository ?: return true
        if (!prefs.notificationsEnabledFlow.first()) return false
        if (type != null && type.id in prefs.disabledNotificationTypesFlow.first()) return false
        return groupId == null || groupId !in prefs.mutedGroupIdsFlow.first()
    }

    /**
     * Posts a notification. Notifications for the same group stack under one summary, and
     * [key] (e.g. the expense id) replaces an earlier notification about the same thing.
     */
    fun show(
        context: Context,
        type: NotificationType?,
        title: String,
        body: String,
        groupId: String? = null,
        groupName: String? = null,
        key: String? = null,
        tapAction: String? = groupId?.let { ACTION_OPEN_GROUP_PREFIX + it }
    ) {
        val id = (key ?: "$title|$body").hashCode()
        val stackKey = groupId?.let { "group:$it" }
        val nm = NotificationManagerCompat.from(context)
        try {
            nm.notify(id, base(context, channelFor(type), title, tapAction, id)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .apply { if (stackKey != null) setGroup(stackKey) }
                .build())
            // Android only bundles a group when a summary exists; it hides the summary until there are two.
            if (stackKey != null) {
                nm.notify(stackKey.hashCode(), base(context, channelFor(type), groupName ?: "OpenSplit", tapAction, stackKey.hashCode())
                    .setGroup(stackKey)
                    .setGroupSummary(true)
                    .setStyle(NotificationCompat.InboxStyle().setSummaryText(groupName))
                    .build())
            }
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted: nothing to show.
        }
    }

    private fun base(context: Context, channel: String, title: String, tapAction: String?, requestCode: Int) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(context.getColor(R.color.ic_launcher_fg))
            .setContentTitle(title)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, requestCode,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        if (tapAction != null) putExtra(OpenSplitBalanceWidgetProvider.EXTRA_WIDGET_ACTION, tapAction)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)
            .setCategory(if (channel == CHANNEL_REMINDERS) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_SOCIAL)

    fun sendDebtReminderNotification(context: Context, totalOwedFormatted: String, groupCount: Int) {
        show(
            context, NotificationType.REMINDER,
            title = "Time to settle up",
            body = "You owe $totalOwedFormatted across $groupCount group${if (groupCount > 1) "s" else ""}.",
            key = ID_DEBT_REMINDER.toString(),
            tapAction = OpenSplitBalanceWidgetProvider.ACTION_SETTLE_UP
        )
    }

    fun sendRecurringExpenseNotification(
        context: Context, description: String, amountFormatted: String, groupId: String, groupName: String
    ) {
        show(
            context, NotificationType.RECURRING,
            title = "Recurring bill posted",
            body = "$description ($amountFormatted) was added to $groupName.",
            groupId = groupId, groupName = groupName
        )
    }
}
