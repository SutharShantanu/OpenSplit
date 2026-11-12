package com.opensplit.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.opensplit.ui.theme.wallpaperColorScheme
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WallpaperColorSchemeTest {
    private val seeds = listOf(
        0xFFD48854, 0xFF436492, 0xFFFFFF00, 0xFF0000FF, 0xFFFF0000, 0xFF00FF00,
        0xFF808080, 0xFF000000, 0xFFFFFFFF
    ).map { it.toInt() }

    private fun assertReadable(name: String, background: Color, text: Color) {
        val contrast = ColorUtils.calculateContrast(text.toArgb(), background.toArgb())
        assertTrue("$name contrast $contrast", contrast >= 4.5)
    }

    @Test
    fun textStaysReadableForAnyWallpaperColour() {
        for (seed in seeds) for (dark in listOf(false, true)) {
            val c = wallpaperColorScheme(seed, seed, seed, dark)
            val where = "seed=${Integer.toHexString(seed)} dark=$dark"
            assertReadable("primary $where", c.primary, c.onPrimary)
            assertReadable("primaryContainer $where", c.primaryContainer, c.onPrimaryContainer)
            assertReadable("secondary $where", c.secondary, c.onSecondary)
            assertReadable("secondaryContainer $where", c.secondaryContainer, c.onSecondaryContainer)
            assertReadable("tertiary $where", c.tertiary, c.onTertiary)
            assertReadable("tertiaryContainer $where", c.tertiaryContainer, c.onTertiaryContainer)
            assertReadable("surface $where", c.surface, c.onSurface)
            assertReadable("surfaceVariant $where", c.surfaceVariant, c.onSurfaceVariant)
        }
    }

    @Test
    fun eachAccentRoleFollowsItsOwnWallpaperColour() {
        val c = wallpaperColorScheme(0xFFD48854.toInt(), 0xFF6F9A4A.toInt(), 0xFF436492.toInt(), false)
        assertNotEquals(c.primary, c.secondary)
        assertNotEquals(c.primary, c.tertiary)
        assertNotEquals(c.secondary, c.tertiary)
    }
}
