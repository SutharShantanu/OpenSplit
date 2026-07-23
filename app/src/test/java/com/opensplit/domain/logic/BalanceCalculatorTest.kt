package com.opensplit.domain.logic

import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.ExpenseSplit
import com.opensplit.domain.model.Settlement
import org.junit.Assert.*
import org.junit.Test

class BalanceCalculatorTest {

    private fun expense(paidBy: String, amount: Double, splits: List<Pair<String, Double>>) =
        Expense(
            id = "e${splits.hashCode()}",
            groupId = "g",
            amount = amount,
            paidBy = paidBy,
            splits = splits.map { ExpenseSplit(uid = it.first, amount = it.second) }
        )

    @Test
    fun netBalances_basic() {
        val expenses = listOf(expense("a", 100.0, listOf("a" to 50.0, "b" to 50.0)))
        val net = BalanceCalculator.netBalances(expenses)
        assertEquals(50.0, net["a"]!!, 0.001)   // a is owed 50
        assertEquals(-50.0, net["b"]!!, 0.001)  // b owes 50
    }

    @Test
    fun netBalances_appliesSettlements() {
        val expenses = listOf(expense("a", 100.0, listOf("a" to 50.0, "b" to 50.0)))
        val settlements = listOf(Settlement(fromUid = "b", toUid = "a", amount = 50.0))
        val net = BalanceCalculator.netBalances(expenses, settlements)
        assertEquals(0.0, net["a"]!!, 0.001)
        assertEquals(0.0, net["b"]!!, 0.001)
    }

    @Test
    fun pairwiseSuggestions_singleDebt() {
        val expenses = listOf(expense("a", 100.0, listOf("a" to 50.0, "b" to 50.0)))
        val suggestions = BalanceCalculator.settlementSuggestions(expenses, emptyList(), simplify = false)
        assertEquals(1, suggestions.size)
        val s = suggestions.first()
        assertEquals("b", s.fromUid)
        assertEquals("a", s.toUid)
        assertEquals(50.0, s.amount, 0.001)
    }

    @Test
    fun pairwiseSuggestions_doNotDoubleCount() {
        // a paid 30 split three ways; b paid 30 split three ways.
        // Net: a +10, b +10, c -20. c should owe exactly 20 total, no over-collection.
        val expenses = listOf(
            expense("a", 30.0, listOf("a" to 10.0, "b" to 10.0, "c" to 10.0)),
            expense("b", 30.0, listOf("a" to 10.0, "b" to 10.0, "c" to 10.0))
        )
        val suggestions = BalanceCalculator.settlementSuggestions(expenses, emptyList(), simplify = false)
        val total = suggestions.sumOf { it.amount }
        assertEquals(20.0, total, 0.001)
        // c is the only debtor.
        assertTrue(suggestions.all { it.fromUid == "c" })
    }

    @Test
    fun simplifySuggestions_sumToTotalDebt() {
        val expenses = listOf(
            expense("a", 30.0, listOf("a" to 10.0, "b" to 10.0, "c" to 10.0)),
            expense("b", 30.0, listOf("a" to 10.0, "b" to 10.0, "c" to 10.0))
        )
        val suggestions = BalanceCalculator.settlementSuggestions(expenses, emptyList(), simplify = true)
        assertEquals(20.0, suggestions.sumOf { it.amount }, 0.001)
    }

    @Test
    fun settlementSuggestions_settledGroupHasNone() {
        val expenses = listOf(expense("a", 100.0, listOf("a" to 50.0, "b" to 50.0)))
        val settlements = listOf(Settlement(fromUid = "b", toUid = "a", amount = 50.0))
        val suggestions = BalanceCalculator.settlementSuggestions(expenses, settlements, simplify = false)
        assertTrue(suggestions.isEmpty())
    }
}
