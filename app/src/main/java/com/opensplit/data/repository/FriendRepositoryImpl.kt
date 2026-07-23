package com.opensplit.data.repository

import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Settlement
import com.opensplit.domain.repository.ExpenseRepository
import com.opensplit.domain.repository.FriendRepository
import com.opensplit.domain.repository.GroupRepository
import com.opensplit.domain.repository.SettlementRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FriendRepositoryImpl(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository
) : FriendRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getFriendsBalances(userId: String): Flow<Map<String, Double>> {
        return groupRepository.getGroupsForUser(userId).flatMapLatest { groups ->
            if (groups.isEmpty()) return@flatMapLatest flowOf(emptyMap())

            // A list of flows, one for each group, representing the net balances in that group
            val groupBalancesFlows = groups.map { group ->
                combine(
                    expenseRepository.getExpensesForGroup(group.id),
                    settlementRepository.getSettlementsForGroup(group.id)
                ) { expenses, settlements ->
                    calculateBalancesForGroup(userId, expenses, settlements)
                }
            }

            // Combine all group balances into one global map
            combine(groupBalancesFlows) { groupBalancesArray ->
                val globalBalances = mutableMapOf<String, Double>()
                for (groupBals in groupBalancesArray) {
                    for ((otherUid, amount) in groupBals) {
                        globalBalances[otherUid] = (globalBalances[otherUid] ?: 0.0) + amount
                    }
                }
                globalBalances
            }
        }
    }

    private fun calculateBalancesForGroup(
        userId: String,
        expenses: List<Expense>,
        settlements: List<Settlement>
    ): Map<String, Double> {
        // We only care about the user's balance relative to everyone else
        // (who owes the user, or who the user owes)
        val balances = mutableMapOf<String, Double>()

        for (expense in expenses) {
            val payers = mutableMapOf<String, Double>()
            if (expense.multiPayer != null) {
                for ((uid, amount) in expense.multiPayer) {
                    payers[uid] = amount
                }
            } else {
                payers[expense.paidBy] = expense.amount
            }

            // Did the user pay?
            val userPaidAmount = payers[userId] ?: 0.0
            
            // What was the user's split?
            val userSplitAmount = expense.splits.find { it.uid == userId }?.amount ?: 0.0
            
            // If the user paid and others were in the split:
            // They owe the user.
            if (userPaidAmount > 0) {
                val totalPaid = payers.values.sum()
                for (split in expense.splits) {
                    if (split.uid != userId) {
                        // Proportion of split.amount that is owed to userId
                        val proportion = userPaidAmount / totalPaid
                        val owesUserId = split.amount * proportion
                        balances[split.uid] = (balances[split.uid] ?: 0.0) + owesUserId
                    }
                }
            }
            
            // If the user was in the split and others paid:
            // User owes them.
            if (userSplitAmount > 0) {
                val totalPaid = payers.values.sum()
                for ((payerUid, paidAmount) in payers) {
                    if (payerUid != userId && totalPaid > 0) {
                        val proportion = paidAmount / totalPaid
                        val userOwesPayer = userSplitAmount * proportion
                        balances[payerUid] = (balances[payerUid] ?: 0.0) - userOwesPayer
                    }
                }
            }
        }

        // Apply settlements
        for (settlement in settlements) {
            if (settlement.fromUid == userId && settlement.toUid != userId) {
                // User paid someone else (user gets credit)
                balances[settlement.toUid] = (balances[settlement.toUid] ?: 0.0) + settlement.amount
            } else if (settlement.toUid == userId && settlement.fromUid != userId) {
                // Someone else paid user (user's credit goes down)
                balances[settlement.fromUid] = (balances[settlement.fromUid] ?: 0.0) - settlement.amount
            }
        }

        return balances
    }
}
