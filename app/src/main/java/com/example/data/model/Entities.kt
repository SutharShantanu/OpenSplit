package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String = "",
    val avatarUrl: String = "",
    val preferredCurrency: String = "$",
    val isCurrentUser: Boolean = false
)

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val category: String = "Other", // Trip, Roommates, Couple, Family, Other
    val avatarUrl: String = "",
    val inviteLink: String = "",
    val currency: String = "$",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_members", primaryKeys = ["groupId", "userId"])
data class GroupMemberCrossRef(
    val groupId: String,
    val userId: String
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    val description: String = "",
    val amount: Double,
    val currency: String = "$",
    val date: Long = System.currentTimeMillis(),
    val paidById: String, // User ID of who paid
    val category: String = "Miscellaneous", // Food, Travel, Rent, Utilities, Shopping, Entertainment, Misc, etc.
    val attachmentPath: String? = null,
    val splitMethod: String = "EQUAL", // EQUAL, PERCENT, EXACT, SHARES, WEIGHTS
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expense_splits")
data class ExpenseSplit(
    @PrimaryKey val id: String,
    val expenseId: String,
    val userId: String,
    val owedAmount: Double,
    val percentage: Double = 0.0,
    val shares: Double = 0.0
)

@Entity(tableName = "settlements")
data class Settlement(
    @PrimaryKey val id: String,
    val groupId: String,
    val senderId: String, // User who paid to settle
    val receiverId: String, // User who received the settlement
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val paymentType: String = "CASH" // CASH, UPI, BANK_TRANSFER, MANUAL
)

@Entity(tableName = "recurring_expenses")
data class RecurringExpense(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    val amount: Double,
    val currency: String = "$",
    val paidById: String,
    val category: String = "Rent",
    val splitMethod: String = "EQUAL",
    val frequency: String = "MONTHLY", // DAILY, WEEKLY, MONTHLY, CUSTOM
    val nextDueDate: Long,
    val isActive: Boolean = true
)
