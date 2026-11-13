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
                // Merge, not update: the profile doc may not exist yet.
                .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            // Broker or firestore disconnected
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "OpenSplit"
        val body = message.notification?.body ?: message.data["body"] ?: return
        val type = com.opensplit.domain.model.NotificationType.fromId(message.data["type"])
        val groupId = message.data["groupId"]
        // Honour the user's master, per-type and per-group switches before showing anything.
        if (!kotlinx.coroutines.runBlocking { NotificationHelper.isAllowed(this@OpenSplitMessagingService, type, groupId) }) return
        NotificationHelper.show(
            this, type, title, body,
            groupId = groupId,
            groupName = message.data["groupName"]?.ifBlank { null },
            key = message.data["expenseId"]?.ifBlank { null }
        )
    }

}
