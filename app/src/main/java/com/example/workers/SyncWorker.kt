package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val expenseDao = database.splitDao()
            val firestore = FirebaseFirestore.getInstance()

            val allExpenses = expenseDao.getAllExpensesImmediate()

            val batch = firestore.batch()
            for (expense in allExpenses) {
                val docRef = firestore.collection("expenses").document(expense.id)
                batch.set(docRef, expense)
            }

            // Await execution
            batch.commit().await()
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // If offline, it would normally throw or we configured failure, but work manager constraint handles offline
            Result.retry()
        }
    }
}
