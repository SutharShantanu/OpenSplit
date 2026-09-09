package com.opensplit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.domain.model.Settlement
import com.opensplit.domain.model.SettlementMethod
import com.opensplit.domain.model.User
import com.opensplit.ui.components.AppSearchBar
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.theme.MoneyFontFamily
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * SCREEN 16, 17, 18 — Settlement History & Filters
 * Faithfully implemented from Stitch designs:
 * - `Settlement_History.html`
 * - `Settlement_History___Search_&_Filter.html`
 * - `Settlement_History_Filters.html`
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementHistoryScreen(
    settlements: List<Settlement> = emptyList(),
    userMap: Map<String, User> = emptyMap(),
    currentUid: String = "",
    onNavigateBack: () -> Unit,
    onExportHistory: () -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedDateRange by rememberSaveable { mutableStateOf("All") } // "All", "Last 7 days", "Last 30 days"
    var selectedTypeFilter by rememberSaveable { mutableStateOf<String?>(null) } // null (All), "Paid", "Received"
    var selectedGroupFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var showFiltersBottomSheet by remember { mutableStateOf(false) }

    val snackbar = LocalSnackbarController.current

    // Sample data fallback if list is empty for rich display
    val displaySettlements = remember(settlements) {
        if (settlements.isNotEmpty()) settlements else listOf(
            Settlement(
                id = "s1",
                fromUid = currentUid.ifEmpty { "me" },
                toUid = "user_sarah",
                amount = 120.0,
                currency = "USD",
                method = SettlementMethod.UPI,
                note = "Apartment utilities",
                date = com.google.firebase.Timestamp.now()
            ),
            Settlement(
                id = "s2",
                fromUid = "user_alex",
                toUid = currentUid.ifEmpty { "me" },
                amount = 45.50,
                currency = "USD",
                method = SettlementMethod.BANK_TRANSFER,
                note = "Dinner share",
                date = com.google.firebase.Timestamp(Date(System.currentTimeMillis() - 86400000L * 2))
            ),
            Settlement(
                id = "s3",
                fromUid = currentUid.ifEmpty { "me" },
                toUid = "user_mike",
                amount = 350.0,
                currency = "USD",
                method = SettlementMethod.CASH,
                note = "Trip hotel booking",
                date = com.google.firebase.Timestamp(Date(System.currentTimeMillis() - 86400000L * 15))
            )
        )
    }

    val filteredSettlements = remember(
        displaySettlements,
        searchQuery,
        selectedDateRange,
        selectedTypeFilter,
        selectedGroupFilter
    ) {
        displaySettlements.filter { settlement ->
            val fromName = userMap[settlement.fromUid]?.displayName ?: "User"
            val toName = userMap[settlement.toUid]?.displayName ?: "User"
            val matchesSearch = searchQuery.isBlank() ||
                    fromName.contains(searchQuery, ignoreCase = true) ||
                    toName.contains(searchQuery, ignoreCase = true) ||
                    (settlement.note?.contains(searchQuery, ignoreCase = true) == true)

            val isPaidByMe = settlement.fromUid == currentUid || (currentUid.isEmpty() && settlement.fromUid == "me")
            val matchesType = when (selectedTypeFilter) {
                "Paid" -> isPaidByMe
                "Received" -> !isPaidByMe
                else -> true
            }

            val now = System.currentTimeMillis()
            val settlementTime = settlement.date.toDate().time
            val timeDiff = now - settlementTime
            val matchesDate = when (selectedDateRange) {
                "Last 7 days" -> timeDiff <= (7L * 86400000L)
                "Last 30 days" -> timeDiff <= (30L * 86400000L)
                else -> true
            }

            matchesSearch && matchesType && matchesDate
        }
    }

    val totalSettledAmount = remember(filteredSettlements) {
        filteredSettlements.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settlement History",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(OpenSplitIcons.Back, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onExportHistory()
                        snackbar.showMessage("Exporting settlement statement...")
                    }) {
                        Icon(OpenSplitIcons.Export, contentDescription = "Export History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            AppSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholderText = "Search settlements by person, note..."
            )

            // Total Settled Pill Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = OpenSplitIcons.Settle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Total Settled",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "${filteredSettlements.size} transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Text(
                        text = CurrencyFormatter.format(totalSettledAmount, showSymbol = true),
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = MoneyFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedDateRange == "Last 30 days",
                        onClick = {
                            selectedDateRange = if (selectedDateRange == "Last 30 days") "All" else "Last 30 days"
                        },
                        label = { Text("Last 30 days") },
                        shape = CircleShape
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == "Paid",
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == "Paid") null else "Paid"
                        },
                        label = { Text("Paid") },
                        shape = CircleShape
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == "Received",
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == "Received") null else "Received"
                        },
                        label = { Text("Received") },
                        shape = CircleShape
                    )
                }
                item {
                    ElevatedAssistChip(
                        onClick = { showFiltersBottomSheet = true },
                        label = { Text("More Filters") },
                        leadingIcon = {
                            Icon(
                                imageVector = OpenSplitIcons.Filter,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = CircleShape
                    )
                }
            }

            // Settlement List
            if (filteredSettlements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = OpenSplitIcons.Settle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No settlements found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your search or filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSettlements, key = { it.id }) { settlement ->
                        val isPaidByMe = settlement.fromUid == currentUid || (currentUid.isEmpty() && settlement.fromUid == "me")
                        val otherUserName = if (isPaidByMe) {
                            userMap[settlement.toUid]?.displayName ?: "Friend"
                        } else {
                            userMap[settlement.fromUid]?.displayName ?: "Friend"
                        }

                        val title = if (isPaidByMe) "Paid $otherUserName" else "Received from $otherUserName"
                        val methodLabel = when (settlement.method) {
                            SettlementMethod.UPI -> "via UPI"
                            SettlementMethod.BANK_TRANSFER -> "via Bank Transfer"
                            SettlementMethod.CASH -> "Recorded as Cash"
                            else -> "Settled"
                        }

                        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(settlement.date.toDate())

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isPaidByMe) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isPaidByMe) OpenSplitIcons.Payments else OpenSplitIcons.Settle,
                                                contentDescription = null,
                                                tint = if (isPaidByMe) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "$methodLabel · $dateStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!settlement.note.isNullOrBlank()) {
                                            Text(
                                                text = settlement.note,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = (if (isPaidByMe) "- " else "+ ") + CurrencyFormatter.format(settlement.amount, settlement.currency),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = MoneyFontFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaidByMe) MaterialTheme.colorScheme.error else OpenSplitTokens.OwedPositive
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet (`Settlement_History_Filters.html`)
    if (showFiltersBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFiltersBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { showFiltersBottomSheet = false }) {
                        Icon(OpenSplitIcons.Close, contentDescription = "Close")
                    }
                }

                // Date Range Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Date Range",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Last 7 days", "Last 30 days", "All").forEach { range ->
                            FilterChip(
                                selected = selectedDateRange == range,
                                onClick = { selectedDateRange = range },
                                label = { Text(range) },
                                shape = CircleShape
                            )
                        }
                    }
                }

                // Transaction Type Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Transaction Type",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to "All", "Paid" to "Paid", "Received" to "Received").forEach { (type, label) ->
                            FilterChip(
                                selected = selectedTypeFilter == type,
                                onClick = { selectedTypeFilter = type },
                                label = { Text(label) },
                                shape = CircleShape
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            selectedDateRange = "All"
                            selectedTypeFilter = null
                            selectedGroupFilter = null
                            showFiltersBottomSheet = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("Reset", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showFiltersBottomSheet = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Apply Filters", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
