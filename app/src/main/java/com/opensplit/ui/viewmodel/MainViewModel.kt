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
import com.opensplit.domain.repository.ExpenseRepository
import com.opensplit.domain.repository.FriendRepository
import com.opensplit.domain.repository.GroupRepository
import com.opensplit.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FriendBalance(val user: User, val balance: Double)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository,
    private val friendRepository: FriendRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)
    fun retry() { retryTrigger.value++ }

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
                    balancesMap.map { (uid, balance) ->
                        async {
                            val user = userRepository.getUser(uid)
                            if (user != null) FriendBalance(user, balance) else null
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

    fun createGroup(name: String, currency: String = "INR") {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val newGroup = Group(
                name = name,
                createdBy = uid,
                memberIds = listOf(uid),
                currency = currency
            )
            groupRepository.createGroup(newGroup)
        }
    }
}
