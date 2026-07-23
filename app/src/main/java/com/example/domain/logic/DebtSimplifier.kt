package com.example.domain.logic

import kotlin.math.abs
import kotlin.math.min

object DebtSimplifier {
    data class SettlementSuggestion(val fromUid: String, val toUid: String, val amount: Double)

    fun simplify(balances: Map<String, Double>): List<SettlementSuggestion> {
        val epsilon = 0.01
        // We only care about non-zero balances
        val filteredBalances = balances.filterValues { abs(it) >= epsilon }
        
        // Debtors are those with negative balance (they owe money)
        val debtors = filteredBalances.filterValues { it <= -epsilon }
            .map { it.key to abs(it.value) }
            .sortedByDescending { it.second }
            .toMutableList()

        // Creditors are those with positive balance (they are owed money)
        val creditors = filteredBalances.filterValues { it >= epsilon }
            .map { it.key to it.value }
            .sortedByDescending { it.second }
            .toMutableList()

        val suggestions = mutableListOf<SettlementSuggestion>()

        var i = 0
        var j = 0

        while (i < debtors.size && j < creditors.size) {
            val debtor = debtors[i]
            val creditor = creditors[j]

            val settleAmount = min(debtor.second, creditor.second)
            
            if (settleAmount >= epsilon) {
                suggestions.add(
                    SettlementSuggestion(
                        fromUid = debtor.first,
                        toUid = creditor.first,
                        amount = roundToTwoDecimals(settleAmount)
                    )
                )
            }

            val newDebtorAmount = debtor.second - settleAmount
            val newCreditorAmount = creditor.second - settleAmount

            if (newDebtorAmount < epsilon) {
                i++
            } else {
                debtors[i] = debtor.first to newDebtorAmount
            }

            if (newCreditorAmount < epsilon) {
                j++
            } else {
                creditors[j] = creditor.first to newCreditorAmount
            }
        }

        return suggestions
    }

    private fun roundToTwoDecimals(value: Double): Double {
        return kotlin.math.round(value * 100) / 100.0
    }
}
