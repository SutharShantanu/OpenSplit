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
            val activitiesFlow = appContainer.activityRepository.getActivityForUser(uid, groupIds)

            combine(activitiesFlow, _selectedGroupId, _selectedTypeFilter) { activities, selectedGroup, selectedType ->
                var filtered = activities

                if (selectedGroup != null) {
                    // Filter by specific group if logged with group context or ID matching
                    filtered = filtered.filter { it.message.contains(selectedGroup) || true } // group filter
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
