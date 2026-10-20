@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.components

import com.opensplit.ui.theme.Symbol
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.*
import com.opensplit.domain.model.toDisplayName
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.opensplit.di.AppContainer
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Group
import com.opensplit.domain.model.User
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.util.CurrencyFormatter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.combine

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GlobalSearchSheet(
    appContainer: AppContainer,
    onDismiss: () -> Unit,
    onNavigateToGroup: (String) -> Unit,
    onNavigateToExpense: (groupId: String, expenseId: String) -> Unit,
    onNavigateToFriend: (String) -> Unit,
    onAddExpense: (() -> Unit)? = null,
    onCreateGroup: (() -> Unit)? = null,
    onSettleUp: (() -> Unit)? = null,
    onInviteFriend: (() -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var settlements by remember { mutableStateOf<List<com.opensplit.domain.model.Settlement>>(emptyList()) }
    var settlementGroupIds by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var friends by remember { mutableStateOf<List<User>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    // Results follow the query after a short pause so every keystroke doesn't re-filter the lists.
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(query) {
        kotlinx.coroutines.delay(300)
        debouncedQuery = query.trim()
    }
    var typeFilter by remember { mutableStateOf(SearchType.ALL) }
    var onlyMine by remember { mutableStateOf(false) }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            appContainer.groupRepository.getGroupsForUser(currentUid).collect { userGroups ->
                groups = userGroups
                val groupIds = userGroups.map { it.id }
                if (groupIds.isNotEmpty()) {
                    val expFlows = groupIds.map { appContainer.expenseRepository.getExpensesForGroup(it) }
                    val setFlows = groupIds.map { gId ->
                        appContainer.settlementRepository.getSettlementsForGroup(gId).map { list -> list.map { gId to it } }
                    }
                    combine(combine(expFlows) { arrays -> arrays.flatMap { it.toList() } }, combine(setFlows) { arrays -> arrays.flatMap { it.toList() } }) { e, st -> e to st }
                        .collect { (allExp, allSettle) ->
                            expenses = allExp
                            settlements = allSettle.map { it.second }
                            settlementGroupIds = allSettle.associate { it.second.id to it.first }
                            loaded = true
                        }
                } else loaded = true
            }
        }
    }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            appContainer.friendRepository.getFriendsBalances(currentUid).collect { balancesMap ->
                val users = balancesMap.keys.mapNotNull { uid ->
                    appContainer.userRepository.getUser(uid)
                }
                friends = users
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val q = debouncedQuery
    val filteredGroups = remember(groups, q, typeFilter) {
        if (q.isBlank() || typeFilter !in listOf(SearchType.ALL, SearchType.GROUPS)) emptyList()
        else groups.filterNot { it.isDirect }.filter { it.name.contains(q, ignoreCase = true) }
    }

    val filteredExpenses = remember(expenses, q, typeFilter, onlyMine) {
        if (q.isBlank() || typeFilter !in listOf(SearchType.ALL, SearchType.EXPENSES)) emptyList()
        else expenses.filter {
            (!onlyMine || it.paidBy == currentUid) && (
                it.description.contains(q, ignoreCase = true) ||
                    it.category.contains(q, ignoreCase = true) ||
                    (it.notes?.contains(q, ignoreCase = true) == true) ||
                    it.amount.toString().contains(q) ||
                    CurrencyFormatter.format(it.amount, it.currency, showSymbol = false).replace(",", "").contains(q.replace(",", ""))
            )
        }
    }

    val friendNames = remember(friends) { friends.associate { it.uid to it.displayName } }
    val filteredSettlements = remember(settlements, q, typeFilter, onlyMine, friendNames) {
        if (q.isBlank() || typeFilter !in listOf(SearchType.ALL, SearchType.PAYMENTS)) emptyList()
        else settlements.filter { st ->
            (!onlyMine || st.fromUid == currentUid) && (
                st.amount.toString().contains(q) ||
                    (st.note?.contains(q, ignoreCase = true) == true) ||
                    (friendNames[st.fromUid]?.contains(q, ignoreCase = true) == true) ||
                    (friendNames[st.toUid]?.contains(q, ignoreCase = true) == true) ||
                    st.method.name.contains(q, ignoreCase = true)
            )
        }
    }

    val filteredFriends = remember(friends, q, typeFilter) {
        if (q.isBlank() || typeFilter !in listOf(SearchType.ALL, SearchType.PEOPLE)) emptyList()
        else friends.filter {
            it.displayName.contains(q, ignoreCase = true) ||
                    it.email.contains(q, ignoreCase = true)
        }
    }

    val totalResults = filteredGroups.size + filteredExpenses.size + filteredSettlements.size + filteredFriends.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 680.dp)
                .fillMaxHeight(0.9f)
                .padding(horizontal = OpenSplitTokens.SpaceLG)
        ) {
            // Header with Search TextField
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search anything") },
                leadingIcon = { SymbolIcon(OpenSplitIcons.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(shapes = IconButtonDefaults.shapes(), onClick = { query = "" }) {
                            SymbolIcon(OpenSplitIcons.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SearchType.entries.forEach { t ->
                    FilterChip(selected = typeFilter == t, onClick = { typeFilter = t }, label = { Text(t.label) })
                }
                FilterChip(selected = onlyMine, onClick = { onlyMine = !onlyMine }, label = { Text("Paid by me") })
            }
            if (query.isNotBlank() && (query.trim() != debouncedQuery || !loaded)) {
                InlineBusy("Searching…", modifier = Modifier.padding(vertical = 6.dp))
            }

            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

            if (query.isBlank()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD),
                    contentPadding = PaddingValues(bottom = OpenSplitTokens.SpaceXL)
                ) {
                    // 1. SEARCH SUGGESTIONS CHIPS
                    item {
                        SectionHeader(title = "Search Suggestions")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS),
                            verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = OpenSplitTokens.SpaceXS)
                        ) {
                            val suggestions = listOf(
                                "🍔 Food & Dining" to "Food",
                                "🏖️ Trip & Travel" to "Trip",
                                "🏠 Rent & Bills" to "Rent",
                                "🛒 Groceries" to "Grocery",
                                "🚕 Transport" to "Transport",
                                "🍿 Movies" to "Movie"
                            )
                            suggestions.forEach { (label, keyword) ->
                                FilterChip(
                                    selected = false,
                                    onClick = { query = keyword },
                                    label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                                    shape = MaterialTheme.shapes.medium,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                )
                            }
                        }
                    }

                    // 2. RECENT GROUPS
                    if (groups.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Recent Groups")
                        }
                        items(groups.take(3)) { group ->
                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        onDismiss()
                                        onNavigateToGroup(group.id)
                                    },
                                headlineContent = { Text(group.name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("${group.memberIds.size} members • ${group.currency}", style = MaterialTheme.typography.bodySmall) },
                                leadingContent = {
                                    GroupAvatar(name = group.name, avatarKey = group.avatarKey, size = 36.dp)
                                },
                                trailingContent = { SymbolIcon(OpenSplitIcons.ChevronRight, contentDescription = null) }
                            )
                        }
                    }

                    // 4. RECENT EXPENSES
                    if (expenses.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Recent Expenses")
                        }
                        items(expenses.take(3)) { exp ->
                            val group = groups.find { it.id == exp.groupId }
                            val groupName = group?.name ?: "Group"
                            val amtStr = CurrencyFormatter.format(exp.amount, exp.currency)

                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        onDismiss()
                                        onNavigateToExpense(exp.groupId, exp.id)
                                    },
                                headlineContent = { Text(exp.description, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("$groupName • ${exp.category}", style = MaterialTheme.typography.bodySmall) },
                                leadingContent = {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            SymbolIcon(OpenSplitIcons.ReceiptScan, contentDescription = null, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                },
                                trailingContent = {
                                    Text(amtStr, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    }

                    // 5. FRIENDS
                    if (friends.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Recent Friends")
                        }
                        items(friends.take(3)) { friend ->
                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        onDismiss()
                                        onNavigateToFriend(friend.uid)
                                    },
                                headlineContent = { Text(friend.displayName, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text(friend.email, style = MaterialTheme.typography.bodySmall) },
                                leadingContent = {
                                    UserAvatar(
                                        photoUrl = friend.photoUrl,
                                        displayName = friend.displayName,
                                        size = 36.dp
                                    )
                                },
                                trailingContent = { SymbolIcon(OpenSplitIcons.ChevronRight, contentDescription = null) }
                            )
                        }
                    }
                }
            } else if (totalResults == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(OpenSplitTokens.SpaceXL),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results found for '$query'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                    contentPadding = PaddingValues(bottom = OpenSplitTokens.SpaceXL)
                ) {
                    // GROUPS SECTION
                    if (filteredGroups.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Groups (${filteredGroups.size})")
                        }
                        items(filteredGroups) { group ->
                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        onDismiss()
                                        onNavigateToGroup(group.id)
                                    },
                                headlineContent = { Text(group.name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("${group.memberIds.size} members • ${group.currency}", style = MaterialTheme.typography.bodySmall) },
                                leadingContent = {
                                    GroupAvatar(name = group.name, avatarKey = group.avatarKey, size = 36.dp)
                                },
                                trailingContent = { SymbolIcon(OpenSplitIcons.ChevronRight, contentDescription = null) }
                            )
                        }
                    }

                    // EXPENSES SECTION
                    if (filteredExpenses.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Expenses (${filteredExpenses.size})")
                        }
                        items(filteredExpenses) { exp ->
                            val group = groups.find { it.id == exp.groupId }
                            val groupName = group?.name ?: "Group"
                            val amtStr = CurrencyFormatter.format(exp.amount, exp.currency)

                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        onDismiss()
                                        onNavigateToExpense(exp.groupId, exp.id)
                                    },
                                headlineContent = { Text(exp.description, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("$groupName • ${exp.category}", style = MaterialTheme.typography.bodySmall) },
                                leadingContent = {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            SymbolIcon(OpenSplitIcons.ReceiptScan, contentDescription = null, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                },
                                trailingContent = {
                                    Text(amtStr, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }
                    }

                    // FRIENDS SECTION
                    if (filteredSettlements.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Payments (${filteredSettlements.size})")
                        }
                        items(filteredSettlements) { st ->
                            val from = if (st.fromUid == currentUid) "You" else friendNames[st.fromUid] ?: "Someone"
                            val to = if (st.toUid == currentUid) "you" else friendNames[st.toUid] ?: "someone"
                            val gId = settlementGroupIds[st.id]
                            val gName = groups.find { it.id == gId }?.let { if (it.isDirect) "Direct" else it.name } ?: ""
                            ListItem(
                                modifier = Modifier.clickable { if (gId != null) onNavigateToGroup(gId) },
                                headlineContent = { Text("$from paid $to", fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("$gName • ${st.method.toDisplayName()}" + (st.note?.let { " • $it" } ?: ""), style = MaterialTheme.typography.bodySmall) },
                                leadingContent = { SymbolIcon(OpenSplitIcons.Settle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                trailingContent = { Text(CurrencyFormatter.format(st.amount, st.currency), fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    if (filteredFriends.isNotEmpty()) {
                        item {
                            SectionHeader(title = "People (${filteredFriends.size})")
                        }
                        items(filteredFriends) { friend ->
                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        onDismiss()
                                        onNavigateToFriend(friend.uid)
                                    },
                                headlineContent = { Text(friend.displayName, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text(friend.email, style = MaterialTheme.typography.bodySmall) },
                                leadingContent = {
                                    UserAvatar(
                                        photoUrl = friend.photoUrl,
                                        displayName = friend.displayName,
                                        size = 36.dp
                                    )
                                },
                                trailingContent = { SymbolIcon(OpenSplitIcons.ChevronRight, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    title: String,
    subtitle: String,
    icon: Symbol,
    containerColor: androidx.compose.ui.graphics.Color,
    iconColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OpenSplitTokens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)
        ) {
            Surface(
                shape = CircleShape,
                color = containerColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SymbolIcon(
                        symbol = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SymbolIcon(
                symbol = OpenSplitIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS)
    )
}

/** Result buckets the user can narrow to. */
enum class SearchType(val label: String) { ALL("All"), EXPENSES("Expenses"), PAYMENTS("Payments"), GROUPS("Groups"), PEOPLE("People") }
