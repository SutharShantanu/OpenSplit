package com.opensplit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.components.SnackbarController
import com.opensplit.ui.navigation.OpenSplitNavGraph
import com.opensplit.ui.theme.OpenSplitTheme

class MainActivity : ComponentActivity() {
    private var pendingResetOobCode by mutableStateOf<String?>(null)
    private var pendingJoinToken by mutableStateOf<String?>(null)
    private var pendingWidgetAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingResetOobCode = extractResetOobCode(intent)
        pendingJoinToken = extractJoinToken(intent)
        pendingWidgetAction = extractWidgetAction(intent)

        val appContainer = (application as OpenSplitApp).container


        setContent {
            val themeMode by appContainer.userPreferencesRepository.themeFlow.collectAsState(initial = "system")
            val colorPalette by appContainer.userPreferencesRepository.colorPaletteFlow.collectAsState(initial = "dynamic")
            val amoledPref by appContainer.userPreferencesRepository.amoledFlow.collectAsState(initial = false)
            val isDark = when (themeMode) {
                "light" -> false
                "dark", "amoled" -> true
                else -> isSystemInDarkTheme()
            }

            // The app can force a theme that differs from the system one, so system bar icon
            // contrast must follow the app theme, not enableEdgeToEdge's system-based default.
            val view = androidx.compose.ui.platform.LocalView.current
            androidx.compose.runtime.SideEffect {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }

            OpenSplitTheme(
                darkTheme = isDark,
                amoled = (themeMode == "amoled" || amoledPref) && isDark,
                dynamicColor = colorPalette == "dynamic",
                palette = colorPalette
            ) {
                // One snackbar host for the whole app, overlaid above every screen so any
                // composable can confirm an action via LocalSnackbarController.
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val snackbarController = remember(snackbarHostState, scope) {
                    SnackbarController(snackbarHostState, scope)
                }

                val busy = remember { com.opensplit.ui.components.BusyController() }
                CompositionLocalProvider(
                    LocalSnackbarController provides snackbarController,
                    com.opensplit.ui.components.LocalAiService provides appContainer.aiService,
                    com.opensplit.ui.components.LocalBusy provides busy
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            OpenSplitNavGraph(
                                appContainer = appContainer,
                                pendingResetOobCode = pendingResetOobCode,
                                onResetOobCodeConsumed = { pendingResetOobCode = null },
                                pendingJoinToken = pendingJoinToken,
                                onJoinTokenConsumed = { pendingJoinToken = null },
                                pendingWidgetAction = pendingWidgetAction,
                                onWidgetActionConsumed = { pendingWidgetAction = null }
                            )
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .imePadding()
                            )
                            com.opensplit.ui.components.BusyPill(
                                controller = busy,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .imePadding()
                                    .padding(bottom = 96.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractResetOobCode(intent)?.let { pendingResetOobCode = it }
        extractJoinToken(intent)?.let { pendingJoinToken = it }
        extractWidgetAction(intent)?.let { pendingWidgetAction = it }
    }

    private fun extractWidgetAction(intent: Intent?): String? {
        return intent?.getStringExtra(com.opensplit.widget.OpenSplitBalanceWidgetProvider.EXTRA_WIDGET_ACTION)
    }

    /** `https://<host>/join/<token>` from a shared invite link. */
    private fun extractJoinToken(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        val token = com.opensplit.domain.model.InviteLink.tokenFrom(uri.toString()) ?: return null
        return when (uri.pathSegments.firstOrNull()) {
            "join" -> token
            "friend" -> "friend:$token"
            else -> null
        }
    }

    /** Pulls the oobCode out of a Firebase Auth "resetPassword" action link, if this intent is one. */
    private fun extractResetOobCode(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        return if (uri.getQueryParameter("mode") == "resetPassword") uri.getQueryParameter("oobCode") else null
    }
}

