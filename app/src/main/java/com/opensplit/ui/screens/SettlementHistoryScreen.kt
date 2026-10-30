@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.components.FilterChip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opensplit.data.export.CsvExportGenerator
import com.opensplit.data.export.ExportUtils
import com.opensplit.data.export.JsonExportGenerator
import com.opensplit.data.export.PdfExportGenerator
import com.opensplit.domain.model.Settlement
import com.opensplit.domain.model.SettlementMethod
import com.opensplit.domain.model.User
import com.opensplit.domain.model.toDisplayName
import com.opensplit.ui.components.AppLoadingIndicator
import com.opensplit.ui.components.AppSearchBar
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.responsiveContentWidth
import com.opensplit.ui.theme.MoneyFontFamily
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettlementReportBasis(val title: String, val subtitle: String) {
    CHRONOLOGICAL("Timeline", "Sequential log of all settlements"),
    BY_PERSON("By Person", "Grouped totals per settled person"),
    BY_METHOD("By Method", "Totals broken down by UPI, Cash & Bank")
}

enum class SettlementReportType(val label: String) {
    ALL("All Settlements"),
    PAID_ONLY("Money Sent (Paid)"),
    RECEIVED_ONLY("Money Received")
}

enum class SettlementReportPeriod(val label: String, val days: Long?) {
    ALL("All Time", null),
    DAYS_30("Last 30 Days", 30L),
    DAYS_90("Last 90 Days", 90L),
    THIS_YEAR("This Year", 365L)
}

enum class SettlementExportDocType(val title: String, val ext: String, val mime: String) {
    PDF("PDF Statement", ".pdf", "application/pdf"),
    CSV("CSV Spreadsheet", ".csv", "text/csv"),
    JSON("JSON Raw Backup", ".json", "application/json")
}

