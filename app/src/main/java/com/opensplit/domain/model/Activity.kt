package com.opensplit.domain.model

import com.google.firebase.Timestamp

enum class ActivityType {
    EXPENSE_ADDED, EXPENSE_EDITED, EXPENSE_DELETED,
    SETTLEMENT_ADDED, MEMBER_ADDED, MEMBER_REMOVED, GROUP_CREATED, COMMENT_ADDED
}

data class Activity(
    val id: String = "",
    val type: ActivityType = ActivityType.GROUP_CREATED,
    val actorUid: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val relatedExpenseId: String? = null
)
