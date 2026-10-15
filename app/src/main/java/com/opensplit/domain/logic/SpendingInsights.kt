package com.opensplit.domain.logic

import kotlin.math.abs
import kotlin.math.roundToInt

/** Plain-language takeaways computed locally; the AI card can elaborate but is never required. */
object SpendingInsights {
    fun describe(
        currency: String,
        monthlyShares: List<Double>,
        categoryShares: List<Pair<String, Double>>,
        totalTracked: Double,
        yourShare: Double
    ): List<String> {
        val out = mutableListOf<String>()
        val total = categoryShares.sumOf { it.second }
        categoryShares.firstOrNull()?.let { (cat, amt) ->
            if (total > 0 && amt > 0) out += "${cat} is ${(amt / total * 100).roundToInt()}% of what you spent"
        }
        if (monthlyShares.size >= 2) {
            val prev = monthlyShares[monthlyShares.size - 2]; val cur = monthlyShares.last()
            when {
                prev > 0.009 && cur > prev * 1.15 -> out += "Up ${((cur - prev) / prev * 100).roundToInt()}% from last month"
                prev > 0.009 && cur < prev * 0.85 -> out += "Down ${((prev - cur) / prev * 100).roundToInt()}% from last month"
                prev > 0.009 -> out += "About the same as last month"
            }
        }
        val nonZero = monthlyShares.filter { it > 0.009 }
        if (nonZero.size >= 3) {
            val avg = nonZero.average()
            out += "You average $currency${fmt(avg)} a month across ${nonZero.size} active months"
        }
        if (totalTracked > 0 && yourShare > 0) {
            val pct = (yourShare / totalTracked * 100).roundToInt()
            if (abs(pct - 50) > 10) out += "Your share is $pct% of everything tracked with others"
        }
        return out.take(4)
    }

    private fun fmt(v: Double) = if (v >= 1000) "%,.0f".format(v) else "%.0f".format(v)
}
