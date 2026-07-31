package com.opensplit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opensplit.domain.model.Activity
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Group
import com.opensplit.domain.model.User
import com.opensplit.domain.repository.ActivityRepository
import com.opensplit.domain.repository.AuthRepository
import com.opensplit.domain.repository.AuthState
import com.opensplit.domain.model.FriendInvite
import com.opensplit.domain.repository.ExpenseRepository
import com.opensplit.domain.repository.FriendInviteRepository
import com.opensplit.domain.repository.FriendRepository
import com.opensplit.domain.repository.GroupRepository
import com.opensplit.domain.repository.UserPreferencesRepository
import com.opensplit.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FriendBalance(val user: User, val balancesByCurrency: Map<String, Double>) {
    /** Non-zero balances as (currencyCode, amount), largest magnitude first. */
    val nonZeroBalances: List<Pair<String, Double>>
        get() = balancesByCurrency.entries
            .filter { kotlin.math.abs(it.value) > 0.01 }
            .sortedByDescending { kotlin.math.abs(it.value) }
            .map { it.key to it.value }

    val owesYou: Boolean get() = balancesByCurrency.values.any { it > 0.01 }
    val youOwe: Boolean get() = balancesByCurrency.values.any { it < -0.01 }
    val maxMagnitude: Double get() = balancesByCurrency.values.maxOfOrNull { kotlin.math.abs(it) } ?: 0.0
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository,
    private val friendRepository: FriendRepository,
    private val activityRepository: ActivityRepository,
    private val friendInviteRepository: FriendInviteRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)
    fun retry() { retryTrigger.value++ }

    val friendInvites: StateFlow<List<FriendInvite>> =
        combine(authRepository.getAuthState(), retryTrigger) { state, _ -> state }
            .flatMapLatest { authState ->
                if (authState is AuthState.LoggedIn) friendInviteRepository.getInvites(authState.uid)
                else flowOf(emptyList())
            }
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendFriendInvite(email: String, onResult: (Boolean) -> Unit = {}) {
        val trimmed = email.trim()
        if (trimmed.isBlank()) { onResult(false); return }
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId()
            if (uid == null) { onResult(false); return@launch }
            if (friendInvites.value.any { it.email.equals(trimmed, ignoreCase = true) }) {
                onResult(false); return@launch
            }
            onResult(friendInviteRepository.sendInvite(uid, trimmed).isSuccess)
        }
    }

    fun revokeFriendInvite(inviteId: String) {
        viewModelScope.launch { friendInviteRepository.revokeInvite(inviteId) }
    }

    val userGroups: StateFlow<ScreenState<List<Group>>> = combine(authRepository.getAuthState(), retryTrigger) { state, _ -> state }
        .flatMapLatest { authState ->
            if (authState is AuthState.LoggedIn) {
                groupRepository.getGroupsForUser(authState.uid)
                    .map<List<Group>, ScreenState<List<Group>>> { ScreenState.Success(it) }
                    .catch { emit(ScreenState.Error(it.message ?: "Failed to load groups", ::retry)) }
            } else {
                flowOf(ScreenState.Success(emptyList()))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScreenState.Loading
        )

    val pinnedGroupIds: StateFlow<Set<String>> = userPreferencesRepository.pinnedGroupIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun togglePinnedGroup(groupId: String) {
        viewModelScope.launch { userPreferencesRepository.togglePinnedGroup(groupId) }
    }

    // Activity has no groupId of its own, so per-group recency is derived by combining each
    // group's own activity subcollection (already ordered newest-first) rather than reusing
    // the flattened getActivityForUser feed.
    val groupLastActivity: StateFlow<Map<String, com.google.firebase.Timestamp>> = userGroups
        .flatMapLatest { state ->
            val ids = (state as? ScreenState.Success)?.data?.map { it.id } ?: emptyList()
            if (ids.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(ids.map { id -> activityRepository.getActivityForGroup(id).map { id to it.firstOrNull()?.timestamp } }) { pairs ->
                    pairs.mapNotNull { (id, ts) -> ts?.let { id to it } }.toMap()
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val recentActivity: StateFlow<ScreenState<List<Activity>>> = combine(authRepository.getAuthState(), retryTrigger) { state, _ -> state }
        .flatMapLatest { authState ->
            if (authState is AuthState.LoggedIn) {
                groupRepository.getGroupsForUser(authState.uid).flatMapLatest { groups ->
                    val groupIds = groups.map { it.id }
                    activityRepository.getActivityForUser(authState.uid, groupIds)
                        .map<List<Activity>, ScreenState<List<Activity>>> { ScreenState.Success(it) }
                }.catch { emit(ScreenState.Error(it.message ?: "Failed to load activity", ::retry)) }
            } else {
                flowOf(ScreenState.Success(emptyList()))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScreenState.Loading
        )

    val friendsBalances: StateFlow<ScreenState<List<FriendBalance>>> = combine(authRepository.getAuthState(), retryTrigger) { state, _ -> state }
        .flatMapLatest { authState ->
            if (authState is AuthState.LoggedIn) {
                friendRepository.getFriendsBalances(authState.uid)
            } else {
                flowOf(emptyMap())
            }
        }
        .flatMapLatest { balancesMap ->
            flow<ScreenState<List<FriendBalance>>> {
                val friendBalancesList = coroutineScope {
                    balancesMap.map { (uid, byCurrency) ->
                        async {
                            val user = userRepository.getUser(uid)
                            if (user != null) FriendBalance(user, byCurrency) else null
                        }
                    }.awaitAll().filterNotNull()
                }
                emit(ScreenState.Success(friendBalancesList))
            }.catch { emit(ScreenState.Error(it.message ?: "Failed to load friends", ::retry)) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScreenState.Loading
        )

    val recentExpenses: StateFlow<ScreenState<List<Expense>>> = combine(authRepository.getAuthState(), retryTrigger) { state, _ -> state }
        .flatMapLatest { authState ->
            if (authState is AuthState.LoggedIn) {
                expenseRepository.getExpensesForUser(authState.uid)
                    .map<List<Expense>, ScreenState<List<Expense>>> { ScreenState.Success(it) }
                    .catch { emit(ScreenState.Error(it.message ?: "Failed to load expenses", ::retry)) }
            } else {
                flowOf(ScreenState.Success(emptyList()))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScreenState.Loading
        )

    /** UID of the signed-in user, for owner-only affordances like deleting a group. */
    val currentUid: StateFlow<String?> = authRepository.getAuthState()
        .map { (it as? AuthState.LoggedIn)?.uid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Removes the signed-in user from [group]; the group itself lives on for other members. */
    fun leaveGroup(group: Group, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            groupRepository.updateGroup(group.copy(memberIds = group.memberIds - uid))
            onDone()
        }
    }

    /** Puts the user back into a group they just left (the Undo half of [leaveGroup]). */
    fun rejoinGroup(group: Group) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            if (group.memberIds.contains(uid)) return@launch
            groupRepository.updateGroup(group.copy(memberIds = group.memberIds + uid))
        }
    }

    fun deleteGroup(groupId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId)
            onDone()
        }
    }

    /**
     * Re-creates a just-deleted group (the Undo half of [deleteGroup]).
     *
     * Deleting a group only removes its document — the expenses/settlements/activity
     * subcollections underneath are left intact by Firestore — so writing the document back
     * under the same id restores the group and its history together.
     */
    fun restoreGroup(group: Group) {
        viewModelScope.launch {
            groupRepository.updateGroup(group)
        }
    }

    fun createGroup(name: String, currency: String = "INR", avatarKey: String? = null) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val newGroup = Group(
                name = name,
                createdBy = uid,
                memberIds = listOf(uid),
                currency = currency,
                avatarKey = avatarKey
            )
            groupRepository.createGroup(newGroup)
        }
    }
}
