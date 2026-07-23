package com.opensplit.domain.model

import com.google.firebase.Timestamp

data class Friend(
    val uid: String = "",
    val netBalance: Double = 0.0,
    val currency: String = "INR",
    val lastUpdated: Timestamp = Timestamp.now()
)
