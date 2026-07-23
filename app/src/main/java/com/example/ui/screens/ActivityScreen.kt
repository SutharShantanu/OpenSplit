package com.example.ui.screens

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
import com.example.ui.components.BellWavesIllustration
import com.example.ui.components.StateLayout
import com.example.ui.components.appHazeHeader
import com.example.ui.components.appHazeSource
import com.example.ui.theme.OpenSplitIcons
import com.example.ui.theme.OpenSplitTokens
import com.example.ui.viewmodel.ActivityCategoryFilter
import com.example.ui.viewmodel.ActivityViewModel
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

                    // Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = OpenSplitTokens.SpaceSM),
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

                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))

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
                                    text = "No Activity Yet",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                                Text(
                                    text = "Activity will show up here as your groups do things.",
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
                                                    imageVector = OpenSplitIcons.Activity,
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

