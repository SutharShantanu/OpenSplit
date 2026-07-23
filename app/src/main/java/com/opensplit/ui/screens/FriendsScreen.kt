package com.opensplit.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(FriendFilterOption.ALL) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        HandshakeIllustration(size = 140.dp)
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXL))
                        Text(
                            text = "You're all settled up!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                        Text(
                            text = "No pending balances with any friends.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceMD)
                ) {
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholderText = "Search friends..."
                    )

                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                    // Filter Chips Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FriendFilterOption.values().forEach { option ->
                            val isSelected = selectedFilter == option
                            FilterChip(
                                selected = isSelected,
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
                            // Show one line per currency (never sum across currencies).
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
}

