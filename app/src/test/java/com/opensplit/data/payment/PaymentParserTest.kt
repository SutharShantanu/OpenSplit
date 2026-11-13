package com.opensplit.data.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Synthetic messages in the shapes Indian banks and UPI apps use; no real account data. */
class PaymentParserTest {
    private fun p(text: String, refunds: Boolean = false) = PaymentParser.parse(text, "test", refunds)

    @Test fun gpayPaid() {
        val r = p("₹1,200 paid to Rahul Kumar")!!
        assertEquals(1200.0, r.amount, 0.0); assertEquals(Direction.DEBIT, r.direction); assertEquals("Rahul Kumar", r.counterparty)
    }

    @Test fun phonePeReceived() {
        val r = p("Received ₹450 from PRIYA SHARMA")!!
        assertEquals(450.0, r.amount, 0.0); assertEquals(Direction.CREDIT, r.direction); assertEquals("Priya Sharma", r.counterparty)
    }

    @Test fun friendSentYouIsCredit() {
        val r = p("Rahul sent you ₹200")!!
        assertEquals(Direction.CREDIT, r.direction)
    }

    @Test fun hdfcDebitWithVpaAndRef() {
        val r = p("Rs.500.00 has been debited from account **1234 to VPA rahul.k@okaxis RAHUL KUMAR on 12-11-26. Your UPI transaction reference number is 612345678901.")!!
        assertEquals(500.0, r.amount, 0.0); assertEquals(Direction.DEBIT, r.direction)
        assertEquals("612345678901", r.reference)
    }

    @Test fun iciciDebitMentioningCreditedPayee() {
        val r = p("ICICI Bank Acct XX123 debited for Rs 2,350.50 on 12-Nov-26; SWIGGY credited. UPI:612345678902.")!!
        assertEquals(2350.50, r.amount, 0.0); assertEquals(Direction.DEBIT, r.direction); assertEquals("612345678902", r.reference)
    }

    @Test fun sbiDebitWithoutCurrencySymbol() {
        val r = p("Dear UPI user A/C X1234 debited by 20.0 on date 12Nov26 trf to CHAI POINT Refno 612345678903")!!
        assertEquals(20.0, r.amount, 0.0); assertEquals(Direction.DEBIT, r.direction)
        assertEquals("Chai Point", r.counterparty); assertEquals("612345678903", r.reference)
    }

    @Test fun axisCredit() {
        val r = p("INR 3,000.00 credited to A/c no. XX5678 on 12-11-26 by VPA priya@oksbi. UTR 612345678904")!!
        assertEquals(3000.0, r.amount, 0.0); assertEquals(Direction.CREDIT, r.direction); assertEquals("612345678904", r.reference)
    }

    @Test fun ignoresNonPayments() {
        assertNull(p("123456 is your OTP for a payment of Rs 500 to AMAZON. Do not share."))
        assertNull(p("Get 10% cashback up to ₹100 on your next recharge!"))
        assertNull(p("Your A/c XX1234 balance is Rs 5,000.00 as of today"))
        assertNull(p("Payment of ₹300 to Rahul failed. Money will not be debited."))
        assertNull(p("Your electricity bill of ₹1,450 is due tomorrow"))
        assertNull(p("Hi, how are you?"))
    }

    @Test fun refundsOnlyWhenAsked() {
        val text = "Refund of Rs 799 for your order has been credited to your account"
        assertNull(p(text))
        assertEquals(Direction.CREDIT, p(text, refunds = true)!!.direction)
    }
}
