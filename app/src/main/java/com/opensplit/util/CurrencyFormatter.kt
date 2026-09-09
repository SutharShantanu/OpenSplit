package com.opensplit.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CurrencyData(
    val code: String,
    val symbol: String,
    val name: String,
    val flag: String,
    val rateToUsd: Double,
    val fractionDigits: Int = 2
)

object CurrencyFormatter {

    val ALL_CURRENCIES: List<CurrencyData> = listOf(
        CurrencyData("USD", "$", "US Dollar", "🇺🇸", 1.0, 2),
        CurrencyData("EUR", "€", "Euro", "🇪🇺", 0.92, 2),
        CurrencyData("GBP", "£", "British Pound", "🇬🇧", 0.79, 2),
        CurrencyData("INR", "₹", "Indian Rupee", "🇮🇳", 86.80, 2),
        CurrencyData("JPY", "¥", "Japanese Yen", "🇯🇵", 152.40, 0),
        CurrencyData("CAD", "C$", "Canadian Dollar", "🇨🇦", 1.38, 2),
        CurrencyData("AUD", "A$", "Australian Dollar", "🇦🇺", 1.54, 2),
        CurrencyData("CHF", "CHF", "Swiss Franc", "🇨🇭", 0.89, 2),
        CurrencyData("CNY", "¥", "Chinese Yuan", "🇨🇳", 7.24, 2),
        CurrencyData("SGD", "S$", "Singapore Dollar", "🇸🇬", 1.34, 2),
        CurrencyData("AED", "د.إ", "UAE Dirham", "🇦🇪", 3.67, 2),
        CurrencyData("SAR", "﷼", "Saudi Riyal", "🇸🇦", 3.75, 2),
        CurrencyData("NZD", "NZ$", "New Zealand Dollar", "🇳🇿", 1.68, 2),
        CurrencyData("BRL", "R$", "Brazilian Real", "🇧🇷", 5.72, 2),
        CurrencyData("ZAR", "R", "South African Rand", "🇿🇦", 18.20, 2),
        CurrencyData("MXN", "$", "Mexican Peso", "🇲🇽", 20.30, 2),
        CurrencyData("HKD", "HK$", "Hong Kong Dollar", "🇭🇰", 7.78, 2),
        CurrencyData("SEK", "kr", "Swedish Krona", "🇸🇪", 10.75, 2),
        CurrencyData("NOK", "kr", "Norwegian Krone", "🇳🇴", 10.90, 2),
        CurrencyData("DKK", "kr", "Danish Krone", "🇩🇰", 6.88, 2),
        CurrencyData("KRW", "₩", "South Korean Won", "🇰🇷", 1390.0, 0),
        CurrencyData("TRY", "₺", "Turkish Lira", "🇹🇷", 34.50, 2),
        CurrencyData("THB", "฿", "Thai Baht", "🇹🇭", 34.20, 2),
        CurrencyData("IDR", "Rp", "Indonesian Rupiah", "🇮🇩", 15800.0, 0),
        CurrencyData("MYR", "RM", "Malaysian Ringgit", "🇲🇾", 4.45, 2),
        CurrencyData("PHP", "₱", "Philippine Peso", "🇵🇭", 58.50, 2),
        CurrencyData("VND", "₫", "Vietnamese Dong", "🇻🇳", 25300.0, 0),
        CurrencyData("PLN", "zł", "Polish Zloty", "🇵🇱", 4.02, 2),
        CurrencyData("CZK", "Kč", "Czech Koruna", "🇨🇿", 23.30, 2),
        CurrencyData("HUF", "Ft", "Hungarian Forint", "🇭🇺", 370.0, 0),
        CurrencyData("ILS", "₪", "Israeli Shekel", "🇮🇱", 3.65, 2),
        CurrencyData("CLP", "$", "Chilean Peso", "🇨🇱", 965.0, 0),
        CurrencyData("COP", "$", "Colombian Peso", "🇨🇴", 4350.0, 0),
        CurrencyData("EGP", "E£", "Egyptian Pound", "🇪🇬", 49.50, 2),
        CurrencyData("KWD", "KD", "Kuwaiti Dinar", "🇰🇼", 0.31, 3),
        CurrencyData("QAR", "QR", "Qatari Riyal", "🇶🇦", 3.64, 2)
    )

    private val currencyMap = ALL_CURRENCIES.associateBy { it.code.uppercase() }

    fun getCurrencyData(code: String): CurrencyData {
        return currencyMap[code.uppercase()] ?: CurrencyData(code.uppercase(), code.uppercase(), code.uppercase(), "🏳️", 1.0, 2)
    }

    fun getCurrencySymbol(code: String): String = getCurrencyData(code).symbol

    fun getCurrencyFlag(code: String): String = getCurrencyData(code).flag

    fun fractionDigits(code: String): Int = getCurrencyData(code).fractionDigits

    fun convert(amount: Double, fromCode: String, toCode: String): Double {
        if (fromCode.equals(toCode, ignoreCase = true)) return amount
        val fromData = getCurrencyData(fromCode)
        val toData = getCurrencyData(toCode)
        if (fromData.rateToUsd <= 0.0) return amount
        val amountInUsd = amount / fromData.rateToUsd
        return amountInUsd * toData.rateToUsd
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
        val data = getCurrencyData(code)
        return "${data.flag} ${data.code} (${data.symbol}) - ${data.name}"
    }
}
