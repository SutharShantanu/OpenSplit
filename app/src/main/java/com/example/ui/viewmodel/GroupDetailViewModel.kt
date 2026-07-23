package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Activity
import com.example.domain.model.ActivityType
import com.example.domain.model.Expense
import com.example.domain.model.Group
import com.example.domain.model.User
import com.example.domain.repository.ActivityRepository
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ExpenseRepository
import com.example.domain.repository.GroupRepository
import com.example.domain.repository.UserRepository
import com.example.domain.model.PendingInvite
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
    val simplifiedSettlements: List<com.example.domain.logic.DebtSimplifier.SettlementSuggestion>,
    val pendingInvites: List<PendingInvite> = emptyList()
)

class GroupDetailViewModel(
    private val groupId: String,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository,
    private val activityRepository: ActivityRepository,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)
    private val _pendingInvites = MutableStateFlow<List<PendingInvite>>(emptyList())

    val uiState: StateFlow<ScreenState<GroupDetailUiState>> = combine(
        retryTrigger,
        _pendingInvites
    ) { _, pendingList ->
        pendingList
    }.flatMapLatest { pendingList ->
        flow {
            emit(ScreenState.Loading)
            try {
                val g = groupRepository.getGroup(groupId) ?: throw Exception("Group not found")
                val members = loadMembers(g.memberIds)

                expenseRepository.getExpensesForGroup(groupId).collect { expList ->
                    val (balances, settlements) = calculateBalances(expList, g.simplifyDebts, g.currency)
                    emit(ScreenState.Success(GroupDetailUiState(g, expList, members, balances, settlements, pendingList)))
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

    private fun calculateBalances(expList: List<Expense>, simplifyDebts: Boolean, groupCurrency: String): Pair<Map<String, Double>, List<com.example.domain.logic.DebtSimplifier.SettlementSuggestion>> {
        val bals = mutableMapOf<String, Double>()

        for (expense in expList) {
            val rate = 1.0
            val convertedAmount = expense.amount * rate
            val payers: Map<String, Double> = expense.multiPayer
                ?: mapOf(expense.paidBy to convertedAmount)

            for ((uid, amount) in payers) {
                bals[uid] = (bals[uid] ?: 0.0) + (amount * rate)
            }

            for (split in expense.splits) {
                bals[split.uid] = (bals[split.uid] ?: 0.0) - (split.amount * rate)
            }
        }

        val settlements = if (simplifyDebts) {
            com.example.domain.logic.DebtSimplifier.simplify(bals)
        } else {
            // Raw pairwise: one suggestion per ordered (debtor, creditor) pair
            bals.entries
                .filter { it.value < -0.01 }
                .flatMap { debtor ->
                    bals.entries
                        .filter { it.value > 0.01 }
                        .map { creditor ->
                            com.example.domain.logic.DebtSimplifier.SettlementSuggestion(
                                fromUid = debtor.key,
                                toUid = creditor.key,
                                amount = minOf(-debtor.value, creditor.value)
                            )
                        }
                }
                .filter { it.amount > 0.01 }
        }
        return bals to settlements
    }

    fun addExpense(expense: Expense, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val res = expenseRepository.addExpense(expense)
            if (res.isSuccess) {
                onSuccess()
            }
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
                // User not registered yet - create pending invite
                val newInvite = PendingInvite(
                    id = java.util.UUID.randomUUID().toString(),
                    groupId = groupId,
                    email = trimmedEmail,
                    invitedBy = actorUid
                )
                _pendingInvites.update { current -> current.filterNot { it.email.equals(trimmedEmail, ignoreCase = true) } + newInvite }
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

    fun revokeInvite(inviteId: String) {
        _pendingInvites.update { list -> list.filterNot { it.id == inviteId } }
    }

    fun addMemberFromContact(emailOrPhone: String, context: android.content.Context) {
        addMemberByEmail(emailOrPhone)
    }
}
