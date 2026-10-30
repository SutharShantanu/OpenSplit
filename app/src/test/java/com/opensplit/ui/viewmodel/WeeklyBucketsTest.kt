package com.opensplit.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyBucketsTest {
    @Test
    fun `sums real amounts per 7-day range`() {
        val weeks = weeklyBuckets(31, listOf(1 to 100.0, 7 to 50.0, 8 to 20.0, 31 to 5.0))
        assertEquals(listOf("1–7", "8–14", "15–21", "22–28", "29–31"), weeks.map { it.monthLabel })
        assertEquals(listOf(150.0, 20.0, 0.0, 0.0, 5.0), weeks.map { it.amount })
    }

    @Test
    fun `february has four weeks`() {
        assertEquals(4, weeklyBuckets(28, emptyList()).size)
    }
}
