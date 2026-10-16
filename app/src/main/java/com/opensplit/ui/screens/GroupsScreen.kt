@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.LocalSnackbarController

import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.animation.animateContentSize
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
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
    // "Settled" only when there were expenses and a payment actually closed them.
    val settledGroupIds = remember(homeState) {
        (homeState as? ScreenState.Success)?.data?.recentGroups
            ?.filter { it.expenseCount > 0 && it.settlementCount > 0 && kotlin.math.abs(it.balance) <= 0.01 }
            ?.map { it.group.id }?.toSet() ?: emptySet()
    }
    val groupsWithExpenses = remember(homeState) {
        (homeState as? ScreenState.Success)?.data?.recentGroups?.filter { it.expenseCount > 0 }?.map { it.group.id }?.toSet() ?: emptySet()
    }
    var breakdownType by remember { mutableStateOf<String?>(null) }
    val pinnedGroupIds by viewModel.pinnedGroupIds.collectAsState()
    val archivedGroupIds by viewModel.archivedGroupIds.collectAsState()
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val groupLastActivity by viewModel.groupLastActivity.collectAsState()
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var infoGroup by remember { mutableStateOf<Group?>(null) }
    val pullRefreshEnabled = com.opensplit.ui.components.LocalPullRefreshEnabled.current
    val friendNames = remember(viewModel) { mutableMapOf<String, String>() }
    val friendsForNames by viewModel.friendsBalances.collectAsState()
    (friendsForNames as? ScreenState.Success)?.data?.forEach { friendNames[it.user.uid] = it.user.displayName }
    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedCurrencyFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSort by rememberSaveable { mutableStateOf(GroupSortOption.RECENT) }
    val listState = rememberLazyListState()
    // Pinned reorder, after the official Compose drag-and-drop pattern: the dragged row is translated by
    // (finger travel + its start offset - its current laid-out offset), so swaps in the data don't move it
    // under the finger; every other row animates with animateItem. The dragged row opts out of placement
    // animation, which is what caused rows to overlap before.
    var draggingId by remember { mutableStateOf<String?>(null) }
    var draggedDistance by remember { mutableStateOf(0f) }
    var draggingInitialOffset by remember { mutableStateOf(0) }
    val releaseOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    var lastDraggedId by remember { mutableStateOf<String?>(null) }
    val dragScope = rememberCoroutineScope()
    fun draggingItem() = draggingId?.let { id -> listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id } }
    val draggingItemOffset: Float = draggingItem()?.let { draggedDistance + draggingInitialOffset - it.offset } ?: 0f
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
                    com.opensplit.ui.components.AppearOnEnter {
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
                        val pinnedList = pinnedGroupIds.mapNotNull { id -> filtered.firstOrNull { it.id == id } }
                        val unpinnedList = sortWithin(filtered.filterNot { pinnedGroupIds.contains(it.id) })
                        pinnedList to unpinnedList
                    }

                    val rowFor: @Composable androidx.compose.foundation.lazy.LazyItemScope.(Group, Boolean) -> Unit = { group, isPinned ->
                        val togglePin = {
                            // Silent: the row animating into the Pinned section is the feedback.
                            viewModel.togglePinnedGroup(group.id) { ok ->
                                if (!ok) snackbar.showMessage("You can pin up to 3 groups. Unpin one first.")
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
                        val isDragging = draggingId == group.id
                        val settling = draggingId == null && releaseOffset.value != 0f && releaseOffset.isRunning && lastDraggedId == group.id
                        SwipeActionRow(
                            modifier = if (isDragging) Modifier
                                .zIndex(1f)
                                .graphicsLayer {
                                    translationY = draggingItemOffset
                                    shadowElevation = 12f
                                    scaleX = 1.02f; scaleY = 1.02f
                                }
                            else Modifier
                                .animateItem()
                                .zIndex(if (settling) 1f else 0f)
                                .graphicsLayer { translationY = if (settling) releaseOffset.value else 0f },
                            endActions = listOf(
                                SwipeAction(icon = OpenSplitIcons.Settle, label = "Settle up", onAction = { onSettleUp(group.id) }),
                                SwipeAction(icon = if (isArchived) OpenSplitIcons.Unarchive else OpenSplitIcons.Archive, label = if (isArchived) "Unarchive" else "Archive", onAction = toggleArchive),
                                SwipeAction(icon = OpenSplitIcons.AddExpense, label = "Add expense", primary = true, onAction = { onAddExpense(group.id) })
                            )
                        ) {
                        GroupRow(
                            group = group,
                            balance = balanceByGroup[group.id],
                            showBalance = group.id in groupsWithExpenses,
                            settled = group.id in settledGroupIds,
                            isPinned = isPinned,
                            pinnedIndex = pinnedGroupIds.indexOf(group.id),
                            pinnedCount = pinnedGroupIds.size,
                            onClick = { onGroupClick(group.id) },
                            onLongPress = togglePin, // press-and-hold pins or unpins straight away
                            onTogglePin = togglePin,
                            onDragStart = {
                                pullRefreshEnabled.value = false
                                draggedDistance = 0f
                                draggingInitialOffset = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == group.id }?.offset ?: 0
                                draggingId = group.id; lastDraggedId = group.id
                            },
                            onDragBy = { dy ->
                                draggedDistance += dy
                                val current = draggingItem()
                                if (current != null) {
                                    val start = current.offset + draggingItemOffset
                                    val middle = start + current.size / 2f
                                    val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                        item.key != current.key && item.key in pinnedGroupIds && middle > item.offset && middle < item.offset + item.size
                                    }
                                    if (target != null) {
                                        val from = pinnedGroupIds.indexOf(current.key); val to = pinnedGroupIds.indexOf(target.key)
                                        if (from >= 0 && to >= 0) viewModel.reorderPinned(pinnedGroupIds.toMutableList().apply { add(to, removeAt(from)) })
                                    }
                                }
                            },
                            onDragEnd = {
                                pullRefreshEnabled.value = true
                                val residual = draggingItemOffset
                                draggingId = null
                                dragScope.launch {
                                    releaseOffset.snapTo(residual)
                                    releaseOffset.animateTo(0f, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
                                }
                            },
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
                            onToggleArchive = toggleArchive,
                            lastActivitySeconds = groupLastActivity[group.id]?.seconds ?: group.createdAt.seconds,
                            onViewInfo = { infoGroup = group }
                        )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    }

                    LazyColumn(
                        userScrollEnabled = draggingId == null,
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
                            // Same key in both sections so a pin/unpin animates the row moving, not vanishing.
                            items(pinned, key = { it.id }) { group -> rowFor(group, true) }
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

    // "View info": everything known about the group since it was created, in one sheet.
    infoGroup?.let { g ->
        val stats = (homeState as? ScreenState.Success)?.data?.recentGroups?.firstOrNull { it.group.id == g.id }
        val nameOf: (String) -> String = { uid -> if (uid == currentUid) "You" else friendNames[uid] ?: uid.take(6) }
        val dateFmt = remember { java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.getDefault()) }
        ModalBottomSheet(onDismissRequest = { infoGroup = null }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OpenSplitTokens.SpaceLG).padding(bottom = OpenSplitTokens.SpaceXL).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.opensplit.ui.components.GroupAvatar(name = g.name, avatarKey = g.avatarKey)
                    Spacer(Modifier.width(OpenSplitTokens.SpaceMD))
                    Column {
                        Text(g.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(updatedLabel(groupLastActivity[g.id]?.seconds ?: g.createdAt.seconds), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(OpenSplitTokens.SpaceLG))
                @Composable fun info(label: String, value: String) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(130.dp))
                        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    }
                }
                Text("Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                info("Created", dateFmt.format(g.createdAt.toDate()))
                info("Created by", nameOf(g.createdBy))
                info("Currency", g.currency)
                info("Default split", g.defaultSplitType.name.lowercase().replaceFirstChar { it.uppercase() })
                info("Simplify debts", if (g.simplifyDebts) "On" else "Off")
                if (stats != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceSM))
                    Text("Activity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    info("Expenses", "${stats.expenseCount}")
                    info("Total spent", CurrencyFormatter.format(stats.totalSpent, currencyCode = g.currency, showSymbol = true))
                    info("Settlements", "${stats.settlementCount}")
                    info("Your balance", when {
                        stats.balance > 0.01 -> "You are owed " + CurrencyFormatter.format(stats.balance, currencyCode = g.currency, showSymbol = true)
                        stats.balance < -0.01 -> "You owe " + CurrencyFormatter.format(-stats.balance, currencyCode = g.currency, showSymbol = true)
                        else -> "Nothing outstanding"
                    })
                    info("Last activity", groupLastActivity[g.id]?.let { dateFmt.format(it.toDate()) } ?: "—")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceSM))
                Text("Members (${g.memberIds.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                g.memberIds.forEach { uid ->
                    androidx.compose.material3.ListItem(
                        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        headlineContent = { Text(nameOf(uid)) },
                        supportingContent = { if (uid == g.createdBy) Text("Owner", style = MaterialTheme.typography.bodySmall) }
                    )
                }
                if (g.formerMemberIds.isNotEmpty()) {
                    Text("Left the group (${g.formerMemberIds.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = OpenSplitTokens.SpaceSM))
                    g.formerMemberIds.forEach { uid -> Text(nameOf(uid), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp)) }
                }
                Spacer(Modifier.height(OpenSplitTokens.SpaceMD))
                Button(shapes = ButtonDefaults.shapes(), onClick = { infoGroup = null; onGroupClick(g.id) }, modifier = Modifier.fillMaxWidth()) { Text("Open group") }
            }
        }
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
    showBalance: Boolean = true,
    settled: Boolean = false,
    isPinned: Boolean,
    pinnedIndex: Int = -1,
    pinnedCount: Int = 0,
    isOwner: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onTogglePin: () -> Unit,
    onDragStart: () -> Unit = {},
    onDragBy: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onAddExpense: () -> Unit,
    onSettleUp: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    isArchived: Boolean = false,
    onToggleArchive: () -> Unit = {},
    lastActivitySeconds: Long = group.createdAt.seconds,
    onViewInfo: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = { haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); onLongPress() }),
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
                    text = "${group.memberIds.size} ${if (group.memberIds.size == 1) "member" else "members"} • ${group.currency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = updatedLabel(lastActivitySeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
                if (balance != null && showBalance && (kotlin.math.abs(balance) > 0.01 || settled)) GroupBalanceChip(balance = balance, currency = group.currency)
            }
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPinned && pinnedCount > 1) {
                    // Drag handle (left, like Gmail/Keep): the row follows the finger while reordering.
                    // Stable pointerInput key + latest callbacks: a swap mid-drag must not restart the gesture.
                    val startNow by androidx.compose.runtime.rememberUpdatedState(onDragStart)
                    val byNow by androidx.compose.runtime.rememberUpdatedState(onDragBy)
                    val endNow by androidx.compose.runtime.rememberUpdatedState(onDragEnd)
                    SymbolIcon(
                        symbol = OpenSplitIcons.DragIndicator,
                        contentDescription = "Drag to reorder pinned group",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp).size(22.dp).pointerInput(Unit) {
                            // Claim the touch before the list or pull-to-refresh can: consume the down in the
                            // Initial pass, then follow the finger until it lifts.
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                down.consume()
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                startNow()
                                try {
                                    drag(down.id) { change ->
                                        byNow(change.positionChange().y); change.consume()
                                    }
                                } finally { endNow() }
                            }
                        }
                    )
                }
                com.opensplit.ui.components.GroupAvatar(name = group.name, avatarKey = group.avatarKey)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPinned) {
                    SymbolIcon(OpenSplitIcons.PinFilled, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp).size(18.dp).rotate(45f))
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
                        DropdownMenuItem(
                            text = { Text("View info") },
                            leadingIcon = { SymbolIcon(OpenSplitIcons.Info, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onViewInfo()
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
    val animatedBg by androidx.compose.animation.animateColorAsState(bg, label = "balanceChipBg")
    Surface(shape = OpenSplitTokens.PillShape, color = animatedBg, modifier = Modifier.animateContentSize()) {
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

/** "Updated just now / 3h ago / 2d ago / 12 Mar" from the group's last activity (or creation). */
private fun updatedLabel(seconds: Long): String {
    val diff = System.currentTimeMillis() / 1000 - seconds
    return when {
        diff < 60 -> "Updated just now"
        diff < 3600 -> "Updated ${diff / 60}m ago"
        diff < 86400 -> "Updated ${diff / 3600}h ago"
        diff < 7 * 86400 -> "Updated ${diff / 86400}d ago"
        else -> "Updated " + java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(java.util.Date(seconds * 1000))
    }
}
