package com.opensplit.domain.model

import com.google.firebase.Timestamp

data class PendingInvite(
    val id: String = "",
    val groupId: String = "",
    val email: String = "",
    val invitedBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp(Timestamp.now().seconds + 7 * 24 * 3600, 0)
)
