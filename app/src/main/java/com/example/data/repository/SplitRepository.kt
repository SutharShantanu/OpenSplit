package com.example.data.repository

import com.example.data.local.SplitDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

data class DebtRelation(
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: Double
)

data class UserBalance(
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val netBalance: Double // Positive: overall owed to them, Negative: they owe overall
)

class SplitRepository(private val splitDao: SplitDao) {

    // Exposure of core tables
    val allGroups: Flow<List<Group>> = splitDao.getAllGroups()
    val allExpenses: Flow<List<Expense>> = splitDao.getAllExpenses()
    val allUsers: Flow<List<User>> = splitDao.getAllUsers()

    fun getGroupMembers(groupId: String): Flow<List<User>> = splitDao.getGroupMembers(groupId)
    fun getExpensesForGroup(groupId: String): Flow<List<Expense>> = splitDao.getExpensesForGroup(groupId)
    fun getSettlementsForGroup(groupId: String): Flow<List<Settlement>> = splitDao.getSettlementsForGroup(groupId)
    fun getRecurringExpensesForGroup(groupId: String): Flow<List<RecurringExpense>> = splitDao.getRecurringExpensesForGroup(groupId)

    suspend fun getUserById(userId: String): User? = splitDao.getUserById(userId)
    suspend fun getGroupById(groupId: String): Group? = splitDao.getGroupById(groupId)

    suspend fun insertUser(user: User) {
        splitDao.insertUser(user)
    }

    // CRUD - Group
    suspend fun createGroup(group: Group, memberIds: List<String>) {
        splitDao.insertGroup(group)
        val crossRefs = memberIds.map { userId -> GroupMemberCrossRef(group.id, userId) }
        splitDao.insertGroupMembers(crossRefs)
    }

    suspend fun deleteGroup(groupId: String) {
        splitDao.deleteGroup(groupId)
        splitDao.clearGroupMembers(groupId)
        splitDao.clearGroupExpenses(groupId)
        splitDao.clearGroupSettlements(groupId)
    }

    // CRUD - Expense
    suspend fun addExpense(expense: Expense, splits: List<ExpenseSplit>) {
        splitDao.insertExpense(expense)
        splitDao.insertSplits(splits)
    }

    suspend fun deleteExpense(expenseId: String) {
        splitDao.deleteExpense(expenseId)
        splitDao.deleteSplitsForExpense(expenseId)
    }

    // CRUD - Settlement
    suspend fun addSettlement(settlement: Settlement) {
        splitDao.insertSettlement(settlement)
    }

    suspend fun deleteSettlement(settlementId: String) {
        splitDao.deleteSettlement(settlementId)
    }

    // CRUD - Recurring
    suspend fun addRecurringExpense(recurring: RecurringExpense) {
        splitDao.insertRecurringExpense(recurring)
    }

