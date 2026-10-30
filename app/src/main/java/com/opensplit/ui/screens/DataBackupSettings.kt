@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

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
import kotlinx.coroutines.launch
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

/* =========================================================================================
 * 6. SUB-SCREEN: DATA, EXPORT & BACKUP
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBackupSubScreen(
    expenseCount: Int,
    onExportClick: () -> Unit,
    onClearCache: suspend () -> Long,
    onBackupSettings: suspend () -> String = { "" },
    onRestoreSettings: suspend (String) -> Int = { 0 },
    onOpenRecurring: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onBack: () -> Unit
) {
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
                title = { Text("Data, Export & Backup", fontWeight = FontWeight.Bold) },
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
                    Column {
                        listOf(
                            Triple("Recurring bills", "Rent, utilities, subscriptions", OpenSplitIcons.Autorenew) to onOpenRecurring,
                            Triple("Recently deleted", "Restore expenses from the last 90 days", OpenSplitIcons.Delete) to onOpenRecentlyDeleted
                        ).forEachIndexed { index, (row, onClick) ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ListItem(
                                modifier = Modifier.clickable(onClick = onClick),
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = { SymbolIcon(row.third, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                headlineContent = { Text(row.first, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text(row.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                trailingContent = { SymbolIcon(OpenSplitIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
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
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    SymbolIcon(
                                        symbol = OpenSplitIcons.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Export Data",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$expenseCount expenses recorded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onExportClick,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SymbolIcon(OpenSplitIcons.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Expenses")
                        }
                    }
                }
            }

            item {
                SettingsBackupCard(onBackupSettings, onRestoreSettings)
            }

            item {
                var isClearing by remember { mutableStateOf(false) }
                var clearedResult by remember { mutableStateOf<String?>(null) }
                val coroutineScope = rememberCoroutineScope()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    SymbolIcon(
                                        symbol = OpenSplitIcons.CleaningServices,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cache",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Images and temporary files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(visible = isClearing) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Clearing cache...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        clearedResult?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                if (!isClearing) {
                                    isClearing = true
                                    clearedResult = null
                                    coroutineScope.launch {
                                        val freed = onClearCache()
                                        clearedResult = "✓ Freed ${com.opensplit.util.StorageStats.formatMb(freed)}"
                                        isClearing = false
                                    }
                                }
                            },
                            enabled = !isClearing,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isClearing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clearing...")
                            } else {
                                SymbolIcon(OpenSplitIcons.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clear Cache")
                            }
                        }
                    }
                }
            }
        }
    }
}
}


/** Save / restore app settings (theme, currency, notifications, budgets, pins...) as a JSON file. */
@Composable
private fun SettingsBackupCard(
    onBackupSettings: suspend () -> String,
    onRestoreSettings: suspend (String) -> Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = com.opensplit.ui.components.LocalSnackbarController.current
    val io = kotlinx.coroutines.Dispatchers.IO

    val saveLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                val json = onBackupSettings()
                kotlinx.coroutines.withContext(io) {
                    context.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) }
                }
            }.isSuccess
            snackbar.showMessage(if (ok) "Settings backed up" else "Couldn't write the backup file")
        }
    }
    val openLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = kotlinx.coroutines.withContext(io) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                }
                onRestoreSettings(json)
            }.onSuccess { snackbar.showMessage("Restored $it settings") }
                .onFailure { snackbar.showMessage(it.message ?: "Not a valid settings backup") }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Settings backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Theme, currency, notifications, budgets and pinned groups. API keys are never included.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { saveLauncher.launch("opensplit-settings.json") },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    SymbolIcon(OpenSplitIcons.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back up")
                }
                OutlinedButton(
                    onClick = { openLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    SymbolIcon(OpenSplitIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore")
                }
            }
        }
    }
}
