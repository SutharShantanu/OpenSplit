package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.di.AppContainer
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenState
import com.example.ui.viewmodel.ViewModelFactory
import kotlin.math.abs
import kotlinx.coroutines.launch

import com.example.ui.components.appHazeHeader
import com.example.ui.components.appHazeSource
import com.example.ui.theme.OpenSplitIcons
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    appContainer: AppContainer,
    rootNavController: NavHostController
) {
    var selectedTab by remember { mutableStateOf(0) }
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
    val currentUserState by appContainer.userRepository.getUserFlow(currentUid).collectAsState(initial = null)
    val userGroupsState by mainViewModel.userGroups.collectAsState()

    val groupIds = (userGroupsState as? ScreenState.Success)?.data?.map { it.id } ?: emptyList()
    val activitiesState by appContainer.activityRepository.getActivityForUser(currentUid, groupIds).collectAsState(initial = emptyList())

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
                                    Badge {
                                        Text(text = if (unreadCount > 99) "99+" else "$unreadCount")
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

                    Spacer(modifier = Modifier.width(4.dp))

                    // Avatar button with dropdown menu
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { menuExpanded = true }
                        ) {
                            com.example.ui.components.UserAvatar(
                                photoUrl = currentUserState?.photoUrl,
                                displayName = currentUserState?.displayName,
                                size = 36.dp
                            )
                        }

                        com.example.ui.components.AccountDropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            onNavigateToAccount = { rootNavController.navigate("account") },
                            onSignOut = {
                                coroutineScope.launch {
                                    appContainer.authRepository.signOut()
                                    rootNavController.navigate("login") {
                                        popUpTo(0)
                                    }
                                }
                            }
                        )
                    }
                }
            )
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
            when (selectedTab) {
                0 -> HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToGroupsTab = { selectedTab = 1 },
                    onNavigateToGroupDetail = { groupId -> rootNavController.navigate("group_detail/$groupId") },
                    onNavigateToActivity = { rootNavController.navigate("activity") },
                    onNavigateToAddExpense = { groupId -> rootNavController.navigate("add_expense/$groupId") },
                    onNavigateToSettleUp = { groupId -> rootNavController.navigate("settle_up/$groupId") }
                )
                1 -> GroupsScreen(
                    viewModel = mainViewModel,
                    onGroupClick = { groupId -> rootNavController.navigate("group_detail/$groupId") }
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
    }

    if (showGlobalSearchSheet) {
        com.example.ui.components.GlobalSearchSheet(
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
            }
        )
    }
}

