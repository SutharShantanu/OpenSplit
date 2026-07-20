package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.navigation.compose.rememberNavController
import com.example.di.AppContainer
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Groups : Screen("groups", "Groups", Icons.Filled.List)
    object Friends : Screen("friends", "Friends", Icons.Filled.Person)
    object Activity : Screen("activity", "Activity", Icons.Filled.Notifications)
    object Account : Screen("account", "Account", Icons.Filled.AccountCircle)
}

@Composable
fun MainDashboard(
    appContainer: AppContainer,
    rootNavController: NavHostController
) {
    val bottomNavController = rememberNavController()
    val mainViewModel: com.example.ui.viewmodel.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.example.ui.viewmodel.ViewModelFactory(appContainer)
    )
    val items = listOf(
        Screen.Groups,
        Screen.Friends,
        Screen.Activity,
        Screen.Account
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            bottomNavController.navigate(screen.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.Groups.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Groups.route) {
                GroupsScreen(viewModel = mainViewModel, onGroupClick = { groupId ->
                    rootNavController.navigate("group_detail/$groupId")
                })
            }
            composable(Screen.Friends.route) {
                FriendsScreen(viewModel = mainViewModel)
            }
            composable(Screen.Activity.route) {
                ActivityScreen(viewModel = mainViewModel)
            }
            composable(Screen.Account.route) {
                AccountScreen(appContainer = appContainer, rootNavController = rootNavController)
            }
        }
    }
}

@Composable
fun GroupsScreen(viewModel: com.example.ui.viewmodel.MainViewModel, onGroupClick: (String) -> Unit) {
    val groups by viewModel.userGroups.collectAsState()
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateGroupDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create Group")
            }
        }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No groups yet. Create one to get started!", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onGroupClick(group.id) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(group.name, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Currency: ${group.currency}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, currency ->
                viewModel.createGroup(name, currency)
                showCreateGroupDialog = false
            }
        )
    }
}

@Composable
fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("Currency") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, currency) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FriendsScreen(viewModel: com.example.ui.viewmodel.MainViewModel) {
    val friends by viewModel.friends.collectAsState()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "Friends",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (friends.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No friends yet. Add members to groups to see them here.", style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(16.dp))
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(friends) { friend ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(friend.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text(friend.email, style = MaterialTheme.typography.bodyMedium)
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
fun ActivityScreen(viewModel: com.example.ui.viewmodel.MainViewModel) {
    val expenses by viewModel.recentExpenses.collectAsState()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No recent expenses.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenses) { exp ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(exp.description, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Amount: ${exp.currency} ${exp.amount}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountScreen(appContainer: AppContainer, rootNavController: NavHostController) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text("Account Screen", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { 
            coroutineScope.launch {
                appContainer.authRepository.signOut()
            }
        }) {
            Text("Sign Out")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { 
            coroutineScope.launch {
                appContainer.authRepository.deleteAccount()
            }
        }) {
            Text("Delete Account", color = MaterialTheme.colorScheme.error)
        }
    }
}
