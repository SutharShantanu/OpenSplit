@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.components.FilterChip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.LocalSnackbarController

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.opensplit.domain.model.FriendInvite
import com.opensplit.ui.components.AppSearchBar
import com.opensplit.ui.components.ExpressiveEmptyState
import com.opensplit.ui.components.ExpressiveScrollBar
import androidx.compose.foundation.lazy.rememberLazyListState
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.components.responsiveContentWidth
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.MainViewModel
import kotlin.math.abs

enum class FriendFilterOption(val label: String) {
    ALL("All"),
    OWED_TO_YOU("Owed to you"),
    YOU_OWE("You owe"),
    SETTLED("Settled")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: MainViewModel,
    onAddFriend: () -> Unit = {},
    onFriendClick: (String) -> Unit,
    onAddExpense: (groupId: String) -> Unit = {},
    onSettleUp: (groupId: String) -> Unit = {}
) {
    val busy = com.opensplit.ui.components.LocalBusy.current
    val friendsState by viewModel.friendsBalances.collectAsStateWithLifecycle()
    val friendInvites by viewModel.friendInvites.collectAsStateWithLifecycle()
    val incomingInvites by viewModel.incomingFriendInvites.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(FriendFilterOption.ALL) }
    val myLink by viewModel.friendLink.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.ensureFriendLink() }
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .responsiveContentWidth(maxWidth = 860.dp)
                .fillMaxHeight()
                .padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceMD)
        ) {
        // Friend requests addressed to me
        if (incomingInvites.isNotEmpty()) {
            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
            Text(
                text = "Friend requests (${incomingInvites.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
                Column {
                    incomingInvites.forEachIndexed { index, invite ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            headlineContent = { Text(invite.inviterName.ifBlank { "Someone" }, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("wants to split expenses with you", style = MaterialTheme.typography.bodySmall) },
                            trailingContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = {
                                        viewModel.respondToFriendInvite(invite, accept = false) { snackbar.showMessage("Request ignored") }
                                    }) { Text("Ignore") }
                                    Button(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = {
                                        viewModel.respondToFriendInvite(invite, accept = true) { ok ->
                                            snackbar.showMessage(if (ok) "You're now friends with ${invite.inviterName.ifBlank { "them" }}" else "Couldn't accept")
                                        }
                                    }) { Text("Accept") }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Invites you've sent: your live link (with its expiry) and any older email invites with their status.
        val openInvites = friendInvites.filter { it.status != "accepted" && it.status != "declined" }
        if (myLink != null || openInvites.isNotEmpty()) {
            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
            Text(
                text = "Invites",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column {
                    myLink?.let { link ->
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            leadingContent = { SymbolIcon(OpenSplitIcons.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            headlineContent = { Text("Your invite link", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text(expiryLabel(link.expiresAt) + " · friends who join appear above", style = MaterialTheme.typography.bodySmall) },
                            trailingContent = { TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = onAddFriend) { Text("Share") } },
                            modifier = Modifier.clickable(onClick = onAddFriend)
                        )
                    }
                    openInvites.forEach { invite ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        InviteRow(invite = invite, onRevoke = {
                            viewModel.revokeFriendInvite(invite.id)
                            snackbar.showMessage(if (invite.expiresAt.seconds <= System.currentTimeMillis() / 1000) "Expired invite removed" else "Invite to ${invite.email} revoked")
                        })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

        StateLayout(state = friendsState) { balances ->
            if (balances.isEmpty()) {
                // No entrance animation of its own: the tab slide already brings it in.
                ExpressiveEmptyState(
                    title = "No friends yet",
                    subtitle = "Add friends to split one-on-one expenses without creating a group.",
                    symbol = OpenSplitIcons.PersonAdd,
                    actionLabel = "Add a Friend",
                    onActionClick = onAddFriend
                )
            } else {
                val friendsListState = rememberLazyListState()
                Column(modifier = Modifier.fillMaxSize()) {
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholderText = "Search friends..."
                    )

                    // Overall Balance Summary Card
                    val netTotal = remember(balances) {
                        balances.sumOf { fb ->
                            fb.nonZeroBalances.sumOf { it.second }
                        }
                    }
                    val formattedNet = remember(netTotal) {
                        val sign = if (netTotal > 0.01) "+" else if (netTotal < -0.01) "-" else ""
                        val absVal = com.opensplit.util.CurrencyFormatter.format(abs(netTotal), showSymbol = true)
                        "$sign$absVal"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = OpenSplitTokens.SpaceMD),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(OpenSplitTokens.SpaceLG)
                        ) {
                            Text(
                                text = "Total Balance",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formattedNet,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (netTotal > 0.01) OpenSplitTokens.OwedPositive else if (netTotal < -0.01) OpenSplitTokens.OwedNegative else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (netTotal > 0.01) "You are owed in total" else if (netTotal < -0.01) "You owe in total" else "All settled up",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FriendFilterOption.values().forEach { option ->
                            FilterChip(
                                selected = selectedFilter == option,
                                onClick = { selectedFilter = option },
                                label = { Text(option.label) },
                                shape = MaterialTheme.shapes.extraLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                    Text(
                        text = "Your Friends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))

                    val processedBalances = remember(balances, searchQuery, selectedFilter) {
                        balances.filter { fb ->
                            val matchesSearch = fb.user.displayName.contains(searchQuery, ignoreCase = true) ||
                                    fb.user.email.contains(searchQuery, ignoreCase = true)
                            val matchesFilter = when (selectedFilter) {
                                FriendFilterOption.ALL -> true
                                FriendFilterOption.OWED_TO_YOU -> fb.owesYou
                                FriendFilterOption.YOU_OWE -> fb.youOwe
                                FriendFilterOption.SETTLED -> !fb.owesYou && !fb.youOwe
                            }
                            matchesSearch && matchesFilter
                        }.sortedByDescending { it.maxMagnitude }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = friendsListState,
                            verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS),
                            contentPadding = PaddingValues(bottom = com.opensplit.ui.components.FloatingNavBarClearance),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(processedBalances, key = { it.user.uid }) { fb ->
                                val lines = fb.nonZeroBalances
                                val hasOwed = lines.any { it.second > 0.01 }
                                val hasOwe = lines.any { it.second < -0.01 }
                                val color = when {
                                    hasOwed && !hasOwe -> OpenSplitTokens.OwedPositive
                                    hasOwe && !hasOwed -> OpenSplitTokens.OwedNegative
                                    else -> OpenSplitTokens.OwedNeutral
                                }
                                val text = if (lines.isEmpty()) {
                                    "Settled up"
                                } else {
                                    lines.joinToString("\n") { (currency, bal) ->
                                        val amt = com.opensplit.util.CurrencyFormatter.format(abs(bal), currency)
                                        if (bal > 0.01) "Owes you $amt" else "You owe $amt"
                                    }
                                }

                                val openDirect: (String, (String) -> Unit) -> Unit = { label, go ->
                                    busy.show(label)
                                    viewModel.openDirectGroup(fb.user.uid) { gId ->
                                        busy.hide()
                                        if (gId != null) go(gId) else snackbar.showMessage("Couldn't open a direct group with ${fb.user.displayName}")
                                    }
                                }
                                com.opensplit.ui.components.SwipeActionRow(
                                    modifier = Modifier.animateItem(),
                                    startAction = com.opensplit.ui.components.SwipeAction(
                                        icon = OpenSplitIcons.AddExpense, label = "Expense",
                                        container = MaterialTheme.colorScheme.primaryContainer,
                                        onContainer = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) { openDirect("Opening…", onAddExpense) },
                                    endAction = com.opensplit.ui.components.SwipeAction(
                                        icon = OpenSplitIcons.Settle, label = "Settle up",
                                        container = MaterialTheme.colorScheme.tertiaryContainer,
                                        onContainer = MaterialTheme.colorScheme.onTertiaryContainer
                                    ) { openDirect("Opening…", onSettleUp) }
                                ) {
                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onFriendClick(fb.user.uid) },
                                    headlineContent = {
                                        Text(
                                            text = fb.user.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = fb.user.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingContent = {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = fb.user.displayName.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        Text(
                                            text = text,
                                            color = color,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                        }

                        ExpressiveScrollBar(
                            listState = friendsListState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight(),
                            labelResolver = { index ->
                                processedBalances.getOrNull(index)?.user?.displayName?.firstOrNull()?.uppercaseChar()?.toString()
                            }
                        )
                    }
                }
            }
        }
    }

    }

}

@Composable
private fun InviteRow(invite: FriendInvite, onRevoke: () -> Unit) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        headlineContent = { Text(invite.email, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(
                text = expiryLabel(invite.expiresAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SymbolIcon(
                        OpenSplitIcons.Invite,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        },
        trailingContent = {
            val expired = invite.expiresAt.seconds <= System.currentTimeMillis() / 1000
            TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = onRevoke) {
                Text(if (expired) "Remove" else "Revoke", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

private fun expiryLabel(expiresAt: Timestamp): String {
    val nowSeconds = System.currentTimeMillis() / 1000
    val diff = expiresAt.seconds - nowSeconds
    return when {
        diff <= 0 -> "Expired"
        diff < 3600 -> "Expires in under an hour"
        diff < 86400 -> "Expires in ${diff / 3600}h"
        else -> "Valid for ${diff / 86400} more days"
    }
}
