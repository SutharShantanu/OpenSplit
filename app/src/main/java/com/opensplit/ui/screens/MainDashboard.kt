@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.theme.Symbol
import com.opensplit.ui.components.SymbolIcon
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.opensplit.di.AppContainer
import com.opensplit.ui.components.CreateGroupDialog
import com.opensplit.ui.components.GlobalSearchSheet
import com.opensplit.ui.components.InviteMemberDialog
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.components.appHazeHeader
import com.opensplit.ui.components.appHazeSource
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitMotion
import com.opensplit.ui.viewmodel.AccountViewModel
import com.opensplit.ui.viewmodel.ActivityViewModel
import com.opensplit.ui.viewmodel.AnalyticsViewModel
import com.opensplit.ui.viewmodel.HomeViewModel
import com.opensplit.ui.viewmodel.MainViewModel
import com.opensplit.ui.viewmodel.ScreenState
import com.opensplit.ui.viewmodel.ViewModelFactory
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

/** Top-level destinations. Order is the bar order. */
enum class DashboardTab(val label: String, val icon: Symbol) {
    GROUPS("Groups", OpenSplitIcons.Groups),
    PEOPLE("People", OpenSplitIcons.Friends),
    ACTIVITY("Activity", OpenSplitIcons.Activity),
    INSIGHTS("Insights", OpenSplitIcons.Insights),
    SETTINGS("Settings", OpenSplitIcons.Account)
}

