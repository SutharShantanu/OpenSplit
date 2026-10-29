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
import kotlinx.coroutines.flow.asStateFlow
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
            val name = authRepository.currentUser?.displayName ?: ""
            onResult(friendInviteRepository.sendInvite(uid, trimmed, name).isSuccess)
        }
    }

    fun revokeFriendInvite(inviteId: String) {
        viewModelScope.launch { friendInviteRepository.revokeInvite(inviteId) }
    }

    /** The signed-in user's own add-me link, created on first use. */
    /** Read marker for the Activity badge. Local pref is the source of truth; the user doc is synced best-effort. */
    val lastSeenActivity: StateFlow<Long> = userPreferencesRepository.lastSeenActivityFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    fun markActivitySeen() {
        viewModelScope.launch {
            val now = com.google.firebase.Timestamp.now()
            userPreferencesRepository.setLastSeenActivity(now.seconds)
            authRepository.getCurrentUserId()?.let { runCatching { userRepository.updateLastSeenActivity(it, now) } }
        }
    }

    private val _friendLink = MutableStateFlow<com.opensplit.domain.model.FriendLink?>(null)
    val friendLink: StateFlow<com.opensplit.domain.model.FriendLink?> = _friendLink.asStateFlow()
    private val _friendLinkError = MutableStateFlow<String?>(null)
    val friendLinkError: StateFlow<String?> = _friendLinkError.asStateFlow()
    /** Loads (or reuses) the user's add-me link; [force] re-fetches after a failure. */
    fun ensureFriendLink(force: Boolean = false) {
        if (_friendLink.value != null && !force) return
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val name = authRepository.currentUser?.displayName ?: ""
            _friendLinkError.value = null
            friendInviteRepository.getOrCreateFriendLink(uid, name)
                .onSuccess { _friendLink.value = it }
                .onFailure { _friendLinkError.value = it.message ?: "Couldn't create your invite link" }
        }
    }

    /** Direct (1:1) group with a friend, created on first use; null when it can't be created. */
    fun openDirectGroup(friendUid: String, onReady: (String?) -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch onReady(null)
            val currency = userRepository.getUser(uid)?.defaultCurrency ?: "INR"
            onReady(groupRepository.getOrCreateDirectGroup(uid, friendUid, currency).getOrNull()?.id)
        }
    }

    /** Opens a scanned/pasted friend link: creates the direct group, returns the friend's uid. */
    fun acceptFriendLink(token: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch onResult(Result.failure(IllegalStateException("Sign in first")))
            val currency = userRepository.getUser(uid)?.defaultCurrency ?: "INR"
            onResult(friendInviteRepository.acceptFriendLink(token, uid, currency))
        }
    }

    /** Friend requests addressed to the signed-in user's email. */
    val incomingFriendInvites: StateFlow<List<FriendInvite>> =
        authRepository.getAuthState()
            .flatMapLatest { state ->
                val email = (state as? AuthState.LoggedIn)?.let { authRepository.currentUser?.email }
                if (email.isNullOrBlank()) flowOf(emptyList()) else friendInviteRepository.getIncomingInvites(email)
            }
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Accepting creates the hidden direct group, which makes the pair show in each other's People tab. */
    fun respondToFriendInvite(invite: FriendInvite, accept: Boolean, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val ok = friendInviteRepository.respond(invite.id, accept).isSuccess
            if (ok && accept) {
                val currency = userRepository.getUser(uid)?.defaultCurrency ?: "INR"
                groupRepository.getOrCreateDirectGroup(uid, invite.inviterUid, currency)
            }
            onResult(ok)
        }
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

    val archivedGroupIds: StateFlow<Set<String>> = userPreferencesRepository.archivedGroupIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setGroupArchived(groupId: String, archived: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setGroupArchived(groupId, archived) }
    }

    val pinnedGroupIds: StateFlow<List<String>> = userPreferencesRepository.pinnedGroupIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** [onResult] gets false when the 3-pin limit blocked the change. */
    fun togglePinnedGroup(groupId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onResult(userPreferencesRepository.togglePinnedGroup(groupId)) }
    }

    fun reorderPinned(ordered: List<String>) {
        viewModelScope.launch { userPreferencesRepository.setPinnedGroups(ordered) }
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

    fun createGroup(name: String, currency: String = "INR", avatarKey: String? = null, onDone: () -> Unit = {}) {
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
            onDone()
        }
    }
}
