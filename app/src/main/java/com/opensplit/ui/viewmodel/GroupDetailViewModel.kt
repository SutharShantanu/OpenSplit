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
    val pendingInvites: List<PendingInvite> = emptyList()
)

class GroupDetailViewModel(
    private val groupId: String,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository,
    private val activityRepository: ActivityRepository,
    private val settlementRepository: SettlementRepository,
    private val pendingInviteRepository: PendingInviteRepository,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<ScreenState<GroupDetailUiState>> = retryTrigger.flatMapLatest {
        flow {
            emit(ScreenState.Loading)
            try {
                val g = groupRepository.getGroup(groupId) ?: throw Exception("Group not found")
                val members = loadMembers(g.memberIds)

                combine(
                    expenseRepository.getExpensesForGroup(groupId),
                    settlementRepository.getSettlementsForGroup(groupId),
                    pendingInviteRepository.getPendingInvites(groupId)
                ) { expList, settleList, pendingList -> Triple(expList, settleList, pendingList) }
                    .collect { (expList, settleList, pendingList) ->
                        val balances = BalanceCalculator.netBalances(expList, settleList)
                        val suggestions = BalanceCalculator.settlementSuggestions(expList, settleList, g.simplifyDebts)
                        emit(
                            ScreenState.Success(
                                GroupDetailUiState(g, expList, members, balances, suggestions, settleList, pendingList)
                            )
                        )
                    }
            } catch (e: Exception) {
                emit(ScreenState.Error(e.message ?: "Unknown error") { retry() })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenState.Loading)

    fun retry() { retryTrigger.value++ }

    private suspend fun loadMembers(userIds: List<String>): List<User> {
        return coroutineScope {
            userIds.map { uid ->
                async { userRepository.getUser(uid) }
            }.awaitAll().filterNotNull()
        }
    }

    fun addExpense(expense: Expense, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val res = expenseRepository.addExpense(expense)
            if (res.isSuccess) {
                onSuccess()
            }
        }
    }

    fun updateExpense(expense: Expense, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val res = expenseRepository.updateExpense(expense)
            if (res.isSuccess) {
                onSuccess()
            }
        }
    }

    // --- Group management ---

    private fun currentGroup(): Group? = (uiState.value as? ScreenState.Success)?.data?.group

    fun renameGroup(newName: String) {
        val g = currentGroup() ?: return
        if (newName.isBlank() || newName.trim() == g.name) return
        viewModelScope.launch {
            groupRepository.updateGroup(g.copy(name = newName.trim()))
            retry()
        }
    }

    fun setGroupCurrency(currency: String) {
        val g = currentGroup() ?: return
        if (currency == g.currency) return
        viewModelScope.launch {
            groupRepository.updateGroup(g.copy(currency = currency))
            retry()
        }
    }

    fun setSimplifyDebts(enabled: Boolean) {
        val g = currentGroup() ?: return
        if (enabled == g.simplifyDebts) return
        viewModelScope.launch {
            groupRepository.updateGroup(g.copy(simplifyDebts = enabled))
            retry()
        }
    }

    fun removeMember(uid: String) {
        val g = currentGroup() ?: return
        if (!g.memberIds.contains(uid)) return
        viewModelScope.launch {
            groupRepository.updateGroup(g.copy(memberIds = g.memberIds - uid))
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
            groupRepository.updateGroup(g.copy(memberIds = g.memberIds - uid))
            onDone()
        }
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
            val user = users.firstOrNull { it.email.equals(trimmedEmail, ignoreCase = true) }
            val actorUid = authRepository?.getCurrentUserId() ?: ""
            if (user != null) {
                val currentGroup = currentState.data.group
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
            } else {
                // User not registered yet - persist a pending invite in Firestore.
                val alreadyInvited = currentState.data.pendingInvites
                    .any { it.email.equals(trimmedEmail, ignoreCase = true) }
                if (!alreadyInvited) {
                    val newInvite = PendingInvite(
                        groupId = groupId,
                        email = trimmedEmail,
                        invitedBy = actorUid
                    )
                    pendingInviteRepository.addInvite(newInvite)
                    activityRepository.logActivity(
                        groupId,
                        Activity(
                            type = ActivityType.MEMBER_ADDED,
                            actorUid = actorUid,
                            message = "sent invite to '$trimmedEmail'"
                        )
                    )
                }
            }
        }
    }

    fun revokeInvite(inviteId: String) {
        viewModelScope.launch {
            pendingInviteRepository.revokeInvite(groupId, inviteId)
        }
    }

    fun addMemberFromContact(emailOrPhone: String, context: android.content.Context) {
        addMemberByEmail(emailOrPhone)
    }
}
