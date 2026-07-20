package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Expense
import com.example.domain.model.Group
import com.example.domain.model.User
import com.example.domain.repository.ExpenseRepository
import com.example.domain.repository.GroupRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    private val groupId: String,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _members = MutableStateFlow<List<User>>(emptyList())
    val members: StateFlow<List<User>> = _members.asStateFlow()

    private val _balances = MutableStateFlow<Map<String, Double>>(emptyMap())
    val balances: StateFlow<Map<String, Double>> = _balances.asStateFlow()

    init {
        loadGroup()
        loadExpenses()
    }

    private fun loadGroup() {
        viewModelScope.launch {
            val g = groupRepository.getGroup(groupId)
            _group.value = g
            g?.memberIds?.let { loadMembers(it) }
        }
    }

    private fun loadExpenses() {
        viewModelScope.launch {
            expenseRepository.getExpensesForGroup(groupId).collect { expList ->
                _expenses.value = expList
                calculateBalances(expList)
            }
        }
    }

    private fun calculateBalances(expList: List<Expense>) {
        val bals = mutableMapOf<String, Double>()
        for (expense in expList) {
            val paidBy = expense.paidBy
            bals[paidBy] = (bals[paidBy] ?: 0.0) + expense.amount
            
            for (split in expense.splits) {
                bals[split.uid] = (bals[split.uid] ?: 0.0) - split.amount
            }
        }
        _balances.value = bals
    }

    private fun loadMembers(userIds: List<String>) {
        viewModelScope.launch {
            val loadedMembers = mutableListOf<User>()
            for (uid in userIds) {
                userRepository.getUser(uid)?.let { loadedMembers.add(it) }
            }
            _members.value = loadedMembers
        }
    }

    fun addExpense(description: String, amount: Double, paidBy: String, splitStrategy: String = "EQUAL") {
        viewModelScope.launch {
            val memberIds = _group.value?.memberIds ?: return@launch
            if (memberIds.isEmpty()) return@launch

            val amountPerPerson = amount / memberIds.size
            val splits = memberIds.map { uid ->
                com.example.domain.model.ExpenseSplit(uid = uid, amount = amountPerPerson)
            }

            val newExpense = Expense(
                groupId = groupId,
                description = description,
                amount = amount,
                currency = _group.value?.currency ?: "USD",
                paidBy = paidBy,
                splits = splits
            )
            expenseRepository.addExpense(newExpense)
        }
    }

    fun addMemberByEmail(email: String) {
        viewModelScope.launch {
            // Find user by email
            userRepository.searchUsersByEmail(email).collect { users ->
                val user = users.firstOrNull { it.email.equals(email, ignoreCase = true) }
                if (user != null) {
                    val currentGroup = _group.value ?: return@collect
                    if (!currentGroup.memberIds.contains(user.uid)) {
                        val updatedMembers = currentGroup.memberIds + user.uid
                        val updatedGroup = currentGroup.copy(memberIds = updatedMembers)
                        groupRepository.updateGroup(updatedGroup)
                        _group.value = updatedGroup
                        loadMembers(updatedMembers)
                    }
                }
            }
        }
    }
}
