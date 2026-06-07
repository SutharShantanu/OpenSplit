package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.DebtRelation
import com.example.data.repository.SplitRepository
import com.example.data.repository.UserBalance
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class SplitViewModel(
    application: Application,
    private val repository: SplitRepository
) : AndroidViewModel(application) {

    // --- Active States ---
    val allGroups: StateFlow<List<Group>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup.asStateFlow()

    // Reactive lists tied to the selected group
    val groupMembers: StateFlow<List<User>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getGroupMembers(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupExpenses: StateFlow<List<Expense>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getExpensesForGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupSettlements: StateFlow<List<Settlement>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getSettlementsForGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupRecurring: StateFlow<List<RecurringExpense>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getRecurringExpensesForGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated balances & debts
    private val _groupBalances = MutableStateFlow<List<UserBalance>>(emptyList())
    val groupBalances: StateFlow<List<UserBalance>> = _groupBalances.asStateFlow()

    private val _simplifiedDebts = MutableStateFlow<List<DebtRelation>>(emptyList())
    val simplifiedDebts: StateFlow<List<DebtRelation>> = _simplifiedDebts.asStateFlow()

    // Sync queue simulation
    private val _syncQueueSize = MutableStateFlow(0)
    val syncQueueSize: StateFlow<Int> = _syncQueueSize.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        viewModelScope.launch {
            // Guarantee sample data on initial boot
            repository.checkAndSeedDatabase()
        }
    }

    fun toggleOnlineStatus() {
        _isOnline.value = !_isOnline.value
        if (_isOnline.value && _syncQueueSize.value > 0) {
            triggerSync()
        }
    }

    fun triggerSync() {
        if (!_isOnline.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            kotlinx.coroutines.delay(1800) // mock sync network time
            _syncQueueSize.value = 0
            _isSyncing.value = false
        }
    }

    fun selectGroup(groupId: String?) {
        _selectedGroupId.value = groupId
        viewModelScope.launch {
            if (groupId == null) {
                _selectedGroup.value = null
                _groupBalances.value = emptyList()
                _simplifiedDebts.value = emptyList()
            } else {
                _selectedGroup.value = repository.getGroupById(groupId)
                recalculateDebtsAndBalances(groupId)
            }
        }
    }

    suspend fun recalculateDebtsAndBalances(groupId: String) {
        val balances = repository.getGroupBalances(groupId)
        val debts = repository.computeSimplifiedDebts(groupId)
        _groupBalances.value = balances
        _simplifiedDebts.value = debts
    }

    // --- Action Methods ---

    fun createNewGroup(name: String, description: String, category: String, memberIds: List<String>, currency: String) {
        viewModelScope.launch {
            val groupId = UUID.randomUUID().toString()
            val newGroup = Group(
                id = groupId,
                name = name,
                description = description,
                category = category,
                currency = currency
            )
            val fullMemberIds = (memberIds + "current_user").distinct()
            repository.createGroup(newGroup, fullMemberIds)

            if (!_isOnline.value) {
                _syncQueueSize.value += 1
            }
            selectGroup(groupId)
        }
    }

    fun deleteSelectedGroup() {
        val activeId = _selectedGroupId.value ?: return
        viewModelScope.launch {
            repository.deleteGroup(activeId)
            selectGroup(null)
            if (!_isOnline.value) {
                _syncQueueSize.value += 1
            }
        }
    }

    // Creates an expense according to target splitting rules
    fun createExpense(
        title: String,
        description: String,
        amount: Double,
        paidById: String,
        category: String,
        splitMethod: String, // EQUAL, PERCENT, EXACT, SHARES
        memberUserIds: List<String>,
        customRatios: Map<String, Double> // maps userId -> values (percent, exact, or share numbers)
    ): String? {
        val groupId = _selectedGroupId.value ?: return "No group selected"

        // Validation & mathematical split division
        val totalMembers = memberUserIds.size
        if (totalMembers == 0) return "At least one member must be selected for splits"

        val splits = mutableListOf<ExpenseSplit>()
        val expenseId = UUID.randomUUID().toString()

        when (splitMethod) {
            "EQUAL" -> {
                val dividedAmt = amount / totalMembers
                memberUserIds.forEach { uid ->
                    splits.add(ExpenseSplit(UUID.randomUUID().toString(), expenseId, uid, dividedAmt))
                }
            }
            "PERCENT" -> {
                val sumP = customRatios.values.sum()
                if (kotlin.math.abs(sumP - 100.0) > 0.1) {
                    return "Percentages must sum to exactly 100% (currently ${sumP}%)"
                }
                memberUserIds.forEach { uid ->
                    val ratioValue = customRatios[uid] ?: 0.0
                    val computedAmt = (ratioValue / 100.0) * amount
                    splits.add(ExpenseSplit(UUID.randomUUID().toString(), expenseId, uid, computedAmt, percentage = ratioValue))
                }
            }
            "EXACT" -> {
                val sumE = customRatios.values.sum()
                if (kotlin.math.abs(sumE - amount) > 0.05) {
                    return "Exact sums must equal the total amount $amount (currently $sumE)"
                }
                memberUserIds.forEach { uid ->
                    val calculatedAmt = customRatios[uid] ?: 0.0
                    splits.add(ExpenseSplit(UUID.randomUUID().toString(), expenseId, uid, calculatedAmt))
                }
            }
            "SHARES" -> {
                val sumS = customRatios.values.sum()
                if (sumS <= 0.0) {
                    return "Total shares must be positive"
                }
                memberUserIds.forEach { uid ->
                    val shareCount = customRatios[uid] ?: 0.0
                    val computedAmt = (shareCount / sumS) * amount
                    splits.add(ExpenseSplit(UUID.randomUUID().toString(), expenseId, uid, computedAmt, shares = shareCount))
                }
            }
        }

        val currency = _selectedGroup.value?.currency ?: "$"
        viewModelScope.launch {
            val element = Expense(
                id = expenseId,
                groupId = groupId,
                title = title,
                description = description,
                amount = amount,
                currency = currency,
                paidById = paidById,
                category = category,
                splitMethod = splitMethod
            )
            repository.addExpense(element, splits)
            recalculateDebtsAndBalances(groupId)

            if (!_isOnline.value) {
                _syncQueueSize.value += 1
            }
        }
        return null // success
    }

    fun removeExpense(expenseId: String) {
        val groupId = _selectedGroupId.value ?: return
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            recalculateDebtsAndBalances(groupId)

            if (!_isOnline.value) {
                _syncQueueSize.value += 1
            }
        }
    }

    fun recordSettlement(senderId: String, receiverId: String, amount: Double, paymentType: String) {
        val groupId = _selectedGroupId.value ?: return
        viewModelScope.launch {
            val settlement = Settlement(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                senderId = senderId,
                receiverId = receiverId,
                amount = amount,
                paymentType = paymentType
            )
            repository.addSettlement(settlement)
            recalculateDebtsAndBalances(groupId)

            if (!_isOnline.value) {
                _syncQueueSize.value += 1
            }
        }
    }

    fun createRecurringExpense(title: String, amount: Double, category: String, paidById: String, frequency: String) {
        val groupId = _selectedGroupId.value ?: return
        val currency = _selectedGroup.value?.currency ?: "$"
        viewModelScope.launch {
            val recurring = RecurringExpense(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                title = title,
                amount = amount,
                currency = currency,
                paidById = paidById,
                category = category,
                frequency = frequency,
                nextDueDate = System.currentTimeMillis() + 86400000 * 30, // next month
                isActive = true
            )
            repository.addRecurringExpense(recurring)
            if (!_isOnline.value) {
                _syncQueueSize.value += 1
            }
        }
    }

    fun addNewUser(name: String, email: String) {
        viewModelScope.launch {
            val customUser = User(
                id = name.lowercase().replace(" ", "_") + "_" + System.currentTimeMillis() % 1000,
                name = name,
                email = email,
                avatarUrl = "user_default"
            )
            repository.insertUser(customUser)

            // Auto enroll user to selected group if exist
            val activeGrp = _selectedGroupId.value
            if (activeGrp != null) {
                repository.createGroup(repository.getGroupById(activeGrp)!!, listOf(customUser.id))
                recalculateDebtsAndBalances(activeGrp)
            }
        }
    }
}

// ViewModelFactory helper provider
class SplitViewModelFactory(
    private val application: Application,
    private val repository: SplitRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SplitViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
