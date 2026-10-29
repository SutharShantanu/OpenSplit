package com.opensplit.util

import android.net.Uri
import java.util.Locale

/** UPI deep links (NPCI "upi://pay" spec) for paying a friend from Settle Up. */
object Upi {
    private val VPA = Regex("^[A-Za-z0-9.\\-_]{2,256}@[A-Za-z][A-Za-z0-9]{1,64}$")

    fun isValidVpa(vpa: String): Boolean = VPA.matches(vpa)

    /** UPI only settles INR, so callers must not use this for other currencies. */
    fun payUri(vpa: String, payeeName: String, amount: Double, note: String): Uri =
        Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", vpa)
            .appendQueryParameter("pn", payeeName)
            .appendQueryParameter("am", String.format(Locale.US, "%.2f", amount))
            .appendQueryParameter("tn", note)
            .appendQueryParameter("cu", "INR")
            .build()
}
