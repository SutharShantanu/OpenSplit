package com.opensplit.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {

    fun getCurrencySymbol(code: String): String = when (code.uppercase()) {
        "INR" -> "₹"
        "USD", "AUD", "CAD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY" -> "¥"
        else -> code.uppercase()
    }

    /** Number of fractional digits (minor units) for a currency. JPY has none. */
    private fun fractionDigits(code: String): Int = when (code.uppercase()) {
        "JPY" -> 0
        else -> 2
    }

    fun getCurrencyFlag(code: String): String = when (code.uppercase()) {
        "INR" -> "🇮🇳"
        "USD" -> "🇺🇸"
        "EUR" -> "🇪🇺"
        "GBP" -> "🇬🇧"
        "JPY" -> "🇯🇵"
        "AUD" -> "🇦🇺"
        "CAD" -> "🇨🇦"
        else -> "🏳️"
    }

    /**
     * Formats amount with proper comma placement, decimals, and symbol.
     * Example: 5240.0 -> "₹5,240.00" or "5,240.00/-"
     */
    fun format(
        amount: Double,
        currencyCode: String = "INR",
        showSymbol: Boolean = true,
        showSuffixDash: Boolean = false,
        showSign: Boolean = false
    ): String {
        val symbol = getCurrencySymbol(currencyCode)
        val absAmount = kotlin.math.abs(amount)

        // Indian lakh/crore grouping only for INR; standard thousands grouping otherwise.
        // Decimal places follow the currency's minor units (e.g. JPY has none).
        val digits = fractionDigits(currencyCode)
        val formattedNum = if (currencyCode.equals("INR", ignoreCase = true)) {
            formatIndianNumber(absAmount, digits)
        } else {
            val grouping = "#,##0"
            val pattern = if (digits > 0) "$grouping.${"0".repeat(digits)}" else grouping
            val formatter = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
            formatter.format(absAmount)
        }

        val base = if (showSymbol) "$symbol$formattedNum" else formattedNum
        val suffix = if (showSuffixDash) "/-" else ""
        val formatted = "$base$suffix"

        return when {
            amount < -0.001 -> "-$formatted"
            amount > 0.001 && showSign -> "+$formatted"
            else -> formatted
        }
    }

    private fun formatIndianNumber(amount: Double, decimals: Int): String {
        val rawStr = if (decimals > 0) String.format(Locale.US, "%.${decimals}f", amount) else String.format(Locale.US, "%.0f", amount)
        val parts = rawStr.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) "." + parts[1] else ""

        if (integerPart.length <= 3) {
            return integerPart + decimalPart
        }

        val lastThree = integerPart.substring(integerPart.length - 3)
        val rest = integerPart.substring(0, integerPart.length - 3)

        val sb = StringBuilder()
        var count = 0
        for (i in rest.length - 1 downTo 0) {
            if (count > 0 && count % 2 == 0) {
                sb.insert(0, ",")
            }
            sb.insert(0, rest[i])
            count++
        }
        return sb.toString() + "," + lastThree + decimalPart
    }

    fun getCurrencyDisplayName(code: String): String {
        val flag = getCurrencyFlag(code)
        val symbol = getCurrencySymbol(code)
        return "$flag $code ($symbol)"
    }
}
