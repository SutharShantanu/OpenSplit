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
        val grouping = if (currencyCode.equals("INR", ignoreCase = true)) "#,##,##0" else "#,##0"
        val digits = fractionDigits(currencyCode)
        val pattern = if (digits > 0) "$grouping.${"0".repeat(digits)}" else grouping
        val formatter = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        val formattedNum = formatter.format(absAmount)

        val base = if (showSymbol) "$symbol$formattedNum" else formattedNum
        val suffix = if (showSuffixDash) "/-" else ""
        val formatted = "$base$suffix"

        return when {
            amount < -0.001 -> "-$formatted"
            amount > 0.001 && showSign -> "+$formatted"
            else -> formatted
        }
    }

    fun getCurrencyDisplayName(code: String): String {
        val flag = getCurrencyFlag(code)
        val symbol = getCurrencySymbol(code)
        return "$flag $code ($symbol)"
    }
}
