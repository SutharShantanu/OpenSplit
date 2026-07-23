package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Activity
import com.example.domain.model.ActivityType
import com.example.domain.model.Expense
import com.example.domain.model.Group
import com.example.domain.model.User
import com.example.domain.repository.ActivityRepository
import com.example.domain.repository.ExpenseRepository
import com.example.domain.repository.GroupRepository
import com.example.domain.repository.UserRepository
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
    val simplifiedSettlements: List<com.example.domain.logic.DebtSimplifier.SettlementSuggestion>
)

class GroupDetailViewModel(
    private val groupId: String,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<ScreenState<GroupDetailUiState>> = retryTrigger
        .flatMapLatest {
            flow {
                emit(ScreenState.Loading)
                try {
                    val g = groupRepository.getGroup(groupId) ?: throw Exception("Group not found")
                    val members = loadMembers(g.memberIds)

                    expenseRepository.getExpensesForGroup(groupId).collect { expList ->
                        val (balances, settlements) = calculateBalances(expList, g.simplifyDebts, g.currency)
                        emit(ScreenState.Success(GroupDetailUiState(g, expList, members, balances, settlements)))
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
            val payers = mutableMapOf<String, Double>()
            payers[expense.paidBy] = convertedAmount

            for ((uid, amount) in payers) {
                bals[uid] = (bals[uid] ?: 0.0) + amount
            }

            for (split in expense.splits) {
                bals[split.uid] = (bals[split.uid] ?: 0.0) - (split.amount * rate)
            }
        }

        val settlements = if (simplifyDebts) {
            com.example.domain.logic.DebtSimplifier.simplify(bals)
        } else {
            com.example.domain.logic.DebtSimplifier.simplify(bals)
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
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState !is ScreenState.Success) return@launch

            val users = userRepository.searchUsersByEmail(email).first()
            val user = users.firstOrNull { it.email.equals(email, ignoreCase = true) }
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
                            actorUid = "",
                            message = "added member '${user.displayName}'"
                        )
                    )
                    retry()
                }
            }
        }
    }

    fun addMemberFromContact(emailOrPhone: String, context: android.content.Context) {
        addMemberByEmail(emailOrPhone)
    }
}
