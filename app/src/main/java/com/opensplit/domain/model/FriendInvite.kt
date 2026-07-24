package com.opensplit.domain.model

import com.google.firebase.Timestamp

/**
 * A standalone friend invite sent by email (independent of any group).
 * Managed by the inviter; expires after 7 days by default.
 */
data class FriendInvite(
    val id: String = "",
    val inviterUid: String = "",
    val email: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp(Timestamp.now().seconds + 7 * 24 * 3600, 0)
)
