@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.opensplit.ui.screens

import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.LocalSnackbarController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import com.opensplit.domain.model.Group
import com.opensplit.ui.components.AppSearchBar
import com.opensplit.ui.components.CreateGroupDialog
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.components.responsiveContentWidth
import com.opensplit.ui.components.responsiveDialogWidth
import com.opensplit.ui.components.SwipeAction
import com.opensplit.ui.components.SwipeActionRow
import com.opensplit.ui.components.WalletIllustration
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.theme.Symbol
import com.opensplit.ui.viewmodel.MainViewModel
import com.opensplit.ui.viewmodel.HomeViewModel
import com.opensplit.ui.viewmodel.ScreenState
import com.opensplit.ui.components.HeroBalanceCard
import com.opensplit.util.CurrencyFormatter

enum class GroupSortOption(
    val label: String,
    val subtitle: String,
    val icon: Symbol
) {
    RECENT("Recently active", "Sorted by newest activity", OpenSplitIcons.Schedule),
    ALPHABETICAL("Name (A to Z)", "Alphabetical order", OpenSplitIcons.Sort),
    ALPHABETICAL_DESC("Name (Z to A)", "Reverse alphabetical order", OpenSplitIcons.Sort),
    BALANCE("Highest balance", "Groups with largest balances first", OpenSplitIcons.Wallet),
    MEMBERS("Most members", "Groups with most participants", OpenSplitIcons.People)
}

