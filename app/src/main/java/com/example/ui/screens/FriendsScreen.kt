package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.HandshakeIllustration
import com.example.ui.components.StateLayout
import com.example.ui.components.getBalanceColor
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: MainViewModel,
    onFriendClick: (String) -> Unit
) {
    val friendsState by viewModel.friendsBalances.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        StateLayout(state = friendsState) { balances ->
            if (balances.isEmpty()) {
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
                        HandshakeIllustration(size = 130.dp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "You're all settled up!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search friends...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sort by balance magnitude descending by default
                    val processedBalances = remember(balances, searchQuery) {
                        balances.filter {
                            it.user.displayName.contains(searchQuery, ignoreCase = true) ||
                                    it.user.email.contains(searchQuery, ignoreCase = true)
                        }.sortedByDescending { abs(it.balance) }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(processedBalances) { fb ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFriendClick(fb.user.uid) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
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

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fb.user.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = fb.user.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    val bal = fb.balance
                                    val color = getBalanceColor(bal)
                                    val formattedVal = String.format("%.2f", abs(bal))
                                    val text = if (bal > 0.01) {
                                        "Owes you\n$$formattedVal"
                                    } else if (bal < -0.01) {
                                        "You owe\n$$formattedVal"
                                    } else {
                                        "Settled up"
                                    }

                                    Text(
                                        text = text,
                                        color = color,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End
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
