package com.opensplit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.opensplit.di.AppContainer
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.User
import com.opensplit.util.CurrencyFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class PersonBalanceUiState(
    val friend: User? = null,
    /** Net balance with this friend per currency; positive = friend owes the user. */
    val balancesByCurrency: Map<String, Double> = emptyMap(),
    val sharedExpenses: List<Expense> = emptyList(),
    val primaryCurrency: String = "INR"
)

class PersonBalanceViewModel(
    val friendId: String,
    private val appContainer: AppContainer
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScreenState<PersonBalanceUiState>> = flow {
        emit(ScreenState.Loading)
        val currentUid = appContainer.authRepository.getCurrentUserId()
        if (currentUid == null) {
            emit(ScreenState.Error("User not logged in"))
            return@flow
        }

        val friendUser = appContainer.userRepository.getUser(friendId)

        // Combine user's groups to find shared groups and shared expenses
        appContainer.groupRepository.getGroupsForUser(currentUid).flatMapLatest { groups ->
            val sharedGroups = groups.filter { it.memberIds.contains(friendId) }
            val sharedGroupIds = sharedGroups.map { it.id }

            if (sharedGroupIds.isEmpty()) {
                flowOf(
                    ScreenState.Success(
                        PersonBalanceUiState(
                            friend = friendUser,
                            balancesByCurrency = emptyMap(),
                            sharedExpenses = emptyList(),
                            primaryCurrency = friendUser?.defaultCurrency ?: "INR"
                        )
                    )
                )
            } else {
                // Per shared group: (currency, expenses, settlements).
                val groupFlows = sharedGroups.map { group ->
                    combine(
                        appContainer.expenseRepository.getExpensesForGroup(group.id),
                        appContainer.settlementRepository.getSettlementsForGroup(group.id)
                    ) { expenses, settlements -> Triple(group.currency.ifEmpty { "INR" }, expenses, settlements) }
                }
                combine(groupFlows) { groupData ->
                    val sharedExp = groupData.flatMap { it.second }
                        .filter { !it.isDeleted }
                        .filter { exp ->
                            val relatesToFriend = exp.paidBy == friendId || exp.splits.any { it.uid == friendId } || exp.multiPayer?.containsKey(friendId) == true
                            val relatesToMe = exp.paidBy == currentUid || exp.splits.any { it.uid == currentUid } || exp.multiPayer?.containsKey(currentUid) == true
                            relatesToFriend && relatesToMe
                        }

                    // Net balance with this friend, per currency (positive = friend owes user).
                    val balancesByCurrency = mutableMapOf<String, Double>()
                    groupData.forEach { (currency, expenses, settlements) ->
                        expenses.filter { !it.isDeleted }.forEach { exp ->
                            val paidByMe = if (exp.multiPayer != null && exp.multiPayer.isNotEmpty()) exp.multiPayer[currentUid] ?: 0.0 else if (exp.paidBy == currentUid) exp.amount else 0.0
                            val paidByFriend = if (exp.multiPayer != null && exp.multiPayer.isNotEmpty()) exp.multiPayer[friendId] ?: 0.0 else if (exp.paidBy == friendId) exp.amount else 0.0
                            val myShare = exp.splits.find { it.uid == currentUid }?.amount ?: 0.0
                            val friendShare = exp.splits.find { it.uid == friendId }?.amount ?: 0.0
                            val delta = (paidByMe - myShare) - (paidByFriend - friendShare)
                            if (delta != 0.0) balancesByCurrency[currency] = (balancesByCurrency[currency] ?: 0.0) + delta
                        }
                        // Apply settlements strictly between these two users.
                        settlements.forEach { s ->
                            when {
                                s.fromUid == currentUid && s.toUid == friendId ->
                                    balancesByCurrency[currency] = (balancesByCurrency[currency] ?: 0.0) + s.amount
                                s.fromUid == friendId && s.toUid == currentUid ->
                                    balancesByCurrency[currency] = (balancesByCurrency[currency] ?: 0.0) - s.amount
                            }
                        }
                    }

                    ScreenState.Success(
                        PersonBalanceUiState(
                            friend = friendUser,
                            balancesByCurrency = balancesByCurrency.filterValues { kotlin.math.abs(it) > 0.001 },
                            sharedExpenses = sharedExp.sortedByDescending { it.date },
                            primaryCurrency = sharedGroups.firstOrNull()?.currency ?: "INR"
                        )
                    )
                }
            }
        }.collect { emit(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenState.Loading)
}

class PersonBalanceViewModelFactory(
    private val friendId: String,
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PersonBalanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PersonBalanceViewModel(friendId, appContainer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
