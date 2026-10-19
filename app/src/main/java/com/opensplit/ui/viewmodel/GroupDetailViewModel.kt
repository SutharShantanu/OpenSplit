package com.opensplit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opensplit.domain.model.Activity
import com.opensplit.domain.model.ActivityType
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Group
import com.opensplit.domain.model.User
import com.opensplit.domain.repository.ActivityRepository
import com.opensplit.domain.repository.AuthRepository
import com.opensplit.domain.repository.ExpenseRepository
import com.opensplit.domain.repository.GroupRepository
import com.opensplit.domain.repository.PendingInviteRepository
import com.opensplit.domain.repository.SettlementRepository
import com.opensplit.domain.repository.StorageRepository
import com.opensplit.domain.repository.UserRepository
import com.opensplit.domain.logic.BalanceCalculator
import com.opensplit.domain.logic.DebtSimplifier
import com.opensplit.domain.model.PendingInvite
import com.opensplit.domain.model.Settlement
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GroupDetailUiState(
    val group: Group,
    val expenses: List<Expense>,
    val members: List<User>,
    val balances: Map<String, Double>,
    val simplifiedSettlements: List<DebtSimplifier.SettlementSuggestion>,
    val settlements: List<Settlement> = emptyList(),
    val pendingInvites: List<PendingInvite> = emptyList(),
    /** People who left or were removed; shown with a "Left" badge, names kept on old expenses. */
    val formerMembers: List<User> = emptyList()
)