/** Material 3 window-size breakpoint: medium and up gets a rail instead of a bar. */
internal fun useNavigationRail(screenWidthDp: Int): Boolean = screenWidthDp >= 600

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainDashboard(
    appContainer: AppContainer,
    rootNavController: NavHostController
) {
    var selectedTab by rememberSaveable { mutableStateOf(DashboardTab.GROUPS) }
    val hazeState = remember { HazeState() }
    val snackbar = LocalSnackbarController.current
    val busy = com.opensplit.ui.components.LocalBusy.current
    val wide = useNavigationRail(LocalConfiguration.current.screenWidthDp)

    val mainViewModel: MainViewModel = viewModel(factory = ViewModelFactory(appContainer))
    val homeViewModel: HomeViewModel = viewModel(factory = ViewModelFactory(appContainer))
    val analyticsViewModel: AnalyticsViewModel = viewModel(factory = ViewModelFactory(appContainer))
    val activityViewModel: ActivityViewModel = viewModel(factory = ViewModelFactory(appContainer))
    val accountViewModel: AccountViewModel = viewModel(factory = ViewModelFactory(appContainer))

    val currentUid = appContainer.authRepository.currentUser?.uid ?: ""
    val currentUserFlow = remember(currentUid) { appContainer.userRepository.getUserFlow(currentUid) }
    val currentUserState by currentUserFlow.collectAsState(initial = null)
    val userGroupsState by mainViewModel.userGroups.collectAsState()
    val allGroups = (userGroupsState as? ScreenState.Success)?.data ?: emptyList()
    val groupIds = allGroups.map { it.id }
    val activitiesFlow = remember(currentUid, groupIds) {
        appContainer.activityRepository.getActivityForUser(currentUid, groupIds)
    }
    val activities by activitiesFlow.collectAsState(initial = emptyList())
    val unreadCount = remember(activities, currentUserState) {
        val lastSeen = currentUserState?.lastSeenActivityTimestamp
        if (lastSeen == null) activities.size else activities.count { it.timestamp.seconds > lastSeen.seconds }
    }

    var showGlobalSearch by remember { mutableStateOf(false) }
    var showAddExpensePicker by remember { mutableStateOf(false) }
    var showSettleUpPicker by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var showInviteFriend by remember { mutableStateOf(false) }
    var showQrSheet by remember { mutableStateOf(false) }
    val friendsState by mainViewModel.friendsBalances.collectAsState()
    val hasFriends = ((friendsState as? ScreenState.Success)?.data?.isNotEmpty() == true)
    val myFriendLink by mainViewModel.friendLinkUrl.collectAsState()
    // On wide layouts Groups and People keep their detail open beside the list.
    var detailGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailFriendId by rememberSaveable { mutableStateOf<String?>(null) }

    fun addExpense() = when (allGroups.size) {
        0 -> snackbar.showMessage("Create a group first")
        1 -> rootNavController.navigate("add_expense/${allGroups.first().id}")
        else -> showAddExpensePicker = true
    }

    // One contextual FAB per tab; tabs without a primary create action have none.
    // No FAB while a tab shows its empty state; the empty state already carries the one call to action.
    val fab: (@Composable () -> Unit)? = when (selectedTab) {
        DashboardTab.GROUPS -> if (allGroups.none { !it.isDirect }) null else ({
            FloatingActionButton(
                onClick = { addExpense() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) { SymbolIcon(OpenSplitIcons.AddExpense, contentDescription = "Add expense") }
        })
        DashboardTab.PEOPLE -> if (!hasFriends) null else ({
            FloatingActionButton(
                onClick = { showInviteFriend = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) { SymbolIcon(OpenSplitIcons.PersonAdd, contentDescription = "Add friend") }
        })
        else -> null
    }

    val tabItem: @Composable (DashboardTab, @Composable () -> Unit) -> Unit = { tab, content ->
        if (tab == DashboardTab.ACTIVITY && unreadCount > 0) {
            BadgedBox(badge = { Badge { Text(if (unreadCount > 99) "99+" else "$unreadCount") } }) { content() }
        } else content()
    }

    Scaffold(
        topBar = {
            if (selectedTab != DashboardTab.SETTINGS) {
                TopAppBar(
                    title = { Text(selectedTab.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier.appHazeHeader(hazeState),
                    actions = {
                        if (selectedTab == DashboardTab.GROUPS) {
                            IconButton(shapes = IconButtonDefaults.shapes(), onClick = { showCreateGroup = true }) {
                                SymbolIcon(OpenSplitIcons.GroupAdd, contentDescription = "New group")
                            }
                            IconButton(shapes = IconButtonDefaults.shapes(), onClick = { rootNavController.navigate("settlement_history") }) {
                                SymbolIcon(OpenSplitIcons.History, contentDescription = "Settlement history")
                            }
                        }
                        if (selectedTab == DashboardTab.PEOPLE) {
                            IconButton(shapes = IconButtonDefaults.shapes(), onClick = { mainViewModel.ensureFriendLink(); showQrSheet = true }) {
                                SymbolIcon(OpenSplitIcons.QrCode, contentDescription = "Show or scan a QR code")
                            }
                        }
                        IconButton(shapes = IconButtonDefaults.shapes(), onClick = { showGlobalSearch = true }) {
                            SymbolIcon(OpenSplitIcons.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        },
        floatingActionButton = { if (!wide) fab?.invoke() },
        bottomBar = {
            if (!wide) {
                // M3 Expressive short navigation bar: tighter than the classic one, pill indicator built in.
                androidx.compose.material3.ShortNavigationBar {
                    DashboardTab.entries.forEach { tab ->
                        androidx.compose.material3.ShortNavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { tabItem(tab) { SymbolIcon(tab.icon, contentDescription = null, filled = selectedTab == tab) } },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (wide) {
                // M3 Expressive wide navigation rail (collapsed): same items, expressive indicator and spacing.
                androidx.compose.material3.WideNavigationRail(header = { fab?.let { Spacer(Modifier.height(8.dp)); it() } }) {
                    // Short landscape windows can't fit five destinations; let the rail scroll.
                    androidx.compose.foundation.layout.Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        DashboardTab.entries.forEach { tab ->
                            androidx.compose.material3.WideNavigationRailItem(
                                railExpanded = false,
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { tabItem(tab) { SymbolIcon(tab.icon, contentDescription = null, filled = selectedTab == tab) } },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            com.opensplit.ui.components.OfflineBanner()
            // Pull down on any tab to refresh. Firestore streams live, so a refresh means: flush queued
            // writes to the server and re-subscribe any flow that is sitting in an error state.
            // Healthy flows are left alone (re-subscribing them flashes their empty state).
            var refreshing by remember { mutableStateOf(false) }
            val refreshScope = androidx.compose.runtime.rememberCoroutineScope()
            val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    refreshing = true
                    if (mainViewModel.userGroups.value is ScreenState.Error || mainViewModel.friendsBalances.value is ScreenState.Error) mainViewModel.retry()
                    if (activityViewModel.uiState.value is ScreenState.Error) activityViewModel.retry()
                    if (analyticsViewModel.uiState.value is ScreenState.Error) analyticsViewModel.retry()
                    if (accountViewModel.uiState.value is ScreenState.Error) accountViewModel.retry()
                    refreshScope.launch {
                        val flush = launch {
                            kotlinx.coroutines.withTimeoutOrNull(2000) {
                                kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance().waitForPendingWrites()
                                        .addOnCompleteListener { if (cont.isActive) cont.resume(Unit) {} }
                                }
                            }
                        }
                        kotlinx.coroutines.delay(700)
                        flush.join()
                        refreshing = false
                    }
                },
                state = pullState,
                indicator = {
                    androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator(
                        state = pullState,
                        isRefreshing = refreshing,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
                    )
                },
                modifier = Modifier.fillMaxSize().appHazeSource(hazeState)
            ) {
                // Content follows the finger (Gmail-style): it slides down by the pull distance and the
                // indicator sits in the gap it opens, instead of floating over the list.
                val pullOffsetPx = with(androidx.compose.ui.platform.LocalDensity.current) {
                    androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.PositionalThreshold.toPx()
                }
                // Peers with no spatial relationship cross-fade (M3 fade-through).
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { OpenSplitMotion.fadeThrough() },
                    label = "dashboardTab",
                    modifier = Modifier.graphicsLayer {
                        translationY = pullState.distanceFraction.coerceIn(0f, 1f) * pullOffsetPx
                    }
                ) { tab ->
                    when (tab) {
                        DashboardTab.GROUPS -> ListDetail(wide = wide, detailKey = detailGroupId,
                            list = {
                                GroupsScreen(
                                    viewModel = mainViewModel,
                                    homeViewModel = homeViewModel,
                                    onGroupClick = { if (wide) detailGroupId = it else rootNavController.navigate("group_detail/$it") },
                                    onAddExpense = { rootNavController.navigate("add_expense/$it") },
                                    onSettleUp = { rootNavController.navigate("settle_up/$it") },
                                    onPersonClick = { rootNavController.navigate("person_balance/$it") }
                                )
                            },
                            detail = { gId ->
                                val vm: com.opensplit.ui.viewmodel.GroupDetailViewModel = viewModel(key = "group_$gId", factory = com.opensplit.ui.viewmodel.GroupDetailViewModelFactory(gId, appContainer))
                                GroupDetailScreen(
                                    viewModel = vm,
                                    onNavigateBack = { detailGroupId = null },
                                    onNavigateToAddExpense = { rootNavController.navigate("add_expense/$gId") },
                                    onNavigateToExpenseDetail = { g, e -> rootNavController.navigate("expense_detail/$g/$e") },
                                    onNavigateToSettleUp = { rootNavController.navigate("settle_up/$gId") },
                                    onNavigateToEditExpense = { g, e -> rootNavController.navigate("edit_expense/$g/$e") }
                                )
                            },
                            placeholder = "Pick a group to see its expenses, balances and members here."
                        )
                        DashboardTab.PEOPLE -> ListDetail(wide = wide, detailKey = detailFriendId,
                            list = {
                                FriendsScreen(
                                    viewModel = mainViewModel,
                                    onFriendClick = { if (wide) detailFriendId = it else rootNavController.navigate("person_balance/$it") },
                                    onAddExpense = { rootNavController.navigate("add_expense/$it") },
                                    onSettleUp = { rootNavController.navigate("settle_up/$it") }
                                )
                            },
                            detail = { fId ->
                                val vm: com.opensplit.ui.viewmodel.PersonBalanceViewModel = viewModel(key = "person_$fId", factory = com.opensplit.ui.viewmodel.PersonBalanceViewModelFactory(fId, appContainer))
                                PersonBalanceScreen(
                                    viewModel = vm,
                                    onNavigateBack = { detailFriendId = null },
                                    onNavigateToSettleUp = { rootNavController.navigate("settle_up/$it") },
                                    onNavigateToGroup = { rootNavController.navigate("group_detail/$it") },
                                    onNavigateToAddExpense = { rootNavController.navigate("add_expense/$it") }
                                )
                            },
                            placeholder = "Pick a person to see what you share and settle up."
                        )
                        DashboardTab.ACTIVITY -> ActivityScreen(
                            viewModel = activityViewModel,
                            onNavigateBack = {},
                            showTopBar = false,
                            onOpenExpense = { gId, eId -> rootNavController.navigate("expense_detail/$gId/$eId") }
                        )
                        DashboardTab.INSIGHTS -> AnalyticsScreen(viewModel = analyticsViewModel)
                        DashboardTab.SETTINGS -> AccountScreen(
                            appContainer = appContainer,
                            rootNavController = rootNavController,
                            viewModel = accountViewModel
                        )
                    }
                }
            }
            }
        }
    }

    if (showGlobalSearch) {
        GlobalSearchSheet(
            appContainer = appContainer,
            onDismiss = { showGlobalSearch = false },
            onNavigateToGroup = { rootNavController.navigate("group_detail/$it") },
            onNavigateToExpense = { groupId, expenseId -> rootNavController.navigate("expense_detail/$groupId/$expenseId") },
            onNavigateToFriend = { rootNavController.navigate("person_balance/$it") },
            onAddExpense = { addExpense() },
            onCreateGroup = { showCreateGroup = true },
            onSettleUp = { showSettleUpPicker = true },
            onInviteFriend = { showInviteFriend = true }
        )
    }

    if (showAddExpensePicker) {
        GroupSelectionDialog(
            title = "Select Group for New Expense",
            groups = allGroups,
            onDismiss = { showAddExpensePicker = false },
            onSelectGroup = {
                showAddExpensePicker = false
                rootNavController.navigate("add_expense/$it")
            }
        )
    }

    if (showSettleUpPicker) {
        GroupSelectionDialog(
            title = "Select Group to Settle Up",
            groups = allGroups.filter { it.memberIds.size > 1 },
            onDismiss = { showSettleUpPicker = false },
            onSelectGroup = {
                showSettleUpPicker = false
                rootNavController.navigate("settle_up/$it")
            }
        )
    }

    if (showCreateGroup) {
        CreateGroupDialog(
            defaultCurrency = currentUserState?.defaultCurrency?.ifBlank { null } ?: "INR",
            onDismiss = { showCreateGroup = false },
            onCreate = { name, currency, avatarKey ->
                showCreateGroup = false
                busy.show("Creating group…")
                mainViewModel.createGroup(name, currency, avatarKey) { busy.hide(); snackbar.showMessage("Created $name") }
            }
        )
    }

    if (showQrSheet) {
        com.opensplit.ui.components.FriendQrSheet(
            myLinkUrl = myFriendLink,
            onDismiss = { showQrSheet = false },
            onOpenLink = { kind, token -> rootNavController.navigate(if (kind == "join") "join/$token" else "friend/$token") }
        )
    }

    if (showInviteFriend) {
        InviteMemberDialog(
            title = "Invite a friend",
            description = "Pick them from your contacts, or invite over WhatsApp or SMS.",
            onDismiss = { showInviteFriend = false },
            onSubmitEmail = { email ->
                mainViewModel.sendFriendInvite(email) { success ->
                    snackbar.showMessage(if (success) "Invite sent" else "Couldn't send (already invited or invalid email)")
                }
            }
        )
    }
}

/** Side-by-side list and detail on medium/expanded widths; the list alone on phones. */
@Composable
private fun ListDetail(
    wide: Boolean,
    detailKey: String?,
    list: @Composable () -> Unit,
    detail: @Composable (String) -> Unit,
    placeholder: String
) {
    if (!wide) { list(); return }
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.42f).fillMaxSize()) { list() }
        androidx.compose.material3.VerticalDivider()
        Box(modifier = Modifier.weight(0.58f).fillMaxSize()) {
            if (detailKey != null) detail(detailKey) else {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}
