package com.opensplit.ui.screens

import com.opensplit.ui.components.LocalSnackbarController

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.opensplit.di.AppContainer
import com.opensplit.ui.viewmodel.AnalyticsViewModel
import com.opensplit.ui.viewmodel.HomeViewModel
import com.opensplit.ui.viewmodel.MainViewModel
import com.opensplit.ui.viewmodel.ScreenState
import com.opensplit.ui.viewmodel.ViewModelFactory
import kotlin.math.abs
import kotlinx.coroutines.launch

import com.opensplit.ui.components.appHazeHeader
import com.opensplit.ui.components.appHazeSource
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitMotion
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainDashboard(
    appContainer: AppContainer,
    rootNavController: NavHostController
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabTitles = listOf("OpenSplit", "Groups", "Friends", "Analytics")
    val tabIcons = listOf(
        OpenSplitIcons.Home,
        OpenSplitIcons.Groups,
        OpenSplitIcons.Friends,
        OpenSplitIcons.Analytics
    )
    val tabLabels = listOf("Home", "Groups", "Friends", "Analytics")
    val hazeState = remember { HazeState() }
    val coroutineScope = rememberCoroutineScope()

    val mainViewModel: MainViewModel = viewModel(factory = ViewModelFactory(appContainer))
    val homeViewModel: HomeViewModel = viewModel(factory = ViewModelFactory(appContainer))
    val analyticsViewModel: AnalyticsViewModel = viewModel(factory = ViewModelFactory(appContainer))

    val currentUid = appContainer.authRepository.currentUser?.uid ?: ""
    // Remember the flows so recomposition doesn't rebuild and re-subscribe them each pass.
    val currentUserFlow = remember(currentUid) { appContainer.userRepository.getUserFlow(currentUid) }
    val currentUserState by currentUserFlow.collectAsState(initial = null)
    val userGroupsState by mainViewModel.userGroups.collectAsState()

    val groupIds = (userGroupsState as? ScreenState.Success)?.data?.map { it.id } ?: emptyList()
    val activitiesFlow = remember(currentUid, groupIds) {
        appContainer.activityRepository.getActivityForUser(currentUid, groupIds)
    }
    val activitiesState by activitiesFlow.collectAsState(initial = emptyList())

    val unreadCount = remember(activitiesState, currentUserState) {
        val lastSeen = currentUserState?.lastSeenActivityTimestamp
        if (lastSeen == null) {
            activitiesState.size
        } else {
            activitiesState.count { it.timestamp.seconds > lastSeen.seconds }
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var showGlobalSearchSheet by remember { mutableStateOf(false) }

    // Global quick-action FAB: which actions it offers depends on the active tab.
    var fabExpanded by remember { mutableStateOf(false) }
    var showFabAddExpensePicker by remember { mutableStateOf(false) }
    var showFabSettleUpPicker by remember { mutableStateOf(false) }
    var showFabCreateGroup by remember { mutableStateOf(false) }
    var showFabInviteFriend by remember { mutableStateOf(false) }

    val allGroups = (userGroupsState as? ScreenState.Success)?.data ?: emptyList()
    val settleableGroups = allGroups.filter { it.memberIds.size > 1 }
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbar = LocalSnackbarController.current

    data class QuickAction(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val onClick: () -> Unit)

    val quickActions: List<QuickAction> = when (selectedTab) {
        0 -> listOfNotNull(
            QuickAction("Add Expense", OpenSplitIcons.AddExpense) {
                if (allGroups.isEmpty()) {
                    snackbar.showMessage("Create a group first")
                } else if (allGroups.size == 1) {
                    rootNavController.navigate("add_expense/${allGroups.first().id}")
                } else {
                    showFabAddExpensePicker = true
                }
            },
            QuickAction("Settle Up", OpenSplitIcons.Settle) {
                when {
                    settleableGroups.isEmpty() -> snackbar.showMessage("Add another member to a group before settling up")
                    settleableGroups.size == 1 -> rootNavController.navigate("settle_up/${settleableGroups.first().id}")
                    else -> showFabSettleUpPicker = true
                }
            },
            QuickAction("New Group", OpenSplitIcons.Groups) { showFabCreateGroup = true },
            QuickAction("Invite a Friend", OpenSplitIcons.Invite) { showFabInviteFriend = true },
            QuickAction("Load Demo Data", OpenSplitIcons.Refresh) {
                mainViewModel.seedMockData { success ->
                    snackbar.showMessage(if (success) "Sample data loaded! Check Home & Analytics!" else "Failed to load sample data.")
                }
            }
        )
        1 -> listOfNotNull(
            QuickAction("New Group", OpenSplitIcons.Groups) { showFabCreateGroup = true },
            QuickAction("Add Expense", OpenSplitIcons.AddExpense) {
                if (allGroups.isEmpty()) {
                    snackbar.showMessage("Create a group first")
                } else if (allGroups.size == 1) {
                    rootNavController.navigate("add_expense/${allGroups.first().id}")
                } else {
                    showFabAddExpensePicker = true
                }
            },
            QuickAction("Settle Up", OpenSplitIcons.Settle) {
                when {
                    settleableGroups.isEmpty() -> snackbar.showMessage("Add another member to a group before settling up")
                    settleableGroups.size == 1 -> rootNavController.navigate("settle_up/${settleableGroups.first().id}")
                    else -> showFabSettleUpPicker = true
                }
            },
            QuickAction("Load Demo Data", OpenSplitIcons.Refresh) {
                mainViewModel.seedMockData { success ->
                    snackbar.showMessage(if (success) "Sample data loaded! Check Home & Analytics!" else "Failed to load sample data.")
                }
            }
        )
        2 -> listOfNotNull(
            QuickAction("Invite a Friend", OpenSplitIcons.Invite) { showFabInviteFriend = true },
            QuickAction("Settle Up", OpenSplitIcons.Settle) {
                when {
                    settleableGroups.isEmpty() -> snackbar.showMessage("Add another member to a group before settling up")
                    settleableGroups.size == 1 -> rootNavController.navigate("settle_up/${settleableGroups.first().id}")
                    else -> showFabSettleUpPicker = true
                }
            },
            QuickAction("Add Expense", OpenSplitIcons.AddExpense) {
                if (allGroups.isEmpty()) {
                    snackbar.showMessage("Create a group first")
                } else if (allGroups.size == 1) {
                    rootNavController.navigate("add_expense/${allGroups.first().id}")
                } else {
                    showFabAddExpensePicker = true
                }
            }
        )
        3 -> listOfNotNull(
            QuickAction("Add Expense", OpenSplitIcons.AddExpense) {
                if (allGroups.isEmpty()) {
                    snackbar.showMessage("Create a group first")
                } else if (allGroups.size == 1) {
                    rootNavController.navigate("add_expense/${allGroups.first().id}")
                } else {
                    showFabAddExpensePicker = true
                }
            },
            QuickAction("Settle Up", OpenSplitIcons.Settle) {
                when {
                    settleableGroups.isEmpty() -> snackbar.showMessage("Add another member to a group before settling up")
                    settleableGroups.size == 1 -> rootNavController.navigate("settle_up/${settleableGroups.first().id}")
                    else -> showFabSettleUpPicker = true
                }
            },
            QuickAction("New Group", OpenSplitIcons.Groups) { showFabCreateGroup = true }
        )
        else -> emptyList()
    }
    LaunchedEffect(selectedTab) { fabExpanded = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) "OpenSplit" else tabTitles[selectedTab],
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                ),
                modifier = Modifier.appHazeHeader(hazeState),
                actions = {
                    // Global Search Button
                    IconButton(onClick = { showGlobalSearchSheet = true }) {
                        Icon(
                            imageVector = OpenSplitIcons.Search,
                            contentDescription = "Search"
                        )
                    }

                    // Activity bell with badge
                    IconButton(onClick = { rootNavController.navigate("activity") }) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.offset(x = (-6).dp, y = 3.dp)
                                    ) {
                                        Text(
                                            text = if (unreadCount > 10) "10+" else "$unreadCount",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 3.dp)
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = OpenSplitIcons.Activity,
                                contentDescription = "Activity Feed"
                            )
                        }
                    }

                    // Direct Account & Settings Button
                    IconButton(onClick = { rootNavController.navigate("account") }) {
                        Icon(
                            imageVector = OpenSplitIcons.Account,
                            contentDescription = "Account & Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (quickActions.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            quickActions.forEach { action ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.clickable {
                                        fabExpanded = false
                                        action.onClick()
                                    }
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        tonalElevation = 3.dp,
                                        shadowElevation = 2.dp
                                    ) {
                                        Text(
                                            text = action.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }

                                    SmallFloatingActionButton(
                                        onClick = {
                                            fabExpanded = false
                                            action.onClick()
                                        },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        shape = CircleShape
                                    ) {
                                        Icon(action.icon, contentDescription = action.label, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    val fabMainIcon = when (selectedTab) {
                        2 -> OpenSplitIcons.Invite
                        1 -> OpenSplitIcons.Groups
                        else -> OpenSplitIcons.AddExpense
                    }

                    FloatingActionButton(
                        onClick = {
                            if (quickActions.size == 1) {
                                quickActions.first().onClick()
                            } else {
                                fabExpanded = !fabExpanded
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (fabExpanded) OpenSplitIcons.Close else fabMainIcon,
                            contentDescription = if (fabExpanded) "Close quick actions" else "Quick actions"
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                tabLabels.forEachIndexed { index, label ->
                    val showGroupBadge = index == 1 && (userGroupsState as? ScreenState.Success)?.data?.isNotEmpty() == true
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            if (showGroupBadge) {
                                BadgedBox(badge = { Badge() }) {
                                    Icon(tabIcons[index], contentDescription = label)
                                }
                            } else {
                                Icon(tabIcons[index], contentDescription = label)
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .appHazeSource(hazeState)
        ) {
            // M3 fade-through: bottom-nav destinations are peers with no spatial
            // relationship, so they cross-fade rather than slide.
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { OpenSplitMotion.fadeThrough() },
                label = "dashboardTab"
            ) { tab ->
                when (tab) {
                    0 -> HomeScreen(
                        viewModel = homeViewModel,
                        mainViewModel = mainViewModel,
                        onNavigateToGroupsTab = { selectedTab = 1 },
                        onNavigateToGroupDetail = { groupId -> rootNavController.navigate("group_detail/$groupId") },
                        onNavigateToActivity = { rootNavController.navigate("activity") },
                        onNavigateToAddExpense = { groupId -> rootNavController.navigate("add_expense/$groupId") },
                        onNavigateToSettleUp = { groupId -> rootNavController.navigate("settle_up/$groupId") },
                        onNavigateToPersonBalance = { friendId -> rootNavController.navigate("person_balance/$friendId") }
                    )
                    1 -> GroupsScreen(
                        viewModel = mainViewModel,
                        onGroupClick = { groupId -> rootNavController.navigate("group_detail/$groupId") },
                        onAddExpense = { groupId -> rootNavController.navigate("add_expense/$groupId") },
                        onSettleUp = { groupId -> rootNavController.navigate("settle_up/$groupId") }
                    )
                    2 -> FriendsScreen(
                        viewModel = mainViewModel,
                        onFriendClick = { friendId -> rootNavController.navigate("person_balance/$friendId") }
                    )
                    3 -> AnalyticsScreen(
                        viewModel = analyticsViewModel,
                        onNavigateToExpenseDetail = { groupId, expenseId ->
                            rootNavController.navigate("expense_detail/$groupId/$expenseId")
                        }
                    )
                }
            }

            // Faded Blue Radial Blur Overlay starting from bottom right when Speed Dial FAB is expanded
            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn(animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeEffect(
                            state = hazeState,
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                blurRadius = 24.dp
                            )
                        )
                        .drawBehind {
                            val center = Offset(size.width, size.height)
                            val radius = size.maxDimension * 1.15f
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF2563EB).copy(alpha = 0.45f), // Radiant vibrant blue at bottom-right FAB
                                        Color(0xFF1E3A8A).copy(alpha = 0.55f), // Deep royal blue transition
                                        Color(0xFF0F172A).copy(alpha = 0.65f), // Dark navy backdrop
                                        Color.Black.copy(alpha = 0.60f)        // Deep dark shadow across screen
                                    ),
                                    center = center,
                                    radius = radius
                                )
                            )
                        }
                        .clickable { fabExpanded = false }
                )
            }
        }
    }

    if (showGlobalSearchSheet) {
        com.opensplit.ui.components.GlobalSearchSheet(
            appContainer = appContainer,
            onDismiss = { showGlobalSearchSheet = false },
            onNavigateToGroup = { groupId ->
                rootNavController.navigate("group_detail/$groupId")
            },
            onNavigateToExpense = { groupId, expenseId ->
                rootNavController.navigate("expense_detail/$groupId/$expenseId")
            },
            onNavigateToFriend = { friendId ->
                rootNavController.navigate("person_balance/$friendId")
            },
            onAddExpense = {
                showFabAddExpensePicker = true
            },
            onCreateGroup = {
                showFabCreateGroup = true
            },
            onSettleUp = {
                showFabSettleUpPicker = true
            },
            onInviteFriend = {
                showFabInviteFriend = true
            }
        )
    }

    if (showFabAddExpensePicker) {
        GroupSelectionDialog(
            title = "Select Group for New Expense",
            groups = allGroups,
            onDismiss = { showFabAddExpensePicker = false },
            onSelectGroup = { groupId ->
                showFabAddExpensePicker = false
                rootNavController.navigate("add_expense/$groupId")
            }
        )
    }

    if (showFabSettleUpPicker) {
        GroupSelectionDialog(
            title = "Select Group to Settle Up",
            groups = settleableGroups,
            onDismiss = { showFabSettleUpPicker = false },
            onSelectGroup = { groupId ->
                showFabSettleUpPicker = false
                rootNavController.navigate("settle_up/$groupId")
            }
        )
    }

    if (showFabCreateGroup) {
        com.opensplit.ui.components.CreateGroupDialog(
            onDismiss = { showFabCreateGroup = false },
            onCreate = { name, currency, avatarKey ->
                mainViewModel.createGroup(name, currency, avatarKey)
                snackbar.showMessage("Created $name")
                showFabCreateGroup = false
            }
        )
    }

    if (showFabInviteFriend) {
        com.opensplit.ui.components.InviteMemberDialog(
            title = "Invite a friend",
            description = "Pick them from your contacts, or invite over WhatsApp or SMS.",
            onDismiss = { showFabInviteFriend = false },
            onSubmitEmail = { email ->
                mainViewModel.sendFriendInvite(email) { success ->
                    snackbar.showMessage(if (success) "Invite sent" else "Couldn't send (already invited or invalid email)")
                }
            }
        )
    }
}

