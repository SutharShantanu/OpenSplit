package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppSearchBar
import com.example.ui.components.CreateGroupDialog
import com.example.ui.components.StateLayout
import com.example.ui.components.WalletIllustration
import com.example.ui.theme.OpenSplitIcons
import com.example.ui.theme.OpenSplitTokens
import com.example.ui.viewmodel.MainViewModel

enum class GroupSortOption(val label: String) {
    RECENT("Recent"),
    ALPHABETICAL("Alphabetical"),
    MEMBERS("Members")
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
    val listState = rememberLazyListState()

    val isExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 10 }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                expanded = isExpanded,
                onClick = { showCreateGroupDialog = true },
                icon = { Icon(OpenSplitIcons.AddExpense, contentDescription = null) },
                text = { Text("New Group") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
                            .padding(OpenSplitTokens.SpaceXL),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            WalletIllustration(size = 140.dp)
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXL))
                            Text(
                                text = "No Groups Yet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                            Text(
                                text = "Create your first group to start splitting expenses with friends.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXL))
                            Button(onClick = { showCreateGroupDialog = true }) {
                                Icon(OpenSplitIcons.AddExpense, contentDescription = null)
                                Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                                Text("New Group")
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceMD)
                    ) {
                        // Search bar
                        AppSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholderText = "Search groups..."
                        )

                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                        // Sort Control AssistChips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sort:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            GroupSortOption.values().forEach { option ->
                                AssistChip(
                                    onClick = { selectedSort = option },
                                    label = { Text(option.label) },
                                    colors = if (selectedSort == option) {
                                        AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    } else {
                                        AssistChipDefaults.assistChipColors()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))

                        // Filtered and sorted groups
                        val filteredGroups = remember(groups, searchQuery, selectedSort) {
                            var list = groups.filter { it.name.contains(searchQuery, ignoreCase = true) }
                            when (selectedSort) {
                                GroupSortOption.RECENT -> list
                                GroupSortOption.ALPHABETICAL -> list.sortedBy { it.name.lowercase() }
                                GroupSortOption.MEMBERS -> list.sortedByDescending { it.memberIds.size }
                            }
                        }

                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredGroups) { group ->
                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { onGroupClick(group.id) },
                                    headlineContent = {
                                        Text(
                                            text = group.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = "${group.memberIds.size} members • ${group.currency}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingContent = {
                                        Surface(
                                            shape = CircleShape,
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
                                    },
                                    trailingContent = {
                                        Icon(
                                            imageVector = OpenSplitIcons.ChevronRight,
                                            contentDescription = "View group",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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

