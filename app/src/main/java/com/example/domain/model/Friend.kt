package com.example.domain.model

import com.google.firebase.Timestamp

data class Friend(
    val uid: String = "",
    val netBalance: Double = 0.0,
    val currency: String = "USD",
    val lastUpdated: Timestamp = Timestamp.now()
)
