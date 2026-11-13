package com.opensplit.service

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.opensplit.MainActivity
import com.opensplit.OpenSplitApp
import com.opensplit.R
import com.opensplit.data.payment.DetectedPayment
import com.opensplit.data.payment.Direction
import com.opensplit.data.payment.PaymentParser
import com.opensplit.util.CurrencyFormatter
import com.opensplit.widget.OpenSplitBalanceWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reads notifications from a fixed list of UPI and bank apps and turns completed payments
 * into reviewable records. Every other app's notifications are ignored before their text is
 * read, and the text is never stored or logged.
 */
class PaymentNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = n.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val source = PaymentDetector.sourceFor(sbn.packageName, title) ?: return
        val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString() ?: return
        // UPI apps often put the amount in the title ("₹500 paid to Rahul"); SMS titles are the sender ID.
        val text = if (source.endsWith("SMS")) body else "$title. $body"
        scope.launch { PaymentDetector.handle(applicationContext, text, source) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

object PaymentDetector {
    /** UPI and bank apps whose notifications may be read, with the name shown to the user. */
    val SOURCES = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "GPay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.org.npci.upiapp" to "BHIM",
        "in.amazon.mShop.android.shopping" to "Amazon Pay",
        "com.dreamplug.androidapp" to "CRED",
        "com.sbi.lotusintouch" to "SBI YONO",
        "com.ge.capital.konysbiapp" to "SBI Card",
        "com.snapwork.hdfc" to "HDFC Bank",
        "com.hdfcbank.android.now" to "HDFC Bank",
        "com.csam.icici.bank.imobile" to "ICICI Bank",
        "com.axis.mobile" to "Axis Bank",
        "com.msf.kbank.mobile" to "Kotak",
        "com.idfcfirstbank.optimus" to "IDFC FIRST",
        "com.fedmobile" to "Federal Bank",
        "com.YesBank" to "Yes Bank"
    )

    /** SMS apps: only messages from bank sender IDs such as "AX-HDFCBK" are read, never personal chats. */
    private val SMS_APPS = setOf("com.google.android.apps.messaging", "com.samsung.android.messaging")
    private val BANK_SENDER = Regex("""^[A-Z]{2}-[A-Z0-9]{3,9}(-[A-Z])?$""")

    fun sourceFor(packageName: String?, title: String): String? = when (packageName) {
        null -> null
        in SMS_APPS -> if (BANK_SENDER.matches(title.trim())) "Bank SMS" else null
        else -> SOURCES[packageName]
    }

    /** True when the user has turned on notification access for OpenSplit in system settings. */
    fun hasAccess(context: Context) =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun accessSettingsIntent(context: Context): Intent =
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, PaymentNotificationListener::class.java).flattenToString())
        } else Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    suspend fun handle(context: Context, text: String, source: String) {
        val container = (context.applicationContext as? OpenSplitApp)?.container ?: return
        val prefs = container.userPreferencesRepository
        if (!prefs.paymentDetectionFlow.first()) return
        val parsed = PaymentParser.parse(text, source, prefs.detectRefundsFlow.first()) ?: return
        if (parsed.direction == Direction.CREDIT && !prefs.detectCreditsFlow.first()) return
        val new = container.detectedPayments.record(parsed) ?: return
        // In the app, MainActivity shows a banner from DetectedPaymentStore.detected instead.
        if (!AppVisibility.foreground) notify(context, new)
    }

    fun summary(p: DetectedPayment): String {
        val amount = CurrencyFormatter.format(p.amount, "INR")
        val who = p.counterparty?.let { if (p.direction == Direction.DEBIT) " to $it" else " from $it" }.orEmpty()
        return (if (p.direction == Direction.DEBIT) "Paid $amount$who" else "Received $amount$who") + " · ${p.source}"
    }

    const val ACTION_EXPENSE_PREFIX = "payment_expense:"
    const val ACTION_SETTLE_PREFIX = "payment_settle:"
    const val ACTION_REVIEW = "payment_review"

    private fun notify(context: Context, p: DetectedPayment) {
        fun open(action: String, code: Int) = PendingIntent.getActivity(
            context, code,
            Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(OpenSplitBalanceWidgetProvider.EXTRA_WIDGET_ACTION, action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val id = (p.id % 100_000).toInt()
        val ignore = PendingIntent.getBroadcast(
            context, 300_000 + id,
            Intent(context, PaymentActionReceiver::class.java).putExtra(PaymentActionReceiver.EXTRA_ID, p.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_PAYMENT_DETECTION)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(context.getColor(R.color.ic_launcher_fg))
            .setContentTitle(if (p.direction == Direction.DEBIT) "Payment detected" else "Money received")
            .setContentText(summary(p))
            .setContentIntent(open(ACTION_REVIEW, 100_000 + id))
            .setAutoCancel(true)
            .addAction(0, "Add expense", open(ACTION_EXPENSE_PREFIX + p.id, 200_000 + id))
            .addAction(0, "Settle up", open(ACTION_SETTLE_PREFIX + p.id, 250_000 + id))
            .addAction(0, "Ignore", ignore)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(PaymentActionReceiver.notificationId(p.id), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted: the payment still waits in Settings › Payment detection.
        }
    }
}

/** "Ignore" on a payment notification. */
class PaymentActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1).takeIf { it > 0 } ?: return
        (context.applicationContext as? OpenSplitApp)?.container?.detectedPayments?.setStatus(id, DetectedPayment.Status.IGNORED)
        NotificationManagerCompat.from(context).cancel(notificationId(id))
    }

    companion object {
        const val EXTRA_ID = "payment_id"
        fun notificationId(paymentId: Long) = 500_000 + (paymentId % 100_000).toInt()
    }
}

/** Whether an OpenSplit screen is visible; MainActivity updates it in onStart/onStop. */
object AppVisibility {
    @Volatile var foreground = false
}
