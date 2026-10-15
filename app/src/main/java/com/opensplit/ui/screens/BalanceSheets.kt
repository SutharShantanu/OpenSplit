@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.components.SymbolIcon
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
import com.opensplit.ui.components.GroupAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GroupSelectionDialog(
    title: String,
    groups: List<com.opensplit.domain.model.Group>,
    onDismiss: () -> Unit,
    onSelectGroup: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { SymbolIcon(OpenSplitIcons.Groups, contentDescription = null) },
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
                            SymbolIcon(
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
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) {
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
                        SymbolIcon(
                            symbol = if (isOwedToYou) OpenSplitIcons.ArrowUpward else OpenSplitIcons.ArrowDownward,
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

                IconButton(shapes = IconButtonDefaults.shapes(), onClick = onDismiss) {
                    SymbolIcon(OpenSplitIcons.Close, contentDescription = "Close")
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
                        SymbolIcon(
                            symbol = OpenSplitIcons.Groups,
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
                        SymbolIcon(
                            symbol = OpenSplitIcons.Friends,
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
                                            FilledTonalButton(shapes = ButtonDefaults.shapes(), 
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
                                        FilledTonalButton(shapes = ButtonDefaults.shapes(), 
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
