package com.opensplit.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opensplit.OpenSplitApp
import com.opensplit.domain.logic.BalanceCalculator
import com.opensplit.domain.model.NotificationType
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

        val prefs = container.userPreferencesRepository
        if (!prefs.notificationsEnabledFlow.first() ||
            NotificationType.REMINDER.id in prefs.disabledNotificationTypesFlow.first()
        ) return Result.success()
        val muted = prefs.mutedGroupIdsFlow.first()

        return try {
            val groups = container.groupRepository.getGroupsForUser(uid).first()
            // Groups can use different currencies; never add INR to USD.
            val owedByCurrency = mutableMapOf<String, Double>()
            var groupsWithDebt = 0

            for (group in groups) {
                if (group.id in muted) continue
                val expenses = container.expenseRepository.getExpensesForGroup(group.id).first()
                val settlements = container.settlementRepository.getSettlementsForGroup(group.id).first()
                val myBal = BalanceCalculator.netBalances(expenses, settlements)[uid] ?: 0.0
                if (myBal < -0.01) {
                    owedByCurrency.merge(group.currency, -myBal, Double::plus)
                    groupsWithDebt++
                }
            }

            if (groupsWithDebt > 0) {
                val formatted = owedByCurrency.entries.joinToString(" + ") { (code, amt) ->
                    CurrencyFormatter.format(amt, code)
                }
                NotificationHelper.sendDebtReminderNotification(applicationContext, formatted, groupsWithDebt)
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.w("DebtReminderWorker", "Reminder check failed", e)
            if (runAttemptCount < 2) Result.retry() else Result.success()
        }
    }

    companion object {
        const val UNIQUE_NAME = "debt_reminders"
    }
}
