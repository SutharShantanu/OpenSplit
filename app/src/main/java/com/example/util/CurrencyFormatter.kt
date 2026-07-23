package com.example.util

import java.text.DecimalFormat
import java.util.Locale

object CurrencyFormatter {

    fun getCurrencySymbol(code: String): String = when (code.uppercase()) {
        "INR" -> "₹"
        "USD", "AUD", "CAD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY" -> "¥"
        else -> "₹"
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
        
        // Format with commas and 2 decimal places
        val formatter = DecimalFormat("#,##,##0.00")
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
