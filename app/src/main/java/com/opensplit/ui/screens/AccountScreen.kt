@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.opensplit.R
import com.opensplit.di.AppContainer
import com.opensplit.ui.components.ExportBottomSheet
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.components.responsiveContentWidth
import com.opensplit.ui.components.responsiveDialogWidth
import com.opensplit.ui.components.appHazeHeader
import com.opensplit.ui.components.appHazeSource
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.viewmodel.AccountUiState
import com.opensplit.ui.viewmodel.AccountViewModel
import com.opensplit.util.CurrencyFormatter
import dev.chrisbanes.haze.HazeState

enum class AccountCategory(
    val title: String,
    val icon: Symbol
) {
    THEME(
        title = "Theme",
        icon = OpenSplitIcons.Palette
    ),
    CURRENCIES(
        title = "Currency",
        icon = OpenSplitIcons.CurrencyExchange
    ),
    NOTIFICATIONS(
        title = "Notifications",
        icon = OpenSplitIcons.Notifications
    ),
    SYNC(
        title = "Cloud Sync",
        icon = OpenSplitIcons.Sync
    ),
    STORAGE(
        title = "Device Storage",
        icon = OpenSplitIcons.CleaningServices
    ),
    AI_PROVIDERS(
        title = "AI Assistant",
        icon = OpenSplitIcons.AutoAwesome
    ),
    DATA_BACKUP(
        title = "Backup & Export",
        icon = OpenSplitIcons.CloudDownload
    ),
    SECURITY(
        title = "Security",
        icon = OpenSplitIcons.Security
    ),
    BUG_REPORT(
        title = "Report a bug",
        icon = OpenSplitIcons.Warning
    ),
    ABOUT(
        title = "About",
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val theme by viewModel.themeFlow.collectAsStateWithLifecycle(initialValue = "system")
    val dynamicColor by viewModel.dynamicColorFlow.collectAsStateWithLifecycle(initialValue = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val notificationsEnabled by viewModel.notificationsEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val aiProvider by viewModel.aiProviderFlow.collectAsStateWithLifecycle(initialValue = "openai")
    val providerApiKeys by viewModel.providerApiKeysFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
    val customEndpoint by viewModel.customEndpointFlow.collectAsStateWithLifecycle(initialValue = "http://localhost:11434/v1")
    val customModel by viewModel.customModelFlow.collectAsStateWithLifecycle(initialValue = "llama3:latest")
    val aiEnabled by viewModel.aiEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val aiDisabledFeatures by viewModel.aiDisabledFeaturesFlow.collectAsStateWithLifecycle(initialValue = emptySet())
    val providerModels by viewModel.providerModelsFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
    val aiUsage by viewModel.aiUsageFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
    val aiTokens by viewModel.aiTokensFlow.collectAsStateWithLifecycle(initialValue = emptyMap())

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
                // Same shared-axis motion as full-screen routes (e.g. Notifications).
                transitionSpec = { com.opensplit.ui.theme.OpenSplitMotion.sharedAxisX(forward = targetState != null) },
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
                            onNavigateToHistory = { rootNavController.navigate("settlement_history") },
                            onNavigateToActivity = { rootNavController.navigate("activity") },
                            onUpdateDisplayName = { newName ->
                                viewModel.updateDisplayName(newName)
                                snackbar.showMessage("Display name updated to $newName")
                            },
                            onExportClick = { showExportBottomSheet = true }
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
                                onClearCache = { com.opensplit.util.StorageStats.clearCache(context) },
                                onBackupSettings = { appContainer.userPreferencesRepository.exportSettings() },
                                onRestoreSettings = { appContainer.userPreferencesRepository.importSettings(it) },
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
                                onUpdateUpiId = { upi ->
                                    viewModel.updateUpiId(upi)
                                    snackbar.showMessage(if (upi == null) "UPI ID removed" else "UPI ID saved")
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
                        AccountCategory.BUG_REPORT -> {
                            BugReportSubScreen(onBack = { selectedCategory = null })
                        }
                        AccountCategory.ABOUT -> {
                            AboutSupportSubScreen(
                                onBack = { selectedCategory = null }
                            )
                        }
                        AccountCategory.SYNC,
                        AccountCategory.STORAGE -> {
                            selectedCategory = null
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
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                Button(
                    shapes = com.opensplit.ui.theme.appButtonShapes(),
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
                TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { showSignOutDialog = false }) {
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
            text = { Text("This will permanently remove your account and all data. This cannot be undone.") },
            confirmButton = {
                Button(
                    shapes = com.opensplit.ui.theme.appButtonShapes(),
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { showDeleteAccountDialog = false }) {
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
    onNavigateToHistory: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onUpdateDisplayName: (String) -> Unit = {},
    onExportClick: () -> Unit = {}
) {
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newDisplayName by remember(data.user.displayName) { mutableStateOf(data.user.displayName ?: "") }

    var isSyncingNow by remember { mutableStateOf(false) }
    var syncProgress by remember { mutableFloatStateOf(0f) }
    var syncStatusText by remember { mutableStateOf("All balances up to date") }
    var lastSyncPingMs by remember { mutableIntStateOf(0) }
    // null while checking; a server read (not the local cache) decides it.
    var serverReachable by remember { mutableStateOf<Boolean?>(null) }
    androidx.compose.runtime.LaunchedEffect(data.user.uid) {
        serverReachable = isServerReachable(data.user.uid)
    }

    var isClearingCache by remember { mutableStateOf(false) }
    val storageContext = LocalContext.current
    var cacheBytes by remember { mutableStateOf(0L) }
    var appDataBytes by remember { mutableStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        cacheBytes = com.opensplit.util.StorageStats.cacheBytes(storageContext)
        appDataBytes = com.opensplit.util.StorageStats.appDataBytes(storageContext)
    }
    var showSyncBottomSheet by remember { mutableStateOf(false) }
    var showStorageBottomSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbar = LocalSnackbarController.current

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            icon = { SymbolIcon(OpenSplitIcons.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Edit Display Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newDisplayName,
                    onValueChange = { newDisplayName = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDisplayName.isNotBlank()) {
                            onUpdateDisplayName(newDisplayName.trim())
                        }
                        showEditNameDialog = false
                    },
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    var settingsSearchQuery by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .responsiveContentWidth(maxWidth = 760.dp)
        ) {
            TopAppBar(
                expandedHeight = 88.dp,
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    com.opensplit.ui.components.HeaderAction(
                        OpenSplitIcons.History, "Settlement History",
                        MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = onNavigateToHistory
                    )
                    Spacer(Modifier.width(8.dp))
                }
            )

            data class CategoryGroup(
                val sectionTitle: String,
                val categories: List<AccountCategory>
            )

            val groupedSections = remember {
                listOf(
                    CategoryGroup(
                        sectionTitle = "Sync & Storage",
                        categories = listOf(
                            AccountCategory.SYNC,
                            AccountCategory.STORAGE
                        )
                    ),
                    CategoryGroup(
                        sectionTitle = "Preferences",
                        categories = listOf(
                            AccountCategory.THEME,
                            AccountCategory.CURRENCIES,
                            AccountCategory.NOTIFICATIONS
                        )
                    ),
                    CategoryGroup(
                        sectionTitle = "Intelligence",
                        categories = listOf(
                            AccountCategory.AI_PROVIDERS
                        )
                    ),
                    CategoryGroup(
                        sectionTitle = "Data & Backup",
                        categories = listOf(
                            AccountCategory.DATA_BACKUP
                        )
                    ),
                    CategoryGroup(
                        sectionTitle = "Security & About",
                        categories = listOf(
                            AccountCategory.SECURITY,
                            AccountCategory.BUG_REPORT,
                            AccountCategory.ABOUT
                        )
                    )
                )
            }

            val filteredSections = remember(groupedSections, settingsSearchQuery) {
                if (settingsSearchQuery.isBlank()) groupedSections
                else {
                    groupedSections.mapNotNull { group ->
                        val matchingCats = group.categories.filter { cat ->
                            cat.title.contains(settingsSearchQuery, ignoreCase = true) ||
                            group.sectionTitle.contains(settingsSearchQuery, ignoreCase = true)
                        }
                        if (matchingCats.isNotEmpty()) group.copy(categories = matchingCats) else null
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = com.opensplit.ui.components.FloatingNavBarClearance),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // ---------------------------------------------------------------------------------
            // HERO PROFILE CARD (AVATAR + AUTH STATUS)
            // ---------------------------------------------------------------------------------
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Box(modifier = Modifier.size(54.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                if (!data.user.photoUrl?.toString().isNullOrBlank()) {
                                    AsyncImage(
                                        model = data.user.photoUrl.toString(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            (data.user.displayName ?: "U").take(1).uppercase(),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF22C55E),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier
                                    .size(13.dp)
                                    .align(Alignment.BottomEnd)
                            ) {}
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = data.user.displayName ?: "User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (!data.user.email.isNullOrBlank()) {
                                Text(
                                    text = data.user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    if (data.authProvider == "Google") {
                                        androidx.compose.material3.Icon(
                                            painter = painterResource(id = R.drawable.ic_google_logo),
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    } else {
                                        SymbolIcon(
                                            symbol = OpenSplitIcons.Verified,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "${data.authProvider} connected",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    SymbolIcon(
                                        symbol = OpenSplitIcons.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledTonalButton(
                            onClick = { showEditNameDialog = true },
                            shape = MaterialTheme.shapes.extraSmall,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Edit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }



            item {
                com.opensplit.ui.components.AppSearchBar(
                    query = settingsSearchQuery,
                    onQueryChange = { settingsSearchQuery = it },
                    placeholderText = "Search preferences, themes, sync..."
                )
            }

            if (filteredSections.isEmpty()) {
                item {
                    com.opensplit.ui.components.ExpressiveEmptyState(
                        title = "No settings found",
                        subtitle = "No preferences match \"$settingsSearchQuery\"",
                        symbol = OpenSplitIcons.Search
                    )
                }
            }

            filteredSections.forEach { group ->
                item {
                    Text(
                        text = group.sectionTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )

                    // Segmented list: rows are separate tiles, big outer corners, tight inner ones.
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        run {
                            group.categories.forEachIndexed { idx, cat ->

                                val badgeText = when (cat) {
                                    AccountCategory.SYNC -> {
                                        when {
                                            isSyncingNow -> "Syncing..."
                                            serverReachable == null -> "Checking…"
                                            serverReachable == false -> "Offline"
                                            lastSyncPingMs > 0 -> "Synced · ${lastSyncPingMs}ms"
                                            else -> "Synced"
                                        }
                                    }
                                    AccountCategory.STORAGE -> {
                                        com.opensplit.util.StorageStats.formatMb(cacheBytes + appDataBytes)
                                    }
                                    AccountCategory.THEME -> {
                                        when (theme) {
                                            "light" -> "Light"
                                            "dark" -> "Dark"
                                            else -> "System"
                                        }
                                    }
                                    AccountCategory.CURRENCIES -> {
                                        "${CurrencyFormatter.getCurrencyFlag(data.defaultCurrency)} ${data.defaultCurrency} (${CurrencyFormatter.getCurrencySymbol(data.defaultCurrency)})"
                                    }
                                    AccountCategory.AI_PROVIDERS -> {
                                        val hasKey = aiProvider == "custom" || !providerApiKeys[aiProvider].isNullOrBlank()
                                        if (!hasKey) "Off" else aiProvider.replaceFirstChar { it.uppercase() }
                                    }
                                    AccountCategory.NOTIFICATIONS -> "On"
                                    AccountCategory.DATA_BACKUP -> data.allExpenses.size.let { "$it ${if (it == 1) "item" else "items"}" }
                                    AccountCategory.SECURITY -> "Protected"
                                    AccountCategory.BUG_REPORT -> "Email"
                                    AccountCategory.ABOUT -> "v${com.opensplit.BuildConfig.VERSION_NAME}"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(com.opensplit.ui.components.segmentShape(idx, group.categories.size))
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .clickable {
                                            when (cat) {
                                                AccountCategory.SYNC -> showSyncBottomSheet = true
                                                AccountCategory.STORAGE -> showStorageBottomSheet = true
                                                else -> onSelectCategory(cat)
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (iconBg, iconFg) = com.opensplit.ui.components.toneFor(cat.ordinal)
                                    Surface(
                                        shape = CircleShape,
                                        color = iconBg,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            SymbolIcon(
                                                symbol = cat.icon,
                                                contentDescription = null,
                                                tint = iconFg,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Text(
                                        text = cat.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            if (cat == AccountCategory.SYNC && !isSyncingNow && serverReachable != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (serverReachable == true) Color(0xFF10B981) else MaterialTheme.colorScheme.error)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                            } else if (cat == AccountCategory.SYNC && isSyncingNow) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(10.dp),
                                                    strokeWidth = 1.5.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                            }
                                            Text(
                                                text = badgeText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = when {
                                                    cat != AccountCategory.SYNC || isSyncingNow || serverReachable == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    serverReachable == true -> Color(0xFF059669)
                                                    else -> MaterialTheme.colorScheme.error
                                                }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    SymbolIcon(
                                        symbol = OpenSplitIcons.ArrowForwardIos,
                                        contentDescription = "Open",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------------------------------
            // FOOTER & ACCOUNT ACTIONS
            // ---------------------------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onSignOutClick,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    SymbolIcon(
                        symbol = OpenSplitIcons.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "v${com.opensplit.BuildConfig.VERSION_NAME} (${com.opensplit.BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showSyncBottomSheet) {
            CloudSyncBottomSheet(
                isSyncing = isSyncingNow,
                syncProgress = syncProgress,
                syncStatusText = syncStatusText,
                lastSyncPingMs = lastSyncPingMs,
                reachable = serverReachable,
                userEmail = data.user.email,
                onDismiss = { showSyncBottomSheet = false },
                onSyncNow = {
                    if (!isSyncingNow) {
                        isSyncingNow = true
                        syncProgress = 0f
                        coroutineScope.launch {
                            syncStatusText = "Syncing..."
                            syncProgress = 0.5f
                            // Real round-trip: wait until every local write is acknowledged by the server.
                            val started = System.currentTimeMillis()
                            val ok = runCatching {
                                kotlinx.coroutines.withTimeout(15_000) {
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        .waitForPendingWrites().await()
                                }
                            }.isSuccess && isServerReachable(data.user.uid)
                            serverReachable = ok
                            syncProgress = 1f
                            isSyncingNow = false
                            if (ok) {
                                lastSyncPingMs = (System.currentTimeMillis() - started).toInt()
                                syncStatusText = "Synced (${lastSyncPingMs}ms)"
                                snackbar.showMessage("All changes synced with cloud")
                            } else {
                                syncStatusText = "Offline: changes will sync when connected"
                                snackbar.showMessage("Couldn't reach the server; changes are saved on this device")
                            }
                        }
                    }
                }
            )
        }

        if (showStorageBottomSheet) {
            DeviceStorageBottomSheet(
                cacheBytes = cacheBytes,
                appDataBytes = appDataBytes,
                isClearing = isClearingCache,
                onDismiss = { showStorageBottomSheet = false },
                onClearCache = {
                    if (!isClearingCache && cacheBytes > 0) {
                        isClearingCache = true
                        coroutineScope.launch {
                            val freed = com.opensplit.util.StorageStats.clearCache(storageContext)
                            cacheBytes = com.opensplit.util.StorageStats.cacheBytes(storageContext)
                            isClearingCache = false
                            snackbar.showMessage("Cache cleared (freed ${com.opensplit.util.StorageStats.formatMb(freed)})")
                        }
                    }
                }
            )
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudSyncBottomSheet(
    isSyncing: Boolean,
    syncProgress: Float,
    syncStatusText: String,
    lastSyncPingMs: Int,
    reachable: Boolean?,
    userEmail: String?,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cloud Sync",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time sync across all your devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    SymbolIcon(OpenSplitIcons.Close, contentDescription = "Close")
                }
            }

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connection Status",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val statusColor = when (reachable) {
                            true -> Color(0xFF10B981)
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = CircleShape,
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (reachable) {
                                        null -> "Checking…"
                                        false -> "Can't reach server"
                                        true -> if (lastSyncPingMs > 0) "Connected · ${lastSyncPingMs}ms" else "Connected"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sync Account",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = userEmail ?: "Google Account",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = syncStatusText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isSyncing) {
                        LinearProgressIndicator(
                            progress = { syncProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = onSyncNow,
                enabled = !isSyncing,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing with cloud...")
                } else {
                    SymbolIcon(
                        symbol = OpenSplitIcons.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Now", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceStorageBottomSheet(
    cacheBytes: Long,
    appDataBytes: Long,
    isClearing: Boolean,
    onDismiss: () -> Unit,
    onClearCache: () -> Unit
) {
    val totalMb = String.format("%.1f", (cacheBytes + appDataBytes) / 1_048_576.0)
    val storageRatio = ((cacheBytes + appDataBytes) / (100.0 * 1_048_576)).toFloat().coerceIn(0.05f, 1f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.CleaningServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Storage",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Local cache, database, and receipt scans",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    SymbolIcon(OpenSplitIcons.Close, contentDescription = "Close")
                }
            }

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total App Storage",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$totalMb MB",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { storageRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val cacheFormatted = com.opensplit.util.StorageStats.formatMb(cacheBytes)
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Cache", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(cacheFormatted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("App data", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(com.opensplit.util.StorageStats.formatMb(appDataBytes), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onClearCache,
                enabled = !isClearing && cacheBytes > 0,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clearing cache...")
                } else {
                    SymbolIcon(
                        symbol = OpenSplitIcons.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (cacheBytes > 0) "Clear Cache (${com.opensplit.util.StorageStats.formatMb(cacheBytes)})" else "Cache is Clean",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


/** One server read (bypassing the offline cache) to tell whether sync can actually reach Firestore. */
private suspend fun isServerReachable(uid: String): Boolean = uid.isNotBlank() && runCatching {
    kotlinx.coroutines.withTimeout(8_000) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .get(com.google.firebase.firestore.Source.SERVER).await()
    }
}.isSuccess
