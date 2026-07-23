package com.example.domain.repository

import com.example.domain.model.Expense
import com.example.domain.model.Group
import com.example.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: com.google.firebase.auth.FirebaseUser?
    fun getAuthState(): Flow<AuthState>
    suspend fun getCurrentUserId(): String?
    suspend fun signInWithEmail(email: String, password: String): Result<String>
    suspend fun signUpWithEmail(email: String, password: String): Result<String>
    suspend fun signInWithGoogle(idToken: String): Result<String>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun updateProfile(displayName: String): Result<Unit>
    suspend fun reauthenticateWithEmail(password: String): Result<Unit>
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
    fun getUserFlow(uid: String): Flow<User?>
    suspend fun saveUser(user: User): Result<Unit>
    suspend fun createUserIfNotFound(uid: String, displayName: String?, email: String?, photoUrl: String?): Result<Unit>
    suspend fun updateCurrency(currency: String): Result<Unit>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun updateLastSeenActivity(uid: String, timestamp: com.google.firebase.Timestamp): Result<Unit>
    fun searchUsersByEmail(query: String): Flow<List<User>>
}

interface FriendRepository {
    fun getFriendsBalances(userId: String): Flow<Map<String, Double>>
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
    fun getCommentsForExpense(groupId: String, expenseId: String): Flow<List<com.example.domain.model.Comment>>
    suspend fun addComment(groupId: String, expenseId: String, comment: com.example.domain.model.Comment): Result<String>
    suspend fun addExpense(expense: Expense): Result<String>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expenseId: String): Result<Unit>
}

interface SettlementRepository {
    fun getSettlementsForGroup(groupId: String): Flow<List<com.example.domain.model.Settlement>>
    suspend fun addSettlement(groupId: String, settlement: com.example.domain.model.Settlement): Result<String>
}

interface ActivityRepository {
    fun getActivityForGroup(groupId: String): Flow<List<com.example.domain.model.Activity>>
    fun getActivityForUser(userId: String, groupIds: List<String>): Flow<List<com.example.domain.model.Activity>>
    suspend fun logActivity(groupId: String, activity: com.example.domain.model.Activity): Result<String>
}
