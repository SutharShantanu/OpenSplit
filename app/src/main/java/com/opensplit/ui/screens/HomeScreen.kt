package com.opensplit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.ui.components.CreateGroupDialog
import com.opensplit.ui.components.HeroBalanceCard
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.components.WalletIllustration
import com.opensplit.ui.components.getBalanceColor
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.HomeUiState
import com.opensplit.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToGroupsTab: () -> Unit,
    onNavigateToGroupDetail: (String) -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToAddExpense: (String) -> Unit,
    onNavigateToSettleUp: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showGroupPickerForAddExpense by remember { mutableStateOf(false) }
    var showGroupPickerForSettleUp by remember { mutableStateOf(false) }

    StateLayout(state = state) { homeState ->
        if (homeState.allGroups.isEmpty()) {
            // Empty state for new user
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
                    WalletIllustration(size = 140.dp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Welcome to OpenSplit!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create your first group to start splitting expenses with friends easily.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showCreateGroupDialog = true },
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Rounded.GroupAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create your first group")
                    }
                }
            }
        } else {
            val scrollState = rememberScrollState()
            val rawName = homeState.user.displayName.takeIf { it.isNotBlank() && !it.equals("User", ignoreCase = true) }
                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: homeState.user.email.substringBefore("@").replaceFirstChar { it.uppercase() }
            val firstName = rawName.split(" ").firstOrNull()?.ifBlank { "Friend" } ?: "Friend"
            val currentDate = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Greeting & Date
                Column {
                    Text(
                        text = "Hi, $firstName",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 2. Hero Net Balance (per currency — never summed across currencies)
                val nonZeroNet = homeState.netByCurrency.filterValues { kotlin.math.abs(it) > 0.001 }
                val primaryCurrency = nonZeroNet.maxByOrNull { kotlin.math.abs(it.value) }?.key
                    ?: homeState.nudgeCurrency
                HeroBalanceCard(
                    amount = homeState.netByCurrency[primaryCurrency] ?: 0.0,
                    currency = primaryCurrency,
                    title = "TOTAL NET BALANCE"
                )
                if (nonZeroNet.size > 1) {
                    Text(
                        text = nonZeroNet.filterKeys { it != primaryCurrency }
                            .entries.joinToString("   ·   ") { (c, amt) ->
                                com.opensplit.util.CurrencyFormatter.format(amt, c, showSign = true)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3. Quick Actions Row (use AssistChips)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
                ) {
                    AssistChip(
                        onClick = {
                            if (homeState.allGroups.size == 1) {
                                onNavigateToAddExpense(homeState.allGroups.first().id)
                            } else {
                                showGroupPickerForAddExpense = true
                            }
                        },
                        label = { Text("Add expense") },
                        leadingIcon = { Icon(OpenSplitIcons.AddExpense, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    AssistChip(
                        onClick = {
                            if (homeState.allGroups.size == 1) {
                                onNavigateToSettleUp(homeState.allGroups.first().id)
                            } else {
                                showGroupPickerForSettleUp = true
                            }
                        },
                        label = { Text("Settle up") },
                        leadingIcon = { Icon(OpenSplitIcons.Settle, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )

                    AssistChip(
                        onClick = { showCreateGroupDialog = true },
                        label = { Text("New group") },
                        leadingIcon = { Icon(OpenSplitIcons.Invite, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                // 4. Smart Settle-up Nudge
                if (homeState.smartNudge != null) {
                    val nudge = homeState.smartNudge
                    val otherUser = homeState.nudgeOtherUser
                    val otherName = otherUser?.displayName ?: "a friend"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(OpenSplitTokens.SpaceLG)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Smart Settle-Up Nudge",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { viewModel.dismissNudge(nudge) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(OpenSplitIcons.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                            Text(
                                text = "Settle up with $otherName for ${com.opensplit.util.CurrencyFormatter.format(nudge.amount, homeState.nudgeCurrency)}?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                            SuggestionChip(
                                onClick = {
                                    val firstGroupId = homeState.allGroups.firstOrNull()?.id ?: ""
                                    if (firstGroupId.isNotEmpty()) onNavigateToSettleUp(firstGroupId)
                                },
                                label = { Text("Settle up with $otherName") },
                                icon = { Icon(OpenSplitIcons.Settle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }

                // 5. Active Groups Section
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Groups",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToGroupsTab) {
                            Text("See all")
                        }
                    }
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                    if (homeState.recentGroups.isEmpty()) {
                        Text(
                            text = "No active groups yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD),
                            contentPadding = PaddingValues(vertical = OpenSplitTokens.SpaceXS)
                        ) {
                            items(homeState.recentGroups, key = { it.group.id }) { groupWithBal ->
                                var menuExpanded by remember { mutableStateOf(false) }
                                val bal = groupWithBal.balance
                                val formattedAmount = com.opensplit.util.CurrencyFormatter.format(
                                    amount = bal,
                                    currencyCode = groupWithBal.group.currency,
                                    showSymbol = true
                                )

                                ElevatedCard(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clickable { onNavigateToGroupDetail(groupWithBal.group.id) },
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(OpenSplitTokens.SpaceMD)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = MaterialTheme.shapes.medium,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = groupWithBal.group.name.take(1).uppercase(),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }

                                            Box {
                                                IconButton(
                                                    onClick = { menuExpanded = true },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = OpenSplitIcons.More,
                                                        contentDescription = "Quick Actions",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                DropdownMenu(
                                                    expanded = menuExpanded,
                                                    onDismissRequest = { menuExpanded = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("Add Expense") },
                                                        leadingIcon = { Icon(OpenSplitIcons.AddExpense, contentDescription = null) },
                                                        onClick = {
                                                            menuExpanded = false
                                                            onNavigateToAddExpense(groupWithBal.group.id)
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Settle Up") },
                                                        leadingIcon = { Icon(OpenSplitIcons.Settle, contentDescription = null) },
                                                        onClick = {
                                                            menuExpanded = false
                                                            onNavigateToSettleUp(groupWithBal.group.id)
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("View Group") },
                                                        leadingIcon = { Icon(OpenSplitIcons.Groups, contentDescription = null) },
                                                        onClick = {
                                                            menuExpanded = false
                                                            onNavigateToGroupDetail(groupWithBal.group.id)
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))

                                        Text(
                                            text = groupWithBal.group.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )

                                        Text(
                                            text = "${groupWithBal.group.memberIds.size} members",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))

                                        // Status badge
                                        val (badgeBg, badgeFg, statusLabel) = when {
                                            bal > 0.01 -> Triple(
                                                OpenSplitTokens.OwedPositive.copy(alpha = 0.15f),
                                                OpenSplitTokens.OwedPositive,
                                                "Owed $formattedAmount"
                                            )
                                            bal < -0.01 -> Triple(
                                                OpenSplitTokens.OwedNegative.copy(alpha = 0.15f),
                                                OpenSplitTokens.OwedNegative,
                                                "You owe ${com.opensplit.util.CurrencyFormatter.format(-bal, groupWithBal.group.currency)}"
                                            )
                                            else -> Triple(
                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                                "Settled up"
                                            )
                                        }

                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = badgeBg,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = statusLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = badgeFg,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Recent Activity Preview (3 items max using ListItem)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToActivity) {
                            Text("See all")
                        }
                    }
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                    if (homeState.recentActivities.isEmpty()) {
                        Text(
                            text = "No recent activity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column {
                            homeState.recentActivities.take(3).forEach { activity ->
                                val timeStr = remember(activity.timestamp) {
                                    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                        .format(activity.timestamp.toDate())
                                }
                                ListItem(
                                    headlineContent = { Text(activity.message, style = MaterialTheme.typography.bodyMedium) },
                                    supportingContent = { Text(timeStr, style = MaterialTheme.typography.labelSmall) },
                                    leadingContent = {
                                        Icon(
                                            OpenSplitIcons.Activity,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs for quick actions and group creation
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, currency ->
                viewModel.createGroup(name, currency)
                showCreateGroupDialog = false
            }
        )
    }

    if (showGroupPickerForAddExpense) {
        val groups = (state as? com.opensplit.ui.viewmodel.ScreenState.Success)?.data?.allGroups ?: emptyList()
        GroupSelectionDialog(
            title = "Select Group for New Expense",
            groups = groups,
            onDismiss = { showGroupPickerForAddExpense = false },
            onSelectGroup = { groupId ->
                showGroupPickerForAddExpense = false
                onNavigateToAddExpense(groupId)
            }
        )
    }

    if (showGroupPickerForSettleUp) {
        val groups = (state as? com.opensplit.ui.viewmodel.ScreenState.Success)?.data?.allGroups ?: emptyList()
        GroupSelectionDialog(
            title = "Select Group to Settle Up",
            groups = groups,
            onDismiss = { showGroupPickerForSettleUp = false },
            onSelectGroup = { groupId ->
                showGroupPickerForSettleUp = false
                onNavigateToSettleUp(groupId)
            }
        )
    }
}

@Composable
fun GroupSelectionDialog(
    title: String,
    groups: List<com.opensplit.domain.model.Group>,
    onDismiss: () -> Unit,
    onSelectGroup: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                groups.forEach { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectGroup(group.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(group.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
