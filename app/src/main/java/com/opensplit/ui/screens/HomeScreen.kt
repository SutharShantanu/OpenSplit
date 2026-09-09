package com.opensplit.ui.screens

import com.opensplit.ui.components.LocalSnackbarController

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensplit.ui.components.CreateGroupDialog
import com.opensplit.ui.components.HeroBalanceCard
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.components.WalletIllustration
import com.opensplit.ui.components.getBalanceColor
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.HomeUiState
import com.opensplit.ui.viewmodel.HomeViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import com.opensplit.ui.components.GroupAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class BadgeSpec(
    val background: androidx.compose.ui.graphics.Color,
    val foreground: androidx.compose.ui.graphics.Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    mainViewModel: com.opensplit.ui.viewmodel.MainViewModel? = null,
    onNavigateToGroupsTab: () -> Unit,
    onNavigateToGroupDetail: (String) -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToAddExpense: (String) -> Unit,
    onNavigateToSettleUp: (String) -> Unit,
    onNavigateToPersonBalance: ((String) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showGroupPickerForAddExpense by remember { mutableStateOf(false) }
    var showGroupPickerForSettleUp by remember { mutableStateOf(false) }
    var breakdownType by remember { mutableStateOf<String?>(null) }

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
                    var isSeeding by remember { mutableStateOf(false) }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showCreateGroupDialog = true },
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Rounded.GroupAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create group")
                        }

                        OutlinedButton(
                            onClick = {
                                isSeeding = true
                                viewModel.seedMockData { success ->
                                    isSeeding = false
                                    snackbar.showMessage(if (success) "Sample data loaded successfully!" else "Failed to load sample data.")
                                }
                            },
                            enabled = !isSeeding,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            if (isSeeding) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading...")
                            } else {
                                Icon(OpenSplitIcons.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Load Sample Data")
                            }
                        }
                    }
                }
            }
        } else {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Hero Net Balance Card (Dark purple with ratio bar)
                val nonZeroNet = homeState.netByCurrency.filterValues { kotlin.math.abs(it) > 0.001 }
                val primaryCurrency = nonZeroNet.maxByOrNull { kotlin.math.abs(it.value) }?.key
                    ?: homeState.nudgeCurrency
                HeroBalanceCard(
                    amount = homeState.netByCurrency[primaryCurrency] ?: 0.0,
                    currency = primaryCurrency,
                    youAreOwed = homeState.youAreOwedByCurrency[primaryCurrency] ?: 0.0,
                    youOwe = homeState.youOweByCurrency[primaryCurrency] ?: 0.0,
                    onOwedToYouClick = { breakdownType = "OWED_TO_YOU" },
                    onYouOweClick = { breakdownType = "YOU_OWE" }
                )

                // 2. Quick Actions Row (3 Stadium Pills matching mockup)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    // + Add Expense (Primary Pill)
                    item {
                        Surface(
                            onClick = { showGroupPickerForAddExpense = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(44.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add Expense",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }

                    // Settle Up (Secondary Container Pill)
                    item {
                        val settleableGroups = homeState.allGroups.filter { it.memberIds.size > 1 }
                        Surface(
                            onClick = {
                                when {
                                    settleableGroups.isEmpty() -> snackbar.showMessage("Add another member to a group before settling up")
                                    settleableGroups.size == 1 -> onNavigateToSettleUp(settleableGroups.first().id)
                                    else -> showGroupPickerForSettleUp = true
                                }
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.height(44.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            ) {
                                Icon(
                                    imageVector = OpenSplitIcons.Settle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Settle Up",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // Scan Receipt (Surface Container High Pill)
                    item {
                        Surface(
                            onClick = {
                                if (homeState.allGroups.isEmpty()) {
                                    snackbar.showMessage("Create a group first")
                                } else {
                                    showGroupPickerForAddExpense = true
                                }
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.height(44.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            ) {
                                Icon(
                                    imageVector = OpenSplitIcons.ReceiptScan,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scan Receipt",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Active Groups Section
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Groups",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(onClick = onNavigateToGroupsTab) {
                            Text(
                                text = "See All",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
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
                            items(homeState.recentGroups, key = { it.group.id }) { groupWithBal ->
                                val bal = groupWithBal.balance
                                val formattedAmount = com.opensplit.util.CurrencyFormatter.format(
                                    amount = kotlin.math.abs(bal),
                                    currencyCode = groupWithBal.group.currency,
                                    showSymbol = true
                                )

                                Surface(
                                    modifier = Modifier
                                        .width(200.dp)
                                        .clickable { onNavigateToGroupDetail(groupWithBal.group.id) },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        com.opensplit.ui.components.GroupAvatar(
                                            name = groupWithBal.group.name,
                                            avatarKey = groupWithBal.group.avatarKey,
                                            size = 44.dp
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = groupWithBal.group.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val debtText = when {
                                                bal < -0.01 -> "You owe $formattedAmount"
                                                bal > 0.01 -> "You are owed $formattedAmount"
                                                else -> "Settled up"
                                            }
                                            val debtColor = when {
                                                bal < -0.01 -> MaterialTheme.colorScheme.error
                                                bal > 0.01 -> OpenSplitTokens.OwedPositive
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                            Text(
                                                text = debtText,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = debtColor
                                                ),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Recent Activity Section (Mockup Container Card)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = onNavigateToActivity) {
                            Icon(
                                imageVector = OpenSplitIcons.Filter,
                                contentDescription = "Filter Activity",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (homeState.recentActivities.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recent activity yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                homeState.recentActivities.take(5).forEachIndexed { index, activity ->
                                    val timeStr = remember(activity.timestamp) {
                                        val diff = System.currentTimeMillis() - activity.timestamp.toDate().time
                                        when {
                                            diff < 3600_000 -> "Just now"
                                            diff < 86400_000 -> "Today"
                                            diff < 172800_000 -> "Yesterday"
                                            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(activity.timestamp.toDate())
                                        }
                                    }

                                    val (icon, iconBg, iconFg) = when (activity.type) {
                                        com.opensplit.domain.model.ActivityType.SETTLEMENT_ADDED ->
                                            Triple(OpenSplitIcons.Settle, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                        com.opensplit.domain.model.ActivityType.EXPENSE_ADDED,
                                        com.opensplit.domain.model.ActivityType.EXPENSE_EDITED ->
                                            Triple(OpenSplitIcons.CategoryFood, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                                        else ->
                                            Triple(OpenSplitIcons.Groups, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToActivity() }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Circular avatar / category icon
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(iconBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = iconFg
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = activity.message,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (index < homeState.recentActivities.take(5).size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            thickness = 0.5.dp
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
            onCreate = { name, currency, avatarKey ->
                viewModel.createGroup(name, currency, avatarKey)
                snackbar.showMessage("Created $name")
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
        val groups = (state as? com.opensplit.ui.viewmodel.ScreenState.Success)?.data?.allGroups
            ?.filter { it.memberIds.size > 1 } ?: emptyList()
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

    val currentBreakdown = breakdownType
    if (currentBreakdown != null) {
        val homeUiState = (state as? com.opensplit.ui.viewmodel.ScreenState.Success)?.data
        if (homeUiState != null) {
            BalanceBreakdownSheet(
                breakdownType = currentBreakdown,
                homeState = homeUiState,
                mainViewModel = mainViewModel,
                onDismiss = { breakdownType = null },
                onNavigateToGroup = { groupId ->
                    breakdownType = null
                    onNavigateToGroupDetail(groupId)
                },
                onNavigateToSettleUp = { groupId ->
                    breakdownType = null
                    onNavigateToSettleUp(groupId)
                },
                onNavigateToPersonBalance = { friendId ->
                    breakdownType = null
                    onNavigateToPersonBalance?.invoke(friendId)
                }
            )
        }
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
        icon = { Icon(OpenSplitIcons.Groups, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)) {
                Text(
                    text = "Pick which group this belongs to.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                groups.forEach { group ->
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelectGroup(group.id) },
                        colors = ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        leadingContent = {
                            com.opensplit.ui.components.GroupAvatar(
                                name = group.name,
                                avatarKey = group.avatarKey,
                                size = 40.dp
                            )
                        },
                        headlineContent = {
                            Text(group.name, fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = {
                            Text(
                                text = "${group.memberIds.size} ${if (group.memberIds.size == 1) "member" else "members"} • ${group.currency}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Icon(
                                OpenSplitIcons.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceBreakdownSheet(
    breakdownType: String, // "OWED_TO_YOU" or "YOU_OWE"
    homeState: HomeUiState,
    mainViewModel: com.opensplit.ui.viewmodel.MainViewModel? = null,
    onDismiss: () -> Unit,
    onNavigateToGroup: (String) -> Unit,
    onNavigateToSettleUp: (String) -> Unit,
    onNavigateToPersonBalance: ((String) -> Unit)? = null
) {
    val isOwedToYou = breakdownType == "OWED_TO_YOU"
    val primaryCurrency = homeState.nudgeCurrency
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = By Group, 1 = By Friend

    val friendsBalancesState by (mainViewModel?.friendsBalances?.collectAsState() ?: remember { mutableStateOf(com.opensplit.ui.viewmodel.ScreenState.Success(emptyList())) })
    val allFriendsBalances = (friendsBalancesState as? com.opensplit.ui.viewmodel.ScreenState.Success)?.data ?: emptyList()

    val relevantGroups = remember(homeState.recentGroups, homeState.allGroups, isOwedToYou) {
        if (isOwedToYou) {
            homeState.recentGroups.filter { it.balance > 0.01 }
        } else {
            homeState.recentGroups.filter { it.balance < -0.01 }
        }
    }

    val relevantFriends = remember(allFriendsBalances, isOwedToYou, primaryCurrency) {
        allFriendsBalances.filter { friend ->
            if (isOwedToYou) friend.owesYou else friend.youOwe
        }.mapNotNull { friend ->
            val net = friend.balancesByCurrency[primaryCurrency]
                ?: friend.balancesByCurrency.values.firstOrNull()
                ?: 0.0
            if ((isOwedToYou && net > 0.01) || (!isOwedToYou && net < -0.01)) {
                friend to net
            } else null
        }
    }

    val totalAmount = if (isOwedToYou) {
        homeState.youAreOwedByCurrency[primaryCurrency] ?: 0.0
    } else {
        homeState.youOweByCurrency[primaryCurrency] ?: 0.0
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceMD)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOwedToYou) OpenSplitTokens.OwedPositive.copy(alpha = 0.15f)
                                else OpenSplitTokens.OwedNegative.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOwedToYou) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = if (isOwedToYou) OpenSplitTokens.OwedPositive else OpenSplitTokens.OwedNegative
                        )
                    }

                    Column {
                        Text(
                            text = if (isOwedToYou) "Owed to You Breakdown" else "You Owe Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total: ${com.opensplit.util.CurrencyFormatter.format(totalAmount, primaryCurrency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOwedToYou) OpenSplitTokens.OwedPositive else OpenSplitTokens.OwedNegative,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(OpenSplitIcons.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))

            // Standard Material 3 SecondaryTabRow
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "By Group (${relevantGroups.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = OpenSplitIcons.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "By Friend (${relevantFriends.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = OpenSplitIcons.Friends,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

            if (selectedTab == 0) {
                // BY GROUP TAB CONTENT
                if (relevantGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OpenSplitTokens.SpaceXL),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isOwedToYou) "No groups currently owe you money." else "You are all settled up in all groups!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = if (isOwedToYou) "Groups where members owe you:" else "Groups where you have pending dues:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = OpenSplitTokens.SpaceSM)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(relevantGroups) { groupWithBal ->
                            val group = groupWithBal.group
                            val bal = groupWithBal.balance
                            val formattedBal = com.opensplit.util.CurrencyFormatter.format(kotlin.math.abs(bal), group.currency)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        onDismiss()
                                        onNavigateToGroup(group.id)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(OpenSplitTokens.SpaceMD),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        GroupAvatar(
                                            name = group.name,
                                            avatarKey = group.avatarKey,
                                            size = 44.dp
                                        )

                                        Column {
                                            Text(
                                                text = group.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${group.memberIds.size} members • ${group.currency}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isOwedToYou) "+$formattedBal" else "-$formattedBal",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOwedToYou) OpenSplitTokens.OwedPositive else OpenSplitTokens.OwedNegative
                                        )
                                        if (!isOwedToYou) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            FilledTonalButton(
                                                onClick = {
                                                    onDismiss()
                                                    onNavigateToSettleUp(group.id)
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("Settle Up", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // BY FRIEND TAB CONTENT
                if (relevantFriends.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OpenSplitTokens.SpaceXL),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isOwedToYou) "No friends currently owe you money." else "You are all settled up with individual friends!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = if (isOwedToYou) "Friends who owe you:" else "Friends you owe money to:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = OpenSplitTokens.SpaceSM)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(relevantFriends) { (friendBal, netAmt) ->
                            val user = friendBal.user
                            val absAmt = kotlin.math.abs(netAmt)
                            val formattedAmt = com.opensplit.util.CurrencyFormatter.format(absAmt, primaryCurrency)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        onDismiss()
                                        onNavigateToPersonBalance?.invoke(user.uid)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(OpenSplitTokens.SpaceMD),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        com.opensplit.ui.components.UserAvatar(
                                            photoUrl = user.photoUrl,
                                            displayName = user.displayName,
                                            size = 44.dp
                                        )

                                        Column {
                                            Text(
                                                text = user.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (user.email.isNotBlank()) {
                                                Text(
                                                    text = user.email,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isOwedToYou) "+$formattedAmt" else "-$formattedAmt",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOwedToYou) OpenSplitTokens.OwedPositive else OpenSplitTokens.OwedNegative
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        FilledTonalButton(
                                            onClick = {
                                                onDismiss()
                                                onNavigateToPersonBalance?.invoke(user.uid)
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text(if (!isOwedToYou) "Settle Up" else "View", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXL))
        }
    }
}
