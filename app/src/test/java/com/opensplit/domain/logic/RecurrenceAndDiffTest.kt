package com.opensplit.domain.logic

import com.google.firebase.Timestamp
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.ExpenseSplit
import com.opensplit.domain.model.RecurrenceFrequency
import com.opensplit.domain.model.RecurrenceRule
import com.opensplit.domain.model.SplitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RecurrenceAndDiffTest {
    @Test
    fun `scheduler steps by calendar unit`() {
        val jan31 = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 31, 9, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        fun step(f: RecurrenceFrequency) = Calendar.getInstance().apply { timeInMillis = RecurrenceScheduler.nextMillis(jan31, f) }
        assertEquals(Calendar.FEBRUARY, step(RecurrenceFrequency.MONTHLY).get(Calendar.MONTH))
        assertEquals(28, step(RecurrenceFrequency.MONTHLY).get(Calendar.DAY_OF_MONTH))
        assertEquals(14, step(RecurrenceFrequency.FORTNIGHTLY).get(Calendar.DAY_OF_MONTH))
        assertEquals(2027, step(RecurrenceFrequency.YEARLY).get(Calendar.YEAR))
        assertEquals(jan31, RecurrenceScheduler.nextMillis(jan31, RecurrenceFrequency.NONE))
    }

    @Test
    fun `diff names every changed field and nothing else`() {
        val old = Expense(id = "e", description = "Dinner", amount = 100.0, paidBy = "a", splitType = SplitType.EQUAL,
            splits = listOf(ExpenseSplit("a", 50.0), ExpenseSplit("b", 50.0)), category = "Food", date = Timestamp(1_000_000, 0))
        val new = old.copy(description = "Dinner at Social", amount = 120.0, paidBy = "b",
            splits = listOf(ExpenseSplit("a", 40.0), ExpenseSplit("b", 40.0), ExpenseSplit("c", 40.0)),
            recurrence = RecurrenceRule(RecurrenceFrequency.MONTHLY))
        val names = mapOf("a" to "Ana", "b" to "Ben", "c" to "Cy")
        val changes = ExpenseDiff.describe(old, new) { names[it] ?: it }
        assertEquals(listOf(
            "Renamed \"Dinner\" to \"Dinner at Social\"",
            "Amount 100 INR to 120 INR",
            "Paid by Ana to Ben",
            "Added Cy to the split",
            "Repeats monthly"
        ), changes)
        assertTrue(ExpenseDiff.describe(old, old).isEmpty())
    }
}