enum class GroupStatusFilter(val label: String) {
    ALL("All"),
    OUTSTANDING("Outstanding"),
    SETTLED("Settled")
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
    val groupInfoById = remember(homeState) {
        (homeState as? ScreenState.Success)?.data?.recentGroups?.associateBy { it.group.id } ?: emptyMap()
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
    var statusFilter by rememberSaveable { mutableStateOf(GroupStatusFilter.ALL) }
    val listState = rememberLazyListState()
    val snackbar = LocalSnackbarController.current
    val busy = com.opensplit.ui.components.LocalBusy.current
    val currentUid by viewModel.currentUid.collectAsState()
    var groupPendingDelete by remember { mutableStateOf<Group?>(null) }
    var optionsGroupId by remember { mutableStateOf<String?>(null) }

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
                            groupLastActivity[it.id]?.seconds ?: groupInfoById[it.id]?.lastActivity?.seconds ?: it.createdAt.seconds
                        }
                        GroupSortOption.ALPHABETICAL -> list.sortedBy { it.name.lowercase() }
                        GroupSortOption.ALPHABETICAL_DESC -> list.sortedByDescending { it.name.lowercase() }
                        GroupSortOption.BALANCE -> list.sortedByDescending {
                            kotlin.math.abs(groupInfoById[it.id]?.balance ?: 0.0)
                        }
                        GroupSortOption.MEMBERS -> list.sortedByDescending { it.memberIds.size }
                    }

                    // Pinned groups always float to the top, regardless of the chosen sort.
                    val archivedCount = groups.count { it.id in archivedGroupIds && !it.isDirect }
                    // Nothing left to show under the archived filter: drop back to the live list.
                    androidx.compose.runtime.LaunchedEffect(archivedCount) { if (archivedCount == 0) showArchived = false }
                    val (pinned, unpinned) = remember(
                        groups,
                        searchQuery,
                        selectedSort,
                        statusFilter,
                        pinnedGroupIds,
                        groupLastActivity,
                        groupInfoById,
                        selectedCurrencyFilter,
                        archivedGroupIds,
                        showArchived
                    ) {
                        val filtered = groups
                            .filterNot { it.isDirect }
                            .filter { it.name.contains(searchQuery, ignoreCase = true) }
                            .filter { selectedCurrencyFilter == null || it.currency == selectedCurrencyFilter }
                            .filter { (it.id in archivedGroupIds) == showArchived }
                            .filter { group ->
                                when (statusFilter) {
                                    GroupStatusFilter.ALL -> true
                                    GroupStatusFilter.OUTSTANDING -> {
                                        val bal = groupInfoById[group.id]?.balance ?: 0.0
                                        kotlin.math.abs(bal) > 0.01
                                    }
                                    GroupStatusFilter.SETTLED -> {
                                        val info = groupInfoById[group.id]
                                        val bal = info?.balance ?: 0.0
                                        kotlin.math.abs(bal) <= 0.01 && (info?.hasTransactions == true)
                                    }
                                }
                            }
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
                                icon = OpenSplitIcons.More,
                                label = "More options",
                                container = MaterialTheme.colorScheme.primaryContainer,
                                onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                                onAction = { optionsGroupId = group.id }
                            ),
                            endAction = SwipeAction(
                                icon = OpenSplitIcons.More,
                                label = "More options",
                                container = MaterialTheme.colorScheme.secondaryContainer,
                                onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                                onAction = { optionsGroupId = group.id }
                            )
                        ) {
                        val groupInfo = groupInfoById[group.id]
                        GroupRow(
                            group = group,
                            balance = groupInfo?.balance,
                            hasTransactions = groupInfo?.hasTransactions ?: false,
                            lastActivity = groupLastActivity[group.id] ?: groupInfo?.lastActivity ?: group.createdAt,
                            isPinned = isPinned,
                            showOptions = optionsGroupId == group.id,
                            onShowOptions = { optionsGroupId = group.id },
                            onDismissOptions = { if (optionsGroupId == group.id) optionsGroupId = null },
                            onClick = { onGroupClick(group.id) },
                            onTogglePin = togglePin,
                            onAddExpense = { onAddExpense(group.id) },
                            onSettleUp = { onSettleUp(group.id) },
                            isOwner = group.createdBy == currentUid,
                            onLeave = {
                                val bal = groupInfo?.balance ?: 0.0
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

                        val hasActiveFilters = searchQuery.isNotBlank() ||
                            selectedSort != GroupSortOption.RECENT ||
                            selectedCurrencyFilter != null ||
                            statusFilter != GroupStatusFilter.ALL ||
                            showArchived

                        val activeFilterCount = (if (searchQuery.isNotBlank()) 1 else 0) +
                            (if (selectedSort != GroupSortOption.RECENT) 1 else 0) +
                            (if (selectedCurrencyFilter != null) 1 else 0) +
                            (if (statusFilter != GroupStatusFilter.ALL) 1 else 0) +
                            (if (showArchived) 1 else 0)

                        item(key = "chips") {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasActiveFilters) {
                                    item(key = "clear_filters_chip") {
                                        FilterChip(
                                            selected = true,
                                            onClick = {
                                                searchQuery = ""
                                                selectedSort = GroupSortOption.RECENT
                                                selectedCurrencyFilter = null
                                                statusFilter = GroupStatusFilter.ALL
                                                showArchived = false
                                            },
                                            label = {
                                                Text(
                                                    text = "Reset ($activeFilterCount)",
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                            },
                                            leadingIcon = {
                                                SymbolIcon(
                                                    symbol = OpenSplitIcons.Close,
                                                    contentDescription = "Clear all filters",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        )
                                    }
                                }
                                item(key = "sort_chip") {
                                    val isCustomSort = selectedSort != GroupSortOption.RECENT
                                    FilterChip(
                                        selected = isCustomSort,
                                        onClick = { showSortSheet = true },
                                        label = {
                                            Text(
                                                text = if (isCustomSort) "Sort: ${selectedSort.label}" else "Sort",
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        },
                                        leadingIcon = {
                                            SymbolIcon(
                                                symbol = OpenSplitIcons.Sort,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            SymbolIcon(
                                                symbol = OpenSplitIcons.Dropdown,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                                item(key = "status_outstanding_chip") {
                                    val isSelected = statusFilter == GroupStatusFilter.OUTSTANDING
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            statusFilter = if (isSelected) GroupStatusFilter.ALL else GroupStatusFilter.OUTSTANDING
                                        },
                                        label = {
                                            Text("Outstanding", fontWeight = FontWeight.Medium, maxLines = 1)
                                        },
                                        leadingIcon = {
                                            SymbolIcon(
                                                symbol = if (isSelected) OpenSplitIcons.Check else OpenSplitIcons.Wallet,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                                item(key = "status_settled_chip") {
                                    val isSelected = statusFilter == GroupStatusFilter.SETTLED
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            statusFilter = if (isSelected) GroupStatusFilter.ALL else GroupStatusFilter.SETTLED
                                        },
                                        label = {
                                            Text("Settled", fontWeight = FontWeight.Medium, maxLines = 1)
                                        },
                                        leadingIcon = {
                                            SymbolIcon(
                                                symbol = if (isSelected) OpenSplitIcons.Check else OpenSplitIcons.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                                item(key = "currency_chip") {
                                    val isSelected = selectedCurrencyFilter != null
                                    val sym = selectedCurrencyFilter?.let { CurrencyFormatter.getCurrencySymbol(it) } ?: ""
                                    val currencyLabel = if (isSelected) {
                                        if (sym.isNotEmpty() && sym != selectedCurrencyFilter) "$sym $selectedCurrencyFilter" else selectedCurrencyFilter!!
                                    } else {
                                        "Currency"
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { showFilterSheet = true },
                                        label = {
                                            Text(
                                                text = if (isSelected) "Currency: $currencyLabel" else "Currency",
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        },
                                        leadingIcon = {
                                            SymbolIcon(
                                                symbol = OpenSplitIcons.Payments,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .clickable { selectedCurrencyFilter = null },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    SymbolIcon(
                                                        symbol = OpenSplitIcons.Close,
                                                        contentDescription = "Clear currency filter",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            } else {
                                                SymbolIcon(
                                                    symbol = OpenSplitIcons.Dropdown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                                if (archivedCount > 0 || showArchived) {
                                    item(key = "archived_chip") {
                                        FilterChip(
                                            selected = showArchived,
                                            onClick = { showArchived = !showArchived },
                                            label = {
                                                Text(
                                                    text = "Archived ($archivedCount)",
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1
                                                )
                                            },
                                            leadingIcon = {
                                                SymbolIcon(
                                                    symbol = if (showArchived) OpenSplitIcons.Check else OpenSplitIcons.Archive,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                        }

                        if (pinned.isNotEmpty()) {
                            item(key = "pinned_header") {
                                Text(
                                    text = "Pinned (${pinned.size})",
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
                                        text = "Other groups (${unpinned.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS)
                                    )
                                }
                            }
                        } else if (hasActiveFilters && unpinned.isNotEmpty()) {
                            item(key = "filtered_header") {
                                Text(
                                    text = "Matching groups (${unpinned.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS)
                                )
                            }
                        }
                        items(unpinned, key = { it.id }) { group -> rowFor(group, false) }

                        if (pinned.isEmpty() && unpinned.isEmpty()) {
                            item(key = "empty_filter_results") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = OpenSplitTokens.SpaceXL, horizontal = OpenSplitTokens.SpaceSM),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.large,
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(OpenSplitTokens.SpaceXL),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                modifier = Modifier.size(64.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    SymbolIcon(
                                                        symbol = OpenSplitIcons.Search,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                                            Text(
                                                text = "No matching groups",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                                            Text(
                                                text = if (searchQuery.isNotBlank()) {
                                                    "No groups match \"$searchQuery\" with the current filters."
                                                } else {
                                                    "No groups match the selected filters."
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
                                            FilledTonalButton(
                                                shapes = ButtonDefaults.shapes(),
                                                onClick = {
                                                    searchQuery = ""
                                                    selectedSort = GroupSortOption.RECENT
                                                    selectedCurrencyFilter = null
                                                    statusFilter = GroupStatusFilter.ALL
                                                    showArchived = false
                                                }
                                            ) {
                                                SymbolIcon(
                                                    symbol = OpenSplitIcons.Refresh,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                                                Text("Reset all filters")
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .responsiveDialogWidth(maxWidth = 560.dp)
                    .padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceSM)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sort groups",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedSort != GroupSortOption.RECENT) {
                        TextButton(
                            shapes = ButtonDefaults.shapes(),
                            onClick = {
                                selectedSort = GroupSortOption.RECENT
                                showSortSheet = false
                            }
                        ) {
                            Text("Reset to default")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                GroupSortOption.values().forEach { option ->
                    val isSelected = selectedSort == option
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                selectedSort = option
                                showSortSheet = false
                            }
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        SymbolIcon(
                                            symbol = option.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            headlineContent = {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = option.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedSort = option
                                        showSortSheet = false
                                    }
                                )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OpenSplitTokens.SpaceSM, vertical = OpenSplitTokens.SpaceXS)
                ) {
                    SymbolIcon(
                        symbol = OpenSplitIcons.PinFilled,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Pinned groups always stay at the top.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
            }
        }
    }

    if (showFilterSheet) {
        val liveGroups = remember(groupsState) {
            ((groupsState as? com.opensplit.ui.viewmodel.ScreenState.Success)?.data ?: emptyList())
                .filterNot { it.isDirect }
        }
        val currencies = remember(liveGroups) {
            liveGroups.map { it.currency }.distinct().sorted()
        }
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .responsiveDialogWidth(maxWidth = 560.dp)
                    .padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceSM)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter by currency",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedCurrencyFilter != null) {
                        TextButton(
                            shapes = ButtonDefaults.shapes(),
                            onClick = {
                                selectedCurrencyFilter = null
                                showFilterSheet = false
                            }
                        ) {
                            Text("Clear")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                val isAllSelected = selectedCurrencyFilter == null
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            selectedCurrencyFilter = null
                            showFilterSheet = false
                        }
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    SymbolIcon(
                                        symbol = OpenSplitIcons.Payments,
                                        contentDescription = null,
                                        tint = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        headlineContent = {
                            Text(
                                text = "All currencies",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "Show groups in any currency (${liveGroups.size} ${if (liveGroups.size == 1) "group" else "groups"})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            RadioButton(
                                selected = isAllSelected,
                                onClick = {
                                    selectedCurrencyFilter = null
                                    showFilterSheet = false
                                }
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                currencies.forEach { currency ->
                    val isSelected = selectedCurrencyFilter == currency
                    val count = liveGroups.count { it.currency == currency }
                    val symbol = CurrencyFormatter.getCurrencySymbol(currency)
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                selectedCurrencyFilter = currency
                                showFilterSheet = false
                            }
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (symbol.isNotEmpty() && symbol != currency) symbol else currency.take(3),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            },
                            headlineContent = {
                                Text(
                                    text = currency,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "$count ${if (count == 1) "group" else "groups"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCurrencyFilter = currency
                                        showFilterSheet = false
                                    }
                                )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
            }
        }
    }
}

@Composable
private fun GroupRow(
    group: Group,
    balance: Double?,
    hasTransactions: Boolean,
    lastActivity: com.google.firebase.Timestamp?,
    isPinned: Boolean,
    isOwner: Boolean,
    showOptions: Boolean = false,
    onShowOptions: () -> Unit = {},
    onDismissOptions: () -> Unit = {},
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onAddExpense: () -> Unit,
    onSettleUp: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    isArchived: Boolean = false,
    onToggleArchive: () -> Unit = {}
) {
    var internalMenuExpanded by remember { mutableStateOf(false) }
    val menuExpanded = showOptions || internalMenuExpanded
    val dismissMenu = {
        internalMenuExpanded = false
        onDismissOptions()
    }

    Box {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        internalMenuExpanded = true
                        onShowOptions()
                    }
                ),
            headlineContent = {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)) {
                    val lastActivityTs = lastActivity ?: group.createdAt
                    val currencyCode = group.currency.ifEmpty { "INR" }
                    val currencySymbol = CurrencyFormatter.getCurrencySymbol(currencyCode)
                    val currencyDisplay = if (currencySymbol.isNotEmpty() && currencySymbol != currencyCode) {
                        "$currencySymbol $currencyCode"
                    } else {
                        currencyCode
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        GroupMetadataBadge(
                            icon = OpenSplitIcons.People,
                            text = "${group.memberIds.size}"
                        )
                        GroupMetadataBadge(
                            text = currencyDisplay
                        )
                        GroupMetadataBadge(
                            icon = OpenSplitIcons.Schedule,
                            text = formatLastActivity(lastActivityTs)
                        )
                    }

                    if (balance != null && (kotlin.math.abs(balance) > 0.01 || (hasTransactions && group.memberIds.size > 1))) {
                        GroupBalanceChip(balance = balance, currency = group.currency)
                    }
                }
            },
            leadingContent = {
                com.opensplit.ui.components.GroupAvatar(name = group.name, avatarKey = group.avatarKey)
            },
            trailingContent = if (isPinned) {
                {
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = onTogglePin) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.PinFilled,
                            contentDescription = "Unpin group",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.rotate(45f)
                        )
                    }
                }
            } else null
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = dismissMenu,
            shape = MaterialTheme.shapes.large
        ) {
            DropdownMenuItem(
                text = { Text("Add Expense") },
                leadingIcon = { SymbolIcon(OpenSplitIcons.AddExpense, contentDescription = null) },
                onClick = {
                    dismissMenu()
                    onAddExpense()
                }
            )
            DropdownMenuItem(
                text = { Text("Settle Up") },
                leadingIcon = { SymbolIcon(OpenSplitIcons.Settle, contentDescription = null) },
                onClick = {
                    dismissMenu()
                    onSettleUp()
                }
            )
            DropdownMenuItem(
                text = { Text("View Group") },
                leadingIcon = { SymbolIcon(OpenSplitIcons.Groups, contentDescription = null) },
                onClick = {
                    dismissMenu()
                    onClick()
                }
            )

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text(if (isPinned) "Unpin" else "Pin") },
                leadingIcon = { SymbolIcon(if (isPinned) OpenSplitIcons.PinOutline else OpenSplitIcons.PinFilled, contentDescription = null) },
                onClick = {
                    dismissMenu()
                    onTogglePin()
                }
            )
            DropdownMenuItem(
                text = { Text(if (isArchived) "Unarchive" else "Archive") },
                leadingIcon = { SymbolIcon(if (isArchived) OpenSplitIcons.Unarchive else OpenSplitIcons.Archive, contentDescription = null) },
                onClick = {
                    dismissMenu()
                    onToggleArchive()
                }
            )
            DropdownMenuItem(
                text = { Text("Leave group") },
                leadingIcon = { SymbolIcon(OpenSplitIcons.Leave, contentDescription = null) },
                onClick = {
                    dismissMenu()
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
                        dismissMenu()
                        onDelete()
                    }
                )
            }
        }
    }
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

private fun formatLastActivity(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
}

@Composable
private fun GroupMetadataBadge(
    text: String,
    icon: com.opensplit.ui.theme.Symbol? = null
) {
    Surface(
        shape = OpenSplitTokens.PillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
        ) {
            if (icon != null) {
                SymbolIcon(
                    symbol = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}


