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

import com.example.ui.components.appHazeHeader
import com.example.ui.components.appHazeSource
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
        Icons.Rounded.Home,
        Icons.Rounded.Group,
        Icons.Rounded.People,
        Icons.Rounded.Analytics
    )
    val tabLabels = listOf("Home", "Groups", "Friends", "Analytics")
    val hazeState = remember { HazeState() }

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
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Activity Feed"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Avatar button
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { rootNavController.navigate("account") },
                        contentAlignment = Alignment.Center
                    ) {
                        val photoUrl = currentUserState?.photoUrl
                        if (!photoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Account Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val displayName = currentUserState?.displayName ?: "User"
                            val initial = displayName.take(1).uppercase()
                            val hue = abs(displayName.hashCode() % 360).toFloat()
                            val avatarColor = Color.hsv(hue, 0.6f, 0.75f)

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabLabels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tabIcons[index], contentDescription = label) },
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
}

