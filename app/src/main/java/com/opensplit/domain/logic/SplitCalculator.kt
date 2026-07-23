package com.opensplit.domain.logic

import com.opensplit.domain.model.ExpenseSplit
import com.opensplit.domain.model.SplitType
import kotlin.math.round

object SplitCalculator {

    /**
     * Calculates the splits for an expense based on the chosen split type.
     * Rounds amounts to 2 decimal places to avoid floating point issues.
     */
    fun calculateSplits(
        totalAmount: Double,
        splitType: SplitType,
        participants: List<String>,
        exactAmounts: Map<String, Double> = emptyMap(),
        percentages: Map<String, Double> = emptyMap(),
        shares: Map<String, Int> = emptyMap(),
        items: List<com.opensplit.domain.model.ExpenseItem>? = null
    ): List<ExpenseSplit> {
        if (participants.isEmpty()) return emptyList()

        return when (splitType) {
            SplitType.EQUAL -> {
                val perPerson = totalAmount / participants.size
                val roundedPerPerson = roundToTwoDecimals(perPerson)
                var remaining = totalAmount
                
                participants.mapIndexed { index, uid ->
                    val amount = if (index == participants.lastIndex) {
                        roundToTwoDecimals(remaining)
                    } else {
                        roundedPerPerson
                    }
                    remaining -= amount
                    ExpenseSplit(uid = uid, amount = amount)
                }
            }
            SplitType.EXACT -> {
                // Ensure exact amounts sum up to total
                val sum = exactAmounts.values.sum()
                if (kotlin.math.abs(sum - totalAmount) > 0.01) {
                    throw IllegalArgumentException("Exact amounts sum ($sum) does not match total amount ($totalAmount)")
                }
                participants.map { uid ->
                    ExpenseSplit(uid = uid, amount = roundToTwoDecimals(exactAmounts[uid] ?: 0.0))
                }
            }
            SplitType.PERCENTAGE -> {
                // Ensure percentages sum up to 100
                val sum = percentages.values.sum()
                if (kotlin.math.abs(sum - 100.0) > 0.01) {
                    throw IllegalArgumentException("Percentages sum ($sum) must be 100")
                }
                var remaining = totalAmount
                participants.mapIndexed { index, uid ->
                    val pct = percentages[uid] ?: 0.0
                    val amount = if (index == participants.lastIndex) {
                        roundToTwoDecimals(remaining)
                    } else {
                        roundToTwoDecimals((pct / 100.0) * totalAmount)
                    }
                    remaining -= amount
                    ExpenseSplit(uid = uid, amount = amount, percentage = pct)
                }
            }
            SplitType.ITEMIZED -> {
                val personOwed = mutableMapOf<String, Double>()
                items?.forEach { item ->
                    if (item.assignedUids.isNotEmpty()) {
                        val perPersonItem = item.price / item.assignedUids.size
                        item.assignedUids.forEach { uid ->
                            personOwed[uid] = (personOwed[uid] ?: 0.0) + perPersonItem
                        }
                    }
                }
                // Reconcile item totals to the entered amount, scaling proportionally in
                // EITHER direction: up covers tax/tip that raises the total, down covers a
                // discount/coupon that lowers it. This keeps owed == paid.
                val assignedSum = personOwed.values.sum()
                if (assignedSum > 0 && kotlin.math.abs(totalAmount - assignedSum) > 1e-9) {
                    val scale = totalAmount / assignedSum
                    personOwed.keys.forEach {
                        personOwed[it] = personOwed[it]!! * scale
                    }
                }
                // Round each and push the rounding remainder onto the last assigned
                // participant so the splits sum exactly to totalAmount.
                val assignedParticipants = participants.filter { (personOwed[it] ?: 0.0) != 0.0 }
                val amounts = mutableMapOf<String, Double>()
                var remaining = roundToTwoDecimals(totalAmount)
                assignedParticipants.forEachIndexed { index, uid ->
                    val amount = if (index == assignedParticipants.lastIndex) {
                        roundToTwoDecimals(remaining)
                    } else {
                        roundToTwoDecimals(personOwed[uid] ?: 0.0)
                    }
                    remaining -= amount
                    amounts[uid] = amount
                }
                participants.map { uid ->
                    ExpenseSplit(uid = uid, amount = amounts[uid] ?: 0.0)
                }
            }
            SplitType.SHARES -> {
                val totalShares = shares.values.sum()
                if (totalShares <= 0) {
                     throw IllegalArgumentException("Total shares must be greater than 0")
                }
                var remaining = totalAmount
                participants.mapIndexed { index, uid ->
                    val share = shares[uid] ?: 0
                    val amount = if (index == participants.lastIndex) {
                        roundToTwoDecimals(remaining)
                    } else {
                         roundToTwoDecimals((share.toDouble() / totalShares.toDouble()) * totalAmount)
                    }
                    remaining -= amount
                    ExpenseSplit(uid = uid, amount = amount, shares = share)
                }
            }
        }
    }

    private fun roundToTwoDecimals(value: Double): Double {
        return round(value * 100) / 100.0
    }
}
