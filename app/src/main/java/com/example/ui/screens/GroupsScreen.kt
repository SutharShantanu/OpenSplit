package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.CreateGroupDialog
import com.example.ui.components.StateLayout
import com.example.ui.components.WalletIllustration
import com.example.ui.components.getBalanceColor
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.abs

enum class GroupSortOption(val label: String) {
    RECENT("Recent"),
    ALPHABETICAL("A-Z"),
    BALANCE("Balance")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: MainViewModel,
    onGroupClick: (String) -> Unit
) {
    val groupsState by viewModel.userGroups.collectAsState()
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(GroupSortOption.RECENT) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateGroupDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New Group")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StateLayout(state = groupsState) { groups ->
                if (groups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            WalletIllustration(size = 130.dp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "No Groups Yet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create your first group to start splitting expenses with friends.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { showCreateGroupDialog = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("New Group")
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search groups...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sort Control Segmented Buttons / Filter Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sort by:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            GroupSortOption.values().forEach { option ->
                                FilterChip(
                                    selected = selectedSort == option,
                                    onClick = { selectedSort = option },
                                    label = { Text(option.label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Filtered and sorted groups
                        val filteredGroups = remember(groups, searchQuery, selectedSort) {
                            var list = groups.filter { it.name.contains(searchQuery, ignoreCase = true) }
                            when (selectedSort) {
                                GroupSortOption.RECENT -> list // Assume server order is recent
                                GroupSortOption.ALPHABETICAL -> list.sortedBy { it.name.lowercase() }
                                GroupSortOption.BALANCE -> list // If balance is available in model
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredGroups) { group ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onGroupClick(group.id) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = group.name.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = group.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${group.memberIds.size} members • ${group.currency}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Chevron or indication
                                        Icon(
                                            imageVector = Icons.Rounded.Group,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
