package com.opensplit.domain.logic

import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Settlement
import kotlin.math.abs
import kotlin.math.round

/**
 * Centralized balance math for a **single currency**.
 *
 * All callers must pass expenses/settlements that share one currency (a group has a
 * single [com.opensplit.domain.model.Group.currency]; cross-currency aggregation must be
 * done per-currency by the caller, never by summing here).
 *
 * Conventions: a positive net balance means the user is **owed** money (creditor); a
 * negative balance means the user **owes** money (debtor).
 */
object BalanceCalculator {

    private const val EPSILON = 0.01

    /** Net balance per uid from expenses and settlements. */
    fun netBalances(expenses: List<Expense>, settlements: List<Settlement> = emptyList()): Map<String, Double> {
        val bals = mutableMapOf<String, Double>()

        for (expense in expenses) {
            val payers = payersOf(expense)
            for ((uid, paid) in payers) {
                bals[uid] = (bals[uid] ?: 0.0) + paid
            }
            for (split in expense.splits) {
                bals[split.uid] = (bals[split.uid] ?: 0.0) - split.amount
            }
        }

        // A settlement from A to B: A's debt shrinks (balance up), B was owed and is paid (balance down).
        for (s in settlements) {
            bals[s.fromUid] = (bals[s.fromUid] ?: 0.0) + s.amount
            bals[s.toUid] = (bals[s.toUid] ?: 0.0) - s.amount
        }

        return bals.mapValues { roundToTwoDecimals(it.value) }
    }

    /**
     * Settlement suggestions.
     *
     * When [simplify] is true, minimizes the number of payments across the whole group
     * (via [DebtSimplifier], operating on net balances).
     *
     * When false, shows the actual **pairwise** debts (who owes whom based on the real
     * expenses), netted per pair, with settlements applied. Suggestion amounts always
     * sum to the true outstanding debt — no double counting.
     */
    fun settlementSuggestions(
        expenses: List<Expense>,
        settlements: List<Settlement>,
        simplify: Boolean
    ): List<DebtSimplifier.SettlementSuggestion> {
        return if (simplify) {
            DebtSimplifier.simplify(netBalances(expenses, settlements))
        } else {
            pairwiseSuggestions(expenses, settlements)
        }
    }

    /** Payer -> amount paid for an expense (single or multi-payer). */
    private fun payersOf(expense: Expense): Map<String, Double> {
        val mp = expense.multiPayer
        return if (mp != null && mp.isNotEmpty()) mp else mapOf(expense.paidBy to expense.amount)
    }

    /**
     * Directed pairwise debt: debt[from][to] = how much `from` owes `to`.
     * Each participant owes each payer a share of what that payer covered, proportional
     * to the payer's contribution. Reciprocal debts are netted and settlements applied.
     */
    private fun pairwiseSuggestions(
        expenses: List<Expense>,
        settlements: List<Settlement>
    ): List<DebtSimplifier.SettlementSuggestion> {
        val debt = mutableMapOf<String, MutableMap<String, Double>>()

        fun add(from: String, to: String, amount: Double) {
            if (from == to || amount == 0.0) return
            val row = debt.getOrPut(from) { mutableMapOf() }
            row[to] = (row[to] ?: 0.0) + amount
        }

        for (expense in expenses) {
            val payers = payersOf(expense)
            val totalPaid = payers.values.sum()
            if (totalPaid <= 0.0) continue
            for (split in expense.splits) {
                val debtor = split.uid
                for ((payer, paid) in payers) {
                    val share = split.amount * (paid / totalPaid)
                    add(debtor, payer, share)
                }
            }
        }

        // Settlements reduce what the payer owes the recipient.
        for (s in settlements) add(s.fromUid, s.toUid, -s.amount)

        // Net reciprocal pairs into a single directed suggestion.
        val suggestions = mutableListOf<DebtSimplifier.SettlementSuggestion>()
        val seen = mutableSetOf<String>()
        val people = (debt.keys + debt.values.flatMap { it.keys }).toSet()
        for (a in people) {
            for (b in people) {
                if (a == b) continue
                val key = if (a < b) "$a|$b" else "$b|$a"
                if (!seen.add(key)) continue
                val net = (debt[a]?.get(b) ?: 0.0) - (debt[b]?.get(a) ?: 0.0)
                when {
                    net > EPSILON -> suggestions.add(DebtSimplifier.SettlementSuggestion(a, b, roundToTwoDecimals(net)))
                    net < -EPSILON -> suggestions.add(DebtSimplifier.SettlementSuggestion(b, a, roundToTwoDecimals(-net)))
                }
            }
        }
        return suggestions.sortedByDescending { it.amount }
    }

    private fun roundToTwoDecimals(value: Double): Double = round(value * 100) / 100.0
}
