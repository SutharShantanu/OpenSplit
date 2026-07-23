package com.example.domain.model

import com.google.firebase.Timestamp

enum class SplitType { EQUAL, EXACT, PERCENTAGE, SHARES, ITEMIZED }

data class ExpenseSplit(
    val uid: String = "",
    val amount: Double = 0.0,
    val percentage: Double? = null,
    val shares: Int? = null
)

data class ExpenseItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val assignedUids: List<String> = emptyList()
)

enum class RecurrenceFrequency { NONE, DAILY, WEEKLY, MONTHLY }
data class RecurrenceRule(val frequency: RecurrenceFrequency, val nextOccurrence: Timestamp)

data class Comment(
    val id: String = "",
    val uid: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class Expense(
    val id: String = "",
    val groupId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val paidBy: String = "",
    val multiPayer: Map<String, Double>? = null, 
    val splitType: SplitType = SplitType.EQUAL,
    val splits: List<ExpenseSplit> = emptyList(),
    val items: List<ExpenseItem>? = null,
    val recurrence: RecurrenceRule? = null,
    val category: String = "Other",
    val date: Timestamp = Timestamp.now(),
    val notes: String? = null,
    val receiptImageUrl: String? = null,
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val isDeleted: Boolean = false
)
