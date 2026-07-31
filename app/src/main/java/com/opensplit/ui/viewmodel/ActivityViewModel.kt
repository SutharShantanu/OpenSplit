package com.opensplit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opensplit.di.AppContainer
import com.opensplit.domain.model.Activity
import com.opensplit.domain.model.ActivityType
import com.opensplit.domain.model.Group
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds

data class ActivityUiState(
    val activities: List<Activity> = emptyList(),
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val selectedTypeFilter: ActivityCategoryFilter = ActivityCategoryFilter.ALL,
    val searchQuery: String = "",
    val sortOrder: ActivitySortOrder = ActivitySortOrder.NEWEST_FIRST,
    val dateRangeStartMillis: Long? = null,
    val dateRangeEndMillis: Long? = null,
    /** Read/unread boundary: anything newer than this was unseen when this screen was opened. */
    val lastSeenBeforeThisVisit: Timestamp? = null
)

enum class ActivityCategoryFilter(val label: String) {
    ALL("All"),
    EXPENSES("Expenses"),
    SETTLEMENTS("Settlements"),
    MEMBERS("Members")
}

enum class ActivitySortOrder(val label: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First")
}

class ActivityViewModel(private val appContainer: AppContainer) : ViewModel() {

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    private val _selectedTypeFilter = MutableStateFlow(ActivityCategoryFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(ActivitySortOrder.NEWEST_FIRST)
    private val _dateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
    private val _retryTrigger = MutableStateFlow(0)

    fun retry() {
        _retryTrigger.value += 1
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGroupFilter(groupId: String?) {
        _selectedGroupId.value = groupId
    }

    fun setTypeFilter(filter: ActivityCategoryFilter) {
        _selectedTypeFilter.value = filter
    }

    fun setSortOrder(sortOrder: ActivitySortOrder) {
        _sortOrder.value = sortOrder
    }

    fun setDateRange(startMillis: Long?, endMillis: Long?) {
        _dateRange.value = startMillis to endMillis
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<ScreenState<ActivityUiState>> = _retryTrigger
        .flatMapLatest {
            flow {
                emit(ScreenState.Loading)
                val uid = appContainer.authRepository.getCurrentUserId()
                if (uid == null) {
                    emit(ScreenState.Error("User not logged in", ::retry))
                    return@flow
                }

                // Capture the read/unread boundary before overwriting it, so items can still be
                // shown as unread for the duration of this visit. Bounded by a timeout so a slow
                // or unreachable backend can't hang the whole feed on this one-shot read.
                val previousLastSeen = try {
                    kotlinx.coroutines.withTimeoutOrNull(3000) {
                        appContainer.userRepository.getUser(uid)?.lastSeenActivityTimestamp
                    }
                } catch (_: Exception) { null }
                try {
                    appContainer.userRepository.updateLastSeenActivity(uid, Timestamp.now())
                } catch (_: Exception) {}

                appContainer.groupRepository.getGroupsForUser(uid).flatMapLatest { groups ->
                    val groupIds = groups.map { it.id }
                    val loggedActivitiesFlow = appContainer.activityRepository.getActivityForUser(uid, groupIds)

                    // Resolve every group member's display name so the feed can say *who* did
                    // what — activity from other members is the whole point of a shared feed.
                    val memberUids = groups.flatMap { it.memberIds }.distinct()
                    val nameByUid: Map<String, String> = kotlinx.coroutines.withTimeoutOrNull(5000) {
                        kotlinx.coroutines.coroutineScope {
                            memberUids.map { memberUid ->
                                async {
                                    memberUid to (appContainer.userRepository.getUser(memberUid)?.displayName ?: "")
                                }
                            }.awaitAll()
                        }.filter { it.second.isNotBlank() }.toMap()
                    } ?: emptyMap()

                    fun actorName(actorUid: String): String = when {
                        actorUid == uid -> "You"
                        else -> nameByUid[actorUid] ?: "Someone"
                    }

                    val expensesFlows = groupIds.map { appContainer.expenseRepository.getExpensesForGroup(it) }
                    val combinedExpensesFlow = if (expensesFlows.isEmpty()) flowOf(emptyList()) else combine(expensesFlows) { arrays -> arrays.flatMap { it } }

                    val settlementsFlows = groupIds.map { appContainer.settlementRepository.getSettlementsForGroup(it) }
                    val combinedSettlementsFlow = if (settlementsFlows.isEmpty()) flowOf(emptyList()) else combine(settlementsFlows) { arrays -> arrays.flatMap { it } }

                    val rawActivitiesFlow = combine(
                        loggedActivitiesFlow,
                        combinedExpensesFlow,
                        combinedSettlementsFlow
                    ) { logged, expenses, settlements ->
                        val allActivities = mutableListOf<Activity>()
                        // Repository-logged entries are already written in actor-prefixed style
                        // ("added member 'X'"), so just prepend the resolved name.
                        allActivities.addAll(
                            logged.map { it.copy(message = "${actorName(it.actorUid)} ${it.message}") }
                        )

                        groups.forEach { group ->
                            allActivities.add(
                                Activity(
                                    id = "grp_${group.id}",
                                    type = ActivityType.GROUP_CREATED,
                                    actorUid = group.createdBy,
                                    message = "${actorName(group.createdBy)} created group '${group.name}'",
                                    timestamp = group.createdAt
                                )
                            )
                        }

                        expenses.forEach { exp ->
                            val groupName = groups.find { it.id == exp.groupId }?.name ?: "Group"
                            val formattedAmt = com.opensplit.util.CurrencyFormatter.format(exp.amount, exp.currency)
                            allActivities.add(
                                Activity(
                                    id = "exp_${exp.id}",
                                    type = ActivityType.EXPENSE_ADDED,
                                    actorUid = exp.createdBy,
                                    message = "${actorName(exp.createdBy)} added '${exp.description}' ($formattedAmt) in '$groupName'",
                                    timestamp = exp.date,
                                    relatedExpenseId = exp.id
                                )
                            )
                        }

                        settlements.forEach { set ->
                            val formattedAmt = com.opensplit.util.CurrencyFormatter.format(set.amount, set.currency)
                            allActivities.add(
                                Activity(
                                    id = "set_${set.id}",
                                    type = ActivityType.SETTLEMENT_ADDED,
                                    actorUid = set.fromUid,
                                    message = "${actorName(set.fromUid)} logged a settlement of $formattedAmt",
                                    timestamp = set.date
                                )
                            )
                        }

                        allActivities.distinctBy { "${it.type}_${it.message}_${it.timestamp.seconds}" }
                    }

                    val filtersFlow = combine(
                        _selectedGroupId,
                        _selectedTypeFilter,
                        _searchQuery,
                        _sortOrder,
                        _dateRange
                    ) { selectedGroup, selectedType, query, sort, dateRange ->
                        listOf(selectedGroup, selectedType, query, sort, dateRange)
                    }

                    combine(rawActivitiesFlow, filtersFlow) { distinctActivities, filterList ->
                        val selectedGroup = filterList[0] as? String
                        val selectedType = filterList[1] as ActivityCategoryFilter
                        val query = filterList[2] as String
                        val sort = filterList[3] as ActivitySortOrder
                        @Suppress("UNCHECKED_CAST")
                        val dateRangePair = filterList[4] as Pair<Long?, Long?>
                        val rangeStart = dateRangePair.first
                        val rangeEnd = dateRangePair.second

                        var filtered = distinctActivities

                        // Filter by date range
                        if (rangeStart != null) {
                            filtered = filtered.filter { it.timestamp.toDate().time >= rangeStart }
                        }
                        if (rangeEnd != null) {
                            filtered = filtered.filter { it.timestamp.toDate().time <= rangeEnd }
                        }

                        // Filter by Group
                        if (!selectedGroup.isNullOrBlank()) {
                            val targetGroup = groups.find { it.id == selectedGroup }
                            if (targetGroup != null) {
                                filtered = filtered.filter { it.message.contains(targetGroup.name, ignoreCase = true) }
                            }
                        }

                        // Filter by Category
                        when (selectedType) {
                            ActivityCategoryFilter.ALL -> {}
                            ActivityCategoryFilter.EXPENSES -> {
                                filtered = filtered.filter {
                                    it.type == ActivityType.EXPENSE_ADDED ||
                                            it.type == ActivityType.EXPENSE_EDITED ||
                                            it.type == ActivityType.EXPENSE_DELETED ||
                                            it.type == ActivityType.COMMENT_ADDED
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

                        // Search query filtering
                        if (query.isNotBlank()) {
                            filtered = filtered.filter {
                                it.message.contains(query, ignoreCase = true)
                            }
                        }

                        // Sorting
                        filtered = when (sort) {
                            ActivitySortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.timestamp.seconds }
                            ActivitySortOrder.OLDEST_FIRST -> filtered.sortedBy { it.timestamp.seconds }
                        }

                        ScreenState.Success(
                            ActivityUiState(
                                activities = filtered,
                                groups = groups,
                                selectedGroupId = selectedGroup,
                                selectedTypeFilter = selectedType,
                                searchQuery = query,
                                sortOrder = sort,
                                dateRangeStartMillis = rangeStart,
                                dateRangeEndMillis = rangeEnd,
                                lastSeenBeforeThisVisit = previousLastSeen
                            )
                        )
                    }
                }.catch { e ->
                    emit(ScreenState.Error(e.message ?: "Failed to load activity feed", ::retry))
                }.collect { emit(it) }
            }
                // If a Firestore listener never emits (e.g. backend unreachable), fail fast with a
                // retryable error instead of spinning forever.
                .timeout(15.seconds)
                .catch { e ->
                    val message = if (e is TimeoutCancellationException) {
                        "Taking too long to load — check your connection and try again."
                    } else {
                        e.message ?: "An unexpected error occurred"
                    }
                    emit(ScreenState.Error(message, ::retry))
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenState.Loading)
}

