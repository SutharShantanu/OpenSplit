package com.opensplit.domain.logic

import com.opensplit.domain.model.ExpenseItem
import com.opensplit.domain.model.SplitType
import org.junit.Assert.*
import org.junit.Test

class SplitCalculatorTest {

    @Test
    fun testEqualSplit() {
        val participants = listOf("user1", "user2", "user3")
        val splits = SplitCalculator.calculateSplits(
            totalAmount = 100.0,
            splitType = SplitType.EQUAL,
            participants = participants
        )
        assertEquals(3, splits.size)
        val sum = splits.sumOf { it.amount }
        assertEquals(100.0, sum, 0.001)
    }

    @Test
    fun testExactSplitValid() {
        val participants = listOf("user1", "user2")
        val exact = mapOf("user1" to 60.0, "user2" to 40.0)
        val splits = SplitCalculator.calculateSplits(
            totalAmount = 100.0,
            splitType = SplitType.EXACT,
            participants = participants,
            exactAmounts = exact
        )
        assertEquals(60.0, splits.first { it.uid == "user1" }.amount, 0.001)
        assertEquals(40.0, splits.first { it.uid == "user2" }.amount, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testExactSplitInvalidSum() {
        val participants = listOf("user1", "user2")
        val exact = mapOf("user1" to 50.0, "user2" to 40.0)
        SplitCalculator.calculateSplits(
            totalAmount = 100.0,
            splitType = SplitType.EXACT,
            participants = participants,
            exactAmounts = exact
        )
    }

    @Test
    fun testPercentageSplitValid() {
        val participants = listOf("user1", "user2")
        val pct = mapOf("user1" to 70.0, "user2" to 30.0)
        val splits = SplitCalculator.calculateSplits(
            totalAmount = 200.0,
            splitType = SplitType.PERCENTAGE,
            participants = participants,
            percentages = pct
        )
        assertEquals(140.0, splits.first { it.uid == "user1" }.amount, 0.001)
        assertEquals(60.0, splits.first { it.uid == "user2" }.amount, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPercentageSplitInvalid() {
        val participants = listOf("user1", "user2")
        val pct = mapOf("user1" to 50.0, "user2" to 40.0)
        SplitCalculator.calculateSplits(
            totalAmount = 100.0,
            splitType = SplitType.PERCENTAGE,
            participants = participants,
            percentages = pct
        )
    }

    @Test
    fun testSharesSplit() {
        val participants = listOf("user1", "user2")
        val shares = mapOf("user1" to 3, "user2" to 1)
        val splits = SplitCalculator.calculateSplits(
            totalAmount = 100.0,
            splitType = SplitType.SHARES,
            participants = participants,
            shares = shares
        )
        assertEquals(75.0, splits.first { it.uid == "user1" }.amount, 0.001)
        assertEquals(25.0, splits.first { it.uid == "user2" }.amount, 0.001)
    }

    @Test
    fun testItemizedSplit() {
        val participants = listOf("user1", "user2")
        val items = listOf(
            ExpenseItem(id = "1", name = "Pizza", price = 20.0, assignedUids = listOf("user1", "user2")),
            ExpenseItem(id = "2", name = "Soda", price = 5.0, assignedUids = listOf("user1"))
        )
        val splits = SplitCalculator.calculateSplits(
            totalAmount = 25.0,
            splitType = SplitType.ITEMIZED,
            participants = participants,
            items = items
        )
        assertEquals(15.0, splits.first { it.uid == "user1" }.amount, 0.001)
        assertEquals(10.0, splits.first { it.uid == "user2" }.amount, 0.001)
    }

    @Test
    fun testItemizedScalesUpForTaxTip() {
        // Items total 25, but the entered amount is 30 (tax/tip). Splits must sum to 30.
        val participants = listOf("user1", "user2")
        val items = listOf(
            ExpenseItem(id = "1", name = "Pizza", price = 20.0, assignedUids = listOf("user1", "user2")),
            ExpenseItem(id = "2", name = "Soda", price = 5.0, assignedUids = listOf("user1"))
        )
        val splits = SplitCalculator.calculateSplits(
            totalAmount = 30.0,
            splitType = SplitType.ITEMIZED,
            participants = participants,
            items = items
        )
        assertEquals(30.0, splits.sumOf { it.amount }, 0.001)
        assertEquals(18.0, splits.first { it.uid == "user1" }.amount, 0.001)
        assertEquals(12.0, splits.first { it.uid == "user2" }.amount, 0.001)
    }

    @Test
    fun testItemizedScalesDownForDiscount() {
        // Items total 25, but a discount brings the entered amount to 20. Splits must sum to 20
        // (previously the code only scaled up, leaving owed != paid).
        val participants = listOf("user1", "user2")
        val items = listOf(
            ExpenseItem(id = "1", name = "Pizza", price = 20.0, assignedUids = listOf("user1", "user2")),
            ExpenseItem(id = "2", name = "Soda", price = 5.0, assignedUids = listOf("user1"))
        )
        val splits = SplitCalculator.calculateSplits(
            totalAmount = 20.0,
            splitType = SplitType.ITEMIZED,
            participants = participants,
            items = items
        )
        assertEquals(20.0, splits.sumOf { it.amount }, 0.001)
        assertEquals(12.0, splits.first { it.uid == "user1" }.amount, 0.001)
        assertEquals(8.0, splits.first { it.uid == "user2" }.amount, 0.001)
    }

    @Test
    fun testEqualSplitRemainderReconciles() {
        // 100 / 3 does not divide evenly; splits must still sum exactly to 100.
        val participants = listOf("a", "b", "c")
        val splits = SplitCalculator.calculateSplits(
            totalAmount = 100.0,
            splitType = SplitType.EQUAL,
            participants = participants
        )
        assertEquals(100.0, splits.sumOf { it.amount }, 0.0001)
    }
}
