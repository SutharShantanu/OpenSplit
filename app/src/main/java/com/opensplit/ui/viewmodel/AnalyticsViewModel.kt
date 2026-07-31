package com.opensplit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opensplit.data.ai.GeminiInsightsGenerator
import com.opensplit.di.AppContainer
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Group
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.time.Duration.Companion.seconds

data class CategorySpend(
    val category: String,
    val amount: Double,
    val percentage: Float
)

data class MonthlyBucket(
    val monthLabel: String,
    val amount: Double
)

data class AnalyticsUiState(
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val monthlySpendTotal: Double = 0.0,
    val currency: String = "$",
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
    val yourShareTotal: Double = 0.0
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

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<ScreenState<AnalyticsUiState>> = flow {
        emit(ScreenState.Loading)
        val uid = appContainer.authRepository.getCurrentUserId()
        if (uid == null) {
            emit(ScreenState.Error("User not logged in"))
            return@flow
        }

        // Bounded so an unreachable backend can't hang the screen on this one-shot read.
        val user = kotlinx.coroutines.withTimeoutOrNull(3000) {
            appContainer.userRepository.getUser(uid)
        }
        val defaultCurrency = user?.defaultCurrency?.ifEmpty { "INR" } ?: "INR"
        val currencySymbol = if (defaultCurrency == "INR") "₹" else if (defaultCurrency == "EUR") "€" else "$"

        val groupsFlow = appContainer.groupRepository.getGroupsForUser(uid)

        combine(groupsFlow, _selectedGroupId) { groups, selectedGroup ->
            val expensesFlow: Flow<List<Expense>> = if (selectedGroup != null) {
                appContainer.expenseRepository.getExpensesForGroup(selectedGroup)
            } else {
                appContainer.expenseRepository.getExpensesForUser(uid)
            }

            expensesFlow.map { expenses ->
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)

                // Current month spend total
                val monthExpenses = expenses.filter { exp ->
                    calendar.time = exp.date.toDate()
                    calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
                }
                val monthlyTotal = monthExpenses.sumOf { exp ->
                    // Calculate user share in expense
                    val userSplit = exp.splits.find { it.uid == uid }?.amount ?: (exp.amount / maxOf(1, exp.splits.size))
                    userSplit
                }

                // Category breakdown across selected scope
                val categoryMap = mutableMapOf<String, Double>()
                var scopeTotal = 0.0
                for (exp in expenses) {
                    val userShare = exp.splits.find { it.uid == uid }?.amount ?: (exp.amount / maxOf(1, exp.splits.size))
                    val cat = exp.category.ifEmpty { "General" }
                    categoryMap[cat] = (categoryMap[cat] ?: 0.0) + userShare
                    scopeTotal += userShare
                }

                val categoryList = categoryMap.map { (cat, amt) ->
                    val pct = if (scopeTotal > 0) (amt / scopeTotal).toFloat() else 0f
                    CategorySpend(cat, amt, pct)
                }.sortedByDescending { it.amount }

                // Monthly buckets (last 6 months)
                val monthBuckets = mutableListOf<MonthlyBucket>()
                val monthFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
                for (i in 5 downTo 0) {
                    val c = Calendar.getInstance()
                    c.add(Calendar.MONTH, -i)
                    val m = c.get(Calendar.MONTH)
                    val y = c.get(Calendar.YEAR)
                    val label = monthFormat.format(c.time)

                    val mTotal = expenses.filter { exp ->
                        c.time = exp.date.toDate()
                        c.get(Calendar.MONTH) == m && c.get(Calendar.YEAR) == y
                    }.sumOf { exp ->
                        exp.splits.find { it.uid == uid }?.amount ?: (exp.amount / maxOf(1, exp.splits.size))
                    }

                    monthBuckets.add(MonthlyBucket(label, mTotal))
                }

                // Top expenses (top 5 by total amount)
                val topExps = expenses.sortedByDescending { it.amount }.take(5)

                // Headline counts. When a single group is selected the scope narrows to it,
                // otherwise it spans every group the user belongs to.
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
                        monthlySpendTotal = monthlyTotal,
                        currency = currencySymbol,
                        categoryBreakdown = categoryList,
                        monthlyBuckets = monthBuckets,
                        topExpenses = topExps,
                        totalExpenseCount = expenses.size,
                        groupCount = scopedGroups.size,
                        peopleCount = peopleCount,
                        totalMoneyTracked = expenses.sumOf { it.amount },
                        yourShareTotal = scopeTotal
                    )
                )
            }
        }.flatMapLatest { it }
            .collect { emit(it) }
    }
        // If Firestore listeners never emit, fail with a message instead of spinning forever.
        .timeout(15.seconds)
        .catch { e ->
            val message = if (e is TimeoutCancellationException) {
                "Taking too long to load — check your connection and try again."
            } else {
                e.message ?: "Failed to load analytics"
            }
            emit(ScreenState.Error(message))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenState.Loading)

    fun selectGroupScope(groupId: String?) {
        _selectedGroupId.value = groupId
        // Insights describe the previous scope, so drop them when the scope changes.
        _insightsState.value = InsightsState.Idle
    }

    private val _insightsState = MutableStateFlow<InsightsState>(InsightsState.Idle)
    val insightsState: StateFlow<InsightsState> = _insightsState.asStateFlow()

    /** Whether an API key is configured; the UI hides the AI card entirely when false. */
    val isAiConfigured: Boolean get() = GeminiInsightsGenerator.isConfigured()

    fun generateInsights() {
        val data = (uiState.value as? ScreenState.Success)?.data ?: return
        if (_insightsState.value is InsightsState.Loading) return

        _insightsState.value = InsightsState.Loading
        viewModelScope.launch {
            val summary = buildSummary(data)
            val result = GeminiInsightsGenerator.generateInsights(summary)
            _insightsState.value = if (result != null) {
                InsightsState.Ready(result)
            } else {
                InsightsState.Failed("Couldn't generate insights right now. Try again.")
            }
        }
    }

    /** Aggregates only — no raw expense descriptions leave the device. */
    private fun buildSummary(data: AnalyticsUiState): String = buildString {
        val cur = data.currency
        appendLine("Currency: $cur")
        appendLine("Scope: ${if (data.selectedGroupId == null) "all groups" else "a single group"}")
        appendLine("Groups: ${data.groupCount}, people shared with: ${data.peopleCount}")
        appendLine("Total expenses recorded: ${data.totalExpenseCount}")
        appendLine("Total value tracked: $cur${"%.2f".format(data.totalMoneyTracked)}")
        appendLine("Your total share: $cur${"%.2f".format(data.yourShareTotal)}")
        appendLine("Your spend this month: $cur${"%.2f".format(data.monthlySpendTotal)}")
        appendLine("Spend by category (your share):")
        data.categoryBreakdown.take(8).forEach {
            appendLine("- ${it.category}: $cur${"%.2f".format(it.amount)} (${(it.percentage * 100).toInt()}%)")
        }
        appendLine("Monthly totals (oldest to newest):")
        data.monthlyBuckets.forEach {
            appendLine("- ${it.monthLabel}: $cur${"%.2f".format(it.amount)}")
        }
    }
}
