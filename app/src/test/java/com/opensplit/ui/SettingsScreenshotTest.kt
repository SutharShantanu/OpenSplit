package com.opensplit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.opensplit.domain.model.User
import com.opensplit.ui.components.FloatingNavBar
import com.opensplit.ui.components.FloatingNavItem
import com.opensplit.ui.screens.AccountOverviewScreen
import com.opensplit.ui.screens.AiProvidersSubScreen
import com.opensplit.ui.screens.AppearanceSettingsSubScreen
import com.opensplit.ui.screens.BugReportSubScreen
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTheme
import com.opensplit.ui.viewmodel.AccountUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Golden images for settings screens in light and dark; run `./gradlew recordRoborazziDebug` to refresh. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun aiContent(dark: Boolean) {
        compose.setContent {
            OpenSplitTheme(darkTheme = dark) {
                Surface(color = MaterialTheme.colorScheme.background) {
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

    private fun accountContent(dark: Boolean) {
        compose.setContent {
            OpenSplitTheme(darkTheme = dark) {
                val snackbarHostState = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                val snackbarController = androidx.compose.runtime.remember {
                    com.opensplit.ui.components.SnackbarController(snackbarHostState, scope)
                }
                androidx.compose.runtime.CompositionLocalProvider(
                    com.opensplit.ui.components.LocalSnackbarController provides snackbarController
                ) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                AccountOverviewScreen(
                                    data = AccountUiState(
                                        user = User(
                                            uid = "",
                                            displayName = "Shantanu",
                                            email = "shantanu@opensplit.org"
                                        ),
                                        groupCount = 3,
                                        friendCount = 8,
                                        netByCurrency = mapOf("INR" to 2450.0),
                                        defaultCurrency = "INR",
                                        allExpenses = emptyList(),
                                        pendingInvites = emptyList()
                                    ),
                                    theme = if (dark) "dark" else "light",
                                    dynamicColor = true,
                                    aiProvider = "anthropic",
                                    providerApiKeys = mapOf("anthropic" to "sk-..."),
                                    onSelectCategory = {},
                                    onSignOutClick = {},
                                    onNavigateToHistory = {},
                                    onNavigateToActivity = {}
                                )
                            }
                            FloatingNavBar(
                                items = listOf(
                                    FloatingNavItem("Groups", OpenSplitIcons.Groups, false, {}),
                                    FloatingNavItem("People", OpenSplitIcons.Friends, false, {}),
                                    FloatingNavItem("Activity", OpenSplitIcons.Activity, false, {}),
                                    FloatingNavItem("Insights", OpenSplitIcons.Insights, false, {}),
                                    FloatingNavItem("Settings", OpenSplitIcons.Account, true, {})
                                ),
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun appearanceContent(dark: Boolean) {
        compose.setContent {
            OpenSplitTheme(darkTheme = dark) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    AppearanceSettingsSubScreen(
                        theme = if (dark) "dark" else "light",
                        onThemeChange = {},
                        onBack = {}
                    )
                }
            }
        }
    }

    private fun bugReportContent(dark: Boolean) {
        compose.setContent {
            OpenSplitTheme(darkTheme = dark) {
                val snackbarHostState = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                val snackbarController = androidx.compose.runtime.remember {
                    com.opensplit.ui.components.SnackbarController(snackbarHostState, scope)
                }
                androidx.compose.runtime.CompositionLocalProvider(
                    com.opensplit.ui.components.LocalSnackbarController provides snackbarController
                ) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                        BugReportSubScreen(onBack = {})
                    }
                }
            }
        }
    }

    @Test
    fun aiSettingsLight() {
        aiContent(dark = false)
        compose.onRoot().captureRoboImage("src/test/screenshots/ai_settings_light.png")
    }

    @Test
    fun aiSettingsDark() {
        aiContent(dark = true)
        compose.onRoot().captureRoboImage("src/test/screenshots/ai_settings_dark.png")
    }

    @Test
    fun accountOverviewLight() {
        accountContent(dark = false)
        compose.onRoot().captureRoboImage("src/test/screenshots/settings_light.png")
    }

    @Test
    fun appearanceSettingsLight() {
        appearanceContent(dark = false)
        compose.onRoot().captureRoboImage("src/test/screenshots/appearance_light.png")
    }

    @Test
    fun bugReportLight() {
        bugReportContent(dark = false)
        compose.onRoot().captureRoboImage("src/test/screenshots/bug_report_light.png")
    }
}
