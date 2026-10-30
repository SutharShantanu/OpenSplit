package com.opensplit.ui

import com.opensplit.ui.screens.useNavigationRail
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShellThemeTest {
    @Test
    fun `rail starts at medium width`() {
        assertFalse(useNavigationRail(411))
        assertTrue(useNavigationRail(600))
    }
}
