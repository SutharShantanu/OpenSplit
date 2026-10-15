package com.opensplit.service

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.flow.first
import com.google.firebase.messaging.RemoteMessage
import com.opensplit.R

/**
 * Receives FCM messages and shows a local notification, and keeps the signed-in user's
 * fcmToken up to date in Firestore.
 *
 * Note: delivering a push to *other* users (e.g. "X added an expense") requires a
 * server component that calls FCM with the recipients' tokens — client SDKs cannot send
 * to other devices. See functions/index.js and docs/SETUP.md. Without that function,
 * only messages sent to this device (or test sends from the Firebase console) appear.
 */
class OpenSplitMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .update("fcmToken", token)
        } catch (e: Exception) {
            // Broker or firestore disconnected
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "OpenSplit"
        val body = message.notification?.body ?: message.data["body"] ?: return
        val type = message.data["type"]
        val groupId = message.data["groupId"]
        // Honour the user's per-type and per-group switches before showing anything.
        val prefs = (application as? com.opensplit.OpenSplitApp)?.container?.userPreferencesRepository
        if (prefs != null) {
            val allowed = kotlinx.coroutines.runBlocking {
                val enabled = prefs.notificationsEnabledFlow.first()
                val disabledTypes = prefs.disabledNotificationTypesFlow.first()
                val muted = prefs.mutedGroupIdsFlow.first()
                enabled && (type == null || type !in disabledTypes) && (groupId == null || groupId !in muted)
            }
            if (!allowed) return
        }
        showNotification(title, body, groupId, message.data["expenseId"])
    }

    private fun showNotification(title: String, body: String, groupId: String?, expenseId: String?) {
        val open = android.content.Intent(this, com.opensplit.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (groupId != null) putExtra("groupId", groupId)
            if (expenseId != null) putExtra("expenseId", expenseId)
        }
        val pending = android.app.PendingIntent.getActivity(
            this, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), open,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted — nothing to show.
        }
    }

    companion object {
        const val CHANNEL_ID = "opensplit_activity"
    }
}