    // Core Optimization: Debt Simplification Algorithm
    suspend fun computeSimplifiedDebts(groupId: String): List<DebtRelation> {
        val members = splitDao.getGroupMembersSync(groupId)
        val expenses = splitDao.getExpensesForGroupSync(groupId)
        val settlements = splitDao.getSettlementsForGroupSync(groupId)
        val allSplits = splitDao.getSplitsForGroupSync(groupId)

        val memberMap = members.associateBy { it.id }
        if (members.isEmpty()) return emptyList()

        // 1. Calculate net balances
        val netBalances = mutableMapOf<String, Double>()
        members.forEach { netBalances[it.id] = 0.0 }

        // Core credits/debits from expenses and Splits
        expenses.forEach { expense ->
            val totalAmt = expense.amount
            val payerId = expense.paidById

            // Add full amount credited to payer
            if (netBalances.containsKey(payerId)) {
                netBalances[payerId] = netBalances[payerId]!! + totalAmt
            }

            // Deduct splits
            val splits = allSplits.filter { it.expenseId == expense.id }
            splits.forEach { split ->
                val debtorId = split.userId
                if (netBalances.containsKey(debtorId)) {
                    netBalances[debtorId] = netBalances[debtorId]!! - split.owedAmount
                }
            }
        }

        // Adjust with settlements
        settlements.forEach { settlement ->
            val sender = settlement.senderId
            val receiver = settlement.receiverId
            val amt = settlement.amount

            if (netBalances.containsKey(sender)) {
                netBalances[sender] = netBalances[sender]!! + amt // sender paid, decreasing their debt (credited)
            }
            if (netBalances.containsKey(receiver)) {
                netBalances[receiver] = netBalances[receiver]!! - amt // receiver received, decreasing their credit (debited)
            }
        }

        // 2. Separate into debtors and creditors
        // We use a precision threshold of 0.01 to avoid small floating point issues
        val debtors = mutableListOf<Pair<String, Double>>()
        val creditors = mutableListOf<Pair<String, Double>>()

        netBalances.forEach { (userId, balance) ->
            if (balance < -0.01) {
                debtors.add(userId to -balance) // store balance as positive for easier math
            } else if (balance > 0.01) {
                creditors.add(userId to balance)
            }
        }

        // 3. Greedy algorithm to simplify transactions
        val simplifiedTrans = mutableListOf<DebtRelation>()

        var dIndex = 0
        var cIndex = 0

        val activeDebtors = debtors.map { it.first to it.second }.toMutableList()
        val activeCreditors = creditors.map { it.first to it.second }.toMutableList()

        while (dIndex < activeDebtors.size && cIndex < activeCreditors.size) {
            val debtor = activeDebtors[dIndex]
            val creditor = activeCreditors[cIndex]

            val dId = debtor.first
            val dAmt = debtor.second

            val cId = creditor.first
            val cAmt = creditor.second

            val minAmt = kotlin.math.min(dAmt, cAmt)

            val fromUser = memberMap[dId]
            val toUser = memberMap[cId]

            if (fromUser != null && toUser != null && minAmt > 0.01) {
                simplifiedTrans.add(
                    DebtRelation(
                        fromUserId = dId,
                        fromUserName = fromUser.name,
                        toUserId = cId,
                        toUserName = toUser.name,
                        amount = minAmt
                    )
                )
            }

            // Update remaining balances
            val newDAmt = dAmt - minAmt
            val newCAmt = cAmt - minAmt

            if (newDAmt < 0.01) {
                dIndex++
            } else {
                activeDebtors[dIndex] = dId to newDAmt
            }

            if (newCAmt < 0.01) {
                cIndex++
            } else {
                activeCreditors[cIndex] = cId to newCAmt
            }
        }

        return simplifiedTrans
    }

    // Calculates individual net balance details in a group for the dashboard cards
    suspend fun getGroupBalances(groupId: String): List<UserBalance> {
        val members = splitDao.getGroupMembersSync(groupId)
        val expenses = splitDao.getExpensesForGroupSync(groupId)
        val settlements = splitDao.getSettlementsForGroupSync(groupId)
        val allSplits = splitDao.getSplitsForGroupSync(groupId)

        val netBalances = mutableMapOf<String, Double>()
        members.forEach { netBalances[it.id] = 0.0 }

        expenses.forEach { expense ->
            val payerId = expense.paidById
            if (netBalances.containsKey(payerId)) {
                netBalances[payerId] = netBalances[payerId]!! + expense.amount
            }
            val splits = allSplits.filter { it.expenseId == expense.id }
            splits.forEach { split ->
                if (netBalances.containsKey(split.userId)) {
                    netBalances[split.userId] = netBalances[split.userId]!! - split.owedAmount
                }
            }
        }

        settlements.forEach { settlement ->
            if (netBalances.containsKey(settlement.senderId)) {
                netBalances[settlement.senderId] = netBalances[settlement.senderId]!! + settlement.amount
            }
            if (netBalances.containsKey(settlement.receiverId)) {
                netBalances[settlement.receiverId] = netBalances[settlement.receiverId]!! - settlement.amount
            }
        }

        return members.map { user ->
            UserBalance(
                userId = user.id,
                userName = user.name,
                userAvatar = user.avatarUrl,
                netBalance = netBalances[user.id] ?: 0.0
            )
        }
    }

    // Auto-seed Initial Sample Data
    suspend fun checkAndSeedDatabase() {
        val existingUsers = splitDao.getAllUsers().first()
        if (existingUsers.isNotEmpty()) return

        // 1. Create just the current user (simulating the Google OAuth sign-in)
        val currentUser = User(
            id = "current_user",
            name = "Shantanu",
            email = "shantanusut2000@gmail.com",
            avatarUrl = "user_me",
            isCurrentUser = true,
            preferredCurrency = "₹"
        )
        splitDao.insertUser(currentUser)
        
        // Removed all dummy groups and dummy users.
    }
}
