package com.example.domain.repository

import com.example.domain.model.Expense
import com.example.domain.model.Group
import com.example.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getAuthState(): Flow<AuthState>
    suspend fun getCurrentUserId(): String?
    suspend fun signInWithEmail(email: String, password: String): Result<String>
    suspend fun signUpWithEmail(email: String, password: String): Result<String>
    suspend fun signInWithGoogle(idToken: String): Result<String>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun signOut()
}

sealed class AuthState {
    object Loading : AuthState()
    data class LoggedIn(val uid: String) : AuthState()
    object LoggedOut : AuthState()
}

interface UserRepository {
    suspend fun getUser(uid: String): User?
    suspend fun saveUser(user: User): Result<Unit>
    suspend fun createUserIfNotFound(uid: String, displayName: String?, email: String?, photoUrl: String?): Result<Unit>
    fun searchUsersByEmail(query: String): Flow<List<User>>
}

interface GroupRepository {
    fun getGroupsForUser(userId: String): Flow<List<Group>>
    suspend fun getGroup(groupId: String): Group?
    suspend fun createGroup(group: Group): Result<String>
    suspend fun updateGroup(group: Group): Result<Unit>
    suspend fun deleteGroup(groupId: String): Result<Unit>
}

interface ExpenseRepository {
    fun getExpensesForGroup(groupId: String): Flow<List<Expense>>
    fun getExpensesForUser(userId: String): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense): Result<String>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expenseId: String): Result<Unit>
}
