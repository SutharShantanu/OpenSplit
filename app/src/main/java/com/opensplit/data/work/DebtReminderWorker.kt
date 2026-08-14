package com.opensplit.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opensplit.OpenSplitApp
import com.opensplit.domain.logic.BalanceCalculator
import com.opensplit.service.NotificationHelper
import com.opensplit.util.CurrencyFormatter
import kotlinx.coroutines.flow.first

/**
 * Periodically checks for outstanding unpaid balances and sends a friendly settlement nudge notification.
 */
class DebtReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? OpenSplitApp ?: return Result.success()
        val container = app.container
        val uid = container.authRepository.getCurrentUserId() ?: return Result.success()

        return try {
            val groups = container.groupRepository.getGroupsForUser(uid).first()
            var totalOwedCents = 0.0
            var groupsWithDebt = 0

            for (group in groups) {
                val expenses = container.expenseRepository.getExpensesForGroup(group.id).first()
                val settlements = container.settlementRepository.getSettlementsForGroup(group.id).first()
                val netBals = BalanceCalculator.netBalances(expenses, settlements)
                val myBal = netBals[uid] ?: 0.0

                if (myBal < -0.01) {
                    totalOwedCents += Math.abs(myBal)
                    groupsWithDebt++
                }
            }

            if (totalOwedCents > 0.01 && groupsWithDebt > 0) {
                val formatted = CurrencyFormatter.format(totalOwedCents, "INR")
                NotificationHelper.sendDebtReminderNotification(applicationContext, formatted, groupsWithDebt)
            }

            Result.success()
        } catch (e: Exception) {
            Result.success() // Fail gracefully without retrying aggressively
        }
    }

    companion object {
        const val UNIQUE_NAME = "debt_reminders"
    }
}
