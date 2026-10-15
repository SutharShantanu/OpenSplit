package com.opensplit.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.opensplit.ui.screens.AiProvidersSubScreen
import com.opensplit.ui.theme.OpenSplitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Golden images for the AI settings screen in light and AMOLED; run `./gradlew recordRoborazziDebug` to refresh. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun content(dark: Boolean) {
        compose.setContent {
            OpenSplitTheme(darkTheme = dark, amoled = dark, dynamicColor = false) {
                androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                AiProvidersSubScreen(
                    activeProvider = "anthropic",
                    providerApiKeys = mapOf("anthropic" to "sk-ant-xxxx1234"),
                    customEndpoint = "http://localhost:11434/v1",
                    customModel = "llama3:latest",
                    onSelectProvider = {}, onSaveProviderKey = { _, _ -> }, onSaveCustomSettings = { _, _ -> },
                    aiEnabled = true, disabledFeatures = setOf("INSIGHTS"), providerModels = emptyMap(),
                    usage = mapOf("anthropic" to 12),
                    onToggleAi = {}, onToggleFeature = { _, _ -> }, onSetModel = { _, _ -> },
                    onTestKey = { _, _, _, _, _ -> }, onBack = {}
                )
                }
            }
        }
    }

    @Test
    fun aiSettingsLight() {
        content(dark = false)
        compose.onRoot().captureRoboImage("src/test/screenshots/ai_settings_light.png")
    }

    @Test
    fun aiSettingsAmoled() {
        content(dark = true)
        compose.onRoot().captureRoboImage("src/test/screenshots/ai_settings_amoled.png")
    }
}
