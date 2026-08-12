package com.opensplit.ui.screens

import com.opensplit.ui.components.LocalSnackbarController

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
import androidx.compose.ui.draw.rotate
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
    onGroupClick: (String) -> Unit,
    onAddExpense: (String) -> Unit,
    onSettleUp: (String) -> Unit
) {
    val groupsState by viewModel.userGroups.collectAsState()
    val pinnedGroupIds by viewModel.pinnedGroupIds.collectAsState()
    val groupLastActivity by viewModel.groupLastActivity.collectAsState()
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedCurrencyFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSort by rememberSaveable { mutableStateOf(GroupSortOption.RECENT) }
    val listState = rememberLazyListState()
    val snackbar = LocalSnackbarController.current
    val currentUid by viewModel.currentUid.collectAsState()
    var groupPendingDelete by remember { mutableStateOf<Group?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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

                        // Labeled FilterChips for Sort and Currency Filter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedSort != GroupSortOption.RECENT,
                                onClick = { showSortSheet = true },
                                label = { Text("Sort: ${selectedSort.label}", fontWeight = FontWeight.Medium, maxLines = 1) },
                                leadingIcon = { Icon(OpenSplitIcons.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                trailingIcon = { Icon(OpenSplitIcons.Dropdown, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )

                            FilterChip(
                                selected = selectedCurrencyFilter != null,
                                onClick = { showFilterSheet = true },
                                label = { Text(if (selectedCurrencyFilter != null) "Currency: $selectedCurrencyFilter" else "Currency", fontWeight = FontWeight.Medium, maxLines = 1) },
                                leadingIcon = { Icon(OpenSplitIcons.Filter, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                trailingIcon = { Icon(OpenSplitIcons.Dropdown, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
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
                        val (pinned, unpinned) = remember(groups, searchQuery, selectedSort, pinnedGroupIds, groupLastActivity, selectedCurrencyFilter) {
                            val filtered = groups
                                .filter { it.name.contains(searchQuery, ignoreCase = true) }
                                .filter { selectedCurrencyFilter == null || it.currency == selectedCurrencyFilter }
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
                                        onTogglePin = {
                                            viewModel.togglePinnedGroup(group.id)
                                            val nowPinned = !pinnedGroupIds.contains(group.id)
                                            snackbar.showUndo(if (nowPinned) "Pinned ${group.name}" else "Unpinned ${group.name}") {
                                                viewModel.togglePinnedGroup(group.id)
                                            }
                                        },
                                        onAddExpense = { onAddExpense(group.id) },
                                        onSettleUp = { onSettleUp(group.id) },
                                        isOwner = group.createdBy == currentUid,
                                        onLeave = {
                                            viewModel.leaveGroup(group)
                                            snackbar.showUndo("Left ${group.name}") {
                                                viewModel.rejoinGroup(group)
                                            }
                                        },
                                        onDelete = { groupPendingDelete = group }
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
                                    onTogglePin = {
                                            viewModel.togglePinnedGroup(group.id)
                                            val nowPinned = !pinnedGroupIds.contains(group.id)
                                            snackbar.showUndo(if (nowPinned) "Pinned ${group.name}" else "Unpinned ${group.name}") {
                                                viewModel.togglePinnedGroup(group.id)
                                            }
                                        },
                                    onAddExpense = { onAddExpense(group.id) },
                                    onSettleUp = { onSettleUp(group.id) },
                                    isOwner = group.createdBy == currentUid,
                                    onLeave = {
                                        viewModel.leaveGroup(group)
                                        snackbar.showUndo("Left ${group.name}") {
                                            viewModel.rejoinGroup(group)
                                        }
                                    },
                                    onDelete = { groupPendingDelete = group }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Deleting a group is destructive and affects every member, so it is confirmed
    // explicitly rather than offered as an undoable snackbar.
    groupPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { groupPendingDelete = null },
            icon = { Icon(OpenSplitIcons.DeleteExpense, contentDescription = null) },
            title = { Text("Delete ${target.name}?") },
            text = {
                Text("This removes the group and its expenses for everyone in it. You'll have a short window to undo.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup(target.id)
                        groupPendingDelete = null
                        snackbar.showUndo("Deleted ${target.name}") {
                            viewModel.restoreGroup(target)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { groupPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, currency, avatarKey ->
                viewModel.createGroup(name, currency, avatarKey)
                snackbar.showMessage("Created $name")
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

    if (showFilterSheet) {
        val currencies = remember(groupsState) {
            ((groupsState as? com.opensplit.ui.viewmodel.ScreenState.Success)?.data ?: emptyList())
                .map { it.currency }.distinct().sorted()
        }
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceMD)) {
                Text(
                    text = "Filter by currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            selectedCurrencyFilter = null
                            showFilterSheet = false
                        },
                    headlineContent = { Text("All currencies") },
                    leadingContent = {
                        RadioButton(selected = selectedCurrencyFilter == null, onClick = {
                            selectedCurrencyFilter = null
                            showFilterSheet = false
                        })
                    }
                )
                currencies.forEach { currency ->
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                selectedCurrencyFilter = currency
                                showFilterSheet = false
                            },
                        headlineContent = { Text(currency) },
                        leadingContent = {
                            RadioButton(selected = selectedCurrencyFilter == currency, onClick = {
                                selectedCurrencyFilter = currency
                                showFilterSheet = false
                            })
                        }
                    )
                }
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
            }
        }
    }
}

@Composable
private fun GroupRow(
    group: Group,
    isPinned: Boolean,
    isOwner: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onAddExpense: () -> Unit,
    onSettleUp: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

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
            com.opensplit.ui.components.GroupAvatar(name = group.name, avatarKey = group.avatarKey)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = if (isPinned) OpenSplitIcons.PinFilled else OpenSplitIcons.PinOutline,
                        contentDescription = if (isPinned) "Unpin group" else "Pin group",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(if (isPinned) 45f else 0f)
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = OpenSplitIcons.More,
                            contentDescription = "Quick actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = MaterialTheme.shapes.large
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Expense") },
                            leadingIcon = { Icon(OpenSplitIcons.AddExpense, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onAddExpense()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settle Up") },
                            leadingIcon = { Icon(OpenSplitIcons.Settle, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onSettleUp()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("View Group") },
                            leadingIcon = { Icon(OpenSplitIcons.Groups, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("Leave group") },
                            leadingIcon = { Icon(OpenSplitIcons.Leave, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onLeave()
                            }
                        )
                        // Deleting removes the group for everyone, so it's owner-only.
                        if (isOwner) {
                            DropdownMenuItem(
                                text = {
                                    Text("Delete group", color = MaterialTheme.colorScheme.error)
                                },
                                leadingIcon = {
                                    Icon(
                                        OpenSplitIcons.DeleteExpense,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}
