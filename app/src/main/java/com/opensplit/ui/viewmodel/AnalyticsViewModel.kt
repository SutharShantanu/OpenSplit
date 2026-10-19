package com.opensplit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opensplit.data.ai.AiFeature
import com.opensplit.di.AppContainer
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Group
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CategorySpend(
    val category: String,
    val amount: Double,
    val percentage: Float
)

data class MonthlyBucket(
    val monthLabel: String,
    val amount: Double
)

enum class AnalyticsPeriodType(val label: String) {
    MONTH("Month"),
    YEAR("Year"),
    ALL_TIME("All Time")
}

data class AnalyticsUiState(
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val monthlySpendTotal: Double = 0.0,
    val currency: String = "₹",
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val monthlyBuckets: List<MonthlyBucket> = emptyList(),
    val topExpenses: List<Expense> = emptyList(),
    val totalExpenseCount: Int = 0,
    /** Number of groups in the current scope. */
    val groupCount: Int = 0,
    /** Distinct people you share groups with, excluding yourself. */
    val peopleCount: Int = 0,
    /** Total value of all expenses in scope (full amounts, not just your share). */
    val totalMoneyTracked: Double = 0.0,
    /** Your share across all expenses in scope. */
    val yourShareTotal: Double = 0.0,
    /** Percentage change vs prior period (+0.15 = +15%); null when prior period had no expenses. */
    val monthOverMonth: Double? = null,
    /** Rule-based takeaways that never need an API key. */
    val localInsights: List<String> = emptyList(),
    /** Active time period type (Month, Year, All Time). */
    val periodType: AnalyticsPeriodType = AnalyticsPeriodType.MONTH,
    /** Selected month (0 = January .. 11 = December). */
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    /** Selected year (e.g. 2026). */
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    /** Formatted human-readable label for the period (e.g. "October 2026", "2025", "All Time"). */
    val periodLabel: String = "This Month",
    /** Context label for comparison pill (e.g. "vs last month", "vs last year"). */
    val comparisonLabel: String = "vs last month",
    /** List of all years for which user has expenses or the current year. */
    val availableYears: List<Int> = listOf(Calendar.getInstance().get(Calendar.YEAR)),
    /** Whether the user can navigate to the next period (future guard). */
    val canGoForward: Boolean = false
)

/** State of the on-demand AI insights card. */
sealed interface InsightsState {
    data object Idle : InsightsState
    data object Loading : InsightsState
    data class Ready(val insights: List<String>) : InsightsState
    data class Failed(val message: String) : InsightsState
}

class AnalyticsViewModel(private val appContainer: AppContainer) : ViewModel() {

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    private val _selectedPeriodType = MutableStateFlow(AnalyticsPeriodType.MONTH)
    val selectedPeriodType: StateFlow<AnalyticsPeriodType> = _selectedPeriodType.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val retryTrigger = MutableStateFlow(0)

    /** Re-subscribes every upstream flow (pull-to-refresh / error retry). */
    fun retry() { retryTrigger.value++ }

    fun setPeriodType(type: AnalyticsPeriodType) {
        _selectedPeriodType.value = type
        _insightsState.value = InsightsState.Idle
    }

    fun setMonth(month: Int) {
        _selectedMonth.value = month.coerceIn(0, 11)
        _insightsState.value = InsightsState.Idle
    }

    fun setYear(year: Int) {
        _selectedYear.value = year
        _insightsState.value = InsightsState.Idle
    }

    fun setMonthAndYear(month: Int, year: Int) {
        _selectedMonth.value = month.coerceIn(0, 11)
        _selectedYear.value = year
        _insightsState.value = InsightsState.Idle
    }

    fun previousPeriod() {
        when (_selectedPeriodType.value) {
            AnalyticsPeriodType.MONTH -> {
                if (_selectedMonth.value == 0) {
                    _selectedMonth.value = 11
                    _selectedYear.value = _selectedYear.value - 1
                } else {
                    _selectedMonth.value = _selectedMonth.value - 1
                }
            }
            AnalyticsPeriodType.YEAR -> {
                _selectedYear.value = _selectedYear.value - 1
            }
            AnalyticsPeriodType.ALL_TIME -> Unit
        }
        _insightsState.value = InsightsState.Idle
    }

