package com.opensplit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.domain.model.RecurrenceFrequency
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.components.appHazeHeader
import com.opensplit.ui.components.appHazeSource
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import dev.chrisbanes.haze.HazeState

data class DummyRecurringExpense(
    val id: String,
    val title: String,
    val amount: Double,
    val currency: String,
    val frequency: RecurrenceFrequency,
    val nextDate: String,
    val groupName: String,
    val isPaused: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    onNavigateBack: () -> Unit
) {
    val snackbar = LocalSnackbarController.current
    val hazeState = remember { HazeState() }

    val recurringList = remember {
        mutableStateListOf(
            DummyRecurringExpense("rec1", "Internet Bill", 1299.0, "INR", RecurrenceFrequency.MONTHLY, "Sep 5, 2026", "Apartment 302", false),
            DummyRecurringExpense("rec2", "Netflix & Spotify", 649.0, "INR", RecurrenceFrequency.MONTHLY, "Sep 12, 2026", "Apartment 302", false),
            DummyRecurringExpense("rec3", "Weekly Groceries", 2500.0, "INR", RecurrenceFrequency.WEEKLY, "Aug 27, 2026", "Home", true)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring Expenses", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(OpenSplitIcons.Back, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                ),
                modifier = Modifier.appHazeHeader(hazeState)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { snackbar.showMessage("Select a group and expense to make it recurring.") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                Icon(OpenSplitIcons.AddExpense, contentDescription = "Add Recurring Expense")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .appHazeSource(hazeState)
                .padding(horizontal = OpenSplitTokens.SpaceLG)
        ) {
            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))

            Text(
                text = "Automatically remind or split regular expenses like rent, subscriptions, and utility bills.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

            if (recurringList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recurring expenses set up yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(recurringList, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isPaused) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(modifier = Modifier.padding(OpenSplitTokens.SpaceMD)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${item.groupName} • ${item.frequency.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = com.opensplit.util.CurrencyFormatter.format(item.amount, item.currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (item.isPaused) "Paused" else "Next: ${item.nextDate}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (item.isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)) {
                                        TextButton(
                                            onClick = {
                                                val index = recurringList.indexOfFirst { it.id == item.id }
                                                if (index >= 0) {
                                                    val updated = item.copy(isPaused = !item.isPaused)
                                                    recurringList[index] = updated
                                                    snackbar.showMessage(if (updated.isPaused) "Paused ${item.title}" else "Resumed ${item.title}")
                                                }
                                            }
                                        ) {
                                            Text(if (item.isPaused) "Resume" else "Pause")
                                        }

                                        IconButton(
                                            onClick = {
                                                recurringList.removeIf { it.id == item.id }
                                                snackbar.showMessage("Deleted ${item.title}")
                                            }
                                        ) {
                                            Icon(
                                                imageVector = OpenSplitIcons.Close,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
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
    }
}
