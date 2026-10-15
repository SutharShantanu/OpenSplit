package com.opensplit.domain.model

/** Push categories the user can switch off one by one. `id` is what FCM data["type"] carries. */
enum class NotificationType(val id: String, val label: String, val description: String) {
    EXPENSE_ADDED("expense_added", "New expenses", "Someone adds an expense to a group you're in"),
    EXPENSE_EDITED("expense_edited", "Expense changes", "An expense that includes you is edited or deleted"),
    PAYMENT("payment", "Payments", "A settlement is recorded with you"),
    FRIEND_REQUEST("friend_request", "Friend requests", "Someone wants to split expenses with you"),
    MEMBER_JOINED("member_joined", "Members", "Someone joins or leaves a group"),
    REMINDER("reminder", "Reminders", "A friend nudges you to settle up"),
    RECURRING("recurring", "Recurring bills", "A recurring bill is posted");

    companion object {
        fun fromId(id: String?): NotificationType? = entries.firstOrNull { it.id == id }
    }
}
