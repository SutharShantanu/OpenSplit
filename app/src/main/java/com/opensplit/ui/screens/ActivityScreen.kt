@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.theme.Symbol
import com.opensplit.ui.components.SymbolIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opensplit.domain.model.Activity
import com.opensplit.domain.model.ActivityType
import com.opensplit.ui.components.BellWavesIllustration
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.components.appHazeHeader
import com.opensplit.ui.components.appHazeSource
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.ActivityCategoryFilter
import com.opensplit.ui.viewmodel.ActivitySortOrder
import com.opensplit.ui.viewmodel.ActivityViewModel
import dev.chrisbanes.haze.HazeState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class DateRangePreset(val label: String) {
    ALL_TIME("All time"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 days"),
    THIS_WEEK("This week"),
    THIS_MONTH("This month"),
    LAST_30_DAYS("Last 30 days"),
    CUSTOM("Custom range")
}

private fun DateRangePreset.icon() = when (this) {
    DateRangePreset.ALL_TIME -> OpenSplitIcons.History
    DateRangePreset.TODAY -> OpenSplitIcons.CalendarToday
    DateRangePreset.LAST_7_DAYS, DateRangePreset.THIS_WEEK -> OpenSplitIcons.DateRange
    DateRangePreset.THIS_MONTH, DateRangePreset.LAST_30_DAYS -> OpenSplitIcons.CalendarToday
    DateRangePreset.CUSTOM -> OpenSplitIcons.Tune
}

private fun ActivitySortOrder.icon() = when (this) {
    ActivitySortOrder.NEWEST_FIRST -> OpenSplitIcons.ArrowDownward
    ActivitySortOrder.OLDEST_FIRST -> OpenSplitIcons.ArrowUpward
    ActivitySortOrder.GROUP_NAME -> OpenSplitIcons.Groups
    ActivitySortOrder.TYPE -> OpenSplitIcons.Layers
}

private fun ActivityCategoryFilter.icon() = when (this) {
    ActivityCategoryFilter.ALL -> null
    ActivityCategoryFilter.EXPENSES -> OpenSplitIcons.Receipt
    ActivityCategoryFilter.SETTLEMENTS -> OpenSplitIcons.Handshake
    ActivityCategoryFilter.MEMBERS -> OpenSplitIcons.People
}

private fun com.opensplit.ui.viewmodel.ActivityActorFilter.icon() = when (this) {
    com.opensplit.ui.viewmodel.ActivityActorFilter.ANYONE -> OpenSplitIcons.People
    com.opensplit.ui.viewmodel.ActivityActorFilter.ME -> OpenSplitIcons.Person
    com.opensplit.ui.viewmodel.ActivityActorFilter.OTHERS -> OpenSplitIcons.Group
}

/** "Today" / "Yesterday" / "MMM d, yyyy" section header for a timestamp. */
private fun dayLabel(date: Date): String {
    val cal = Calendar.getInstance().apply { time = date }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
        cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = false,
    onOpenExpense: (groupId: String, expenseId: String) -> Unit = { _, _ -> },
    onOpenGroup: (groupId: String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val hazeState = remember { HazeState() }

    Scaffold(
        contentWindowInsets = if (showTopBar) androidx.compose.material3.ScaffoldDefaults.contentWindowInsets else androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Activity Feed", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(shapes = IconButtonDefaults.shapes(), onClick = onNavigateBack) {
                            SymbolIcon(OpenSplitIcons.Back, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier.appHazeHeader(hazeState)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .appHazeSource(hazeState)
        ) {
            StateLayout(state = state) { uiState ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = OpenSplitTokens.SpaceLG)
                ) {

                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))

                    // Group Selector & Sort Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = OpenSplitTokens.SpaceXS),
                        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Group dropdown
                        var groupDropdownExpanded by remember { mutableStateOf(false) }
                        val currentGroupName = remember(uiState.selectedGroupId, uiState.groups) {
                            if (uiState.selectedGroupId == null) "All Groups"
                            else uiState.groups.find { it.id == uiState.selectedGroupId }?.name ?: "Selected Group"
                        }

                        Box {
                            FilterChip(
                                selected = uiState.selectedGroupId != null,
                                onClick = { groupDropdownExpanded = true },
                                label = { Text(currentGroupName) },
                                leadingIcon = { SymbolIcon(OpenSplitIcons.Groups, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                trailingIcon = { SymbolIcon(OpenSplitIcons.Dropdown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = MaterialTheme.shapes.medium
                            )

                            DropdownMenu(
                                expanded = groupDropdownExpanded,
                                onDismissRequest = { groupDropdownExpanded = false },
                                shape = MaterialTheme.shapes.large
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Groups") },
                                    onClick = {
                                        viewModel.setGroupFilter(null)
                                        groupDropdownExpanded = false
                                    },
                                    leadingIcon = {
                                        if (uiState.selectedGroupId == null) {
                                            SymbolIcon(OpenSplitIcons.Check, contentDescription = null)
                                        }
                                    }
                                )
                                uiState.groups.forEach { group ->
                                    DropdownMenuItem(
                                        text = { Text(group.name) },
                                        onClick = {
                                            viewModel.setGroupFilter(group.id)
                                            groupDropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            if (uiState.selectedGroupId == group.id) {
                                                SymbolIcon(OpenSplitIcons.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Sort Order
                        var sortDropdownExpanded by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = uiState.sortOrder != ActivitySortOrder.NEWEST_FIRST,
                                onClick = { sortDropdownExpanded = true },
                                label = { Text(uiState.sortOrder.label) },
                                leadingIcon = { SymbolIcon(uiState.sortOrder.icon(), contentDescription = "Sort order", modifier = Modifier.size(16.dp)) },
                                trailingIcon = { SymbolIcon(OpenSplitIcons.Dropdown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = MaterialTheme.shapes.medium
                            )

                            DropdownMenu(
                                expanded = sortDropdownExpanded,
                                onDismissRequest = { sortDropdownExpanded = false },
                                shape = MaterialTheme.shapes.large
                            ) {
                                ActivitySortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label) },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            sortDropdownExpanded = false
                                        },
                                        leadingIcon = { SymbolIcon(order.icon(), contentDescription = null) },
                                        trailingIcon = { if (uiState.sortOrder == order) SymbolIcon(OpenSplitIcons.Check, contentDescription = null) }
                                    )
                                }
                            }
                        }

                        // Date range
                        var datePreset by remember { mutableStateOf(DateRangePreset.ALL_TIME) }
                        var customRangeLabel by remember { mutableStateOf<String?>(null) }
                        var dateDropdownExpanded by remember { mutableStateOf(false) }
                        var showCustomRangeDialog by remember { mutableStateOf(false) }

                        fun applyPreset(preset: DateRangePreset) {
                            // Custom only becomes the active preset once a range is actually applied.
                            if (preset != DateRangePreset.CUSTOM) datePreset = preset
                            val now = Calendar.getInstance()
                            when (preset) {
                                DateRangePreset.ALL_TIME -> viewModel.setDateRange(null, null)
                                DateRangePreset.TODAY -> {
                                    val start = (now.clone() as Calendar).apply {
                                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                    }
                                    viewModel.setDateRange(start.timeInMillis, now.timeInMillis)
                                }
                                DateRangePreset.LAST_7_DAYS -> viewModel.setDateRange(now.timeInMillis - 7L * 86_400_000, now.timeInMillis)
                                DateRangePreset.LAST_30_DAYS -> viewModel.setDateRange(now.timeInMillis - 30L * 86_400_000, now.timeInMillis)
                                DateRangePreset.THIS_WEEK -> {
                                    val start = (now.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                    }
                                    viewModel.setDateRange(start.timeInMillis, now.timeInMillis)
                                }
                                DateRangePreset.THIS_MONTH -> {
                                    val start = (now.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, 1)
                                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                    }
                                    viewModel.setDateRange(start.timeInMillis, now.timeInMillis)
                                }
                                DateRangePreset.CUSTOM -> showCustomRangeDialog = true
                            }
                        }

                        Box {
                            FilterChip(
                                selected = datePreset != DateRangePreset.ALL_TIME,
                                onClick = { dateDropdownExpanded = true },
                                label = { Text(if (datePreset == DateRangePreset.CUSTOM && customRangeLabel != null) customRangeLabel!! else datePreset.label) },
                                leadingIcon = { SymbolIcon(datePreset.icon(), contentDescription = "Date range", modifier = Modifier.size(16.dp)) },
                                trailingIcon = { SymbolIcon(OpenSplitIcons.Dropdown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = MaterialTheme.shapes.medium
                            )

                            DropdownMenu(
                                expanded = dateDropdownExpanded,
                                onDismissRequest = { dateDropdownExpanded = false },
                                shape = MaterialTheme.shapes.large
                            ) {
                                DateRangePreset.entries.forEach { preset ->
                                    DropdownMenuItem(
                                        text = { Text(preset.label) },
                                        onClick = {
                                            dateDropdownExpanded = false
                                            applyPreset(preset)
                                        },
                                        leadingIcon = { SymbolIcon(preset.icon(), contentDescription = null) },
                                        trailingIcon = { if (datePreset == preset) SymbolIcon(OpenSplitIcons.Check, contentDescription = null) }
                                    )
                                }
                            }
                        }

                        // Who did it
                        var actorDropdownExpanded by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = uiState.actorFilter != com.opensplit.ui.viewmodel.ActivityActorFilter.ANYONE,
                                onClick = { actorDropdownExpanded = true },
                                label = { Text(uiState.actorFilter.label) },
                                leadingIcon = { SymbolIcon(uiState.actorFilter.icon(), contentDescription = "Who", modifier = Modifier.size(16.dp)) },
                                trailingIcon = { SymbolIcon(OpenSplitIcons.Dropdown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = MaterialTheme.shapes.medium
                            )
                            DropdownMenu(expanded = actorDropdownExpanded, onDismissRequest = { actorDropdownExpanded = false }, shape = MaterialTheme.shapes.large) {
                                com.opensplit.ui.viewmodel.ActivityActorFilter.values().forEach { who ->
                                    DropdownMenuItem(
                                        text = { Text(who.label) },
                                        onClick = { viewModel.setActorFilter(who); actorDropdownExpanded = false },
                                        leadingIcon = { SymbolIcon(who.icon(), contentDescription = null) },
                                        trailingIcon = { if (uiState.actorFilter == who) SymbolIcon(OpenSplitIcons.Check, contentDescription = null) }
                                    )
                                }
                            }
                        }

                        // Unread toggle
                        FilterChip(
                            selected = uiState.unreadOnly,
                            onClick = { viewModel.setUnreadOnly(!uiState.unreadOnly) },
                            label = { Text("Unread") },
                            leadingIcon = { SymbolIcon(if (uiState.unreadOnly) OpenSplitIcons.Notifications else OpenSplitIcons.Notification, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            shape = MaterialTheme.shapes.medium
                        )

                        // Reset, only when something is active
                        androidx.compose.animation.AnimatedVisibility(visible = uiState.hasActiveFilters || datePreset != DateRangePreset.ALL_TIME) {
                            AssistChip(
                                onClick = { viewModel.clearFilters(); datePreset = DateRangePreset.ALL_TIME },
                                label = { Text("Reset") },
                                leadingIcon = { SymbolIcon(OpenSplitIcons.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = MaterialTheme.shapes.medium
                            )
                        }

                        if (showCustomRangeDialog) {
                            // Activity can't be in the future: only up to today is selectable.
                            val todayUtcMidnight = remember {
                                val local = Calendar.getInstance()
                                Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                    set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0); set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }
                            val rangeState = rememberDateRangePickerState(selectableDates = object : androidx.compose.material3.SelectableDates {
                                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtcMidnight
                                override fun isSelectableYear(year: Int) = year <= Calendar.getInstance().get(Calendar.YEAR)
                            })
                            DatePickerDialog(
                                onDismissRequest = { showCustomRangeDialog = false },
                                confirmButton = {
                                    TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                                        val start = rangeState.selectedStartDateMillis
                                        val end = rangeState.selectedEndDateMillis ?: start
                                        if (start != null && end != null) {
                                            // Picker days are UTC midnights; cover the whole local day on both ends.
                                            viewModel.setDateRange(localDayStart(start), localDayEnd(end))
                                            customRangeLabel = rangeLabel(start, end)
                                            datePreset = DateRangePreset.CUSTOM
                                        }
                                        showCustomRangeDialog = false
                                    }) { Text("Apply") }
                                },
                                dismissButton = {
                                    TextButton(shapes = ButtonDefaults.shapes(), onClick = { showCustomRangeDialog = false }) { Text("Cancel") }
                                }
                            ) {
                                DateRangePicker(state = rangeState, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Category Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = OpenSplitTokens.SpaceSM),
                        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.Filter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        ActivityCategoryFilter.values().forEach { filter ->
                            val icon = filter.icon()
                            FilterChip(
                                selected = uiState.selectedTypeFilter == filter,
                                onClick = { viewModel.setTypeFilter(filter) },
                                label = { Text(filter.label, maxLines = 1) },
                                leadingIcon = if (icon != null) ({ SymbolIcon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }) else null,
                                shape = MaterialTheme.shapes.extraLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))

                    if (uiState.activities.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()) // lets pull-to-refresh catch the gesture
                                .padding(OpenSplitTokens.SpaceXL),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                BellWavesIllustration(size = 140.dp)
                                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXL))
                                Text(
                                    text = if (uiState.searchQuery.isNotBlank()) "No Matching Activity" else "No Activity Yet",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                                Text(
                                    text = if (uiState.searchQuery.isNotBlank()) "Try refining your search query or filters." else "Activity will show up here as your groups do things.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val lastSeen = uiState.lastSeenBeforeThisVisit
                        val grouped = remember(uiState.activities) {
                            uiState.activities.groupBy { dayLabel(it.timestamp.toDate()) }
                        }

                        // New items arrive at the top; keep the list at the top unless the user has scrolled.
                        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                        val newestId = uiState.activities.firstOrNull()?.id
                        androidx.compose.runtime.LaunchedEffect(newestId) {
                            if (listState.firstVisibleItemIndex <= 1) listState.scrollToItem(0)
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS),
                            contentPadding = PaddingValues(bottom = OpenSplitTokens.SpaceLG)
                        ) {
                            grouped.forEach { (label, activitiesForDay) ->
                                item(key = "header_$label") {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS)
                                    )
                                }
                                items(activitiesForDay, key = { it.id }) { act ->
                                    val isUnread = lastSeen != null && act.timestamp.seconds > lastSeen.seconds
                                    androidx.compose.foundation.layout.Box(Modifier.animateItem()) {
                                        ActivityRow(
                                            act = act,
                                            isUnread = isUnread,
                                            onOpenExpense = onOpenExpense,
                                            onOpenGroup = onOpenGroup
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(
    act: Activity,
    isUnread: Boolean,
    onOpenExpense: (groupId: String, expenseId: String) -> Unit = { _, _ -> },
    onOpenGroup: (groupId: String) -> Unit = {}
) {
    val timeStr = remember(act.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(act.timestamp.toDate())
    }

    val icon = when (act.type) {
        ActivityType.EXPENSE_ADDED, ActivityType.EXPENSE_EDITED, ActivityType.EXPENSE_DELETED -> OpenSplitIcons.CategoryBills
        ActivityType.SETTLEMENT_ADDED -> OpenSplitIcons.Settle
        ActivityType.GROUP_CREATED, ActivityType.MEMBER_ADDED, ActivityType.MEMBER_REMOVED -> OpenSplitIcons.Groups
        ActivityType.COMMENT_ADDED -> OpenSplitIcons.CategoryOther
    }

    val canOpenExpense = act.relatedExpenseId != null && act.groupId.isNotBlank()
    val canOpenGroup = (act.type == ActivityType.GROUP_CREATED || act.type == ActivityType.MEMBER_ADDED || act.type == ActivityType.MEMBER_REMOVED) && act.groupId.isNotBlank()
    val isClickable = canOpenExpense || canOpenGroup

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = isClickable) {
                if (canOpenExpense) {
                    onOpenExpense(act.groupId, act.relatedExpenseId!!)
                } else if (canOpenGroup) {
                    onOpenGroup(act.groupId)
                }
            },
        colors = ListItemDefaults.colors(
            containerColor = if (isUnread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent
        ),
        headlineContent = {
            Text(
                text = act.message,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SymbolIcon(
                        symbol = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        trailingContent = if (isUnread) {
            {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        } else null
    )
}

/** The picker returns UTC midnight for a day; map that calendar day to the local zone's start / end. */
private fun utcDayToLocal(utcMillis: Long): Calendar {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0); set(Calendar.MILLISECOND, 0)
    }
}
private fun localDayStart(utcMillis: Long): Long = utcDayToLocal(utcMillis).timeInMillis
private fun localDayEnd(utcMillis: Long): Long = utcDayToLocal(utcMillis).apply { add(Calendar.DAY_OF_MONTH, 1); add(Calendar.MILLISECOND, -1) }.timeInMillis
private fun rangeLabel(startUtc: Long, endUtc: Long): String {
    val a = utcDayToLocal(startUtc); val b = utcDayToLocal(endUtc)
    val day = java.text.SimpleDateFormat("d MMM", Locale.getDefault())
    return if (a.timeInMillis == b.timeInMillis) day.format(a.time)
    else if (a.get(Calendar.MONTH) == b.get(Calendar.MONTH) && a.get(Calendar.YEAR) == b.get(Calendar.YEAR)) "${a.get(Calendar.DAY_OF_MONTH)}–${day.format(b.time)}"
    else "${day.format(a.time)} – ${day.format(b.time)}"
}
