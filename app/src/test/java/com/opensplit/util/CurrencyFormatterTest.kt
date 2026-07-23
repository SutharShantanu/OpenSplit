package com.opensplit.util

import org.junit.Assert.*
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun inrUsesIndianGrouping() {
        assertEquals("₹10,00,000.00", CurrencyFormatter.format(1_000_000.0, "INR"))
    }

    @Test
    fun nonInrUsesThousandsGrouping() {
        assertEquals("$1,000,000.00", CurrencyFormatter.format(1_000_000.0, "USD"))
    }

    @Test
    fun jpyHasNoDecimals() {
        assertEquals("¥1,000", CurrencyFormatter.format(1000.0, "JPY"))
    }

    @Test
    fun negativeAmountsGetLeadingMinus() {
        assertEquals("-₹50.00", CurrencyFormatter.format(-50.0, "INR"))
    }

    @Test
    fun showSignAddsPlusForPositive() {
        assertEquals("+$12.50", CurrencyFormatter.format(12.5, "USD", showSign = true))
    }

    @Test
    fun unknownCurrencyFallsBackToCode() {
        assertEquals("CHF", CurrencyFormatter.getCurrencySymbol("CHF"))
    }
}
