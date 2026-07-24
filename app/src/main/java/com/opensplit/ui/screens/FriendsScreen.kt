package com.opensplit.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.google.firebase.Timestamp
import com.opensplit.domain.model.FriendInvite
import com.opensplit.ui.components.AppSearchBar
import com.opensplit.ui.components.HandshakeIllustration
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.MainViewModel
import kotlin.math.abs

enum class FriendFilterOption(val label: String) {
    ALL("All"),
    OWED_TO_YOU("Owed to you"),
    YOU_OWE("You owe")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: MainViewModel,
    onFriendClick: (String) -> Unit
) {
    val friendsState by viewModel.friendsBalances.collectAsState()
    val friendInvites by viewModel.friendInvites.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(FriendFilterOption.ALL) }
    var showInviteDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceMD)
    ) {
        // Invite a friend
        Button(
            onClick = { showInviteDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(OpenSplitIcons.Invite, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
            Text("Invite a friend")
        }

        // Pending invites
        if (friendInvites.isNotEmpty()) {
            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
            Text(
                text = "Pending invites (${friendInvites.size})",
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
                    friendInvites.forEachIndexed { index, invite ->
                        InviteRow(invite = invite, onRevoke = { viewModel.revokeFriendInvite(invite.id) })
                        if (index < friendInvites.lastIndex) HorizontalDivider()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

        StateLayout(state = friendsState) { balances ->
            if (balances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(OpenSplitTokens.SpaceXL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        HandshakeIllustration(size = 120.dp)
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
                        Text(
                            text = "You're all settled up!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                        Text(
                            text = "Invite a friend, or add an expense in a group to start tracking balances.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholderText = "Search friends..."
                    )

                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

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

                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))

                    val processedBalances = remember(balances, searchQuery, selectedFilter) {
                        balances.filter { fb ->
                            val matchesSearch = fb.user.displayName.contains(searchQuery, ignoreCase = true) ||
                                    fb.user.email.contains(searchQuery, ignoreCase = true)
                            val matchesFilter = when (selectedFilter) {
                                FriendFilterOption.ALL -> true
                                FriendFilterOption.OWED_TO_YOU -> fb.owesYou
                                FriendFilterOption.YOU_OWE -> fb.youOwe
                            }
                            matchesSearch && matchesFilter
                        }.sortedByDescending { it.maxMagnitude }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS),
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

                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onFriendClick(fb.user.uid) },
                                headlineContent = {
                                    Text(
                                        text = fb.user.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = fb.user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        textAlign = TextAlign.End
                                    )
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        var email by rememberSaveable { mutableStateOf("") }
        val emailValid = email.trim().contains("@") && email.trim().contains(".")
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            icon = { Icon(OpenSplitIcons.Invite, contentDescription = null) },
            title = { Text("Invite a friend") },
            text = {
                Column {
                    Text("They'll be able to join once they sign up with this email. The invite expires in 7 days.")
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendFriendInvite(email) { success ->
                            Toast.makeText(
                                context,
                                if (success) "Invite sent" else "Couldn't send (already invited or invalid email)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showInviteDialog = false
                    },
                    enabled = emailValid
                ) { Text("Send invite") }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Cancel") }
            }
        )
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
                    Icon(
                        OpenSplitIcons.Invite,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        },
        trailingContent = {
            TextButton(onClick = onRevoke) {
                Text("Revoke", color = MaterialTheme.colorScheme.error)
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
        else -> "Expires in ${diff / 86400}d"
    }
}
