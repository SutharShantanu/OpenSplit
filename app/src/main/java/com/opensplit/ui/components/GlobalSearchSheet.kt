package com.opensplit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var friends by remember { mutableStateOf<List<User>>(emptyList()) }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            appContainer.groupRepository.getGroupsForUser(currentUid).collect { userGroups ->
                groups = userGroups
                val groupIds = userGroups.map { it.id }
                if (groupIds.isNotEmpty()) {
                    val expFlows = groupIds.map { appContainer.expenseRepository.getExpensesForGroup(it) }
                    combine(expFlows) { arrays -> arrays.flatMap { it.toList() } }.collect { allExp ->
                        expenses = allExp
                    }
                }
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

    val filteredGroups = remember(groups, query) {
        if (query.isBlank()) emptyList()
        else groups.filter { it.name.contains(query, ignoreCase = true) }
    }

    val filteredExpenses = remember(expenses, query) {
        if (query.isBlank()) emptyList()
        else expenses.filter {
            it.description.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true) ||
                    it.amount.toString().contains(query)
        }
    }

    val filteredFriends = remember(friends, query) {
        if (query.isBlank()) emptyList()
        else friends.filter {
            it.displayName.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
        }
    }

    val totalResults = filteredGroups.size + filteredExpenses.size + filteredFriends.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = OpenSplitTokens.SpaceLG)
        ) {
            // Header with Search TextField
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search groups, expenses, friends...") },
                leadingIcon = { Icon(OpenSplitIcons.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(OpenSplitIcons.Close, contentDescription = "Clear")
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
                                trailingContent = { Icon(OpenSplitIcons.ChevronRight, contentDescription = null) }
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
                                            Icon(OpenSplitIcons.ReceiptScan, contentDescription = null, modifier = Modifier.size(20.dp))
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
                                trailingContent = { Icon(OpenSplitIcons.ChevronRight, contentDescription = null) }
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
                                trailingContent = { Icon(OpenSplitIcons.ChevronRight, contentDescription = null) }
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
                                            Icon(OpenSplitIcons.ReceiptScan, contentDescription = null, modifier = Modifier.size(20.dp))
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
                    if (filteredFriends.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Friends (${filteredFriends.size})")
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
                                trailingContent = { Icon(OpenSplitIcons.ChevronRight, contentDescription = null) }
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                    Icon(
                        imageVector = icon,
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
            Icon(
                imageVector = OpenSplitIcons.ChevronRight,
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