    fun nextPeriod() {
        val now = Calendar.getInstance()
        val curYear = now.get(Calendar.YEAR)
        val curMonth = now.get(Calendar.MONTH)

        when (_selectedPeriodType.value) {
            AnalyticsPeriodType.MONTH -> {
                if (_selectedYear.value < curYear || (_selectedYear.value == curYear && _selectedMonth.value < curMonth)) {
                    if (_selectedMonth.value == 11) {
                        _selectedMonth.value = 0
                        _selectedYear.value = _selectedYear.value + 1
                    } else {
                        _selectedMonth.value = _selectedMonth.value + 1
                    }
                }
            }
            AnalyticsPeriodType.YEAR -> {
                if (_selectedYear.value < curYear) {
                    _selectedYear.value = _selectedYear.value + 1
                }
            }
            AnalyticsPeriodType.ALL_TIME -> Unit
        }
        _insightsState.value = InsightsState.Idle
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<ScreenState<AnalyticsUiState>> = retryTrigger.flatMapLatest { flow {
        emit(ScreenState.Loading)
        val uid = appContainer.authRepository.getCurrentUserId()
        if (uid == null) {
            emit(ScreenState.Error("User not logged in"))
            return@flow
        }

        val user = kotlinx.coroutines.withTimeoutOrNull(3000) {
            appContainer.userRepository.getUser(uid)
        }
        val defaultCurrency = user?.defaultCurrency?.ifEmpty { "INR" } ?: "INR"
        val currencySymbol = if (defaultCurrency == "INR") "₹" else if (defaultCurrency == "EUR") "€" else "$"

        val groupsFlow = appContainer.groupRepository.getGroupsForUser(uid)

        combine(
            groupsFlow,
            _selectedGroupId,
            _selectedPeriodType,
            _selectedYear,
            _selectedMonth
        ) { groups, selectedGroup, periodType, selYear, selMonth ->
            val expensesFlow: Flow<List<Expense>> = if (selectedGroup != null) {
                appContainer.expenseRepository.getExpensesForGroup(selectedGroup)
            } else {
                val groupIds = groups.map { it.id }
                val flows = groupIds.map { appContainer.expenseRepository.getExpensesForGroup(it) } +
                    appContainer.expenseRepository.getExpensesForUser(uid)
                if (flows.isEmpty()) flowOf(emptyList())
                else combine(flows) { arrays -> arrays.flatMap { it }.distinctBy { it.id } }
            }

            expensesFlow.map { expenses ->
                val now = Calendar.getInstance()
                val curYear = now.get(Calendar.YEAR)
                val curMonth = now.get(Calendar.MONTH)

                // Available years based on actual expense dates
                val cal = Calendar.getInstance()
                val expenseYears = expenses.map { exp ->
                    cal.time = exp.date.toDate()
                    cal.get(Calendar.YEAR)
                }
                val minYear = (expenseYears.minOrNull() ?: curYear).coerceAtMost(curYear)
                val availableYears = (minYear..curYear).toList()

                val canGoForward = when (periodType) {
                    AnalyticsPeriodType.MONTH -> selYear < curYear || (selYear == curYear && selMonth < curMonth)
                    AnalyticsPeriodType.YEAR -> selYear < curYear
                    AnalyticsPeriodType.ALL_TIME -> false
                }

                // Filter expenses for current period and comparison prior period
                val (periodExpenses, prevPeriodExpenses, periodLabel, comparisonLabel) = when (periodType) {
                    AnalyticsPeriodType.MONTH -> {
                        val current = expenses.filter { exp ->
                            cal.time = exp.date.toDate()
                            cal.get(Calendar.MONTH) == selMonth && cal.get(Calendar.YEAR) == selYear
                        }
                        val prevMonth = if (selMonth == 0) 11 else selMonth - 1
                        val prevYear = if (selMonth == 0) selYear - 1 else selYear
                        val previous = expenses.filter { exp ->
                            cal.time = exp.date.toDate()
                            cal.get(Calendar.MONTH) == prevMonth && cal.get(Calendar.YEAR) == prevYear
                        }
                        val labelCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, selYear)
                            set(Calendar.MONTH, selMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(labelCal.time)
                        Quadruple(current, previous, label, "vs last month")
                    }
                    AnalyticsPeriodType.YEAR -> {
                        val current = expenses.filter { exp ->
                            cal.time = exp.date.toDate()
                            cal.get(Calendar.YEAR) == selYear
                        }
                        val previous = expenses.filter { exp ->
                            cal.time = exp.date.toDate()
                            cal.get(Calendar.YEAR) == selYear - 1
                        }
                        Quadruple(current, previous, "$selYear", "vs last year")
                    }
                    AnalyticsPeriodType.ALL_TIME -> {
                        Quadruple(expenses, emptyList(), "All Time", "overall")
                    }
                }

                // Calculate user share total for the selected period
                val periodUserTotal = periodExpenses.sumOf { exp ->
                    exp.splits.find { it.uid == uid }?.amount ?: (exp.amount / maxOf(1, exp.splits.size))
                }
                val prevUserTotal = prevPeriodExpenses.sumOf { exp ->
                    exp.splits.find { it.uid == uid }?.amount ?: (exp.amount / maxOf(1, exp.splits.size))
                }
                val changePct = if (prevUserTotal > 0.009) (periodUserTotal - prevUserTotal) / prevUserTotal else null

                // Category breakdown across selected period
                val categoryMap = mutableMapOf<String, Double>()
                for (exp in periodExpenses) {
                    val userShare = exp.splits.find { it.uid == uid }?.amount ?: (exp.amount / maxOf(1, exp.splits.size))
                    val cat = exp.category.ifEmpty { "General" }
                    categoryMap[cat] = (categoryMap[cat] ?: 0.0) + userShare
                }

                val categoryList = categoryMap.map { (cat, amt) ->
                    val pct = if (periodUserTotal > 0) (amt / periodUserTotal).toFloat() else 0f
                    CategorySpend(cat, amt, pct)
                }.sortedByDescending { it.amount }

                // Buckets for the chart
                val buckets = mutableListOf<MonthlyBucket>()
                when (periodType) {
                    AnalyticsPeriodType.MONTH -> {
                        // 6 months trailing up to the selected month
                        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                        val c = Calendar.getInstance().apply {
                            set(Calendar.YEAR, selYear)
                            set(Calendar.MONTH, selMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        for (i in 5 downTo 0) {
                            val bucketCal = (c.clone() as Calendar).apply { add(Calendar.MONTH, -i) }
                            val m = bucketCal.get(Calendar.MONTH)
                            val y = bucketCal.get(Calendar.YEAR)
                            val label = monthFormat.format(bucketCal.time)
                            val total = expenses.filter { exp ->
                                cal.time = exp.date.toDate()
                                cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == y
                            }.sumOf { exp ->
                                exp.splits.find { it.uid == uid }?.amount ?: (exp.amount / maxOf(1, exp.splits.size))
                            }
                            buckets.add(MonthlyBucket(label, total))
                        }
                    }
                    AnalyticsPeriodType.YEAR -> {
                        // All 12 months of the selected year
                        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                        val monthCal = Calendar.getInstance().apply { set(Calendar.YEAR, selYear) }
                        for (m in 0..11) {
                            monthCal.set(Calendar.MONTH, m)
                            monthCal.set(Calendar.DAY_OF_MONTH, 1)
                            val label = monthFormat.format(monthCal.time)
                            val total = expenses.filter { exp ->
                                cal.time = exp.date.toDate()
                                cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == selYear
                            }.sumOf { exp ->
                                exp.splits.find { it.uid == uid }?.amount ?: (exp.amount / maxOf(1, exp.splits.size))
                            }
                            buckets.add(MonthlyBucket(label, total))
                        }
                    }
                    AnalyticsPeriodType.ALL_TIME -> {
                        // Available years
                        for (y in availableYears) {
                            val total = expenses.filter { exp ->
                                cal.time = exp.date.toDate()
                                cal.get(Calendar.YEAR) == y
                            }.sumOf { exp ->
                                exp.splits.find { it.uid == uid }?.amount ?: (exp.amount / maxOf(1, exp.splits.size))
                            }
                            buckets.add(MonthlyBucket(y.toString(), total))
                        }
                    }
                }

                // Top expenses for the selected period
                val topExps = periodExpenses.sortedByDescending { it.amount }.take(5)

                val scopedGroups = if (selectedGroup != null) groups.filter { it.id == selectedGroup } else groups
                val peopleCount = scopedGroups
                    .flatMap { it.memberIds }
                    .filter { it != uid }
                    .distinct()
                    .size

                ScreenState.Success(
                    AnalyticsUiState(
                        groups = groups,
                        selectedGroupId = selectedGroup,
                        monthlySpendTotal = periodUserTotal,
                        currency = currencySymbol,
                        categoryBreakdown = categoryList,
                        monthlyBuckets = buckets,
                        topExpenses = topExps,
                        totalExpenseCount = periodExpenses.size,
                        groupCount = scopedGroups.size,
                        peopleCount = peopleCount,
                        totalMoneyTracked = periodExpenses.sumOf { it.amount },
                        yourShareTotal = periodUserTotal,
                        monthOverMonth = changePct,
                        localInsights = com.opensplit.domain.logic.SpendingInsights.describe(
                            currencySymbol,
                            buckets.map { it.amount },
                            categoryList.map { it.category to it.amount },
                            periodExpenses.sumOf { it.amount },
                            periodUserTotal
                        ),
                        periodType = periodType,
                        selectedMonth = selMonth,
                        selectedYear = selYear,
                        periodLabel = periodLabel,
                        comparisonLabel = comparisonLabel,
                        availableYears = availableYears,
                        canGoForward = canGoForward
                    )
                )
            }
        }.flatMapLatest { it }
            .collect { emit(it) }
    }
        .catch { e ->
            emit(ScreenState.Error(e.message ?: "Failed to load analytics"))
        } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenState.Loading)

    val budgets: StateFlow<Map<String, Double>> = appContainer.userPreferencesRepository.categoryBudgetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setBudget(category: String, amount: Double?) {
        viewModelScope.launch { appContainer.userPreferencesRepository.setCategoryBudget(category, amount) }
    }

    fun selectGroupScope(groupId: String?) {
        _selectedGroupId.value = groupId
        _insightsState.value = InsightsState.Idle
    }

    private val _insightsState = MutableStateFlow<InsightsState>(InsightsState.Idle)
    val insightsState: StateFlow<InsightsState> = _insightsState.asStateFlow()

    val aiAvailable: StateFlow<Boolean> = appContainer.aiService.availability(AiFeature.INSIGHTS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun generateInsights() {
        val data = (uiState.value as? ScreenState.Success)?.data ?: return
        if (_insightsState.value is InsightsState.Loading) return

        _insightsState.value = InsightsState.Loading
        viewModelScope.launch {
            val summary = buildSummary(data)
            val result = appContainer.aiService.insights(summary)
            _insightsState.value = if (result != null) {
                InsightsState.Ready(result)
            } else {
                InsightsState.Failed("Couldn't generate insights right now. Try again.")
            }
        }
    }

    private fun buildSummary(data: AnalyticsUiState): String = buildString {
        val cur = data.currency
        appendLine("Currency: $cur")
        appendLine("Period: ${data.periodLabel} (${data.periodType.label})")
        appendLine("Scope: ${if (data.selectedGroupId == null) "all groups" else "a single group"}")
        appendLine("Groups: ${data.groupCount}, people shared with: ${data.peopleCount}")
        appendLine("Expenses in period: ${data.totalExpenseCount}")
        appendLine("Total value tracked: $cur${"%.2f".format(data.totalMoneyTracked)}")
        appendLine("Your total share in period: $cur${"%.2f".format(data.yourShareTotal)}")
        appendLine("Spend by category (your share):")
        data.categoryBreakdown.take(8).forEach {
            appendLine("- ${it.category}: $cur${"%.2f".format(it.amount)} (${(it.percentage * 100).toInt()}%)")
        }
        appendLine("Trend buckets:")
        data.monthlyBuckets.forEach {
            appendLine("- ${it.monthLabel}: $cur${"%.2f".format(it.amount)}")
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
