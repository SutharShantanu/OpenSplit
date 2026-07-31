package com.opensplit.ui.screens

import com.opensplit.ui.components.AppLoadingIndicator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.ui.components.ChartBarsIllustration
import com.opensplit.ui.components.HeroBalanceCard
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.components.getCategoryColor
import com.opensplit.ui.components.getCategoryIcon
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.AnalyticsViewModel
import com.opensplit.ui.viewmodel.CategorySpend
import com.opensplit.ui.viewmodel.InsightsState
import com.opensplit.ui.viewmodel.MonthlyBucket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateToExpenseDetail: (String, String) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsState()
    val insights by viewModel.insightsState.collectAsState()

    StateLayout(state = state) { analyticsState ->
        // Only a user with no groups at all has nothing to show; once groups exist the headline
        // stats are meaningful even before the first expense is added.
        if (analyticsState.groupCount == 0 && analyticsState.totalExpenseCount == 0) {
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
                    ChartBarsIllustration(size = 140.dp)
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXL))
                    Text(
                        text = "No Analytics Yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                    Text(
                        text = "Analytics will show up once you've added expenses to your groups.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(OpenSplitTokens.SpaceLG),
                verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceLG)
            ) {
                // Scope tabs: "All Groups" followed by one tab per group.
                val scopeIds: List<String?> = listOf(null) + analyticsState.groups.map { it.id }
                val selectedScopeIndex = scopeIds.indexOf(analyticsState.selectedGroupId)
                    .coerceAtLeast(0)

                ScrollableTabRow(
                    selectedTabIndex = selectedScopeIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    scopeIds.forEachIndexed { index, groupId ->
                        val label = if (groupId == null) {
                            "All Groups"
                        } else {
                            analyticsState.groups.find { it.id == groupId }?.name ?: "Group"
                        }
                        Tab(
                            selected = index == selectedScopeIndex,
                            onClick = { viewModel.selectGroupScope(groupId) },
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (index == selectedScopeIndex) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // Monthly Spend Hero Card (isSpendTotal = true)
                HeroBalanceCard(
                    amount = analyticsState.monthlySpendTotal,
                    currency = analyticsState.currency,
                    title = "THIS MONTH'S SPEND",
                    isSpendTotal = true
                )

                // Headline stats: groups / people / money / expense count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
                ) {
                    AnalyticsStatCard(
                        modifier = Modifier.weight(1f),
                        icon = OpenSplitIcons.Groups,
                        label = "Groups",
                        value = analyticsState.groupCount.toString()
                    )
                    AnalyticsStatCard(
                        modifier = Modifier.weight(1f),
                        icon = OpenSplitIcons.Friends,
                        label = "People",
                        value = analyticsState.peopleCount.toString()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
                ) {
                    AnalyticsStatCard(
                        modifier = Modifier.weight(1f),
                        icon = OpenSplitIcons.CategoryBills,
                        label = "Total tracked",
                        value = "${analyticsState.currency}${String.format("%.2f", analyticsState.totalMoneyTracked)}"
                    )
                    AnalyticsStatCard(
                        modifier = Modifier.weight(1f),
                        icon = OpenSplitIcons.SplitEqual,
                        label = "Your share",
                        value = "${analyticsState.currency}${String.format("%.2f", analyticsState.yourShareTotal)}"
                    )
                }
                AnalyticsStatCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = OpenSplitIcons.CategoryOther,
                    label = "Expenses recorded",
                    value = analyticsState.totalExpenseCount.toString()
                )

                // AI insights — only offered when an API key is configured, and only once there
                // is enough data for the model to say something meaningful.
                if (viewModel.isAiConfigured && analyticsState.totalExpenseCount > 0) {
                    AiInsightsCard(
                        state = insights,
                        onGenerate = { viewModel.generateInsights() }
                    )
                }

                if (analyticsState.totalExpenseCount == 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(OpenSplitTokens.SpaceLG),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No expenses yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                            Text(
                                text = "Charts and AI insights unlock once this scope has expenses.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Category Breakdown Donut Chart
                if (analyticsState.categoryBreakdown.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(OpenSplitTokens.SpaceLG)) {
                            Text(
                                text = "Category Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                DonutChart(categories = analyticsState.categoryBreakdown)
                            }

                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                            // Legend list
                            analyticsState.categoryBreakdown.forEach { cat ->
                                val color = getCategoryColor(cat.category)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = OpenSplitTokens.SpaceXS),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = color,
                                            modifier = Modifier.size(12.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                                        Text(
                                            text = cat.category,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "${analyticsState.currency}${String.format("%.2f", cat.amount)} (${(cat.percentage * 100).toInt()}%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Monthly Spending Over Time Bar Chart. The 6 buckets always exist, so gate on
                // there actually being expenses — otherwise this renders as an empty axis.
                if (analyticsState.totalExpenseCount > 0 && analyticsState.monthlyBuckets.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(OpenSplitTokens.SpaceLG)) {
                            Text(
                                text = "Spending Over Time (6 Months)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                            BarChart(
                                buckets = analyticsState.monthlyBuckets,
                                currency = analyticsState.currency
                            )
                        }
                    }
                }

                // Top Expenses List
                if (analyticsState.topExpenses.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Top Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))

                        analyticsState.topExpenses.forEach { exp ->
                            val catColor = getCategoryColor(exp.category)
                            val catIcon = getCategoryIcon(exp.category)

                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onNavigateToExpenseDetail(exp.groupId, exp.id) },
                                headlineContent = {
                                    Text(
                                        text = exp.description,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = exp.category.ifEmpty { "General" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingContent = {
                                    Surface(
                                        shape = CircleShape,
                                        color = catColor.copy(alpha = 0.15f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = catIcon,
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                trailingContent = {
                                    Text(
                                        text = "${analyticsState.currency}${String.format("%.2f", exp.amount)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
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

@Composable
fun DonutChart(categories: List<CategorySpend>) {
    Canvas(modifier = Modifier.size(160.dp)) {
        val strokeWidth = 28.dp.toPx()
        val arcSize = size.width - strokeWidth
        var startAngle = -90f

        categories.forEach { cat ->
            val sweepAngle = cat.percentage * 360f
            val color = getCategoryColor(cat.category)
            if (sweepAngle > 0f) {
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun BarChart(buckets: List<MonthlyBucket>, currency: String) {
    val maxVal = remember(buckets) { maxOf(1.0, buckets.maxOfOrNull { it.amount } ?: 1.0) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val width = size.width
            val height = size.height
            val barCount = buckets.size
            val barWidth = (width / barCount) * 0.45f
            val gap = width / barCount

            buckets.forEachIndexed { i, bucket ->
                val barHeight = (bucket.amount / maxVal * (height * 0.85f)).toFloat()
                val x = i * gap + (gap - barWidth) / 2
                val y = height - barHeight

                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, maxOf(4f, barHeight)),
                    cornerRadius = CornerRadius(barWidth * 0.2f, barWidth * 0.2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            buckets.forEach { bucket ->
                Text(
                    text = bucket.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


/** Compact headline metric tile used in the Analytics stat grid. */
@Composable
private fun AnalyticsStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(OpenSplitTokens.SpaceMD)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceXS))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/** On-demand AI summary of the current analytics scope. */
@Composable
private fun AiInsightsCard(
    state: InsightsState,
    onGenerate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(OpenSplitTokens.SpaceLG)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = OpenSplitIcons.ReceiptScan,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                Text(
                    text = "AI Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))

            when (state) {
                is InsightsState.Idle -> {
                    Text(
                        text = "Get a plain-language read on where your money is going.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                    Button(onClick = onGenerate) {
                        Text("Generate insights")
                    }
                }

                is InsightsState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLoadingIndicator(size = 18.dp)
                        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                        Text(
                            text = "Analyzing your spending...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is InsightsState.Ready -> {
                    state.insights.forEach { insight ->
                        Row(
                            modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                            Text(
                                text = insight,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                    TextButton(onClick = onGenerate) {
                        Text("Regenerate")
                    }
                }

                is InsightsState.Failed -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                    Button(onClick = onGenerate) {
                        Text("Try again")
                    }
                }
            }
        }
    }
}
