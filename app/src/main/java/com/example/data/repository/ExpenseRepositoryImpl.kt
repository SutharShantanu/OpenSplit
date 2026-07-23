package com.example.data.repository

import com.example.domain.model.Expense
import com.example.domain.model.Activity
import com.example.domain.model.ActivityType
import com.example.domain.repository.ActivityRepository
import com.example.domain.repository.ExpenseRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ExpenseRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val activityRepository: ActivityRepository
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

    override fun getCommentsForExpense(groupId: String, expenseId: String): Flow<List<com.example.domain.model.Comment>> = callbackFlow {
        val listener = expensesCollection.document(expenseId).collection("comments").addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            val comments = snapshot?.documents?.mapNotNull { it.toObject(com.example.domain.model.Comment::class.java) } ?: emptyList()
            trySend(comments.sortedBy { it.timestamp })
        }
        awaitClose { listener.remove() }
    }

    override suspend fun addComment(groupId: String, expenseId: String, comment: com.example.domain.model.Comment): Result<String> {
        return try {
            val docRef = expensesCollection.document(expenseId).collection("comments").document()
            val newComment = comment.copy(id = docRef.id)
            docRef.set(newComment).await()
            activityRepository.logActivity(groupId, Activity(type = ActivityType.COMMENT_ADDED, actorUid = comment.uid, message = "commented on an expense", relatedExpenseId = expenseId))
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addExpense(expense: Expense): Result<String> {
        return try {
            val docRef = expensesCollection.document()
            val newExpense = expense.copy(id = docRef.id)
            docRef.set(newExpense).await()
            activityRepository.logActivity(
                expense.groupId,
                Activity(
                    type = ActivityType.EXPENSE_ADDED,
                    actorUid = expense.createdBy,
                    message = "added expense '${expense.description}'",
                    relatedExpenseId = docRef.id
                )
            )
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            expensesCollection.document(expense.id).set(expense).await()
            activityRepository.logActivity(
                expense.groupId,
                Activity(
                    type = ActivityType.EXPENSE_EDITED,
                    actorUid = expense.createdBy, // We might need current user id, but using createdBy for now
                    message = "updated expense '${expense.description}'",
                    relatedExpenseId = expense.id
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            // Ideally soft delete or we fetch the expense to know the groupId
            // For now, let's just delete it
            expensesCollection.document(expenseId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
