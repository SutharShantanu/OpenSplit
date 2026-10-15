package com.opensplit.domain.logic

import com.google.firebase.Timestamp
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.RecurrenceFrequency
import java.util.Calendar

/** Single place that knows how far one recurrence step moves a date. */
object RecurrenceScheduler {
    fun next(from: Timestamp, frequency: RecurrenceFrequency): Timestamp = Timestamp(nextMillis(from.toDate().time, frequency) / 1000, 0)

    fun nextMillis(fromMillis: Long, frequency: RecurrenceFrequency): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = fromMillis }
        when (frequency) {
            RecurrenceFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceFrequency.FORTNIGHTLY -> cal.add(Calendar.WEEK_OF_YEAR, 2)
            RecurrenceFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurrenceFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
            RecurrenceFrequency.NONE -> Unit
        }
        return cal.timeInMillis
    }

    fun label(frequency: RecurrenceFrequency): String = when (frequency) {
        RecurrenceFrequency.NONE -> "Never"
        RecurrenceFrequency.DAILY -> "Daily"
        RecurrenceFrequency.WEEKLY -> "Weekly"
        RecurrenceFrequency.FORTNIGHTLY -> "Fortnightly"
        RecurrenceFrequency.MONTHLY -> "Monthly"
        RecurrenceFrequency.YEARLY -> "Yearly"
    }
}

/** Human summary of what changed between two versions of an expense, for the edit history. */
object ExpenseDiff {
    fun describe(old: Expense, new: Expense, nameOf: (String) -> String = { it }): List<String> {
        val out = mutableListOf<String>()
        if (old.description != new.description) out += "Renamed \"${old.description}\" to \"${new.description}\""
        if (kotlin.math.abs(old.amount - new.amount) > 0.004 || old.currency != new.currency) {
            out += "Amount ${fmt(old.amount)} ${old.currency} to ${fmt(new.amount)} ${new.currency}"
        }
        if (old.category != new.category) out += "Category ${old.category} to ${new.category}"
        if (old.paidBy != new.paidBy) out += "Paid by ${nameOf(old.paidBy)} to ${nameOf(new.paidBy)}"
        if (old.splitType != new.splitType) out += "Split ${old.splitType.name.lowercase()} to ${new.splitType.name.lowercase()}"
        val oldWho = old.splits.map { it.uid }.toSet(); val newWho = new.splits.map { it.uid }.toSet()
        (newWho - oldWho).forEach { out += "Added ${nameOf(it)} to the split" }
        (oldWho - newWho).forEach { out += "Removed ${nameOf(it)} from the split" }
        if (old.date.seconds / 86400 != new.date.seconds / 86400) out += "Date changed"
        if ((old.notes ?: "") != (new.notes ?: "")) out += "Notes updated"
        if (old.receiptImageUrl != new.receiptImageUrl) out += if (new.receiptImageUrl == null) "Receipt removed" else "Receipt updated"
        if (old.recurrence?.frequency != new.recurrence?.frequency) out += "Repeats ${RecurrenceScheduler.label(new.recurrence?.frequency ?: RecurrenceFrequency.NONE).lowercase()}"
        if (old.recurrence != null && new.recurrence != null && old.recurrence.paused != new.recurrence.paused) out += if (new.recurrence.paused) "Recurring paused" else "Recurring resumed"
        return out
    }

    private fun fmt(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else "%.2f".format(v)
}
