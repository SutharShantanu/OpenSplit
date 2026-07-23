package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.AppContainer
import com.example.domain.model.Expense
import com.example.domain.model.Group
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Calendar

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
    val totalExpenseCount: Int = 0
)

class AnalyticsViewModel(private val appContainer: AppContainer) : ViewModel() {

    private val _selectedGroupId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScreenState<AnalyticsUiState>> = flow {
        emit(ScreenState.Loading)
        val uid = appContainer.authRepository.getCurrentUserId()
        if (uid == null) {
            emit(ScreenState.Error("User not logged in"))
            return@flow
        }

        val user = appContainer.userRepository.getUser(uid)
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

                ScreenState.Success(
                    AnalyticsUiState(
                        groups = groups,
                        selectedGroupId = selectedGroup,
                        monthlySpendTotal = monthlyTotal,
                        currency = currencySymbol,
                        categoryBreakdown = categoryList,
                        monthlyBuckets = monthBuckets,
                        topExpenses = topExps,
                        totalExpenseCount = expenses.size
                    )
                )
            }
        }.flatMapLatest { it }
            .collect { emit(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenState.Loading)

    fun selectGroupScope(groupId: String?) {
        _selectedGroupId.value = groupId
    }
}
