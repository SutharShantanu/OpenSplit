@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.theme.Symbol
import com.opensplit.data.ai.AI_PROVIDERS_CATALOG
import com.opensplit.data.ai.AiProviderInfo
import com.opensplit.data.ai.AiFeature
import com.opensplit.ui.components.SymbolIcon
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

enum class AccountCategory(
    val title: String,
    val subtitle: String,
    val icon: Symbol
) {
    THEME(
        title = "Appearance & Theme",
        subtitle = "System, Light, Dark or AMOLED",
        icon = OpenSplitIcons.Palette
    ),
    CURRENCIES(
        title = "Currencies & Converter",
        subtitle = "Default currency, real-time converter, 36+ currencies",
        icon = OpenSplitIcons.CurrencyExchange
    ),
    AI_PROVIDERS(
        title = "AI Intelligence & Providers",
        subtitle = "Multi-provider AI keys (OpenAI, Gemini, Claude, Groq, Mistral, Ollama)",
        icon = OpenSplitIcons.AutoAwesome
    ),
    NOTIFICATIONS(
        title = "Notifications & Reminders",
        subtitle = "Push alerts, balance updates, expense reminders",
        icon = OpenSplitIcons.Notifications
    ),
    DATA_BACKUP(
        title = "Data, Export & Backup",
        subtitle = "Export CSV, PDF, JSON, cache & storage",
        icon = OpenSplitIcons.CloudDownload
    ),
    SECURITY(
        title = "Account & Security",
        subtitle = "Profile name, password reset, account protection",
        icon = OpenSplitIcons.Security
    ),
    M3_CATALOG(
        title = "Material 3 Catalog",
        subtitle = "Interactive gallery of all official components from m3.material.io",
        icon = OpenSplitIcons.Widgets
    ),
    ABOUT(
        title = "About OpenSplit",
        subtitle = "Version 2.4.1 (Build 890), Open Source, FAQs",
        icon = OpenSplitIcons.Info
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    appContainer: AppContainer,
    rootNavController: NavController,
    viewModel: AccountViewModel,
    showTopBar: Boolean = false
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    val uiState by viewModel.uiState.collectAsState()

    val theme by viewModel.themeFlow.collectAsState(initial = "system")
    val dynamicColor by viewModel.dynamicColorFlow.collectAsState(initial = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val notificationsEnabled by viewModel.notificationsEnabledFlow.collectAsState(initial = true)
    val aiProvider by viewModel.aiProviderFlow.collectAsState(initial = "openai")
    val providerApiKeys by viewModel.providerApiKeysFlow.collectAsState(initial = emptyMap())
    val customEndpoint by viewModel.customEndpointFlow.collectAsState(initial = "http://localhost:11434/v1")
    val customModel by viewModel.customModelFlow.collectAsState(initial = "llama3:latest")
    val aiEnabled by viewModel.aiEnabledFlow.collectAsState(initial = true)
    val aiDisabledFeatures by viewModel.aiDisabledFeaturesFlow.collectAsState(initial = emptySet())
    val providerModels by viewModel.providerModelsFlow.collectAsState(initial = emptyMap())
    val aiUsage by viewModel.aiUsageFlow.collectAsState(initial = emptyMap())
    val aiTokens by viewModel.aiTokensFlow.collectAsState(initial = emptyMap())

    var selectedCategory by rememberSaveable { mutableStateOf<AccountCategory?>(null) }

    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showExportBottomSheet by remember { mutableStateOf(false) }

    val hazeState = remember { HazeState() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        StateLayout(state = uiState) { data ->
            AnimatedContent(
                targetState = selectedCategory,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally(tween(300)) { width -> width / 3 } + fadeIn()).togetherWith(
                            slideOutHorizontally(tween(300)) { width -> -width / 3 } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally(tween(300)) { width -> -width / 3 } + fadeIn()).togetherWith(
                            slideOutHorizontally(tween(300)) { width -> width / 3 } + fadeOut()
                        )
                    }
                },
                label = "accountCategoryNavigation"
            ) { category ->
                when (category) {
                    null -> {
                        AccountOverviewScreen(
                            data = data,
                            theme = theme,
                            dynamicColor = dynamicColor,
                            aiProvider = aiProvider,
                            providerApiKeys = providerApiKeys,
                            aiUsage = aiUsage,
                            aiTokens = aiTokens,
                            onSelectCategory = { selectedCategory = it },
                            onSignOutClick = { showSignOutDialog = true },
                            onDeleteAccountClick = { showDeleteAccountDialog = true },
                            onNavigateToHistory = { rootNavController.navigate("settlement_history") },
                            onNavigateToActivity = { rootNavController.navigate("activity") }
                        )
                    }
                        AccountCategory.THEME -> {
                            AppearanceSettingsSubScreen(
                                theme = theme,
                                onThemeChange = { viewModel.setTheme(it) },
                                onBack = { selectedCategory = null }
                            )
                        }
                        AccountCategory.CURRENCIES -> {
                            CurrencySettingsSubScreen(
                                defaultCurrency = data.defaultCurrency,
                                onCurrencySelected = {
                                    viewModel.updateDefaultCurrency(it)
                                    snackbar.showMessage("Default currency set to $it (${CurrencyFormatter.getCurrencySymbol(it)})")
                                },
                                onBack = { selectedCategory = null }
                            )
                        }
                        AccountCategory.AI_PROVIDERS -> {
                            AiProvidersSubScreen(
                                activeProvider = aiProvider,
                                providerApiKeys = providerApiKeys,
                                customEndpoint = customEndpoint,
                                customModel = customModel,
                                onSelectProvider = {
                                    viewModel.setAiProvider(it)
                                    snackbar.showMessage("Active AI Engine set to ${AI_PROVIDERS_CATALOG.find { p -> p.id == it }?.name ?: it}")
                                },
                                onSaveProviderKey = { providerId, key ->
                                    viewModel.setProviderApiKey(providerId, key)
                                    val name = AI_PROVIDERS_CATALOG.find { p -> p.id == providerId }?.name ?: providerId
                                    snackbar.showMessage(if (key.isBlank()) "API key removed for $name" else "API key saved for $name")
                                },
                                onSaveCustomSettings = { endpoint, model ->
                                    viewModel.setCustomEndpoint(endpoint)
                                    viewModel.setCustomModel(model)
                                    snackbar.showMessage("Custom AI settings updated")
                                },
                                aiEnabled = aiEnabled,
                                disabledFeatures = aiDisabledFeatures,
                                providerModels = providerModels,
                                usage = aiUsage,
                                tokens = aiTokens,
                                onToggleAi = { viewModel.setAiEnabled(it) },
                                onToggleFeature = { f, on -> viewModel.setAiFeatureEnabled(f, on) },
                                onSetModel = { providerId, model -> viewModel.setProviderModel(providerId, model) },
                                onTestKey = { providerId, key, endpoint, model, cb -> viewModel.testAiKey(providerId, key, endpoint, model, cb) },
                                onBack = { selectedCategory = null }
                            )
                        }
                        AccountCategory.NOTIFICATIONS -> {
                            // One persisted notification screen for the whole app (also reachable from the primer).
                            androidx.compose.runtime.LaunchedEffect(Unit) {
                                selectedCategory = null
                                rootNavController.navigate("notification_settings")
                            }
                        }
                        AccountCategory.DATA_BACKUP -> {
                            DataBackupSubScreen(
                                expenseCount = data.allExpenses.size,
                                onExportClick = { showExportBottomSheet = true },
                                onClearCache = {
                                    snackbar.showMessage("Application cache cleared successfully")
                                },
                                onOpenRecurring = { rootNavController.navigate("recurring_expenses") },
                                onOpenRecentlyDeleted = { rootNavController.navigate("recently_deleted") },
                                onBack = { selectedCategory = null }
                            )
                        }
                        AccountCategory.SECURITY -> {
                            AccountSecuritySubScreen(
                                data = data,
                                onUpdateDisplayName = { newName ->
                                    viewModel.updateDisplayName(newName)
                                    snackbar.showMessage("Display name updated to $newName")
                                },
                                onSendPasswordReset = { email ->
                                    viewModel.sendPasswordResetEmail(email) { result ->
                                        if (result.isSuccess) {
                                            snackbar.showMessage("Password reset email sent to $email")
                                        } else {
                                            snackbar.showMessage("Failed to send reset email: ${result.exceptionOrNull()?.message}")
                                        }
                                    }
                                },
                                onDeleteAccountClick = { showDeleteAccountDialog = true },
                                onBack = { selectedCategory = null }
                            )
                        }
                        AccountCategory.M3_CATALOG -> {
                            M3ComponentsCatalogSubScreen(
                                onBack = { selectedCategory = null }
                            )
                        }
                        AccountCategory.ABOUT -> {
                            AboutSupportSubScreen(
                                onBack = { selectedCategory = null }
                            )
                        }
                    }
                }
            }
        }

    if (showExportBottomSheet) {
        val expenses = (uiState as? com.opensplit.ui.viewmodel.ScreenState.Success)?.data?.allExpenses ?: emptyList()
        ExportBottomSheet(
            scopeName = "All Personal Data",
            expenses = expenses,
            onDismiss = { showExportBottomSheet = false }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = { SymbolIcon(OpenSplitIcons.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Log Out?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of OpenSplit on this device?") },
            confirmButton = {
                Button(shapes = ButtonDefaults.shapes(), 
                    onClick = {
                        viewModel.signOut()
                        showSignOutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            icon = { SymbolIcon(OpenSplitIcons.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Account?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("This will permanently remove your account, group memberships, and cloud data. This action cannot be undone.") },
            confirmButton = {
                Button(shapes = ButtonDefaults.shapes(), 
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Permanently Delete")
                }
            },
            dismissButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/* =========================================================================================
 * 1. MASTER OVERVIEW SCREEN
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountOverviewScreen(
    data: AccountUiState,
    theme: String,
    dynamicColor: Boolean,
    aiProvider: String,
    providerApiKeys: Map<String, String>,
    aiUsage: Map<String, Int> = emptyMap(),
    aiTokens: Map<String, Int> = emptyMap(),
    onSelectCategory: (AccountCategory) -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToActivity: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            windowInsets = WindowInsets(0, 0, 0, 0),
            actions = {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = onNavigateToHistory) {
                    SymbolIcon(
                        symbol = OpenSplitIcons.History,
                        contentDescription = "Settlement History"
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        item {
            // Compact identity row: avatar, name, email. Nothing here is actionable, so it stays small.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    if (!data.user.photoUrl?.toString().isNullOrBlank()) {
                        AsyncImage(model = data.user.photoUrl.toString(), contentDescription = "User Avatar", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text((data.user.displayName ?: "U").take(1).uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(data.user.displayName ?: "OpenSplit User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(data.user.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }

        item {
            Text(
                text = "Preferences & Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
            )
        }

        items(AccountCategory.values()) { cat ->
            val badgeText = when (cat) {
                AccountCategory.THEME -> {
                    when (theme) {
                        "light" -> "Light"
                        "dark" -> "Dark"
                        "amoled" -> "AMOLED"
                        else -> "System"
                    }
                }
                AccountCategory.CURRENCIES -> {
                    "${CurrencyFormatter.getCurrencyFlag(data.defaultCurrency)} ${data.defaultCurrency} (${CurrencyFormatter.getCurrencySymbol(data.defaultCurrency)})"
                }
                AccountCategory.AI_PROVIDERS -> {
                    // Locked until a key exists; afterwards the badge is a live usage meter.
                    val hasKey = aiProvider == "custom" || !providerApiKeys[aiProvider].isNullOrBlank()
                    if (!hasKey) "No key · locked" else {
                        val calls = aiUsage[aiProvider] ?: 0
                        val tokens = aiTokens[aiProvider] ?: 0
                        val t = when { tokens >= 1_000_000 -> "%.1fM".format(tokens / 1e6); tokens >= 1000 -> "%.1fk".format(tokens / 1e3); else -> "$tokens" }
                        "$calls calls · $t tokens"
                    }
                }
                AccountCategory.NOTIFICATIONS -> "Enabled"
                AccountCategory.DATA_BACKUP -> "${data.allExpenses.size} expenses"
                AccountCategory.SECURITY -> "Google SSO"
                AccountCategory.M3_CATALOG -> "All 30+ Components"
                AccountCategory.ABOUT -> "v2.4.1"
            }

            Card(
                onClick = { onSelectCategory(cat) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            SymbolIcon(
                                symbol = cat.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cat.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = cat.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    SymbolIcon(
                        symbol = OpenSplitIcons.ArrowForwardIos,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onSignOutClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SymbolIcon(
                        symbol = OpenSplitIcons.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = onDeleteAccountClick) {
                    SymbolIcon(
                        symbol = OpenSplitIcons.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delete Account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "OpenSplit App Version 2.4.1 (Build 890)",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
}

