package com.example.domain.logic

import com.example.domain.model.ExpenseSplit
import com.example.domain.model.SplitType
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
        items: List<com.example.domain.model.ExpenseItem>? = null
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
                // Distribute remaining amount (e.g. tax/tip) equally among participants who had items assigned
                // Wait, prompt says: "an optional even split of tax/tip across all items proportionally". 
                // Let us just compute a simple version: sum of item splits. Tax/tip can be handled by scaling.
                val assignedSum = personOwed.values.sum()
                if (assignedSum > 0 && totalAmount > assignedSum) {
                    val scale = totalAmount / assignedSum
                    personOwed.keys.forEach {
                        personOwed[it] = personOwed[it]!! * scale
                    }
                }
                participants.map { uid ->
                    ExpenseSplit(uid = uid, amount = roundToTwoDecimals(personOwed[uid] ?: 0.0))
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
