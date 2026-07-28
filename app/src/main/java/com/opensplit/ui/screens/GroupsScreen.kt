package com.opensplit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.domain.model.Group
import com.opensplit.ui.components.AppSearchBar
import com.opensplit.ui.components.CreateGroupDialog
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.components.WalletIllustration
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.MainViewModel

enum class GroupSortOption(val label: String) {
    RECENT("Recent activity"),
    ALPHABETICAL("Alphabetical"),
    MEMBERS("Most members")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: MainViewModel,
    onGroupClick: (String) -> Unit
) {
    val groupsState by viewModel.userGroups.collectAsState()
    val pinnedGroupIds by viewModel.pinnedGroupIds.collectAsState()
    val groupLastActivity by viewModel.groupLastActivity.collectAsState()
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSort by rememberSaveable { mutableStateOf(GroupSortOption.RECENT) }
    val listState = rememberLazyListState()

    val isExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 10 }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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

                        // Dedicated sort & filter entry point (opens a bottom sheet instead of inline chips)
                        OutlinedButton(onClick = { showSortSheet = true }) {
                            Icon(OpenSplitIcons.SortFilter, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                            Text("Sort & filter: ${selectedSort.label}")
                        }

                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))

                        fun sortWithin(list: List<Group>): List<Group> = when (selectedSort) {
                            GroupSortOption.RECENT -> list.sortedByDescending {
                                groupLastActivity[it.id]?.seconds ?: it.createdAt.seconds
                            }
                            GroupSortOption.ALPHABETICAL -> list.sortedBy { it.name.lowercase() }
                            GroupSortOption.MEMBERS -> list.sortedByDescending { it.memberIds.size }
                        }

                        // Pinned groups always float to the top, regardless of the chosen sort.
                        val (pinned, unpinned) = remember(groups, searchQuery, selectedSort, pinnedGroupIds, groupLastActivity) {
                            val filtered = groups.filter { it.name.contains(searchQuery, ignoreCase = true) }
                            val pinnedList = sortWithin(filtered.filter { pinnedGroupIds.contains(it.id) })
                            val unpinnedList = sortWithin(filtered.filterNot { pinnedGroupIds.contains(it.id) })
                            pinnedList to unpinnedList
                        }

                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (pinned.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Pinned",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS)
                                    )
                                }
                                items(pinned, key = { "pinned_${it.id}" }) { group ->
                                    GroupRow(
                                        group = group,
                                        isPinned = true,
                                        onClick = { onGroupClick(group.id) },
                                        onTogglePin = { viewModel.togglePinnedGroup(group.id) }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                }
                                if (unpinned.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "All groups",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS)
                                        )
                                    }
                                }
                            }
                            items(unpinned, key = { it.id }) { group ->
                                GroupRow(
                                    group = group,
                                    isPinned = false,
                                    onClick = { onGroupClick(group.id) },
                                    onTogglePin = { viewModel.togglePinnedGroup(group.id) }
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

    if (showSortSheet) {
        ModalBottomSheet(onDismissRequest = { showSortSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceMD)) {
                Text(
                    text = "Sort groups by",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                GroupSortOption.values().forEach { option ->
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                selectedSort = option
                                showSortSheet = false
                            },
                        headlineContent = { Text(option.label) },
                        leadingContent = {
                            RadioButton(selected = selectedSort == option, onClick = {
                                selectedSort = option
                                showSortSheet = false
                            })
                        }
                    )
                }
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                Text(
                    text = "Pinned groups always stay at the top.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
            }
        }
    }
}

@Composable
private fun GroupRow(
    group: Group,
    isPinned: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = if (isPinned) OpenSplitIcons.PinFilled else OpenSplitIcons.PinOutline,
                        contentDescription = if (isPinned) "Unpin group" else "Pin group",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = OpenSplitIcons.ChevronRight,
                    contentDescription = "View group",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