class GroupDetailViewModel(
    val groupId: String,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository,
    private val activityRepository: ActivityRepository,
    private val settlementRepository: SettlementRepository,
    private val pendingInviteRepository: PendingInviteRepository,
    private val authRepository: AuthRepository? = null,
    private val storageRepository: StorageRepository? = null,
    private val inviteLinkRepository: com.opensplit.domain.repository.InviteLinkRepository? = null
) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val inviteLinks: StateFlow<List<com.opensplit.domain.model.InviteLink>> =
        (inviteLinkRepository?.linksForGroup(groupId) ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createInviteLink(expiresInSeconds: Long?, maxUses: Int, onResult: (Boolean) -> Unit = {}) {
        val g = currentGroup() ?: return
        viewModelScope.launch {
            val uid = authRepository?.getCurrentUserId() ?: return@launch
            onResult(inviteLinkRepository?.create(g, uid, expiresInSeconds, maxUses)?.isSuccess == true)
        }
    }

    fun revokeInviteLink(token: String) {
        viewModelScope.launch { inviteLinkRepository?.revoke(token) }
    }

    fun setDefaultSplitType(type: com.opensplit.domain.model.SplitType) {
        val g = currentGroup() ?: return
        viewModelScope.launch { groupRepository.updateGroup(g.copy(defaultSplitType = type)); retry() }
    }

    /** Net balance of [uid] in this group; leaving is only allowed at zero. */
    fun balanceOf(uid: String): Double = (uiState.value as? ScreenState.Success)?.data?.balances?.get(uid) ?: 0.0

    private val retryTrigger = MutableStateFlow(0)
    private var lastKnownGroup: Group? = null

    val uiState: StateFlow<ScreenState<GroupDetailUiState>> = retryTrigger.flatMapLatest {
        flow {
            emit(ScreenState.Loading)
            try {
                val g = groupRepository.getGroup(groupId) ?: lastKnownGroup ?: throw Exception("Group not found")
                lastKnownGroup = g
                val members = loadMembers(g.memberIds)
                val former = loadMembers(g.formerMemberIds.filterNot { it in g.memberIds })

                combine(
                    expenseRepository.getExpensesForGroup(groupId),
                    settlementRepository.getSettlementsForGroup(groupId),
                    pendingInviteRepository.getPendingInvites(groupId)
                ) { expList, settleList, pendingList -> Triple(expList, settleList, pendingList) }
                    .collect { (expList, settleList, pendingList) ->
                        val balances = BalanceCalculator.netBalances(expList, settleList)
                        val suggestions = BalanceCalculator.settlementSuggestions(expList, settleList, g.simplifyDebts)
                        val resolvedMembers = members.map { member ->
                            if (member.displayName == "Member" || member.email.isBlank()) {
                                val match = pendingList.firstOrNull { it.groupId == groupId }
                                if (match != null && match.email.isNotBlank()) {
                                    val name = match.email.substringBefore("@")
                                        .replace(".", " ")
                                        .split(" ")
                                        .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } }
                                    val updated = member.copy(displayName = name, email = match.email)
                                    userRepository.saveUser(updated)
                                    updated
                                } else member
                            } else member
                        }
                        emit(
                            ScreenState.Success(
                                GroupDetailUiState(g, expList, resolvedMembers, balances, suggestions, settleList, pendingList, former)
                            )
                        )
                    }
            } catch (e: Exception) {
                emit(ScreenState.Error(e.message ?: "Unknown error") { retry() })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenState.Loading)

    fun retry() { retryTrigger.value++ }

    val currentUserId: String get() = authRepository?.currentUserId ?: ""

    private suspend fun loadMembers(userIds: List<String>): List<User> {
        val friendsMap = com.opensplit.data.local.InMemoryDataStore.friends.value.associateBy { it.uid }
        val currentUser = authRepository?.currentUser
        val currentUid = authRepository?.currentUserId
        return userIds.map { uid ->
            friendsMap[uid]
                ?: (if (currentUid != null && currentUid == uid) User(uid = uid, displayName = currentUser?.displayName ?: "You", email = currentUser?.email ?: "") else null)
                ?: userRepository.getUser(uid)
                ?: User(uid = uid, displayName = "Member", email = "")
        }
    }

    /** Uploads [receiptUri] (if any) to storage first, then persists the expense with its resulting URL. */
    private suspend fun withUploadedReceipt(expense: Expense, receiptUri: android.net.Uri?): Expense {
        if (receiptUri == null || storageRepository == null) return expense
        val uploaded = storageRepository.uploadReceipt(groupId, receiptUri).getOrNull()
        return if (uploaded != null) expense.copy(receiptImageUrl = uploaded) else expense
    }

    fun addExpense(expense: Expense, receiptUri: android.net.Uri? = null, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val finalExpense = withUploadedReceipt(expense, receiptUri)
            expenseRepository.addExpense(finalExpense)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Couldn't save the expense") }
        }
    }

    fun updateExpense(expense: Expense, receiptUri: android.net.Uri? = null, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val finalExpense = withUploadedReceipt(expense, receiptUri)
            val actor = authRepository?.getCurrentUserId() ?: expense.createdBy
            expenseRepository.updateExpense(finalExpense, actor)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Couldn't update the expense") }
        }
    }

    // --- Group management ---

    private fun currentGroup(): Group? = (uiState.value as? ScreenState.Success)?.data?.group

    fun renameGroup(newName: String) {
        val g = currentGroup() ?: return
        if (newName.isBlank() || newName.trim() == g.name) return
        val updated = g.copy(name = newName.trim())
        lastKnownGroup = updated
        viewModelScope.launch {
            groupRepository.updateGroup(updated)
            retry()
        }
    }

    fun setGroupCurrency(currency: String) {
        val g = currentGroup() ?: return
        if (currency == g.currency) return
        val updated = g.copy(currency = currency)
        lastKnownGroup = updated
        viewModelScope.launch {
            groupRepository.updateGroup(updated)
            retry()
        }
    }

    fun setSimplifyDebts(enabled: Boolean) {
        val g = currentGroup() ?: return
        if (enabled == g.simplifyDebts) return
        val updated = g.copy(simplifyDebts = enabled)
        lastKnownGroup = updated
        viewModelScope.launch {
            groupRepository.updateGroup(updated)
            retry()
        }
    }

    fun setGroupAvatar(avatarKey: String) {
        val g = currentGroup() ?: return
        if (avatarKey == g.avatarKey) return
        val updated = g.copy(avatarKey = avatarKey)
        lastKnownGroup = updated
        viewModelScope.launch {
            groupRepository.updateGroup(updated)
            retry()
        }
    }

    /** Puts a removed member back into the group (the Undo half of [removeMember]). */
    fun restoreMember(uid: String) {
        val g = currentGroup() ?: return
        if (g.memberIds.contains(uid)) return
        viewModelScope.launch {
            groupRepository.updateGroup(g.copy(memberIds = g.memberIds + uid, formerMemberIds = g.formerMemberIds - uid))
            retry()
        }
    }

    fun removeMember(uid: String) {
        val g = currentGroup() ?: return
        if (!g.memberIds.contains(uid)) return
        viewModelScope.launch {
            groupRepository.updateGroup(g.copy(memberIds = g.memberIds - uid, formerMemberIds = (g.formerMemberIds + uid).distinct()))
            val actor = authRepository?.getCurrentUserId() ?: ""
            val name = (uiState.value as? ScreenState.Success)?.data?.members?.find { it.uid == uid }?.displayName ?: "a member"
            activityRepository.logActivity(
                groupId,
                Activity(type = ActivityType.MEMBER_REMOVED, actorUid = actor, message = "removed '$name'")
            )
            retry()
        }
    }

    fun leaveGroup(onDone: () -> Unit) {
        val g = currentGroup() ?: return
        viewModelScope.launch {
            val uid = authRepository?.getCurrentUserId() ?: return@launch
            groupRepository.updateGroup(g.copy(memberIds = g.memberIds - uid, formerMemberIds = (g.formerMemberIds + uid).distinct()))
            activityRepository.logActivity(groupId, Activity(type = ActivityType.MEMBER_REMOVED, actorUid = uid, message = "left the group"))
            onDone()
        }
    }

    fun deleteExpense(expenseId: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(expenseRepository.deleteExpense(groupId, expenseId).isSuccess) }
    }

    fun restoreExpense(expenseId: String) {
        viewModelScope.launch { expenseRepository.restoreExpense(groupId, expenseId) }
    }

    fun deleteGroup(onDone: () -> Unit) {
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId)
            onDone()
        }
    }

    fun addMemberByEmail(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) return
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState !is ScreenState.Success) return@launch

            val users = userRepository.searchUsersByEmail(trimmedEmail).first()
            var user = users.firstOrNull { it.email.equals(trimmedEmail, ignoreCase = true) }
            val actorUid = authRepository?.getCurrentUserId() ?: ""
            val currentGroup = currentState.data.group

            if (user == null) {
                val newUid = "user_" + java.util.UUID.randomUUID().toString().take(8)
                val displayName = trimmedEmail.substringBefore("@")
                    .replace(".", " ")
                    .split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } }
                val newUser = User(uid = newUid, displayName = displayName, email = trimmedEmail)
                userRepository.saveUser(newUser)
                user = newUser

                val alreadyInvited = currentState.data.pendingInvites
                    .any { it.email.equals(trimmedEmail, ignoreCase = true) }
                if (!alreadyInvited) {
                    val newInvite = PendingInvite(
                        groupId = groupId,
                        email = trimmedEmail,
                        invitedBy = actorUid
                    )
                    pendingInviteRepository.addInvite(newInvite)
                }
            }

            if (!currentGroup.memberIds.contains(user.uid)) {
                val updatedMembers = currentGroup.memberIds + user.uid
                val updatedGroup = currentGroup.copy(memberIds = updatedMembers)
                groupRepository.updateGroup(updatedGroup)

                activityRepository.logActivity(
                    groupId,
                    Activity(
                        type = ActivityType.MEMBER_ADDED,
                        actorUid = actorUid,
                        message = "added member '${user.displayName}'"
                    )
                )
                retry()
            }
        }
    }

    fun revokeInvite(inviteId: String) {
        viewModelScope.launch {
            pendingInviteRepository.revokeInvite(groupId, inviteId)
        }
    }

}
