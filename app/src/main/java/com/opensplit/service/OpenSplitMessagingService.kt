package com.opensplit.service

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .update("fcmToken", token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "OpenSplit"
        val body = message.notification?.body ?: message.data["body"] ?: return
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
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
