package com.opensplit.ui

import com.opensplit.ui.theme.MonoFontFamily
import com.opensplit.ui.theme.Typography
import com.opensplit.ui.theme.openSplitTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TypographyFontTest {
    @Test
    fun defaultAndUnknownKeepTheBrandTypography() {
        assertSame(Typography, openSplitTypography("default"))
        assertSame(Typography, openSplitTypography("something-else"))
    }

    @Test
    fun chosenFontAppliesToEveryStyle() {
        val t = openSplitTypography("mono")
        listOf(t.displayLarge, t.headlineMedium, t.titleSmall, t.bodyLarge, t.labelSmall).forEach {
            assertEquals("mono", MonoFontFamily, it.fontFamily)
        }
    }

    @Test
    fun everyPickerChoiceMapsToItsOwnFamily() {
        com.opensplit.ui.theme.FontChoices.filter { it.first != "default" }.forEach { (key, _, family) ->
            assertSame(key, family, openSplitTypography(key).bodyLarge.fontFamily)
        }
    }
}
