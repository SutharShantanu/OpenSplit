@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.theme.Symbol
import com.opensplit.ui.components.SymbolIcon
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.opensplit.ui.viewmodel.AnalyticsPeriodType
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToExpenseDetail: (String, String) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsState()
    val insights by viewModel.insightsState.collectAsState()
    val aiAvailable by viewModel.aiAvailable.collectAsState()
    val currentMonthName = remember {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
    }
    var chartMode by remember { mutableStateOf("Weekly") } // "Weekly" or "Monthly"
    var showPeriodPickerDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = if (onNavigateBack != null) androidx.compose.material3.ScaffoldDefaults.contentWindowInsets else androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            if (onNavigateBack != null) {
                TopAppBar(
                    title = { Text("Spending Analytics", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(shapes = IconButtonDefaults.shapes(), onClick = onNavigateBack) {
                            SymbolIcon(OpenSplitIcons.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
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

                // Scope tabs: "All Groups" followed by one tab per group
                val scopeIds: List<String?> = listOf(null) + analyticsState.groups.map { it.id }
                val selectedScopeIndex = scopeIds.indexOf(analyticsState.selectedGroupId).coerceAtLeast(0)

                androidx.compose.material3.PrimaryScrollableTabRow(
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

                // Period selection & historical navigation
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Period type chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalyticsPeriodType.entries.forEach { pType ->
                            FilterChip(
                                selected = analyticsState.periodType == pType,
                                onClick = { viewModel.setPeriodType(pType) },
                                label = {
                                    Text(
                                        text = pType.label,
                                        fontWeight = if (analyticsState.periodType == pType) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (analyticsState.periodType == pType) {
                                    {
                                        SymbolIcon(
                                            symbol = OpenSplitIcons.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                    // Historical Navigator (Only when not "All Time")
                    if (analyticsState.periodType != AnalyticsPeriodType.ALL_TIME) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.previousPeriod() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    SymbolIcon(
                                        symbol = OpenSplitIcons.ArrowBack,
                                        contentDescription = "Previous Period",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.clickable { showPeriodPickerDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        SymbolIcon(
                                            symbol = OpenSplitIcons.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = analyticsState.periodLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        SymbolIcon(
                                            symbol = OpenSplitIcons.Dropdown,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.nextPeriod() },
                                    enabled = analyticsState.canGoForward,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    SymbolIcon(
                                        symbol = OpenSplitIcons.ArrowForward,
                                        contentDescription = "Next Period",
                                        tint = if (analyticsState.canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (showPeriodPickerDialog) {
                    PeriodPickerDialog(
                        periodType = analyticsState.periodType,
                        selectedMonth = analyticsState.selectedMonth,
                        selectedYear = analyticsState.selectedYear,
                        availableYears = analyticsState.availableYears,
                        onSelectMonthAndYear = { m, y -> viewModel.setMonthAndYear(m, y) },
                        onSelectYear = { y -> viewModel.setYear(y) },
                        onDismiss = { showPeriodPickerDialog = false }
                    )
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
                                text = "Total Spent · ${analyticsState.periodLabel}",
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
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = com.opensplit.ui.theme.MoneyFontFamily,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1
                                )

                                val mom = analyticsState.monthOverMonth
                                if (analyticsState.periodType != AnalyticsPeriodType.ALL_TIME && mom != null) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            SymbolIcon(
                                                symbol = if (mom >= 0) OpenSplitIcons.ArrowUpward else OpenSplitIcons.ArrowDownward,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "${kotlin.math.abs(mom * 100).toInt()}% ${analyticsState.comparisonLabel}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
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

                            if (analyticsState.periodType == AnalyticsPeriodType.MONTH) {
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
                            } else if (analyticsState.periodType == AnalyticsPeriodType.YEAR) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = "12 Months",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val (chartData, chartLabels) = remember(analyticsState, chartMode) {
                            if (analyticsState.periodType == AnalyticsPeriodType.YEAR) {
                                val buckets = analyticsState.monthlyBuckets
                                if (buckets.isNotEmpty()) {
                                    buckets.map { it.amount } to buckets.map { it.monthLabel }
                                } else {
                                    listOf(0.0) to listOf("No data")
                                }
                            } else if (chartMode == "Weekly") {
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
                                    listOf(0.0) to listOf("No data")
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
                                            fontFamily = com.opensplit.ui.theme.MoneyFontFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3b. Local insights (always available) and category budgets
                if (analyticsState.localInsights.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(OpenSplitTokens.SpaceLG), verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                            Text("What stands out", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            analyticsState.localInsights.forEach { line ->
                                Row(verticalAlignment = Alignment.Top) {
                                    SymbolIcon(OpenSplitIcons.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(OpenSplitTokens.SpaceSM))
                                    Text(line, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                val budgets by viewModel.budgets.collectAsState()
                var budgetEditCategory by remember { mutableStateOf<String?>(null) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(OpenSplitTokens.SpaceLG), verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Monthly budgets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(shapes = ButtonDefaults.shapes(), onClick = { budgetEditCategory = "" }) { Text("Add") }
                        }
                        if (budgets.isEmpty()) {
                            Text("Set a limit per category and watch this month's share fill it up.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        budgets.entries.sortedBy { it.key }.forEach { (cat, limit) ->
                            val spent = analyticsState.categoryBreakdown.firstOrNull { it.category == cat }?.amount ?: 0.0
                            val frac = (spent / limit).coerceIn(0.0, 1.0).toFloat()
                            Column(modifier = Modifier.fillMaxWidth().clickable { budgetEditCategory = cat }) {
                                Row {
                                    Text(cat, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text(
                                        "${analyticsState.currency}${"%,.0f".format(spent)} / ${analyticsState.currency}${"%,.0f".format(limit)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = com.opensplit.ui.theme.MoneyFontFamily),
                                        color = if (spent > limit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                androidx.compose.material3.LinearWavyProgressIndicator(
                                    progress = { frac },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (spent > limit) MaterialTheme.colorScheme.error else getCategoryColor(cat),
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            }
                        }
                    }
                }
                budgetEditCategory?.let { editing ->
                    var cat by remember(editing) { mutableStateOf(editing.ifBlank { com.opensplit.data.ai.AiService.CATEGORIES.first() }) }
                    var amount by remember(editing) { mutableStateOf(budgets[editing]?.let { "%.0f".format(it) } ?: "") }
                    AlertDialog(
                        onDismissRequest = { budgetEditCategory = null },
                        title = { Text(if (editing.isBlank()) "New budget" else "Edit budget") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                                if (editing.isBlank()) {
                                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        com.opensplit.data.ai.AiService.CATEGORIES.filter { it !in budgets }.forEach { c ->
                                            FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) })
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = amount, onValueChange = { v -> amount = v.filter { it.isDigit() || it == '.' } },
                                    label = { Text("Monthly limit (${analyticsState.currency})") }, singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(shapes = ButtonDefaults.shapes(), enabled = amount.toDoubleOrNull()?.let { it > 0 } == true, onClick = {
                                viewModel.setBudget(cat, amount.toDoubleOrNull()); budgetEditCategory = null
                            }) { Text("Save") }
                        },
                        dismissButton = {
                            Row {
                                if (editing.isNotBlank()) TextButton(shapes = ButtonDefaults.shapes(), onClick = { viewModel.setBudget(editing, null); budgetEditCategory = null }) { Text("Remove") }
                                TextButton(shapes = ButtonDefaults.shapes(), onClick = { budgetEditCategory = null }) { Text("Cancel") }
                            }
                        }
                    )
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
                if (analyticsState.totalExpenseCount > 0) {
                    com.opensplit.ui.components.AiLocked(unlocked = aiAvailable) {
                        AiInsightsCard(
                            state = insights,
                            onGenerate = { viewModel.generateInsights() }
                        )
                    }
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
                                            SymbolIcon(
                                                symbol = catIcon,
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

    val chartDescription = labels.zip(values).joinToString(", ") { (l, v) -> "$l $currency${"%.0f".format(v)}" }
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .semantics { contentDescription = "Spending over time: $chartDescription" }
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
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val description = categories.take(4).joinToString(", ") { "${it.category} ${(it.percentage * 100).toInt()} percent" }
    Canvas(modifier = modifier.size(130.dp).semantics { contentDescription = "Spending by category: $description" }) {
        val strokeWidth = 20.dp.toPx()
        val arcSize = size.width - strokeWidth
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val arcRectSize = Size(arcSize, arcSize)

        // Draw track background circle
        drawArc(
            color = trackColor,
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
    icon: Symbol,
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
                SymbolIcon(
                    symbol = icon,
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
                fontFamily = com.opensplit.ui.theme.MoneyFontFamily,
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
                SymbolIcon(
                    symbol = OpenSplitIcons.AutoAwesome,
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
                    TextButton(shapes = ButtonDefaults.shapes(), onClick = onGenerate) {
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

@Composable
private fun PeriodPickerDialog(
    periodType: AnalyticsPeriodType,
    selectedMonth: Int,
    selectedYear: Int,
    availableYears: List<Int>,
    onSelectMonthAndYear: (Int, Int) -> Unit,
    onSelectYear: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var tempYear by remember { mutableIntStateOf(selectedYear) }
    val monthNames = remember {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    val yearsToDisplay = remember(availableYears, selectedYear) {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val set = (availableYears + listOf(currentYear, currentYear - 1, currentYear - 2, selectedYear)).toSortedSet().reversed()
        set.toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (periodType == AnalyticsPeriodType.MONTH) "Select Month & Year" else "Select Year",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Year selector
                Text(
                    text = "Year",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(yearsToDisplay) { y ->
                        FilterChip(
                            selected = (tempYear == y),
                            onClick = {
                                tempYear = y
                                if (periodType == AnalyticsPeriodType.YEAR) {
                                    onSelectYear(y)
                                    onDismiss()
                                }
                            },
                            label = { Text(y.toString()) }
                        )
                    }
                }

                if (periodType == AnalyticsPeriodType.MONTH) {
                    Text(
                        text = "Month",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 4 rows x 3 columns month grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0 until 4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0 until 3) {
                                    val mIdx = row * 3 + col
                                    val isSelected = (mIdx == selectedMonth && tempYear == selectedYear)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                onSelectMonthAndYear(mIdx, tempYear)
                                                onDismiss()
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = monthNames[mIdx],
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
