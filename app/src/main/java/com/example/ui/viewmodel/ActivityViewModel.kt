package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.AppContainer
import com.example.domain.model.Activity
import com.example.domain.model.ActivityType
import com.example.domain.model.Group
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ActivityUiState(
    val activities: List<Activity> = emptyList(),
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val selectedTypeFilter: ActivityCategoryFilter = ActivityCategoryFilter.ALL
)

enum class ActivityCategoryFilter(val label: String) {
    ALL("All"),
    EXPENSES("Expenses"),
    SETTLEMENTS("Settlements"),
    MEMBERS("Members")
}

class ActivityViewModel(private val appContainer: AppContainer) : ViewModel() {

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    private val _selectedTypeFilter = MutableStateFlow(ActivityCategoryFilter.ALL)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScreenState<ActivityUiState>> = flow {
        emit(ScreenState.Loading)
        val uid = appContainer.authRepository.getCurrentUserId()
        if (uid == null) {
            emit(ScreenState.Error("User not logged in"))
            return@flow
        }

        // On entry, update lastSeenActivityTimestamp to clear header bell badge
        appContainer.userRepository.updateLastSeenActivity(uid, Timestamp.now())

        val groupsFlow = appContainer.groupRepository.getGroupsForUser(uid)

        groupsFlow.flatMapLatest { groups ->
            val groupIds = groups.map { it.id }
            val loggedActivitiesFlow = appContainer.activityRepository.getActivityForUser(uid, groupIds)

            // Flow of expenses from all user groups
            val expensesFlows = groupIds.map { appContainer.expenseRepository.getExpensesForGroup(it) }
            val combinedExpensesFlow = if (expensesFlows.isEmpty()) flowOf(emptyList()) else combine(expensesFlows) { arrays -> arrays.flatMap { it } }

            // Flow of settlements from all user groups
            val settlementsFlows = groupIds.map { appContainer.settlementRepository.getSettlementsForGroup(it) }
            val combinedSettlementsFlow = if (settlementsFlows.isEmpty()) flowOf(emptyList()) else combine(settlementsFlows) { arrays -> arrays.flatMap { it } }

            combine(
                loggedActivitiesFlow,
                combinedExpensesFlow,
                combinedSettlementsFlow,
                _selectedGroupId,
                _selectedTypeFilter
            ) { logged, expenses, settlements, selectedGroup, selectedType ->
                val allActivities = mutableListOf<Activity>()
                allActivities.addAll(logged)

                // Synthesize group creation activities
                groups.forEach { group ->
                    allActivities.add(
                        Activity(
                            id = "grp_${group.id}",
                            type = ActivityType.GROUP_CREATED,
                            actorUid = group.createdBy,
                            message = "Group '${group.name}' was created",
                            timestamp = group.createdAt
                        )
                    )
                }

                // Synthesize expense activities
                expenses.forEach { exp ->
                    val groupName = groups.find { it.id == exp.groupId }?.name ?: "Group"
                    val formattedAmt = com.example.util.CurrencyFormatter.format(exp.amount, exp.currency)
                    allActivities.add(
                        Activity(
                            id = "exp_${exp.id}",
                            type = ActivityType.EXPENSE_ADDED,
                            actorUid = exp.createdBy,
                            message = "Expense '${exp.description}' ($formattedAmt) was added in '$groupName'",
                            timestamp = exp.date,
                            relatedExpenseId = exp.id
                        )
                    )
                }

                // Synthesize settlement activities
                settlements.forEach { set ->
                    val formattedAmt = com.example.util.CurrencyFormatter.format(set.amount, set.currency)
                    allActivities.add(
                        Activity(
                            id = "set_${set.id}",
                            type = ActivityType.SETTLEMENT_ADDED,
                            actorUid = set.fromUid,
                            message = "Settlement of $formattedAmt was logged",
                            timestamp = set.date
                        )
                    )
                }

                // Deduplicate by distinct key / id and sort by timestamp DESC
                val distinctActivities = allActivities
                    .distinctBy { "${it.type}_${it.message}_${it.timestamp.seconds}" }
                    .sortedByDescending { it.timestamp.seconds }

                var filtered = distinctActivities

                if (selectedGroup != null) {
                    val targetGroup = groups.find { it.id == selectedGroup }
                    if (targetGroup != null) {
                        filtered = filtered.filter { it.message.contains(targetGroup.name, ignoreCase = true) }
                    }
                }

                when (selectedType) {
                    ActivityCategoryFilter.ALL -> {}
                    ActivityCategoryFilter.EXPENSES -> {
                        filtered = filtered.filter {
                            it.type == ActivityType.EXPENSE_ADDED ||
                                    it.type == ActivityType.EXPENSE_EDITED ||
                                    it.type == ActivityType.EXPENSE_DELETED
                        }
                    }
                    ActivityCategoryFilter.SETTLEMENTS -> {
                        filtered = filtered.filter { it.type == ActivityType.SETTLEMENT_ADDED }
                    }
                    ActivityCategoryFilter.MEMBERS -> {
                        filtered = filtered.filter {
                            it.type == ActivityType.MEMBER_ADDED ||
                                    it.type == ActivityType.MEMBER_REMOVED ||
                                    it.type == ActivityType.GROUP_CREATED
                        }
                    }
                }

                ScreenState.Success(
                    ActivityUiState(
                        activities = filtered,
                        groups = groups,
                        selectedGroupId = selectedGroup,
                        selectedTypeFilter = selectedType
                    )
                )
            }
        }.collect { emit(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenState.Loading)

    fun setGroupFilter(groupId: String?) {
        _selectedGroupId.value = groupId
    }

    fun setTypeFilter(filter: ActivityCategoryFilter) {
        _selectedTypeFilter.value = filter
    }
}
