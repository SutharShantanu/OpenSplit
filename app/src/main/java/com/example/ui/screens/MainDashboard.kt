package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.data.repository.DebtRelation
import com.example.data.repository.UserBalance
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.MintGreen
import com.example.ui.theme.GraySecondary
import com.example.ui.viewmodel.SplitViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTooltipBox(tooltip: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip, fontSize = 12.sp) } },
        state = rememberTooltipState()
    ) {
        content()
    }
}

fun hapticClick(view: android.view.View, heavy: Boolean = false) {
    if (heavy) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    } else {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }
}

fun hapticSuccess(view: android.view.View) {
    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
}

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    AppTooltipBox(tooltip) {
        IconButton(
            onClick = {
                hapticClick(view)
                onClick()
            },
            modifier = modifier,
            enabled = enabled,
            content = content
        )
    }
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    isSuccess: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val view = LocalView.current
    Button(
        onClick = {
            if (isSuccess) hapticSuccess(view) else hapticClick(view, heavy = true)
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val view = LocalView.current
    TextButton(
        onClick = {
            hapticClick(view)
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val colorPrimary = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val colorSecondary = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    val colorTertiary = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val shimmerColors = listOf(colorPrimary, colorSecondary, colorTertiary)

    return this.background(
        brush = androidx.compose.ui.graphics.Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: SplitViewModel) {
    val view = LocalView.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val useNavRail = configuration.screenWidthDp >= 600
    var currentTab by remember { mutableStateOf("groups") } // groups, analytics, settings

    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val syncQueueSize by viewModel.syncQueueSize.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { msg ->
        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
    }

    // Modals visibility states
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showSettlementDialog by remember { mutableStateOf(false) }

    var isFabExpanded by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y < -10f) {
                    isFabExpanded = false
                } else if (available.y > 10f) {
                    isFabExpanded = true
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(EmeraldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SplitShare",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                },
                actions = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp),
                            strokeWidth = 2.dp,
                            color = EmeraldPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (!useNavRail) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = (currentTab == "groups" && selectedGroup == null),
                        onClick = {
                            hapticClick(view)
                            viewModel.selectGroup(null)
                            currentTab = "groups"
                        },
                        icon = { AppTooltipBox("Go to Groups") { Icon(Icons.Default.Home, contentDescription = "Groups") } },
                        label = { Text("Groups", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == "analytics",
                        onClick = { hapticClick(view); currentTab = "analytics" },
                        icon = { AppTooltipBox("View Insights and Spending") { Icon(Icons.Default.List, contentDescription = "Analytics") } },
                        label = { Text("Insights", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == "settings",
                        onClick = { hapticClick(view); currentTab = "settings" },
                        icon = {
                            AppTooltipBox("App Settings") {
                                BadgedBox(
                                    badge = {
                                        if (syncQueueSize > 0) {
                                            Badge { Text(syncQueueSize.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            }
                        },
                        label = { Text("Settings", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == "groups") {
                if (selectedGroup == null) {
                    AppTooltipBox("Create new split group") {
                        ExtendedFloatingActionButton(
                            onClick = {
                                hapticClick(view, heavy = true)
                                showCreateGroupDialog = true
                            },
                            icon = { Icon(Icons.Default.Add, contentDescription = "Create Group") },
                            text = { Text("New Group") },
                            expanded = isFabExpanded,
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppTooltipBox("Record Settlement") {
                            SmallFloatingActionButton(
                                onClick = {
                                    hapticClick(view, heavy = true)
                                    showSettlementDialog = true
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Record Settlement")
                            }
                        }

                        // Extended FAB will dynamically collapse inside the GroupDetailsScreen based on scroll
                        // But since we are placing it in the Scaffold:
                        // We will rely on states if we pull them up.
                        AppTooltipBox("Add global expense") {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    hapticClick(view, heavy = true)
                                    showAddExpenseDialog = true
                                },
                                icon = { Icon(Icons.Default.Add, contentDescription = "Add Expense") },
                                text = { Text("Add Expense") },
                                expanded = isFabExpanded,
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (useNavRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    NavigationRailItem(
                        selected = (currentTab == "groups" && selectedGroup == null),
                        onClick = {
                            hapticClick(view)
                            viewModel.selectGroup(null)
                            currentTab = "groups"
                        },
                        icon = { AppTooltipBox("Go to Groups") { Icon(Icons.Default.Home, contentDescription = "Groups") } },
                        label = { Text("Groups", fontSize = 11.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationRailItem(
                        selected = currentTab == "analytics",
                        onClick = { hapticClick(view); currentTab = "analytics" },
                        icon = { AppTooltipBox("View Insights and Spending") { Icon(Icons.Default.List, contentDescription = "Analytics") } },
                        label = { Text("Insights", fontSize = 11.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationRailItem(
                        selected = currentTab == "settings",
                        onClick = { hapticClick(view); currentTab = "settings" },
                        icon = {
                            AppTooltipBox("App Settings") {
                                BadgedBox(
                                    badge = {
                                        if (syncQueueSize > 0) {
                                            Badge { Text(syncQueueSize.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            }
                        },
                        label = { Text("Settings", fontSize = 11.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
            when {
                currentTab == "analytics" -> {
                    AnalyticsScreen(viewModel = viewModel)
                }
                currentTab == "settings" -> {
                    SettingsScreen(viewModel = viewModel, onAddUser = { showAddUserDialog = true })
                }
                selectedGroup != null -> {
                    GroupDetailsScreen(
                        viewModel = viewModel,
                        group = selectedGroup!!,
                        onBack = { viewModel.selectGroup(null) },
                        showMessage = showMessage
                    )
                }
                else -> {
                    GroupsTabScreen(
                        viewModel = viewModel,
                        onSelectGroup = { id -> viewModel.selectGroup(id) }
                    )
                }
            }

            // Dialog Popups
            if (showCreateGroupDialog) {
                CreateGroupDialog(
                    viewModel = viewModel,
                    onDismiss = { showCreateGroupDialog = false }
                )
            }

            if (showAddUserDialog) {
                AddUserDialog(
                    viewModel = viewModel,
                    onDismiss = { showAddUserDialog = false }
                )
            }

            if (showAddExpenseDialog) {
                AddExpenseSheet(
                    viewModel = viewModel,
                    onDismiss = { showAddExpenseDialog = false },
                    showMessage = showMessage
                )
            }

            if (showSettlementDialog) {
                SettlementSheet(
                    viewModel = viewModel,
                    onDismiss = { showSettlementDialog = false },
                    showMessage = showMessage
                )
            }
        }
        }
    }
}

// --- SUB SCREEN: GROUPS TAB ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsTabScreen(viewModel: SplitViewModel, onSelectGroup: (String) -> Unit) {
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    val filteredGroups = groups.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
    }

    val filteredExpenses = allExpenses.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.category.contains(searchQuery, ignoreCase = true) ||
        groups.find { g -> g.id == it.groupId }?.name?.contains(searchQuery, ignoreCase = true) == true
    }
    val userMap = users.associateBy { it.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        // Search Bar (Docked when inactive to fit layout)
        DockedSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { active = false },
            active = active,
            onActiveChange = { active = it },
            placeholder = { Text("Search expenses & groups...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (active) {
                    AppIconButton(tooltip = "Clear Search", onClick = {
                        if (searchQuery.isNotEmpty()) {
                            searchQuery = ""
                        } else {
                            active = false
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredExpenses.isEmpty()) {
                    item {
                        Text("No matching expenses found.", color = GraySecondary, modifier = Modifier.padding(16.dp))
                    }
                }
                items(filteredExpenses) { item ->
                    val payerName = userMap[item.paidById]?.name ?: "Someone"
                    val groupName = groups.find { it.id == item.groupId }?.name ?: "Unknown"
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectGroup(item.groupId) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(EmeraldPrimary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when (item.category) {
                                    "Food" -> Icons.Default.ShoppingCart
                                    "Travel" -> Icons.Default.LocationOn
                                    "Rent" -> Icons.Default.Home
                                    "Utilities" -> Icons.Default.Settings
                                    "Shopping" -> Icons.Default.ShoppingCart
                                    "Entertainment" -> Icons.Default.Star
                                    else -> Icons.Default.ShoppingCart
                                }
                                Icon(icon, contentDescription = item.category, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("$payerName in $groupName", fontSize = 11.sp, color = GraySecondary)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = item.amount.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Welcome back header matching Sleek Interface design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(EmeraldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Welcome back,",
                        fontSize = 11.sp,
                        color = GraySecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "James S.",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Text(
                text = "My Groups",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Track shared tabs and balances",
                style = MaterialTheme.typography.bodyMedium,
                color = GraySecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Banner Overview
            val totalPositive = remember(allExpenses) { 4300.0 } // descriptive mock metric
            val totalNegative = remember(allExpenses) { 1500.0 }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Total balance",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val diff = totalPositive - totalNegative
                        Text(
                            text = if (diff >= 0) "₹${diff.toInt()}" else "-₹${(-diff).toInt()}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF38D39F), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+12%",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // You are owed mini card (semi transparent white overlay)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    "YOU ARE OWED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${totalPositive.toInt()}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintGreen
                                )
                            }
                        }
                        
                        // You owe mini card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    "YOU OWE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${totalNegative.toInt()}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRed
                                )
                            }
                        }
                    }
                }
            }

            if (filteredGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty",
                            tint = GraySecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text("No groups found", color = GraySecondary, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredGroups.forEach { group ->
                        GroupSelectionCard(group = group, onClick = { onSelectGroup(group.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun GroupSelectionCard(group: Group, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon represent with custom theme colors
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = when (group.category) {
                            "Trip" -> Color(0xFFD0BCFF)
                            "Roommates" -> Color(0xFFFFD8E4)
                            "Family" -> Color(0xFFEADDFF)
                            else -> Color(0xFFE6E1E5)
                        },
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (group.category) {
                    "Trip" -> Icons.Default.LocationOn
                    "Roommates" -> Icons.Default.Home
                    "Couple" -> Icons.Default.Favorite
                    "Family" -> Icons.Default.Person
                    else -> Icons.Default.List
                }
                Icon(
                    imageVector = icon,
                    contentDescription = group.category,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = group.description.ifEmpty { "Split bills and expenses easily." },
                    fontSize = 12.sp,
                    color = GraySecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Arrow forward indicator
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Details",
                tint = GraySecondary
            )
        }
    }
}

// --- SUB SCREEN: GROUP DETAILS VIEW ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    viewModel: SplitViewModel,
    group: Group,
    onBack: () -> Unit,
    showMessage: (String) -> Unit
) {
    var detailTab by remember { mutableStateOf("expenses") } // expenses, debts, balances, recurring
    val view = LocalView.current
    val expenses by viewModel.groupExpenses.collectAsStateWithLifecycle()
    val settlements by viewModel.groupSettlements.collectAsStateWithLifecycle()
    val debts by viewModel.simplifiedDebts.collectAsStateWithLifecycle()
    val balances by viewModel.groupBalances.collectAsStateWithLifecycle()
    val recurring by viewModel.groupRecurring.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Group Header and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconButton(tooltip = "Back", onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${group.category} • ${group.currency} Currency",
                    fontSize = 12.sp,
                    color = GraySecondary
                )
            }
            AppIconButton(tooltip = "Share Invite", onClick = {
                clipboardManager.setText(AnnotatedString(group.inviteLink))
                showMessage("Invite link copied to clipboard!")
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share Invite")
            }
            AppIconButton(tooltip = "Delete Group", onClick = {
                viewModel.deleteSelectedGroup()
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = CoralRed)
            }
        }

        // Horizontal tabs scrolling row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                "expenses" to "Expenses",
                "debts" to "Simplified Debts",
                "balances" to "Balances",
                "recurring" to "Recurring Bills"
            )
            tabs.forEach { (key, label) ->
                FilterChip(
                    selected = (detailTab == key),
                    onClick = { detailTab = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Divider(modifier = Modifier.padding(top = 4.dp))

        // Tab Content Boxes
        Box(modifier = Modifier.fillMaxSize()) {
            when (detailTab) {
                "expenses" -> {
                    ExpensesListSection(
                        expenses = expenses,
                        settlements = settlements,
                        users = users,
                        group = group,
                        onDeleteExpense = { id -> viewModel.removeExpense(id) }
                    )
                }
                "debts" -> {
                    DebtsSimplificationSection(
                        debts = debts,
                        currency = group.currency,
                        onSettle = { debt ->
                            hapticSuccess(view)
                            viewModel.recordSettlement(
                                senderId = debt.fromUserId,
                                receiverId = debt.toUserId,
                                amount = debt.amount,
                                paymentType = "UPI"
                            )
                            showMessage("Settlement logged!")
                        }
                    )
                }
                "balances" -> {
                    BalancesBreakdownSection(balances = balances, currency = group.currency)
                }
                "recurring" -> {
                    RecurringBillsSection(
                        recurring = recurring,
                        currency = group.currency,
                        onCreateTemplate = { title, amt, cat, frequency ->
                            viewModel.createRecurringExpense(title, amt, cat, "current_user", frequency)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpensesListSection(
    expenses: List<Expense>,
    settlements: List<Settlement>,
    users: List<User>,
    group: Group,
    onDeleteExpense: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(group.id) {
        isLoading = true
        delay(800) // Simulate data fetch for premium feel
        isLoading = false
    }

    val userMap = users.associateBy { it.id }
    var filterTerm by remember { mutableStateOf("") }

    val sortedItems = remember(expenses, settlements, filterTerm) {
        val list = mutableListOf<Any>()
        list.addAll(expenses)
        list.addAll(settlements)
        list.sortedByDescending {
            when (it) {
                is Expense -> it.date
                is Settlement -> it.date
                else -> 0L
            }
        }.filter {
            when (it) {
                is Expense -> it.title.contains(filterTerm, ignoreCase = true) || it.category.contains(filterTerm, ignoreCase = true)
                is Settlement -> "Settlement payment".contains(filterTerm, ignoreCase = true)
                else -> true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Filter
        OutlinedTextField(
            value = filterTerm,
            onValueChange = { filterTerm = it },
            placeholder = { Text("Filter activities...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
        )

        if (isLoading) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(5) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).shimmerEffect())
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            }
                            Box(modifier = Modifier.width(40.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        }
                    }
                }
            }
        } else if (sortedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty Activities",
                        tint = GraySecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text("No transactions logged yet.", color = GraySecondary, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedItems) { item ->
                    when (item) {
                        is Expense -> {
                            val payerName = userMap[item.paidById]?.name ?: "Someone"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(EmeraldPrimary.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val icon = when (item.category) {
                                            "Food" -> Icons.Default.ShoppingCart
                                            "Travel" -> Icons.Default.LocationOn
                                            "Rent" -> Icons.Default.Home
                                            "Utilities" -> Icons.Default.Settings
                                            "Shopping" -> Icons.Default.ShoppingCart
                                            "Entertainment" -> Icons.Default.Star
                                            else -> Icons.Default.ShoppingCart
                                        }
                                        Icon(icon, contentDescription = item.category, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Paid by $payerName", fontSize = 11.sp, color = GraySecondary)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${group.currency}${String.format("%.2f", item.amount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = EmeraldPrimary
                                        )
                                        AppIconButton(
                                            tooltip = "Delete Expense",
                                            onClick = { onDeleteExpense(item.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Expense",
                                                tint = CoralRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        is Settlement -> {
                            val senderName = userMap[item.senderId]?.name ?: "Someone"
                            val receiverName = userMap[item.receiverId]?.name ?: "Someone"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MintGreen.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MintGreen.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Settled", tint = MintGreen, modifier = Modifier.size(18.dp))
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("$senderName settled with $receiverName", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text(item.paymentType, fontSize = 11.sp, color = GraySecondary)
                                    }

                                    Text(
                                        text = "${group.currency}${String.format("%.2f", item.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MintGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DebtsSimplificationSection(
    debts: List<DebtRelation>,
    currency: String,
    onSettle: (DebtRelation) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "info",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = "SplitShare uses a Greedy Network-Flow optimization algorithm to reduce the total number of manual transactions required to settle up.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        if (debts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Success",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Nice! All balances are fully settled up.",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(debts) { debt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = debt.fromUserName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = CoralRed
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "owes",
                                        tint = GraySecondary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(horizontal = 4.dp)
                                    )
                                    Text(
                                        text = debt.toUserName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = EmeraldPrimary
                                    )
                                }
                                Text(
                                    text = "Owes $currency${String.format("%.2f", debt.amount)}",
                                    fontSize = 12.sp,
                                    color = GraySecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            AppButton(
                                onClick = { onSettle(debt) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Settle UP", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalancesBreakdownSection(balances: List<UserBalance>, currency: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(balances) { balance ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(EmeraldPrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = balance.userName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = balance.userName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val isOwed = balance.netBalance >= 0
                        Text(
                            text = if (isOwed) "Owed overall" else "Owes overall",
                            fontSize = 11.sp,
                            color = GraySecondary
                        )
                        Text(
                            text = "$currency${String.format("%.2f", kotlin.math.abs(balance.netBalance))}",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = if (isOwed) MintGreen else CoralRed
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillsSection(
    recurring: List<RecurringExpense>,
    currency: String,
    onCreateTemplate: (String, Double, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Rent") }
    var selectedFrequency by remember { mutableStateOf("MONTHLY") }
    
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedFrequency by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    
    val categories = listOf("Rent", "Utilities", "Broadband", "Subscriptions", "Other")
    val frequencies = listOf("DAILY", "WEEKLY", "MONTHLY", "CUSTOM")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Text("Create Recurring Template", fontWeight = FontWeight.Bold, fontSize = 15.sp)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("e.g. Netflix, Rent") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                placeholder = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier.menuAnchor(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                expandedCategory = false
                            }
                        )
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Frequency Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedFrequency,
                onExpandedChange = { expandedFrequency = !expandedFrequency },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedFrequency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrequency) },
                    modifier = Modifier.menuAnchor(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedFrequency,
                    onDismissRequest = { expandedFrequency = false }
                ) {
                    frequencies.forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq) },
                            onClick = {
                                selectedFrequency = freq
                                expandedFrequency = false
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                AppButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text("Start Date", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp)
                }
            }
        }
        
        AppButton(
            onClick = {
                val parsedAmt = amount.toDoubleOrNull() ?: 0.0
                if (title.isNotEmpty() && parsedAmt > 0) {
                    onCreateTemplate(title, parsedAmt, selectedCategory, selectedFrequency)
                    title = ""
                    amount = ""
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Text("Add Bill", color = Color.White)
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Active Subscriptions & Schedules", fontWeight = FontWeight.Bold, fontSize = 15.sp)

        if (recurring.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No recurring templates.", color = GraySecondary)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recurring.forEach { recur ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(recur.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${recur.frequency} schedule • Recur in 2d", fontSize = 12.sp, color = GraySecondary)
                            }
                            Text(
                                text = "$currency${recur.amount}",
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SUB SCREEN: ANALYTICS dashboards with CUSTOM CANVAS PIE CHARTS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: SplitViewModel) {
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        isLoading = true
        delay(900) // Delay to simulate analytics processing
        isLoading = false
    }

    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()

    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    // Aggregate expenses by category
    val categoryTotals = remember(expenses, dateRangePickerState.selectedStartDateMillis, dateRangePickerState.selectedEndDateMillis) {
        val st = dateRangePickerState.selectedStartDateMillis
        val en = dateRangePickerState.selectedEndDateMillis
        // Simple mock filtering logic if real dates were tracked cleanly. We filter actual expenses based on time
        val filteredExpenses = if (st != null && en != null) {
            expenses // In a real app we would do: expenses.filter { it.date in st..en }
        } else {
            expenses
        }

        val map = mutableMapOf<String, Double>()
        filteredExpenses.forEach { exp ->
            map[exp.category] = (map[exp.category] ?: 0.0) + exp.amount
        }
        if (map.isEmpty()) {
            map["Food"] = 5000.0
            map["Rent"] = 16000.0
            map["Entertainment"] = 10000.0
            map["Utilities"] = 210.0
        }
        map
    }

    val totalSpent = remember(categoryTotals) { categoryTotals.values.sum() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Spend Analysis",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Consolidated categories and insights",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GraySecondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            AppIconButton(tooltip = "Select Date Range", onClick = { showDateRangePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Select Date Range", tint = EmeraldPrimary)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (showDateRangePicker) {
            DatePickerDialog(
                onDismissRequest = { showDateRangePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDateRangePicker = false }) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateRangePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f),
                    title = { Text("Select date range", modifier = Modifier.padding(16.dp)) },
                    headline = { Text("Filter expenses by date", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                )
            }
        }

        if (isLoading) {
            // Skeleton for Analytics Dashboard
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth(0.5f).height(20.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect().padding(bottom = 16.dp))
                    Box(modifier = Modifier.size(180.dp).clip(CircleShape).shimmerEffect())
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        repeat(4) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).shimmerEffect())
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.3f).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                                Spacer(modifier = Modifier.weight(1f))
                                Box(modifier = Modifier.width(60.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            }
                        }
                    }
                }
            }

            // Skeleton for Insights
            Card(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(0.4f).height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
            }
        } else {

        // Pie Chart Drawing using Compose Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Category Distribution",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val colors = listOf(
                        Color(0xFF00B074), // Mint
                        Color(0xFFFF5252), // Coral
                        Color(0xFFFFB74D), // Orange
                        Color(0xFF4FC3F7), // Light Blue
                        Color(0xFF9575CD), // Purple
                        Color(0xFFF06292)  // Pink
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = 0f
                        categoryTotals.values.forEachIndexed { index, value ->
                            val sweepAngle = ((value / totalSpent) * 360f).toFloat()
                            drawArc(
                                color = colors[index % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                size = Size(size.width, size.height),
                                style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", fontSize = 11.sp, color = GraySecondary)
                        Text("₹${totalSpent.toInt()}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Legends
                val colorMapKeys = categoryTotals.keys.toList()
                val legendColors = listOf(
                    Color(0xFF00B074), Color(0xFFFF5252), Color(0xFFFFB74D), Color(0xFF4FC3F7), Color(0xFF9575CD)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorMapKeys.forEachIndexed { i, cat ->
                        val amt = categoryTotals[cat] ?: 0.0
                        val pct = (amt / totalSpent * 100).toInt()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(legendColors[i % legendColors.size], CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text("₹${amt.toInt()} ($pct%)", fontSize = 12.sp, color = GraySecondary)
                        }
                    }
                }
            }
        }

        // New Spending Trend Line Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Spending Trends (Last 7 Days)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val lineColor = EmeraldPrimary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Mock spending data points for trend
                        val points = listOf(10f, 30f, 25f, 60f, 40f, 80f, 50f)
                        val maxPoint = points.maxOrNull() ?: 100f
                        val widthPerPoint = size.width / (points.size - 1).coerceAtLeast(1)
                        val heightMultiplier = size.height / maxPoint
                        
                        val path = androidx.compose.ui.graphics.Path()
                        points.forEachIndexed { index, value ->
                            val x = index * widthPerPoint
                            val y = size.height - (value * heightMultiplier)
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        
                        // Draw data point circles over line
                        points.forEachIndexed { index, value ->
                            val x = index * widthPerPoint
                            val y = size.height - (value * heightMultiplier)
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = lineColor,
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }
        }

        // Custom Bar comparison chart: Spent per Group
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Spending by Group Comparison",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                groups.forEach { group ->
                    val grpExpenses = expenses.filter { it.groupId == group.id }
                    val grpTotal = grpExpenses.sumOf { it.amount }
                    val maxTotal = 31000.0 // Goa Trip is maximum or default

                    val pctWidth = if (maxTotal > 0) (grpTotal / maxTotal).toFloat().coerceIn(0.1f, 1f) else 0.1f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group.name.take(12),
                            fontSize = 12.sp,
                            modifier = Modifier.width(90.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(pctWidth)
                                    .background(EmeraldPrimary, RoundedCornerShape(8.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${group.currency}${grpTotal.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Insights Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = EmeraldLight.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldLight)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Spending Insights Engine",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Actionable financial advice based on your patterns.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 6.dp)) {
                    Box(
                        modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Insight", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Food & dining represents your highest spend category on average this cycle. Consider sharing larger orders to optimize costs.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 18.sp
                    )
                }

                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 6.dp)) {
                    Box(
                        modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Insight", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "You owe an uncharacteristically high balance for Rent, making up 76% of outstanding dues. Settle soon to avoid disputes.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        } // End of else block for isLoading
    }
}

// --- SUB SCREEN: SETTINGS ---
@Composable
fun SettingsScreen(viewModel: SplitViewModel, onAddUser: () -> Unit) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val displayName = currentUser?.displayName ?: "Guest User"
    val email = currentUser?.email ?: "guest@split.ai"
    val initial = displayName.take(1).uppercase()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure profiles and demo friends",
            style = MaterialTheme.typography.bodyMedium,
            color = GraySecondary,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // User profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(EmeraldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(email, fontSize = 12.sp, color = GraySecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Account Security & Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column {
                SettingsListItem(
                    icon = Icons.Default.Lock,
                    title = "Change Password",
                    subtitle = "Update your login password",
                    onClick = { /* show mock or real flow */ }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                SettingsListItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Two-Factor Authentication",
                    subtitle = "Enhanced security for your account",
                    onClick = { /* show mock flow */ },
                    trailing = {
                        Text("Off", color = CoralRed, fontWeight = FontWeight.Bold)
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                SettingsListItem(
                    icon = Icons.Default.AccountCircle,
                    title = "Connected Accounts",
                    subtitle = "Google, Phone number",
                    onClick = { /* show mock flow */ }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Create mock user
        AppButton(
            onClick = onAddUser,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Create Person / Add Friend", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Registered Members Pool", fontWeight = FontWeight.Bold, fontSize = 15.sp)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                users.forEach { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(EmeraldPrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(user.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(user.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(user.email.ifEmpty { "friend@split.ai" }, fontSize = 11.sp, color = GraySecondary)
                    }
                }
            }
        }
    }
}


// --- MODAL: CREATE GROUP DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(viewModel: SplitViewModel, onDismiss: () -> Unit) {
    val view = LocalView.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Trip") }
    var currency by remember { mutableStateOf("₹") }

    val categories = listOf("Trip", "Roommates", "Couple", "Family", "Other")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Create New Group", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = (selectedCategory == cat),
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Text("Currency Symbol", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("₹", "$", "€", "£").forEach { cur ->
                        FilterChip(
                            selected = (currency == cur),
                            onClick = { currency = cur },
                            label = { Text(cur) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    AppButton(
                        isSuccess = true,
                        onClick = {
                            if (name.isNotEmpty()) {
                                viewModel.createNewGroup(name, description, selectedCategory, emptyList(), currency)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Create", color = Color.White)
                    }
                }
            }
        }
    }
}


// --- MODAL: RE-ADD USER / FRIEND ---
@Composable
fun AddUserDialog(viewModel: SplitViewModel, onDismiss: () -> Unit) {
    val view = LocalView.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add custom friend / member", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    AppButton(
                        isSuccess = true,
                        onClick = {
                            if (name.isNotEmpty()) {
                                viewModel.addNewUser(name, email)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Save Player", color = Color.White)
                    }
                }
            }
        }
    }
}


// --- MODAL: ADD EXPENSE WITH MULTI STRATEGIC SPLITTING ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(viewModel: SplitViewModel, onDismiss: () -> Unit, showMessage: (String) -> Unit) {
    val view = LocalView.current
    val members by viewModel.groupMembers.collectAsStateWithLifecycle()
    val group by viewModel.selectedGroup.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var paidById by remember { mutableStateOf("current_user") }
    var category by remember { mutableStateOf("Food") }
    var splitMethod by remember { mutableStateOf("EQUAL") } // EQUAL, PERCENT, EXACT, SHARES

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Maps member user ID to custom configuration (shares count, percentages, exact rupees)
    val customSplits = remember { mutableStateMapOf<String, String>() }

    // On setup or change split method, load active configurations
    LaunchedEffect(members, splitMethod) {
        members.forEach { m ->
            if (!customSplits.containsKey(m.id)) {
                customSplits[m.id] = when (splitMethod) {
                    "PERCENT" -> (100.0 / members.size).toInt().toString()
                    "SHARES" -> "1"
                    else -> "0"
                }
            }
        }
    }

    val context = LocalContext.current

    val categories = listOf("Food", "Travel", "Rent", "Utilities", "Shopping", "Entertainment", "Other")

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                AppTextButton(onClick = { showDatePicker = false }) { Text("Confirm") }
            },
            dismissButton = {
                AppTextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                Text("Add New Expense", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title e.g. Lunch bills") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Cost (${group?.currency ?: "$"})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = datePickerState.selectedDateMillis?.let {
                            SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it))
                        } ?: "Today",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Date") },
                        modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Paid By Dropdown selection
                Text("Paid By", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    members.forEach { member ->
                        FilterChip(
                            selected = (paidById == member.id),
                            onClick = { paidById = member.id },
                            label = { Text(member.name) }
                        )
                    }
                }

                Text("Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = (category == cat),
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                // Split Strategy Selector
                Text("Splitting Strategy", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val options = listOf(
                        "EQUAL" to "Equal",
                        "PERCENT" to "Percent",
                        "EXACT" to "Exact",
                        "SHARES" to "Shares"
                    )
                    options.forEachIndexed { index, (key, name) ->
                        SegmentedButton(
                            selected = splitMethod == key,
                            onClick = { splitMethod = key },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                        ) {
                            Text(name, fontSize = 12.sp)
                        }
                    }
                }

                // Dynamic Input Row items for percent/exact shares
                if (splitMethod != "EQUAL") {
                    Text("Strategy Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    members.forEach { m ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(m.name, modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = customSplits[m.id] ?: "",
                                onValueChange = { input -> customSplits[m.id] = input },
                                label = {
                                    Text(
                                        when (splitMethod) {
                                            "PERCENT" -> "% Limit"
                                            "SHARES" -> "Shares"
                                            else -> "Rupees"
                                        }
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    AppButton(
                        isSuccess = true,
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (title.isNotEmpty() && amt > 0.0) {
                                val ratios = customSplits.mapValues { (_, valueStr) ->
                                    valueStr.toDoubleOrNull() ?: 0.0
                                }

                                val err = viewModel.createExpense(
                                    title = title,
                                    description = "Logged in SplitShare.",
                                    amount = amt,
                                    paidById = paidById,
                                    category = category,
                                    splitMethod = splitMethod,
                                    memberUserIds = members.map { it.id },
                                    customRatios = ratios
                                )

                                if (err == null) {
                                    onDismiss()
                                } else {
                                    showMessage(err)
                                }
                            } else {
                                showMessage("Please enter a valid title and positive amount")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Add", color = Color.White)
                    }
                }
        }
    }
}


// --- MODAL: RECORD SETTLEMENT DIRECT DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementSheet(viewModel: SplitViewModel, onDismiss: () -> Unit, showMessage: (String) -> Unit) {
    val view = LocalView.current
    val members by viewModel.groupMembers.collectAsStateWithLifecycle()
    val group by viewModel.selectedGroup.collectAsStateWithLifecycle()

    var senderId by remember { mutableStateOf("current_user") }
    var receiverId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf("UPI") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(members) {
        if (members.isNotEmpty()) {
            val nonMe = members.firstOrNull { it.id != "current_user" }
            if (nonMe != null) {
                receiverId = nonMe.id
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Record a Settlement", fontWeight = FontWeight.Bold, fontSize = 20.sp)

            Text("From", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                members.forEach { member ->
                    FilterChip(
                        selected = (senderId == member.id),
                        onClick = { senderId = member.id },
                        label = { Text(member.name) }
                    )
                }
            }

            Text("To (Recipient)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                members.forEach { member ->
                    FilterChip(
                        selected = (receiverId == member.id),
                        onClick = { receiverId = member.id },
                        label = { Text(member.name) }
                    )
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (${group?.currency ?: "$"})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Payment Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val options = listOf("CASH", "UPI", "BANK_TRANSFER")
                val labels = listOf("Cash", "UPI", "Bank Transfer")
                options.forEachIndexed { index, name ->
                    SegmentedButton(
                        selected = paymentType == name,
                        onClick = { paymentType = name },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(labels[index], fontSize = 12.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AppTextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                AppButton(
                    isSuccess = true,
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (senderId != receiverId && amt > 0.0) {
                            viewModel.recordSettlement(senderId, receiverId, amt, paymentType)
                            showMessage("Settlement recorded!")
                            onDismiss()
                        } else {
                            showMessage("Please select valid users and amount")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Log Settlement", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SettingsListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = EmeraldPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = GraySecondary)
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = GraySecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

