package com.example.ui.components

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
import com.example.di.AppContainer
import com.example.domain.model.Expense
import com.example.domain.model.Group
import com.example.domain.model.User
import com.example.ui.theme.OpenSplitIcons
import com.example.ui.theme.OpenSplitTokens
import com.example.util.CurrencyFormatter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.combine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchSheet(
    appContainer: AppContainer,
    onDismiss: () -> Unit,
    onNavigateToGroup: (String) -> Unit,
    onNavigateToExpense: (groupId: String, expenseId: String) -> Unit,
    onNavigateToFriend: (String) -> Unit
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
                .fillMaxHeight(0.85f)
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(OpenSplitTokens.SpaceXL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = OpenSplitIcons.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                        Text(
                            text = "Search OpenSplit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                        Text(
                            text = "Type to search across all your groups, expenses, and friends.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
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
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(OpenSplitIcons.Groups, contentDescription = null, modifier = Modifier.size(20.dp))
                                        }
                                    }
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS)
    )
}
