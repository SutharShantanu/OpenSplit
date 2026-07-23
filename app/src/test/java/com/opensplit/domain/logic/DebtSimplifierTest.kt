package com.opensplit.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class DebtSimplifierTest {

    private fun verifyBalances(
        initialBalances: Map<String, Double>,
        suggestions: List<DebtSimplifier.SettlementSuggestion>
    ) {
        val finalBalances = initialBalances.toMutableMap()
        for (suggestion in suggestions) {
            finalBalances[suggestion.fromUid] = (finalBalances[suggestion.fromUid] ?: 0.0) + suggestion.amount
            finalBalances[suggestion.toUid] = (finalBalances[suggestion.toUid] ?: 0.0) - suggestion.amount
        }
        
        for ((_, balance) in finalBalances) {
            assertEquals(0.0, balance, 0.02)
        }
    }

    @Test
    fun testTwoPeople() {
        val balances = mapOf(
            "A" to -10.0,
            "B" to 10.0
        )
        val suggestions = DebtSimplifier.simplify(balances)
        assertEquals(1, suggestions.size)
        assertEquals("A", suggestions[0].fromUid)
        assertEquals("B", suggestions[0].toUid)
        assertEquals(10.0, suggestions[0].amount, 0.01)
        verifyBalances(balances, suggestions)
    }

    @Test
    fun testThreePersonCycle() {
        // A owes B 10, B owes C 10, C owes A 10
        // Net: 0 for all
        val balances = mapOf(
            "A" to 0.0,
            "B" to 0.0,
            "C" to 0.0
        )
        val suggestions = DebtSimplifier.simplify(balances)
        assertEquals(0, suggestions.size)
        verifyBalances(balances, suggestions)
        
        // Let's do A owes B 10, B owes C 10
        // Net: A: -10, B: 0, C: +10
        val balances2 = mapOf(
            "A" to -10.0,
            "B" to 0.0,
            "C" to 10.0
        )
        val suggestions2 = DebtSimplifier.simplify(balances2)
        assertEquals(1, suggestions2.size)
        assertEquals("A", suggestions2[0].fromUid)
        assertEquals("C", suggestions2[0].toUid)
        assertEquals(10.0, suggestions2[0].amount, 0.01)
        verifyBalances(balances2, suggestions2)
    }

    @Test
    fun testAlreadySettled() {
        val balances = mapOf(
            "A" to 0.0,
            "B" to 0.001,
            "C" to -0.001
        )
        val suggestions = DebtSimplifier.simplify(balances)
        assertEquals(0, suggestions.size)
        verifyBalances(balances, suggestions)
    }

    @Test
    fun testOneOwesEveryone() {
        val balances = mapOf(
            "A" to -30.0,
            "B" to 10.0,
            "C" to 10.0,
            "D" to 10.0
        )
        val suggestions = DebtSimplifier.simplify(balances)
        assertEquals(3, suggestions.size)
        verifyBalances(balances, suggestions)
    }
}
