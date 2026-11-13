package com.opensplit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opensplit.data.payment.DetectedPayment
import com.opensplit.data.payment.Direction
import com.opensplit.service.PaymentDetector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Feeds a synthetic UPI notification through the real detector (prefs, parser, store).
 * Pass `-e keep true` to leave the record for checking the review screen by hand.
 */
@RunWith(AndroidJUnit4::class)
class PaymentDetectionTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = (context.applicationContext as OpenSplitApp).container.detectedPayments

    @Test
    fun detectsMergesAndIgnoresNoise() = runBlocking {
        val before = store.items.value.map { it.id }.toSet()
        PaymentDetector.handle(context, "₹1,200 paid to Rahul Kumar. UPI Ref 612345678901", "GPay")
        PaymentDetector.handle(context, "Rs.1200.00 debited from a/c **1234 to VPA rahul@okaxis. UPI Ref 612345678901", "HDFC Bank")
        PaymentDetector.handle(context, "123456 is your OTP for ₹999", "GPay")
        val added = store.items.value.filter { it.id !in before }
        assertEquals(1, added.size)
        val p = added.single()
        assertEquals(1200.0, p.amount, 0.0)
        assertEquals(Direction.DEBIT, p.direction)
        assertEquals("Rahul Kumar", p.counterparty)
        assertEquals(DetectedPayment.Status.PENDING, p.status)
        assertNull(PaymentDetector.sourceFor("com.whatsapp", "Rahul"))
        assertNull(PaymentDetector.sourceFor("com.google.android.apps.messaging", "Mom"))
        assertEquals("Bank SMS", PaymentDetector.sourceFor("com.google.android.apps.messaging", "AX-HDFCBK"))
        if (InstrumentationRegistry.getArguments().getString("keep") != "true") store.setStatus(p.id, DetectedPayment.Status.IGNORED)
    }
}
