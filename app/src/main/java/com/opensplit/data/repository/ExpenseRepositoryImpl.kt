package com.opensplit.data.repository

import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Activity
import com.opensplit.domain.model.ActivityType
import com.opensplit.domain.repository.ActivityRepository
import com.opensplit.domain.repository.ExpenseRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ExpenseRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val activityRepository: ActivityRepository
) : ExpenseRepository {
    
    private val topLevelExpensesCollection = firestore.collection("expenses")

    private fun getGroupExpensesRef(groupId: String) =
        firestore.collection("groups").document(groupId).collection("expenses")

    override fun getExpensesForGroup(groupId: String): Flow<List<Expense>> = callbackFlow {
        if (groupId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val listener = getGroupExpensesRef(groupId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)?.copy(id = doc.id)
                }?.filter { !it.isDeleted } ?: emptyList()
                trySend(expenses.sortedByDescending { it.date })
            }
        awaitClose { listener.remove() }
    }

    override fun getExpensesForUser(userId: String): Flow<List<Expense>> = callbackFlow {
        val listener = topLevelExpensesCollection
            .whereEqualTo("paidBy", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)?.copy(id = doc.id)
                }?.filter { !it.isDeleted } ?: emptyList()
                trySend(expenses.sortedByDescending { it.date })
            }
        awaitClose { listener.remove() }
    }

    override fun getCommentsForExpense(groupId: String, expenseId: String): Flow<List<com.opensplit.domain.model.Comment>> = callbackFlow {
        val commentsRef = if (groupId.isNotBlank()) {
            getGroupExpensesRef(groupId).document(expenseId).collection("comments")
        } else {
            topLevelExpensesCollection.document(expenseId).collection("comments")
        }

        val listener = commentsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            val comments = snapshot?.documents?.mapNotNull { it.toObject(com.opensplit.domain.model.Comment::class.java) } ?: emptyList()
            trySend(comments.sortedBy { it.timestamp })
        }
        awaitClose { listener.remove() }
    }

    override suspend fun addComment(groupId: String, expenseId: String, comment: com.opensplit.domain.model.Comment): Result<String> {
        return try {
            val commentsRef = if (groupId.isNotBlank()) {
                getGroupExpensesRef(groupId).document(expenseId).collection("comments")
            } else {
                topLevelExpensesCollection.document(expenseId).collection("comments")
            }
            val docRef = commentsRef.document()
            val newComment = comment.copy(id = docRef.id)
            docRef.set(newComment).await()
            if (groupId.isNotBlank()) {
                activityRepository.logActivity(groupId, Activity(type = ActivityType.COMMENT_ADDED, actorUid = comment.uid, message = "commented on an expense", relatedExpenseId = expenseId))
            }
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addExpense(expense: Expense): Result<String> {
        return try {
            val docRef = if (expense.groupId.isNotBlank()) {
                getGroupExpensesRef(expense.groupId).document()
            } else {
                topLevelExpensesCollection.document()
            }
            val newExpense = expense.copy(id = docRef.id)
            docRef.set(newExpense).await()

            if (expense.groupId.isNotBlank()) {
                activityRepository.logActivity(
                    expense.groupId,
                    Activity(
                        type = ActivityType.EXPENSE_ADDED,
                        actorUid = expense.createdBy,
                        message = "added expense '${expense.description}'",
                        relatedExpenseId = docRef.id
                    )
                )
            }
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            if (expense.groupId.isNotBlank()) {
                getGroupExpensesRef(expense.groupId).document(expense.id).set(expense).await()
            } else {
                topLevelExpensesCollection.document(expense.id).set(expense).await()
            }

            if (expense.groupId.isNotBlank()) {
                val actType = if (expense.isDeleted) ActivityType.EXPENSE_DELETED else ActivityType.EXPENSE_EDITED
                val actMsg = if (expense.isDeleted) "deleted expense '${expense.description}'" else "updated expense '${expense.description}'"
                activityRepository.logActivity(
                    expense.groupId,
                    Activity(
                        type = actType,
                        actorUid = expense.createdBy,
                        message = actMsg,
                        relatedExpenseId = expense.id
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExpense(groupId: String, expenseId: String): Result<Unit> {
        return try {
            // Group expenses live under groups/{groupId}/expenses; personal ones live at
            // the top level. Soft-delete in the correct collection.
            val docRef = if (groupId.isNotBlank()) {
                getGroupExpensesRef(groupId).document(expenseId)
            } else {
                topLevelExpensesCollection.document(expenseId)
            }
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update("isDeleted", true).await()
                if (groupId.isNotBlank()) {
                    val expense = snapshot.toObject(Expense::class.java)
                    activityRepository.logActivity(
                        groupId,
                        Activity(
                            type = ActivityType.EXPENSE_DELETED,
                            actorUid = expense?.createdBy ?: "",
                            message = "deleted expense '${expense?.description ?: ""}'",
                            relatedExpenseId = expenseId
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
