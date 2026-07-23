package com.opensplit.domain.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val defaultCurrency: String = "INR",
    val fcmToken: String? = null,
    val lastSeenActivityTimestamp: Timestamp? = null
)
