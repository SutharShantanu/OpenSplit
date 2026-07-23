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
    val balance: Double = 0.0,
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
                            balance = 0.0,
                            sharedExpenses = emptyList(),
                            primaryCurrency = friendUser?.defaultCurrency ?: "INR"
                        )
                    )
                )
            } else {
                val expenseFlows = sharedGroupIds.map { appContainer.expenseRepository.getExpensesForGroup(it) }
                combine(expenseFlows) { expenseArrays ->
                    val allExpenses = expenseArrays.flatMap { it }.filter { !it.isDeleted }
                    val sharedExp = allExpenses.filter { exp ->
                        val relatesToFriend = exp.paidBy == friendId || exp.splits.any { it.uid == friendId } || exp.multiPayer?.containsKey(friendId) == true
                        val relatesToMe = exp.paidBy == currentUid || exp.splits.any { it.uid == currentUid } || exp.multiPayer?.containsKey(currentUid) == true
                        relatesToFriend && relatesToMe
                    }

                    // Compute balance specifically with this friend
                    var netBalance = 0.0
                    var mainCurrency = sharedGroups.firstOrNull()?.currency ?: "INR"

                    sharedExp.forEach { exp ->
                        val paidByMe = if (exp.multiPayer != null && exp.multiPayer.isNotEmpty()) exp.multiPayer[currentUid] ?: 0.0 else if (exp.paidBy == currentUid) exp.amount else 0.0
                        val paidByFriend = if (exp.multiPayer != null && exp.multiPayer.isNotEmpty()) exp.multiPayer[friendId] ?: 0.0 else if (exp.paidBy == friendId) exp.amount else 0.0

                        val myShare = exp.splits.find { it.uid == currentUid }?.amount ?: 0.0
                        val friendShare = exp.splits.find { it.uid == friendId }?.amount ?: 0.0

                        netBalance += (paidByMe - myShare) - (paidByFriend - friendShare)
                    }

                    ScreenState.Success(
                        PersonBalanceUiState(
                            friend = friendUser,
                            balance = netBalance,
                            sharedExpenses = sharedExp.sortedByDescending { it.date },
                            primaryCurrency = mainCurrency
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
