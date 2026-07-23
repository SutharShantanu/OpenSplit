package com.example.ui.screens

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
import com.example.ui.components.ChartBarsIllustration
import com.example.ui.components.HeroBalanceCard
import com.example.ui.components.StateLayout
import com.example.ui.components.getCategoryColor
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.OpenSplitIcons
import com.example.ui.theme.OpenSplitTokens
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.CategorySpend
import com.example.ui.viewmodel.MonthlyBucket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateToExpenseDetail: (String, String) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsState()
    var scopeMenuExpanded by remember { mutableStateOf(false) }

    StateLayout(state = state) { analyticsState ->
        if (analyticsState.totalExpenseCount == 0) {
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
                // Scope Picker Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { scopeMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(OpenSplitTokens.SpaceMD),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selectedName = if (analyticsState.selectedGroupId == null) {
                                "All Groups"
                            } else {
                                analyticsState.groups.find { it.id == analyticsState.selectedGroupId }?.name ?: "Group"
                            }
                            Text(
                                text = "Scope: $selectedName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(OpenSplitIcons.Dropdown, contentDescription = "Select scope")
                        }
                    }

                    DropdownMenu(
                        expanded = scopeMenuExpanded,
                        onDismissRequest = { scopeMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Groups") },
                            onClick = {
                                viewModel.selectGroupScope(null)
                                scopeMenuExpanded = false
                            }
                        )
                        analyticsState.groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    viewModel.selectGroupScope(group.id)
                                    scopeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Monthly Spend Hero Card (isSpendTotal = true)
                HeroBalanceCard(
                    amount = analyticsState.monthlySpendTotal,
                    currency = analyticsState.currency,
                    title = "THIS MONTH'S SPEND",
                    isSpendTotal = true
                )

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

                // Monthly Spending Over Time Bar Chart
                if (analyticsState.monthlyBuckets.isNotEmpty()) {
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

