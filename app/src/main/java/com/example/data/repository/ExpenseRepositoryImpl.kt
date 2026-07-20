package com.example.data.repository

import com.example.domain.model.Expense
import com.example.domain.repository.ExpenseRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ExpenseRepositoryImpl(
    private val firestore: FirebaseFirestore
) : ExpenseRepository {
    
    private val expensesCollection = firestore.collection("expenses")

    override fun getExpensesForGroup(groupId: String): Flow<List<Expense>> = callbackFlow {
        val listener = expensesCollection
            .whereEqualTo("groupId", groupId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { it.toObject(Expense::class.java) } ?: emptyList()
                trySend(expenses.sortedByDescending { it.date })
            }
        awaitClose { listener.remove() }
    }

    override fun getExpensesForUser(userId: String): Flow<List<Expense>> = callbackFlow {
        // This query requires a composite index in Firestore to work properly
        val listener = expensesCollection
            .whereEqualTo("paidBy", userId)
            // Ideally we also want whereArrayContains for splits but Firestore limits queries
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { it.toObject(Expense::class.java) } ?: emptyList()
                trySend(expenses.sortedByDescending { it.date })
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addExpense(expense: Expense): Result<String> {
        return try {
            val docRef = expensesCollection.document()
            val newExpense = expense.copy(id = docRef.id)
            docRef.set(newExpense).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            expensesCollection.document(expense.id).set(expense).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            expensesCollection.document(expenseId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
