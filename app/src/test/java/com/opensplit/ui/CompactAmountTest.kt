package com.opensplit.ui

import com.opensplit.ui.screens.compactAmount
import org.junit.Assert.assertEquals
import org.junit.Test

class CompactAmountTest {
    @Test
    fun shortensThousandsAndMillions() {
        assertEquals("0", compactAmount(0.0))
        assertEquals("950", compactAmount(950.0))
        assertEquals("1k", compactAmount(1_000.0))
        assertEquals("4.2k", compactAmount(4_249.0))
        assertEquals("51k", compactAmount(50_950.0))
        assertEquals("1.3M", compactAmount(1_250_000.0))
    }
}
