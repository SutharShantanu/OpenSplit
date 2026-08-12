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
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.components.SnackbarController
import com.opensplit.ui.navigation.OpenSplitNavGraph
import com.opensplit.ui.theme.OpenSplitTheme

class MainActivity : ComponentActivity() {
    private var pendingResetOobCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingResetOobCode = extractResetOobCode(intent)

        val appContainer = (application as OpenSplitApp).container

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val uid = appContainer.authRepository.getCurrentUserId()
            if (uid != null) {
                com.opensplit.util.MockDataSeeder.seedMockData(
                    appContainer.firestore,
                    uid
                )
            }
        }

        setContent {
            val themeMode by appContainer.userPreferencesRepository.themeFlow.collectAsState(initial = "system")
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            OpenSplitTheme(darkTheme = isDark, dynamicColor = true) {
                // One snackbar host for the whole app, overlaid above every screen so any
                // composable can confirm an action via LocalSnackbarController.
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val snackbarController = remember(snackbarHostState, scope) {
                    SnackbarController(snackbarHostState, scope)
                }

                CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            OpenSplitNavGraph(
                                appContainer = appContainer,
                                pendingResetOobCode = pendingResetOobCode,
                                onResetOobCodeConsumed = { pendingResetOobCode = null }
                            )
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .imePadding()
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
    }

    /** Pulls the oobCode out of a Firebase Auth "resetPassword" action link, if this intent is one. */
    private fun extractResetOobCode(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        return if (uri.getQueryParameter("mode") == "resetPassword") uri.getQueryParameter("oobCode") else null
    }
}

