@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.LocalSnackbarController

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.opensplit.ui.components.SwipeAction
import com.opensplit.ui.components.SwipeActionRow
import com.opensplit.ui.components.WalletIllustration
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.MainViewModel
import com.opensplit.ui.viewmodel.HomeViewModel
import com.opensplit.ui.viewmodel.ScreenState
import com.opensplit.ui.components.HeroBalanceCard
import com.opensplit.util.CurrencyFormatter

enum class GroupSortOption(val label: String) {
    RECENT("Recent activity"),
    ALPHABETICAL("Alphabetical"),
    MEMBERS("Most members")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: MainViewModel,
    homeViewModel: HomeViewModel,
    onGroupClick: (String) -> Unit,
    onAddExpense: (String) -> Unit,
    onSettleUp: (String) -> Unit,
    onPersonClick: (String) -> Unit
) {
    val groupsState by viewModel.userGroups.collectAsState()
    val homeState by homeViewModel.uiState.collectAsState()
    val balanceByGroup = remember(homeState) {
        (homeState as? ScreenState.Success)?.data?.recentGroups?.associate { it.group.id to it.balance } ?: emptyMap()
    }
    var breakdownType by remember { mutableStateOf<String?>(null) }
    val pinnedGroupIds by viewModel.pinnedGroupIds.collectAsState()
    val archivedGroupIds by viewModel.archivedGroupIds.collectAsState()
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val groupLastActivity by viewModel.groupLastActivity.collectAsState()
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedCurrencyFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSort by rememberSaveable { mutableStateOf(GroupSortOption.RECENT) }
    val listState = rememberLazyListState()
    val snackbar = LocalSnackbarController.current
    val busy = com.opensplit.ui.components.LocalBusy.current
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
                if (groups.none { !it.isDirect }) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()) // lets pull-to-refresh catch the gesture
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
                            Button(shapes = ButtonDefaults.shapes(), onClick = { showCreateGroupDialog = true }) {
                                SymbolIcon(OpenSplitIcons.AddExpense, contentDescription = null)
                                Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                                Text("New Group")
                            }
                        }
                    }
                } else {
                    fun sortWithin(list: List<Group>): List<Group> = when (selectedSort) {
                        GroupSortOption.RECENT -> list.sortedByDescending {
                            groupLastActivity[it.id]?.seconds ?: it.createdAt.seconds
                        }
                        GroupSortOption.ALPHABETICAL -> list.sortedBy { it.name.lowercase() }
                        GroupSortOption.MEMBERS -> list.sortedByDescending { it.memberIds.size }
                    }

                    // Pinned groups always float to the top, regardless of the chosen sort.
                    val archivedCount = groups.count { it.id in archivedGroupIds && !it.isDirect }
                    // Nothing left to show under the archived filter: drop back to the live list.
                    androidx.compose.runtime.LaunchedEffect(archivedCount) { if (archivedCount == 0) showArchived = false }
                    val (pinned, unpinned) = remember(groups, searchQuery, selectedSort, pinnedGroupIds, groupLastActivity, selectedCurrencyFilter, archivedGroupIds, showArchived) {
                        val filtered = groups
                            .filterNot { it.isDirect }
                            .filter { it.name.contains(searchQuery, ignoreCase = true) }
                            .filter { selectedCurrencyFilter == null || it.currency == selectedCurrencyFilter }
                            .filter { (it.id in archivedGroupIds) == showArchived }
                        val pinnedList = sortWithin(filtered.filter { pinnedGroupIds.contains(it.id) })
                        val unpinnedList = sortWithin(filtered.filterNot { pinnedGroupIds.contains(it.id) })
                        pinnedList to unpinnedList
                    }

                    val rowFor: @Composable (Group, Boolean) -> Unit = { group, isPinned ->
                        val togglePin = {
                            viewModel.togglePinnedGroup(group.id)
                            val nowPinned = !pinnedGroupIds.contains(group.id)
                            snackbar.showUndo(if (nowPinned) "Pinned ${group.name}" else "Unpinned ${group.name}") {
                                viewModel.togglePinnedGroup(group.id)
                            }
                        }
                        val isArchived = group.id in archivedGroupIds
                        val toggleArchive = {
                            val nowArchived = !isArchived
                            viewModel.setGroupArchived(group.id, nowArchived)
                            snackbar.showUndo(if (nowArchived) "Archived ${group.name}" else "Unarchived ${group.name}") {
                                viewModel.setGroupArchived(group.id, !nowArchived)
                            }
                        }
                        SwipeActionRow(
                            startAction = SwipeAction(
                                icon = if (isPinned) OpenSplitIcons.PinOutline else OpenSplitIcons.PinFilled,
                                label = if (isPinned) "Unpin" else "Pin",
                                container = MaterialTheme.colorScheme.primaryContainer,
                                onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                                onAction = togglePin
                            ),
                            endAction = SwipeAction(
                                icon = if (isArchived) OpenSplitIcons.Unarchive else OpenSplitIcons.Archive,
                                label = if (isArchived) "Unarchive" else "Archive",
                                container = MaterialTheme.colorScheme.tertiaryContainer,
                                onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                                onAction = toggleArchive
                            )
                        ) {
                        GroupRow(
                            group = group,
                            balance = balanceByGroup[group.id],
                            isPinned = isPinned,
                            onClick = { onGroupClick(group.id) },
                            onTogglePin = togglePin,
                            onAddExpense = { onAddExpense(group.id) },
                            onSettleUp = { onSettleUp(group.id) },
                            isOwner = group.createdBy == currentUid,
                            onLeave = {
                                val bal = balanceByGroup[group.id] ?: 0.0
                                if (kotlin.math.abs(bal) > 0.01) {
                                    snackbar.showMessage("Settle up first: you still have a balance in ${group.name}.")
                                } else {
                                    viewModel.leaveGroup(group)
                                    snackbar.showUndo("Left ${group.name}") {
                                        viewModel.rejoinGroup(group)
                                    }
                                }
                            },
                            onDelete = { groupPendingDelete = group },
                            isArchived = isArchived,
                            onToggleArchive = toggleArchive
                        )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    }

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS),
                        contentPadding = PaddingValues(
                            start = OpenSplitTokens.SpaceLG, end = OpenSplitTokens.SpaceLG,
                            top = OpenSplitTokens.SpaceMD, bottom = 96.dp
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Net balance hero. Per-currency; the largest outstanding currency leads.
                        item(key = "hero") {
                            val home = (homeState as? ScreenState.Success)?.data
                            if (home != null) {
                                val nonZeroNet = home.netByCurrency.filterValues { kotlin.math.abs(it) > 0.001 }
                                val primaryCurrency = nonZeroNet.maxByOrNull { kotlin.math.abs(it.value) }?.key
                                    ?: home.nudgeCurrency
                                HeroBalanceCard(
                                    amount = home.netByCurrency[primaryCurrency] ?: 0.0,
                                    currency = primaryCurrency,
                                    youAreOwed = home.youAreOwedByCurrency[primaryCurrency] ?: 0.0,
                                    youOwe = home.youOweByCurrency[primaryCurrency] ?: 0.0,
                                    onOwedToYouClick = { breakdownType = "OWED_TO_YOU" },
                                    onYouOweClick = { breakdownType = "YOU_OWE" },
                                    modifier = Modifier.padding(bottom = OpenSplitTokens.SpaceMD)
                                )
                            }
                        }

                        item(key = "search") {
                            AppSearchBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                placeholderText = "Search groups..."
                            )
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                        }

                        item(key = "chips") {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedSort != GroupSortOption.RECENT,
                                        onClick = { showSortSheet = true },
                                        label = { Text("Sort: ${selectedSort.label}", fontWeight = FontWeight.Medium, maxLines = 1) },
                                        leadingIcon = { SymbolIcon(OpenSplitIcons.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        trailingIcon = { SymbolIcon(OpenSplitIcons.Dropdown, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                                if (archivedCount > 0 || showArchived) item {
                                    FilterChip(
                                        selected = showArchived,
                                        onClick = { showArchived = !showArchived },
                                        label = { Text(if (showArchived) "Archived ($archivedCount)" else "Archived ($archivedCount)", fontWeight = FontWeight.Medium, maxLines = 1) },
                                        leadingIcon = { SymbolIcon(OpenSplitIcons.Archive, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedCurrencyFilter != null,
                                        onClick = { showFilterSheet = true },
                                        label = { Text(if (selectedCurrencyFilter != null) "Currency: $selectedCurrencyFilter" else "Currency", fontWeight = FontWeight.Medium, maxLines = 1) },
                                        leadingIcon = { SymbolIcon(OpenSplitIcons.Filter, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        trailingIcon = { SymbolIcon(OpenSplitIcons.Dropdown, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                        }

                        if (pinned.isNotEmpty()) {
                            item(key = "pinned_header") {
                                Text(
                                    text = "Pinned",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS)
                                )
                            }
                            items(pinned, key = { "pinned_${it.id}" }) { group -> rowFor(group, true) }
                            if (unpinned.isNotEmpty()) {
                                item(key = "all_header") {
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
                        items(unpinned, key = { it.id }) { group -> rowFor(group, false) }
                    }
                }
            }
        }
    }

    breakdownType?.let { type ->
        val home = (homeState as? ScreenState.Success)?.data
        if (home != null) {
            BalanceBreakdownSheet(
                breakdownType = type,
                homeState = home,
                mainViewModel = viewModel,
                onDismiss = { breakdownType = null },
                onNavigateToGroup = { breakdownType = null; onGroupClick(it) },
                onNavigateToSettleUp = { breakdownType = null; onSettleUp(it) },
                onNavigateToPersonBalance = { breakdownType = null; onPersonClick(it) }
            )
        }
    }

    // Deleting a group is destructive and affects every member, so it is confirmed
    // explicitly rather than offered as an undoable snackbar.
    groupPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { groupPendingDelete = null },
            icon = { SymbolIcon(OpenSplitIcons.DeleteExpense, contentDescription = null) },
            title = { Text("Delete ${target.name}?") },
            text = {
                Text("This removes the group and its expenses for everyone in it. You'll have a short window to undo.")
            },
            confirmButton = {
                Button(shapes = ButtonDefaults.shapes(), 
                    onClick = {
                        groupPendingDelete = null
                        busy.show("Deleting group…")
                        viewModel.deleteGroup(target.id) { busy.hide() }
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
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { groupPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            defaultCurrency = (homeState as? ScreenState.Success)?.data?.user?.defaultCurrency?.ifBlank { null } ?: "INR",
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, currency, avatarKey ->
                showCreateGroupDialog = false
                busy.show("Creating group…")
                viewModel.createGroup(name, currency, avatarKey) { busy.hide(); snackbar.showMessage("Created $name") }
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
    balance: Double?,
    isPinned: Boolean,
    isOwner: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onAddExpense: () -> Unit,
    onSettleUp: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    isArchived: Boolean = false,
    onToggleArchive: () -> Unit = {}
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
            Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)) {
                Text(
                    text = "${group.memberIds.size} members • ${group.currency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (balance != null) GroupBalanceChip(balance = balance, currency = group.currency)
            }
        },
        leadingContent = {
            com.opensplit.ui.components.GroupAvatar(name = group.name, avatarKey = group.avatarKey)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = onTogglePin) {
                    SymbolIcon(
                        symbol = if (isPinned) OpenSplitIcons.PinFilled else OpenSplitIcons.PinOutline,
                        contentDescription = if (isPinned) "Unpin group" else "Pin group",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(if (isPinned) 45f else 0f)
                    )
                }
                Box {
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = { menuExpanded = true }) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.More,
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
                            leadingIcon = { SymbolIcon(OpenSplitIcons.AddExpense, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onAddExpense()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settle Up") },
                            leadingIcon = { SymbolIcon(OpenSplitIcons.Settle, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onSettleUp()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("View Group") },
                            leadingIcon = { SymbolIcon(OpenSplitIcons.Groups, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text(if (isArchived) "Unarchive" else "Archive") },
                            leadingIcon = { SymbolIcon(if (isArchived) OpenSplitIcons.Unarchive else OpenSplitIcons.Archive, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onToggleArchive()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Leave group") },
                            leadingIcon = { SymbolIcon(OpenSplitIcons.Leave, contentDescription = null) },
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
                                    SymbolIcon(
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

/** "You owe ₹1,250" / "You are owed ₹1,200" / "Settled" as a tonal pill. */
@Composable
private fun GroupBalanceChip(balance: Double, currency: String) {
    val amount = CurrencyFormatter.format(kotlin.math.abs(balance), currencyCode = currency, showSymbol = true)
    val (label, bg, fg) = when {
        balance < -0.01 -> Triple("You owe $amount", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        balance > 0.01 -> Triple("You are owed $amount", OpenSplitTokens.OwedPositive.copy(alpha = 0.15f), OpenSplitTokens.OwedPositive)
        else -> Triple("Settled", MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(shape = OpenSplitTokens.PillShape, color = bg) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}
