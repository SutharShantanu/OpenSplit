package com.opensplit.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val hazeState = remember { HazeState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Feed", fontWeight = FontWeight.Bold) },
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

                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = OpenSplitTokens.SpaceSM),
                        placeholder = { Text("Search activities...") },
                        leadingIcon = { Icon(OpenSplitIcons.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(OpenSplitIcons.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    // Group Selector & Sort Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = OpenSplitTokens.SpaceXS),
                        horizontalArrangement = Arrangement.SpaceBetween,
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
                                trailingIcon = { Icon(OpenSplitIcons.Dropdown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                shape = MaterialTheme.shapes.medium
                            )

                            DropdownMenu(
                                expanded = groupDropdownExpanded,
                                onDismissRequest = { groupDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Groups") },
                                    onClick = {
                                        viewModel.setGroupFilter(null)
                                        groupDropdownExpanded = false
                                    },
                                    leadingIcon = {
                                        if (uiState.selectedGroupId == null) {
                                            Icon(OpenSplitIcons.Check, contentDescription = null)
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
                                                Icon(OpenSplitIcons.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Sort Order
                        var sortDropdownExpanded by remember { mutableStateOf(false) }
                        Box {
                            AssistChip(
                                onClick = { sortDropdownExpanded = true },
                                label = { Text(uiState.sortOrder.label) },
                                leadingIcon = { Icon(OpenSplitIcons.Sort, contentDescription = "Sort", modifier = Modifier.size(16.dp)) },
                                shape = MaterialTheme.shapes.medium
                            )

                            DropdownMenu(
                                expanded = sortDropdownExpanded,
                                onDismissRequest = { sortDropdownExpanded = false }
                            ) {
                                ActivitySortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label) },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            sortDropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            if (uiState.sortOrder == order) {
                                                Icon(OpenSplitIcons.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Category Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = OpenSplitTokens.SpaceSM),
                        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = OpenSplitIcons.Filter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        ActivityCategoryFilter.values().forEach { filter ->
                            FilterChip(
                                selected = uiState.selectedTypeFilter == filter,
                                onClick = { viewModel.setTypeFilter(filter) },
                                label = { Text(filter.label) },
                                shape = MaterialTheme.shapes.extraLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))

                    if (uiState.activities.isEmpty()) {
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
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS),
                            contentPadding = PaddingValues(bottom = OpenSplitTokens.SpaceLG)
                        ) {
                            items(uiState.activities) { act ->
                                val timeStr = remember(act.timestamp) {
                                    SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                                        .format(act.timestamp.toDate())
                                }

                                val icon = when (act.type) {
                                    ActivityType.EXPENSE_ADDED, ActivityType.EXPENSE_EDITED, ActivityType.EXPENSE_DELETED -> OpenSplitIcons.CategoryBills
                                    ActivityType.SETTLEMENT_ADDED -> OpenSplitIcons.Settle
                                    ActivityType.GROUP_CREATED, ActivityType.MEMBER_ADDED, ActivityType.MEMBER_REMOVED -> OpenSplitIcons.Groups
                                    ActivityType.COMMENT_ADDED -> OpenSplitIcons.CategoryOther
                                }

                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium),
                                    headlineContent = {
                                        Text(
                                            text = act.message,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
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
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
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
}


