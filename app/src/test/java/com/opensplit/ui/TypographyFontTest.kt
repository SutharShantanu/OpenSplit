package com.opensplit.ui

import androidx.compose.ui.text.font.FontFamily
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
        for ((key, family) in listOf("system" to FontFamily.Default, "serif" to FontFamily.Serif, "mono" to MonoFontFamily)) {
            val t = openSplitTypography(key)
            listOf(t.displayLarge, t.headlineMedium, t.titleSmall, t.bodyLarge, t.labelSmall).forEach {
                assertEquals(key, family, it.fontFamily)
            }
        }
    }

    @Test
    fun everyPickerChoiceMapsToItsOwnFamily() {
        com.opensplit.ui.theme.FontChoices.filter { it.first != "default" }.forEach { (key, _, family) ->
            assertSame(key, family, openSplitTypography(key).bodyLarge.fontFamily)
        }
    }
}
