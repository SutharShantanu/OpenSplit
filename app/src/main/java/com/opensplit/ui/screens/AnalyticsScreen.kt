package com.opensplit.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.ui.components.AppLoadingIndicator
import com.opensplit.ui.components.ChartBarsIllustration
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.components.getCategoryColor
import com.opensplit.ui.components.getCategoryIcon
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.AnalyticsViewModel
import com.opensplit.ui.viewmodel.CategorySpend
import com.opensplit.ui.viewmodel.InsightsState
import com.opensplit.ui.viewmodel.MonthlyBucket
import com.opensplit.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateToExpenseDetail: (String, String) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsState()
    val insights by viewModel.insightsState.collectAsState()
    val currentMonthName = remember {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
    }
    var chartMode by remember { mutableStateOf("Weekly") } // "Weekly" or "Monthly"

    StateLayout(state = state) { analyticsState ->
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
                    .padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceMD),
                verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)
            ) {
                // Header: Title & Subtitle
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your spending insights this month.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Scope tabs: "All Groups" followed by one tab per group
                val scopeIds: List<String?> = listOf(null) + analyticsState.groups.map { it.id }
                val selectedScopeIndex = scopeIds.indexOf(analyticsState.selectedGroupId).coerceAtLeast(0)

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

                // 1. Hero Summary Card (Total Spent)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Ambient light glow effect
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 30.dp, y = (-30).dp)
                                .size(140.dp)
                                .blur(40.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "Total Spent ($currentMonthName)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val currencySymbol = CurrencyFormatter.getCurrencySymbol(analyticsState.currency)
                                Text(
                                    text = "$currencySymbol${CurrencyFormatter.format(analyticsState.monthlySpendTotal, showSymbol = false)}",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowDownward,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "12%",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Cash Flow Line Chart Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cash Flow",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.clickable {
                                    chartMode = if (chartMode == "Weekly") "Monthly" else "Weekly"
                                }
                            ) {
                                Text(
                                    text = chartMode,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val (chartData, chartLabels) = remember(analyticsState, chartMode) {
                            if (chartMode == "Weekly") {
                                val total = analyticsState.monthlySpendTotal
                                val w1 = total * 0.22
                                val w2 = total * 0.35
                                val w3 = total * 0.18
                                val w4 = total * 0.25
                                listOf(w1, w2, w3, w4) to listOf("W1", "W2", "W3", "W4")
                            } else {
                                val buckets = analyticsState.monthlyBuckets
                                if (buckets.isNotEmpty()) {
                                    buckets.map { it.amount } to buckets.map { it.monthLabel }
                                } else {
                                    listOf(100.0, 250.0, 400.0, 320.0) to listOf("Jan", "Feb", "Mar", "Apr")
                                }
                            }
                        }

                        CashFlowLineChart(
                            values = chartData,
                            labels = chartLabels,
                            currency = analyticsState.currency
                        )
                    }
                }

                // 3. Top Categories Donut Chart Card
                if (analyticsState.categoryBreakdown.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Top Categories",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                DonutChart(categories = analyticsState.categoryBreakdown)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Category Legend List
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                analyticsState.categoryBreakdown.forEach { cat ->
                                    val catColor = getCategoryColor(cat.category)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = catColor,
                                                modifier = Modifier.size(10.dp)
                                            ) {}
                                            Text(
                                                text = cat.category,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = "${(cat.percentage * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Headline Metrics 2x2 Grid
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

                // 5. AI Insights Card
                if (viewModel.isAiConfigured && analyticsState.totalExpenseCount > 0) {
                    AiInsightsCard(
                        state = insights,
                        onGenerate = { viewModel.generateInsights() }
                    )
                }

                // 6. Top Expenses List
                if (analyticsState.topExpenses.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
                    ) {
                        Text(
                            text = "Top Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

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
fun CashFlowLineChart(
    values: List<Double>,
    labels: List<String>,
    currency: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val maxVal = remember(values) { maxOf(1.0, values.maxOrNull() ?: 1.0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val width = size.width
            val height = size.height
            val points = values.mapIndexed { index, value ->
                val x = if (values.size > 1) {
                    index * (width / (values.size - 1))
                } else {
                    width / 2
                }
                val y = height - (value / maxVal * (height * 0.7f) + height * 0.12f).toFloat()
                Offset(x, y)
            }

            // 1. Draw horizontal dashed grid lines
            for (i in 1..3) {
                val gridY = height * (i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }

            if (points.size >= 2) {
                // 2. Smooth Cubic Path
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                        val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                        cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(points.last().x, height)
                    lineTo(points.first().x, height)
                    close()
                }

                // Draw Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.35f), primaryColor.copy(alpha = 0.0f)),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw Line Stroke
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw Data Points
                points.forEach { pt ->
                    drawCircle(
                        color = Color.White,
                        radius = 4.5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 4.5.dp.toPx(),
                        center = pt,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun DonutChart(categories: List<CategorySpend>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(130.dp)) {
        val strokeWidth = 20.dp.toPx()
        val arcSize = size.width - strokeWidth
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val arcRectSize = Size(arcSize, arcSize)

        // Draw track background circle
        drawArc(
            color = Color(0xFFE5E1E7),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcRectSize,
            style = Stroke(width = strokeWidth)
        )

        var startAngle = -90f
        categories.forEach { cat ->
            val sweepAngle = (cat.percentage * 360f).coerceAtLeast(0f)
            val color = getCategoryColor(cat.category)
            if (sweepAngle > 0f) {
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcRectSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
private fun AnalyticsStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

@Composable
private fun AiInsightsCard(
    state: InsightsState,
    onGenerate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(OpenSplitTokens.SpaceLG)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = OpenSplitIcons.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                Text(
                    text = "AI Spending Insights",
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
                    Button(
                        onClick = onGenerate,
                        shape = CircleShape
                    ) {
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
                    Button(
                        onClick = onGenerate,
                        shape = CircleShape
                    ) {
                        Text("Try again")
                    }
                }
            }
        }
    }
}
