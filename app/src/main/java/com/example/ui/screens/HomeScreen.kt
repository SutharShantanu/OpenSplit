package com.example.ui.screens

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
import com.example.ui.components.CreateGroupDialog
import com.example.ui.components.HeroBalanceCard
import com.example.ui.components.StateLayout
import com.example.ui.components.WalletIllustration
import com.example.ui.components.getBalanceColor
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.HomeViewModel
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
            val firstName = homeState.user.displayName.split(" ").firstOrNull() ?: "there"
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

                // 2. Hero Net Balance
                HeroBalanceCard(
                    amount = homeState.netBalance,
                    currency = homeState.currency,
                    title = "TOTAL NET BALANCE"
                )

                // 3. Quick Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedFilterChip(
                        selected = true,
                        onClick = {
                            if (homeState.allGroups.size == 1) {
                                onNavigateToAddExpense(homeState.allGroups.first().id)
                            } else {
                                showGroupPickerForAddExpense = true
                            }
                        },
                        label = { Text("Add Expense") },
                        leadingIcon = { Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    ElevatedFilterChip(
                        selected = true,
                        onClick = {
                            if (homeState.allGroups.size == 1) {
                                onNavigateToSettleUp(homeState.allGroups.first().id)
                            } else {
                                showGroupPickerForSettleUp = true
                            }
                        },
                        label = { Text("Settle Up") },
                        leadingIcon = { Icon(Icons.Rounded.Handshake, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )

                    ElevatedFilterChip(
                        selected = false,
                        onClick = { showCreateGroupDialog = true },
                        label = { Text("New Group") },
                        leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                // 4. Smart Settle-up Nudge (conditional)
                if (homeState.smartNudge != null) {
                    val nudge = homeState.smartNudge
                    val otherUser = homeState.nudgeOtherUser
                    val otherName = otherUser?.displayName ?: "a friend"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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
                                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "You could settle up with $otherName for ${homeState.currency}${String.format("%.2f", nudge.amount)}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val firstGroupId = homeState.allGroups.firstOrNull()?.id ?: ""
                                    if (firstGroupId.isNotEmpty()) onNavigateToSettleUp(firstGroupId)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Text("Settle Up Now")
                            }
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
                    Spacer(modifier = Modifier.height(8.dp))
                    if (homeState.recentGroups.isEmpty()) {
                        Text(
                            text = "No active groups yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(homeState.recentGroups) { groupWithBal ->
                                Card(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .clickable { onNavigateToGroupDetail(groupWithBal.group.id) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(36.dp)
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
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = groupWithBal.group.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val bal = groupWithBal.balance
                                        val color = getBalanceColor(bal)
                                        val balText = if (bal > 0.01) {
                                            "You are owed ${homeState.currency}${String.format("%.2f", bal)}"
                                        } else if (bal < -0.01) {
                                            "You owe ${homeState.currency}${String.format("%.2f", -bal)}"
                                        } else {
                                            "Settled up"
                                        }
                                        Text(
                                            text = balText,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = color,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Recent Activity Preview
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
                    Spacer(modifier = Modifier.height(8.dp))
                    if (homeState.recentActivities.isEmpty()) {
                        Text(
                            text = "No recent activity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            homeState.recentActivities.forEach { activity ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = activity.message,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        val timeStr = remember(activity.timestamp) {
                                            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                                .format(activity.timestamp.toDate())
                                        }
                                        Text(
                                            text = timeStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
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
        val groups = (state as? com.example.ui.viewmodel.ScreenState.Success)?.data?.allGroups ?: emptyList()
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
        val groups = (state as? com.example.ui.viewmodel.ScreenState.Success)?.data?.allGroups ?: emptyList()
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
    groups: List<com.example.domain.model.Group>,
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
