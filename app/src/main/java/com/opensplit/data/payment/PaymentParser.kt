package com.opensplit.data.payment

/** A payment read from a bank or UPI app notification. Only these fields are ever kept. */
data class ParsedPayment(
    val amount: Double,
    val direction: Direction,
    val counterparty: String?,
    val reference: String?,
    /** Where it came from, e.g. "GPay"; never the message text. */
    val source: String
)

enum class Direction { DEBIT, CREDIT }

/**
 * Turns one bank/UPI notification into a [ParsedPayment], or null when it is not a completed
 * payment (OTP, promotion, failure, balance alert, refund unless asked for). Runs on-device;
 * the text is only read here and never stored or logged.
 */
object PaymentParser {

    private val IC = RegexOption.IGNORE_CASE

    private val OTP = Regex("""\b(OTP|one[- ]time password|verification code)\b""", IC)
    private val FAILED = Regex("""\b(failed|declined|unsuccessful|could not be (processed|completed)|reversed|pending)\b""", IC)
    private val REFUND = Regex("""\brefund""", IC)

    // Earliest keyword wins: "A/c debited ...; RAHUL credited" is a debit.
    private val DEBIT = Regex("""\b(debited|paid|withdrawn|spent|sent(?! you)|transferred to|trf to|purchase)\b""", IC)
    private val CREDIT = Regex("""\b(credited|received|sent you|deposited|added to your)\b""", IC)

    private val NUMBER = """([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
    private val AMOUNT = Regex("""(?:₹|\bRs\.?|\bINR)\s*$NUMBER""", IC)
    // Some bank texts omit the currency: "A/C X1234 debited by 20.0".
    private val AMOUNT_AFTER_VERB = Regex("""\b(?:debited|credited)\s+(?:by|for|with)\s+(?:₹|Rs\.?|INR)?\s*$NUMBER""", IC)

    private val VPA = Regex("""\b([\w.\-]{2,}@[a-z]\w+)\b""", IC)
    private val NAME_STOP = """(?=\s+(?:on|via|ref|refno|upi|using|from|for|at|is|has|with|a/c|account)\b|[.,;:(]|\s*$)"""
    private val TO_NAME = Regex("""\b(?:paid to|sent to|trf to|transferred to|to)\s+(?:VPA\s+)?([A-Za-z][A-Za-z .&'\-]{1,40}?)$NAME_STOP""", IC)
    private val FROM_NAME = Regex("""\b(?:from|by)\s+(?:VPA\s+)?([A-Za-z][A-Za-z .&'\-]{1,40}?)$NAME_STOP""", IC)
    private val NOT_A_NAME = Regex("""^(your|you|a/?c|account|acct|bank|upi|the|vpa|card)\b""", IC)

    private val REF = Regex("""\b(?:UPI\s*Ref(?:erence)?(?:\s*(?:No\.?|number|ID))?|Ref(?:erence)?\s*(?:No\.?|number|ID)|Refno|RRN|UTR|UPI)\s*(?:is\s*)?[:.#-]?\s*(\d{6,})""", IC)

    fun parse(text: String, source: String, detectRefunds: Boolean = false): ParsedPayment? {
        val t = text.replace(' ', ' ').replace(Regex("""\s+"""), " ").trim()
        if (t.isEmpty() || OTP.containsMatchIn(t) || FAILED.containsMatchIn(t)) return null
        val refund = REFUND.containsMatchIn(t)
        if (refund && !detectRefunds) return null

        // No money-moving verb means a promotion, balance alert or reminder: skip, don't guess.
        val debit = DEBIT.find(t)
        val credit = CREDIT.find(t)
        val direction = when {
            refund -> Direction.CREDIT
            debit != null && (credit == null || debit.range.first < credit.range.first) -> Direction.DEBIT
            credit != null -> Direction.CREDIT
            else -> return null
        }

        val amountText = (AMOUNT_AFTER_VERB.find(t) ?: AMOUNT.find(t))?.groupValues?.get(1) ?: return null
        val amount = amountText.replace(",", "").toDoubleOrNull()?.takeIf { it > 0 } ?: return null

        return ParsedPayment(amount, direction, counterparty(t, direction), REF.find(t)?.groupValues?.get(1), source)
    }

    private fun counterparty(t: String, direction: Direction): String? {
        val pattern = if (direction == Direction.DEBIT) TO_NAME else FROM_NAME
        val name = pattern.findAll(t).map { it.groupValues[1].trim() }
            .firstOrNull { it.length >= 2 && !NOT_A_NAME.containsMatchIn(it) }
        return name?.let(::titleCase) ?: VPA.find(t)?.groupValues?.get(1)
    }

    private fun titleCase(s: String) =
        s.split(' ').filter { it.isNotBlank() }.joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
}
