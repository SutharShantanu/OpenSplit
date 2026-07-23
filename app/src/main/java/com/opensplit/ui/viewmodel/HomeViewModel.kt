package com.opensplit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opensplit.di.AppContainer
import com.opensplit.domain.logic.DebtSimplifier
import com.opensplit.domain.model.Activity
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Group
import com.opensplit.domain.model.Settlement
import com.opensplit.domain.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GroupWithBalance(
    val group: Group,
    val balance: Double
)

data class HomeUiState(
    val user: User,
    /** Net balance per currency (currencyCode -> amount); never summed across currencies. */
    val netByCurrency: Map<String, Double>,
    val recentGroups: List<GroupWithBalance>,
    val recentActivities: List<Activity>,
    val smartNudge: DebtSimplifier.SettlementSuggestion? = null,
    val nudgeCurrency: String = "INR",
    val nudgeOtherUser: User? = null,
    val allGroups: List<Group> = emptyList()
)

class HomeViewModel(private val appContainer: AppContainer) : ViewModel() {

    private val _dismissedNudgeKeys = MutableStateFlow<Set<String>>(emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScreenState<HomeUiState>> = flow {
        emit(ScreenState.Loading)
        val currentUserId = appContainer.authRepository.getCurrentUserId()
        if (currentUserId == null) {
            emit(ScreenState.Error("User not authenticated"))
            return@flow
        }

        val userFlow = appContainer.userRepository.getUserFlow(currentUserId)
        val groupsFlow = appContainer.groupRepository.getGroupsForUser(currentUserId)
        val friendBalancesFlow = appContainer.friendRepository.getFriendsBalances(currentUserId)

        combine(userFlow, groupsFlow, friendBalancesFlow, _dismissedNudgeKeys) { user, groups, friendBalances, dismissedKeys ->
            val currentUser = user ?: User(uid = currentUserId, displayName = "User")
            val groupIds = groups.map { it.id }

            // Fetch expenses and settlements for all groups to calculate per-group balances
            val groupBalancesList = groups.map { group ->
                val expensesFlow = appContainer.expenseRepository.getExpensesForGroup(group.id)
                val settlementsFlow = appContainer.settlementRepository.getSettlementsForGroup(group.id)
                combine(expensesFlow, settlementsFlow) { expenses, settlements ->
                    val groupBal = calculateUserGroupBalance(currentUserId, expenses, settlements)
                    GroupWithBalance(group, groupBal)
                }
            }

            val groupBalancesCombined: Flow<List<GroupWithBalance>> = if (groupBalancesList.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(groupBalancesList) { it.toList() }
            }

            val activitiesFlow = appContainer.activityRepository.getActivityForUser(currentUserId, groupIds)

            combine(groupBalancesCombined, activitiesFlow) { groupWithBalances, activities ->
                // Net balance per currency — each group carries its own currency; never sum across them.
                val netByCurrency = groupWithBalances
                    .groupBy { it.group.currency.ifEmpty { "INR" } }
                    .mapValues { entry -> entry.value.sumOf { it.balance } }
                val defaultCurrency = currentUser.defaultCurrency.ifEmpty { "INR" }

                // Recent groups sorted by recent activity or group creation
                val sortedGroups = groupWithBalances.take(5)
                val recentActivities = activities.take(3)

                // Smart nudge: largest outstanding balance with a single friend, in the
                // user's default currency. friendBalances is friendUid -> currency -> net
                // (positive = friend owes the user).
                val nudgeCandidates = friendBalances.mapNotNull { (friendUid, byCurrency) ->
                    val amt = byCurrency[defaultCurrency] ?: 0.0
                    when {
                        amt >= 1.0 -> DebtSimplifier.SettlementSuggestion(friendUid, currentUserId, amt)
                        amt <= -1.0 -> DebtSimplifier.SettlementSuggestion(currentUserId, friendUid, -amt)
                        else -> null
                    }
                }.sortedByDescending { it.amount }

                val relevantSuggestion = nudgeCandidates.firstOrNull { !dismissedKeys.contains(nudgeKey(it)) }

                var nudgeOtherUser: User? = null
                if (relevantSuggestion != null) {
                    val otherUid = if (relevantSuggestion.fromUid == currentUserId) relevantSuggestion.toUid else relevantSuggestion.fromUid
                    nudgeOtherUser = appContainer.userRepository.getUser(otherUid)
                }

                ScreenState.Success(
                    HomeUiState(
                        user = currentUser,
                        netByCurrency = netByCurrency,
                        recentGroups = sortedGroups,
                        recentActivities = recentActivities,
                        smartNudge = relevantSuggestion,
                        nudgeCurrency = defaultCurrency,
                        nudgeOtherUser = nudgeOtherUser,
                        allGroups = groups
                    )
                )
            }
        }.flatMapLatest { it }
            .collect { emit(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenState.Loading)

    fun dismissNudge(suggestion: DebtSimplifier.SettlementSuggestion) {
        _dismissedNudgeKeys.value = _dismissedNudgeKeys.value + nudgeKey(suggestion)
    }

    // Stable key based on integer minor units so tiny float differences don't re-show a
    // dismissed nudge.
    private fun nudgeKey(s: DebtSimplifier.SettlementSuggestion): String {
        val minor = kotlin.math.round(s.amount * 100).toLong()
        return "${s.fromUid}_${s.toUid}_$minor"
    }

    fun createGroup(name: String, currency: String) {
        viewModelScope.launch {
            val uid = appContainer.authRepository.getCurrentUserId() ?: return@launch
            val newGroup = Group(
                name = name,
                currency = currency,
                createdBy = uid,
                memberIds = listOf(uid)
            )
            val result = appContainer.groupRepository.createGroup(newGroup)
            if (result.isSuccess) {
                val groupId = result.getOrNull()
                if (groupId != null) {
                    appContainer.activityRepository.logActivity(
                        groupId = groupId,
                        activity = Activity(
                            type = com.opensplit.domain.model.ActivityType.GROUP_CREATED,
                            actorUid = uid,
                            message = "Created group $name"
                        )
                    )
                }
            }
        }
    }

    private fun calculateUserGroupBalance(
        userId: String,
        expenses: List<Expense>,
        settlements: List<Settlement>
    ): Double {
        var net = 0.0
        for (expense in expenses) {
            val payers = mutableMapOf<String, Double>()
            if (expense.multiPayer != null) {
                payers.putAll(expense.multiPayer)
            } else {
                payers[expense.paidBy] = expense.amount
            }

            val userPaid = payers[userId] ?: 0.0
            val userSplit = expense.splits.find { it.uid == userId }?.amount ?: 0.0
            net += (userPaid - userSplit)
        }

        for (settlement in settlements) {
            if (settlement.fromUid == userId) {
                net += settlement.amount
            } else if (settlement.toUid == userId) {
                net -= settlement.amount
            }
        }
        return net
    }
}