/**
 * SCREEN 16, 17, 18 — Revamped Settlement History & Export Hub
 * Material Design 3 Expressive compliant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementHistoryScreen(
    settlements: List<Settlement> = emptyList(),
    userMap: Map<String, User> = emptyMap(),
    currentUid: String = "",
    defaultCurrency: String = "INR",
    onNavigateBack: () -> Unit,
    onExportHistory: () -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedDateRange by rememberSaveable { mutableStateOf("All") } // "All", "Last 30 days", "Last 90 days"
    var selectedTypeFilter by rememberSaveable { mutableStateOf<String?>(null) } // null (All), "Paid", "Received"
    var selectedMethodFilter by rememberSaveable { mutableStateOf<SettlementMethod?>(null) }
    var showExportBottomSheet by rememberSaveable { mutableStateOf(false) }
    var inspectSettlement by remember { mutableStateOf<Settlement?>(null) }

    val resolvedUserMap = userMap
    val displaySettlements = settlements

    val filteredSettlements = remember(
        displaySettlements,
        searchQuery,
        selectedDateRange,
        selectedTypeFilter,
        selectedMethodFilter,
        resolvedUserMap
    ) {
        displaySettlements.filter { settlement ->
            val fromName = resolvedUserMap[settlement.fromUid]?.displayName ?: settlement.fromUid
            val toName = resolvedUserMap[settlement.toUid]?.displayName ?: settlement.toUid
            val matchesSearch = searchQuery.isBlank() ||
                    fromName.contains(searchQuery, ignoreCase = true) ||
                    toName.contains(searchQuery, ignoreCase = true) ||
                    (settlement.note?.contains(searchQuery, ignoreCase = true) == true) ||
                    settlement.method.name.contains(searchQuery, ignoreCase = true)

            val isPaidByMe = settlement.fromUid == currentUid || (currentUid.isEmpty() && settlement.fromUid == "me")
            val matchesType = when (selectedTypeFilter) {
                "Paid" -> isPaidByMe
                "Received" -> !isPaidByMe
                else -> true
            }

            val matchesMethod = selectedMethodFilter == null || settlement.method == selectedMethodFilter

            val now = System.currentTimeMillis()
            val settlementTime = settlement.date.toDate().time
            val timeDiff = now - settlementTime
            val matchesDate = when (selectedDateRange) {
                "Last 30 days" -> timeDiff <= (30L * 86400000L)
                "Last 90 days" -> timeDiff <= (90L * 86400000L)
                else -> true
            }

            matchesSearch && matchesType && matchesMethod && matchesDate
        }
    }

    val primaryCurrency = remember(filteredSettlements, displaySettlements) {
        filteredSettlements.firstOrNull()?.currency
            ?: displaySettlements.firstOrNull()?.currency
            ?: defaultCurrency
    }
    // Totals are shown in one currency, so only settlements in that currency count.
    val sameCurrency = remember(filteredSettlements, primaryCurrency) {
        filteredSettlements.filter { it.currency == primaryCurrency }
    }

    val totalSettledAmount = remember(sameCurrency) {
        sameCurrency.sumOf { it.amount }
    }

    val totalPaidOut = remember(sameCurrency, currentUid) {
        sameCurrency.filter { it.fromUid == currentUid || (currentUid.isEmpty() && it.fromUid == "me") }
            .sumOf { it.amount }
    }

    val totalReceived = remember(sameCurrency, currentUid) {
        sameCurrency.filter { it.toUid == currentUid || (currentUid.isNotEmpty() && it.fromUid != currentUid) || (currentUid.isEmpty() && it.fromUid != "me") }
            .sumOf { it.amount }
    }

    val hasActiveFilters = selectedDateRange != "All" || selectedTypeFilter != null || selectedMethodFilter != null || searchQuery.isNotBlank()

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
                    com.opensplit.ui.components.RoundBackButton(onClick = onNavigateBack)
                },
                actions = {
                    // Named 'Export' button using distinct M3 FilledTonalButton
                    FilledTonalButton(
                        onClick = { showExportBottomSheet = true },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.Export,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .responsiveContentWidth(maxWidth = 860.dp)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // Search Bar
            AppSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholderText = "Search by person, note, method..."
            )

            // Revamped Hero Overview Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                                    SymbolIcon(
                                        symbol = OpenSplitIcons.Settle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Total Settlement Volume",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${filteredSettlements.size} transactions settled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Currency tag badge
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = primaryCurrency,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Large Total Number
                    Text(
                        text = CurrencyFormatter.format(totalSettledAmount, primaryCurrency, showSymbol = true),
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MoneyFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )

                    // 2-Column Submetrics Breakdown (Paid Out vs Received)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Paid Out Metric
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        SymbolIcon(
                                            symbol = OpenSplitIcons.ArrowUpward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Paid Out",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "-${CurrencyFormatter.format(totalPaidOut, primaryCurrency, showSymbol = true)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MoneyFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // Received Metric
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = OpenSplitTokens.OwedPositive.copy(alpha = 0.15f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        SymbolIcon(
                                            symbol = OpenSplitIcons.ArrowDownward,
                                            contentDescription = null,
                                            tint = OpenSplitTokens.OwedPositive,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Received",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "+${CurrencyFormatter.format(totalReceived, primaryCurrency, showSymbol = true)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MoneyFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = OpenSplitTokens.OwedPositive
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("All") },
                        shape = CircleShape
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == "Paid",
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == "Paid") null else "Paid"
                        },
                        leadingIcon = if (selectedTypeFilter == "Paid") {
                            { SymbolIcon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        label = { Text("Paid Out") },
                        shape = CircleShape
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == "Received",
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == "Received") null else "Received"
                        },
                        leadingIcon = if (selectedTypeFilter == "Received") {
                            { SymbolIcon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        label = { Text("Received") },
                        shape = CircleShape
                    )
                }
                item {
                    FilterChip(
                        selected = selectedDateRange == "Last 30 days",
                        onClick = {
                            selectedDateRange = if (selectedDateRange == "Last 30 days") "All" else "Last 30 days"
                        },
                        label = { Text("30 Days") },
                        shape = CircleShape
                    )
                }
                item {
                    FilterChip(
                        selected = selectedDateRange == "Last 90 days",
                        onClick = {
                            selectedDateRange = if (selectedDateRange == "Last 90 days") "All" else "Last 90 days"
                        },
                        label = { Text("90 Days") },
                        shape = CircleShape
                    )
                }
                // Payment Method chips
                SettlementMethod.values().filter { it != SettlementMethod.OTHER }.forEach { method ->
                    item {
                        FilterChip(
                            selected = selectedMethodFilter == method,
                            onClick = {
                                selectedMethodFilter = if (selectedMethodFilter == method) null else method
                            },
                            label = { Text(method.toDisplayName()) },
                            shape = CircleShape
                        )
                    }
                }

                if (hasActiveFilters) {
                    item {
                        AssistChip(
                            onClick = {
                                searchQuery = ""
                                selectedDateRange = "All"
                                selectedTypeFilter = null
                                selectedMethodFilter = null
                            },
                            label = { Text("Reset") },
                            leadingIcon = {
                                SymbolIcon(OpenSplitIcons.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            shape = CircleShape
                        )
                    }
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SymbolIcon(
                                    symbol = OpenSplitIcons.Settle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = "No settlements found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hasActiveFilters) "Try adjusting or clearing your filters." else "Settle up an expense to see history here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSettlements, key = { it.id }) { settlement ->
                        val isPaidByMe = settlement.fromUid == currentUid || (currentUid.isEmpty() && settlement.fromUid == "me")
                        val otherUserName = if (isPaidByMe) {
                            resolvedUserMap[settlement.toUid]?.displayName ?: "Friend (${settlement.toUid.take(6)})"
                        } else {
                            resolvedUserMap[settlement.fromUid]?.displayName ?: "Friend (${settlement.fromUid.take(6)})"
                        }

                        val title = if (isPaidByMe) "Paid $otherUserName" else "Received from $otherUserName"
                        val methodLabel = settlement.method.toDisplayName()
                        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(settlement.date.toDate())

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inspectSettlement = settlement },
                            shape = MaterialTheme.shapes.medium,
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
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Avatar with Direction Badge
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        com.opensplit.ui.components.UserAvatar(
                                            photoUrl = resolvedUserMap[if (isPaidByMe) settlement.toUid else settlement.fromUid]?.photoUrl,
                                            displayName = otherUserName,
                                            size = 46.dp
                                        )

                                        // Direction Pill Overlay
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isPaidByMe) MaterialTheme.colorScheme.error else OpenSplitTokens.OwedPositive,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                SymbolIcon(
                                                    symbol = if (isPaidByMe) OpenSplitIcons.ArrowUpward else OpenSplitIcons.ArrowDownward,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Method Badge
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                                            ) {
                                                Text(
                                                    text = methodLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "· $dateStr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (!settlement.note.isNullOrBlank()) {
                                            Text(
                                                text = settlement.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Amount
                                Text(
                                    text = (if (isPaidByMe) "- " else "+ ") + CurrencyFormatter.format(settlement.amount, settlement.currency, showSymbol = true),
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
}

    // Settlement Details Dialog
    inspectSettlement?.let { item ->
        val isPaidByMe = item.fromUid == currentUid || (currentUid.isEmpty() && item.fromUid == "me")
        val otherUserName = if (isPaidByMe) {
            resolvedUserMap[item.toUid]?.displayName ?: item.toUid
        } else {
            resolvedUserMap[item.fromUid]?.displayName ?: item.fromUid
        }

        AlertDialog(
            onDismissRequest = { inspectSettlement = null },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = if (isPaidByMe) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SymbolIcon(
                            symbol = if (isPaidByMe) OpenSplitIcons.ArrowUpward else OpenSplitIcons.ArrowDownward,
                            contentDescription = null,
                            tint = if (isPaidByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = if (isPaidByMe) "Payment to $otherUserName" else "Payment from $otherUserName",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = (if (isPaidByMe) "- " else "+ ") + CurrencyFormatter.format(item.amount, item.currency, showSymbol = true),
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MoneyFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = if (isPaidByMe) MaterialTheme.colorScheme.error else OpenSplitTokens.OwedPositive
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Method", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.method.toDisplayName(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(item.date.toDate()),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!item.note.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Note", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.note, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Transaction ID", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.id.ifEmpty { "Local TX" }, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(onClick = { inspectSettlement = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Rich Settlement Report Customizer Bottom Sheet
    if (showExportBottomSheet) {
        SettlementReportBottomSheet(
            settlements = displaySettlements,
            currentUid = currentUid,
            userMap = resolvedUserMap,
            onDismiss = { showExportBottomSheet = false }
        )
    }
}

/**
 * Rich Settlement Report Configuration Bottom Sheet
 * Allows user to configure:
 * - Basis / Grouping: Timeline, By Contact, By Payment Method
 * - Scope: All, Paid Only, Received Only
 * - Date Period: All Time, 30 Days, 90 Days, This Year
 * - Document Format: PDF, CSV, JSON
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementReportBottomSheet(
    settlements: List<Settlement>,
    currentUid: String,
    userMap: Map<String, User>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    val coroutineScope = rememberCoroutineScope()

    var selectedBasis by rememberSaveable { mutableStateOf(SettlementReportBasis.CHRONOLOGICAL) }
    var selectedScope by rememberSaveable { mutableStateOf(SettlementReportType.ALL) }
    var selectedPeriod by rememberSaveable { mutableStateOf(SettlementReportPeriod.ALL) }
    var selectedFormat by rememberSaveable { mutableStateOf(SettlementExportDocType.PDF) }
    var isExporting by remember { mutableStateOf(false) }

    // Filter settlements based on chosen options
    val exportSettlements = remember(settlements, selectedScope, selectedPeriod, currentUid) {
        val now = System.currentTimeMillis()
        settlements.filter { s ->
            val isPaidByMe = s.fromUid == currentUid || (currentUid.isEmpty() && s.fromUid == "me")
            val matchesScope = when (selectedScope) {
                SettlementReportType.ALL -> true
                SettlementReportType.PAID_ONLY -> isPaidByMe
                SettlementReportType.RECEIVED_ONLY -> !isPaidByMe
            }

            val timeDiff = now - s.date.toDate().time
            val matchesPeriod = selectedPeriod.days?.let { days ->
                timeDiff <= (days * 86400000L)
            } ?: true

            matchesScope && matchesPeriod
        }
    }

    val totalVolume = remember(exportSettlements) { exportSettlements.sumOf { it.amount } }
    val primaryCurrency = remember(exportSettlements) { exportSettlements.firstOrNull()?.currency ?: "USD" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 640.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Export Settlement Report",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Configure basis, transaction scope, and format",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(shapes = com.opensplit.ui.theme.appIconButtonShapes(), onClick = onDismiss) {
                    SymbolIcon(OpenSplitIcons.Close, contentDescription = "Close")
                }
            }

            // 1. Report Basis / Grouping
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Report Basis (Grouping)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettlementReportBasis.values().forEach { basis ->
                        val isSelected = selectedBasis == basis
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable { selectedBasis = basis },
                            shape = MaterialTheme.shapes.small,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = basis.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 2. Transaction Scope
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Transaction Scope",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettlementReportType.values().forEach { scope ->
                        FilterChip(
                            selected = selectedScope == scope,
                            onClick = { selectedScope = scope },
                            label = { Text(scope.label) },
                            shape = CircleShape
                        )
                    }
                }
            }

            // 3. Time Period
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Period",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettlementReportPeriod.values().forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.label) },
                            shape = CircleShape
                        )
                    }
                }
            }

            // 4. File Format
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Export Format",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettlementExportDocType.values().forEach { format ->
                        val isSelected = selectedFormat == format
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFormat = format },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SymbolIcon(
                                    symbol = when (format) {
                                        SettlementExportDocType.PDF -> OpenSplitIcons.PictureAsPdf
                                        SettlementExportDocType.CSV -> OpenSplitIcons.TableChart
                                        SettlementExportDocType.JSON -> OpenSplitIcons.Code
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = format.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Live Scope Preview Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Scope Summary",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${exportSettlements.size} settlements included",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = CurrencyFormatter.format(totalVolume, primaryCurrency, showSymbol = true),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MoneyFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = CircleShape,
                    enabled = !isExporting
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (exportSettlements.isEmpty()) {
                            snackbar.showMessage("No settlements match the selected criteria")
                            return@Button
                        }
                        isExporting = true
                        coroutineScope.launch {
                            try {
                                val scopeTitle = "Settlements_${selectedBasis.name}_${selectedPeriod.name}"
                                val file = when (selectedFormat) {
                                    SettlementExportDocType.PDF -> {
                                        PdfExportGenerator().generate(
                                            context = context,
                                            scopeName = scopeTitle,
                                            expenses = emptyList(),
                                            settlements = exportSettlements
                                        )
                                    }
                                    SettlementExportDocType.CSV -> {
                                        CsvExportGenerator().generate(
                                            context = context,
                                            scopeName = scopeTitle,
                                            expenses = emptyList(),
                                            settlements = exportSettlements
                                        )
                                    }
                                    SettlementExportDocType.JSON -> {
                                        JsonExportGenerator().generate(
                                            context = context,
                                            scopeName = scopeTitle,
                                            expenses = emptyList(),
                                            settlements = exportSettlements
                                        )
                                    }
                                }
                                ExportUtils.shareFile(context, file, selectedFormat.mime)
                                isExporting = false
                                onDismiss()
                            } catch (e: Exception) {
                                isExporting = false
                                snackbar.showMessage("Export failed: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp),
                    shape = CircleShape,
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        SymbolIcon(
                            symbol = OpenSplitIcons.Export,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Report", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
