package com.opensplit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.opensplit.ui.screens.useNavigationRail
import com.opensplit.ui.theme.OpenSplitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShellThemeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `amoled theme uses true black surface and keeps brand primary`() {
        var surface = Color.Unspecified
        var primaryAmoled = Color.Unspecified
        var primaryDark = Color.Unspecified
        compose.setContent {
            OpenSplitTheme(darkTheme = true, amoled = true) {
                surface = MaterialTheme.colorScheme.surface
                primaryAmoled = MaterialTheme.colorScheme.primary
            }
            OpenSplitTheme(darkTheme = true, amoled = false) {
                primaryDark = MaterialTheme.colorScheme.primary
            }
        }
        assertEquals(Color.Black, surface)
        assertEquals(primaryDark, primaryAmoled)
    }

    @Test
    fun `rail starts at medium width`() {
        assertFalse(useNavigationRail(411))
        assertTrue(useNavigationRail(600))
    }
}
