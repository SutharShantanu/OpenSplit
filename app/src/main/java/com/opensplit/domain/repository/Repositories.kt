package com.opensplit.domain.repository

import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Group
import com.opensplit.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: com.google.firebase.auth.FirebaseUser?
    val currentUserId: String? get() = currentUser?.uid
    fun getAuthState(): Flow<AuthState>
    suspend fun getCurrentUserId(): String?
    suspend fun signInWithEmail(email: String, password: String): Result<String>
    suspend fun signUpWithEmail(email: String, password: String): Result<String>
    suspend fun signInWithGoogle(idToken: String): Result<String>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun confirmPasswordReset(oobCode: String, newPassword: String): Result<Unit>
    suspend fun updateProfile(displayName: String): Result<Unit>
    suspend fun reauthenticateWithEmail(password: String): Result<Unit>
    suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit>
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
    suspend fun updateUpiId(upiId: String?): Result<Unit>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun updateLastSeenActivity(uid: String, timestamp: com.google.firebase.Timestamp): Result<Unit>
    fun searchUsersByEmail(query: String): Flow<List<User>>
}

interface FriendRepository {
    /**
     * Net balance with each friend, broken down **per currency**:
     * `friendUid -> (currencyCode -> netAmount)`. Positive means the friend owes the
     * user; negative means the user owes the friend. Amounts are never summed across
     * currencies — callers must display each currency separately.
     */
    fun getFriendsBalances(userId: String): Flow<Map<String, Map<String, Double>>>
}

interface GroupRepository {
    fun getGroupsForUser(userId: String): Flow<List<Group>>
    suspend fun getGroup(groupId: String): Group?
    /** The hidden two-person group between [me] and [other], created on first use. */
    suspend fun getOrCreateDirectGroup(me: String, other: String, currency: String): Result<Group>
    suspend fun createGroup(group: Group): Result<String>
    suspend fun updateGroup(group: Group): Result<Unit>
    suspend fun deleteGroup(groupId: String): Result<Unit>
}

interface ExpenseRepository {
    fun getExpensesForGroup(groupId: String): Flow<List<Expense>>
    fun getExpensesForUser(userId: String): Flow<List<Expense>>
    fun getCommentsForExpense(groupId: String, expenseId: String): Flow<List<com.opensplit.domain.model.Comment>>
    suspend fun addComment(groupId: String, expenseId: String, comment: com.opensplit.domain.model.Comment): Result<String>
    suspend fun addExpense(expense: Expense): Result<String>
    /** Saves [expense] and appends an edit-history entry attributed to [actorUid]. */
    suspend fun updateExpense(expense: Expense, actorUid: String = expense.createdBy): Result<Unit>
    /** Soft delete: hides the expense and stamps deletedAt so it can be restored for 90 days. */
    suspend fun deleteExpense(groupId: String, expenseId: String): Result<Unit>
    suspend fun restoreExpense(groupId: String, expenseId: String): Result<Unit>
    /** Permanent delete; only offered from "Recently deleted". */
    suspend fun purgeExpense(groupId: String, expenseId: String): Result<Unit>
    /** Soft-deleted expenses across [groupIds], newest deletion first, within the 90-day window. */
    fun getDeletedExpensesForGroups(groupIds: List<String>): Flow<List<Expense>>
    /** Recurring templates in a group whose next occurrence is due (<= nowSeconds). */
    suspend fun getDueRecurringExpenses(groupId: String, nowSeconds: Long): List<Expense>
}

interface SettlementRepository {
    fun getSettlementsForGroup(groupId: String): Flow<List<com.opensplit.domain.model.Settlement>>
    suspend fun addSettlement(groupId: String, settlement: com.opensplit.domain.model.Settlement): Result<String>
}

interface PendingInviteRepository {
    fun getPendingInvites(groupId: String): Flow<List<com.opensplit.domain.model.PendingInvite>>
    suspend fun addInvite(invite: com.opensplit.domain.model.PendingInvite): Result<String>
    suspend fun revokeInvite(groupId: String, inviteId: String): Result<Unit>
}

interface FriendInviteRepository {
    fun getInvites(inviterUid: String): Flow<List<com.opensplit.domain.model.FriendInvite>>
    suspend fun sendInvite(inviterUid: String, email: String, inviterName: String = ""): Result<String>
    suspend fun revokeInvite(inviteId: String): Result<Unit>
    /** Pending requests addressed to [email] (the signed-in user's). */
    fun getIncomingInvites(email: String): Flow<List<com.opensplit.domain.model.FriendInvite>>
    suspend fun respond(inviteId: String, accept: Boolean): Result<Unit>
    /** A shareable "add me" link for [uid]; reused while valid, 30-day expiry. Returns the full URL. */
    suspend fun getOrCreateFriendLink(uid: String, displayName: String): Result<com.opensplit.domain.model.FriendLink>
    /** Resolves a friend link and creates the direct group; returns the link owner's uid. */
    suspend fun acceptFriendLink(token: String, me: String, currency: String): Result<String>
}

interface ActivityRepository {
    fun getActivityForGroup(groupId: String): Flow<List<com.opensplit.domain.model.Activity>>
    fun getActivityForUser(userId: String, groupIds: List<String>): Flow<List<com.opensplit.domain.model.Activity>>
    suspend fun logActivity(groupId: String, activity: com.opensplit.domain.model.Activity): Result<String>
}

interface StorageRepository {
    /** Uploads a locally-picked receipt (image or PDF) and returns its public download URL. */
    suspend fun uploadReceipt(groupId: String, localUri: android.net.Uri): Result<String>
}

interface InviteLinkRepository {
    /** Creates a link for [group]; [expiresInSeconds] null = never, [maxUses] 0 = unlimited. */
    suspend fun create(group: Group, createdBy: String, expiresInSeconds: Long?, maxUses: Int): Result<com.opensplit.domain.model.InviteLink>
    suspend fun get(token: String): com.opensplit.domain.model.InviteLink?
    fun linksForGroup(groupId: String): Flow<List<com.opensplit.domain.model.InviteLink>>
    suspend fun revoke(token: String): Result<Unit>
    /** Validates the link and adds [uid] to the group. Returns the group id on success. */
    suspend fun join(token: String, uid: String): Result<String>
}

