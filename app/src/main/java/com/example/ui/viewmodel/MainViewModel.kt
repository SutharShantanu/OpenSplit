package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Group
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.GroupRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.domain.repository.UserRepository
import com.example.domain.repository.ExpenseRepository

class MainViewModel(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // Fetch groups for the current user
    val userGroups: StateFlow<List<Group>> = authRepository.getAuthState()
        .flatMapLatest { authState ->
            if (authState is com.example.domain.repository.AuthState.LoggedIn) {
                groupRepository.getGroupsForUser(authState.uid)
            } else {
                emptyFlow()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentExpenses: StateFlow<List<com.example.domain.model.Expense>> = authRepository.getAuthState()
        .flatMapLatest { authState ->
            if (authState is com.example.domain.repository.AuthState.LoggedIn) {
                expenseRepository.getExpensesForUser(authState.uid)
            } else {
                emptyFlow()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val friends: StateFlow<List<com.example.domain.model.User>> = userGroups
        .flatMapLatest { groups ->
            val allMemberIds = groups.flatMap { it.memberIds }.distinct()
            val currentUserId = authRepository.getCurrentUserId()
            val friendIds = allMemberIds.filter { it != currentUserId }
            // Fetch users for these ids
            kotlinx.coroutines.flow.flow {
                val friendsList = friendIds.mapNotNull { uid ->
                    userRepository.getUser(uid)
                }
                emit(friendsList)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    fun createGroup(name: String, currency: String = "USD") {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val newGroup = Group(
                name = name,
                createdBy = uid,
                memberIds = listOf(uid), // Add creator as a member
                currency = currency
            )
            groupRepository.createGroup(newGroup)
        }
    }
}
