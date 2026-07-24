package com.opensplit.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opensplit.OpenSplitApp
import com.opensplit.domain.model.RecurrenceFrequency
import com.opensplit.domain.model.RecurrenceRule
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Materializes due recurring expenses once a day.
 *
 * For each of the current user's groups, finds recurring templates whose
 * [RecurrenceRule.nextOccurrence] is in the past, creates a concrete expense for that
 * occurrence, and advances the template's next occurrence. To avoid duplicate creation
 * across group members' devices, only the template's creator materializes it.
 */
class RecurringExpenseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? OpenSplitApp ?: return Result.success()
        val container = app.container
        val uid = container.authRepository.getCurrentUserId() ?: return Result.success()
        val expenseRepository = container.expenseRepository

        return try {
            val groups = container.groupRepository.getGroupsForUser(uid).first()
            val nowSeconds = System.currentTimeMillis() / 1000
            for (group in groups) {
                val due = expenseRepository.getDueRecurringExpenses(group.id, nowSeconds)
                for (template in due) {
                    val rule = template.recurrence ?: continue
                    // Only the creator's device materializes, so members don't duplicate it.
                    if (template.createdBy != uid) continue

                    val instance = template.copy(
                        id = "",
                        date = Timestamp.now(),
                        createdAt = Timestamp.now(),
                        recurrence = null
                    )
                    expenseRepository.addExpense(instance)

                    val advanced = advance(rule.nextOccurrence, rule.frequency)
                    expenseRepository.updateExpense(
                        template.copy(recurrence = rule.copy(nextOccurrence = advanced))
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun advance(from: Timestamp, frequency: RecurrenceFrequency): Timestamp {
        val cal = Calendar.getInstance().apply { time = from.toDate() }
        when (frequency) {
            RecurrenceFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurrenceFrequency.NONE -> Unit
        }
        return Timestamp(cal.time)
    }

    companion object {
        const val UNIQUE_NAME = "recurring_expenses"
    }
}
