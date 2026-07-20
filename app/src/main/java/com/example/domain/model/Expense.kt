package com.example.domain.model

import com.google.firebase.Timestamp

enum class SplitType { EQUAL, EXACT, PERCENTAGE, SHARES }

data class ExpenseSplit(
    val uid: String = "",
    val amount: Double = 0.0,
    val percentage: Double? = null,
    val shares: Int? = null
)

data class Expense(
    val id: String = "",
    val groupId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val paidBy: String = "",
    val multiPayer: Map<String, Double>? = null, // if null, paidBy is used
    val splitType: SplitType = SplitType.EQUAL,
    val splits: List<ExpenseSplit> = emptyList(),
    val category: String = "Other",
    val date: Timestamp = Timestamp.now(),
    val notes: String? = null,
    val receiptImageUrl: String? = null,
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val isDeleted: Boolean = false
)
