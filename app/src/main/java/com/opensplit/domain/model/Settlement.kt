package com.opensplit.domain.model

import com.google.firebase.Timestamp

enum class SettlementMethod { CASH, UPI, BANK_TRANSFER, OTHER }

data class Settlement(
    val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val date: Timestamp = Timestamp.now(),
    val note: String? = null,
    val method: SettlementMethod = SettlementMethod.CASH
)
