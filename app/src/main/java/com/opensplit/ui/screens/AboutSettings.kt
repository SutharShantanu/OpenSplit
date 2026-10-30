@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensplit.ui.theme.Symbol
import com.opensplit.data.ai.AI_PROVIDERS_CATALOG
import com.opensplit.data.ai.AiProviderInfo
import com.opensplit.data.ai.AiFeature
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.responsiveContentWidth
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.opensplit.di.AppContainer
import com.opensplit.ui.components.ExportBottomSheet
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.components.appHazeHeader
import com.opensplit.ui.components.appHazeSource
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.viewmodel.AccountUiState
import com.opensplit.ui.viewmodel.AccountViewModel
import com.opensplit.util.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

/* =========================================================================================
 * 8. SUB-SCREEN: ABOUT & SUPPORT
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSupportSubScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val updater = com.opensplit.ui.components.LocalAppUpdater.current
    val updateState by updater.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarController.current
    val scope = rememberCoroutineScope()
    var showLicenses by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }

    if (showChangelog) {
        com.opensplit.ui.components.ChangelogBottomSheet(onDismiss = { showChangelog = false })
    }
    if (showLicenses) {
        OpenSourceLicenses(onBack = { showLicenses = false })
        return
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .responsiveContentWidth(maxWidth = 760.dp)
        ) {
            androidx.compose.material3.TopAppBar(
                title = { Text("About OpenSplit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    com.opensplit.ui.components.RoundBackButton(onClick = onBack)
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                SymbolIcon(
                                    symbol = OpenSplitIcons.PieChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "OpenSplit",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version ${com.opensplit.BuildConfig.VERSION_NAME} (Build ${com.opensplit.BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Free, open-source group expense sharing & debt optimization.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    val checking = updateState is com.opensplit.data.update.AppUpdater.State.Checking
                    Surface(
                        onClick = {
                            scope.launch {
                                when (val result = updater.check()) {
                                    is com.opensplit.data.update.AppUpdater.State.UpToDate ->
                                        snackbar.showMessage("You're on the latest version")
                                    is com.opensplit.data.update.AppUpdater.State.Failed ->
                                        snackbar.showMessage(result.message)
                                    else -> Unit // Available: UpdateSheet opens from MainActivity
                                }
                            }
                        },
                        enabled = !checking,
                        shape = MaterialTheme.shapes.large,
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (checking) {
                                com.opensplit.ui.components.AppLoadingIndicator(size = 24.dp)
                            } else {
                                SymbolIcon(OpenSplitIcons.Refresh, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Check for updates", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (checking) "Contacting GitHub…" else "Installs the latest GitHub release",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SymbolIcon(OpenSplitIcons.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        listOf(
                            Triple("What's new in OpenSplit", "Recent updates and release notes", CHANGELOG),
                            Triple("GitHub Repository", "View open-source project code", "https://github.com/SutharShantanu/OpenSplit"),
                            Triple("Open-source licenses", "Libraries OpenSplit is built with", LICENSES),
                            Triple("Privacy Policy", "Learn how your data is handled", "https://opensplit.app/privacy"),
                            Triple("Terms of Service", "Read service terms & conditions", "https://opensplit.app/terms")
                        ).forEach { (title, subtitle, url) ->
                            Surface(
                                onClick = {
                                    if (url == CHANGELOG) showChangelog = true
                                    else if (url == LICENSES) showLicenses = true
                                    else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                },
                                shape = MaterialTheme.shapes.small,
                                color = Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    SymbolIcon(OpenSplitIcons.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

private const val LICENSES = "licenses"
private const val CHANGELOG = "changelog"

/** Third-party licenses, generated at build time by the AboutLibraries Gradle plugin. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenSourceLicenses(onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    val context = LocalContext.current
    val libraries by com.mikepenz.aboutlibraries.ui.compose.produceLibraries {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            context.resources.openRawResource(com.opensplit.R.raw.aboutlibraries).bufferedReader().use { it.readText() }
        }
    }
    Column(Modifier.fillMaxSize()) {
        androidx.compose.material3.TopAppBar(
            title = { Text("Open-source licenses", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                com.opensplit.ui.components.RoundBackButton(onClick = onBack)
            },
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
        com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer(libraries, Modifier.fillMaxSize())
    }
}
