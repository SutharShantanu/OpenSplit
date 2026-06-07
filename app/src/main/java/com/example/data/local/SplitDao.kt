package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDao {

    // --- Users ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): User?

    @Query("""
        SELECT users.* FROM users 
        INNER JOIN group_members ON users.id = group_members.userId 
        WHERE group_members.groupId = :groupId
    """)
    fun getGroupMembers(groupId: String): Flow<List<User>>

    @Query("""
        SELECT users.* FROM users 
        INNER JOIN group_members ON users.id = group_members.userId 
        WHERE group_members.groupId = :groupId
    """)
    suspend fun getGroupMembersSync(groupId: String): List<User>

    // --- Groups ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group)

    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): Group?

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    // --- Group Members Junc ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMember(crossRef: GroupMemberCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(crossRefs: List<GroupMemberCrossRef>)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun deleteGroupMember(groupId: String, userId: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun clearGroupMembers(groupId: String)

    // --- Expenses ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesImmediate(): List<Expense>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY date DESC")
    fun getExpensesForGroup(groupId: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY date DESC")
    suspend fun getExpensesForGroupSync(groupId: String): List<Expense>

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    @Query("DELETE FROM expenses WHERE groupId = :groupId")
    suspend fun clearGroupExpenses(groupId: String)

    // --- Expense Splits ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplit>)

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    fun getSplitsForExpense(expenseId: String): Flow<List<ExpenseSplit>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpenseSync(expenseId: String): List<ExpenseSplit>

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: String)

    @Query("""
        SELECT expense_splits.* FROM expense_splits
        INNER JOIN expenses ON expense_splits.expenseId = expenses.id
        WHERE expenses.groupId = :groupId
    """)
    suspend fun getSplitsForGroupSync(groupId: String): List<ExpenseSplit>

    // --- Settlements ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: Settlement)

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY date DESC")
    fun getSettlementsForGroup(groupId: String): Flow<List<Settlement>>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY date DESC")
    suspend fun getSettlementsForGroupSync(groupId: String): List<Settlement>

    @Query("DELETE FROM settlements WHERE id = :settlementId")
    suspend fun deleteSettlement(settlementId: String)

    @Query("DELETE FROM settlements WHERE groupId = :groupId")
    suspend fun clearGroupSettlements(groupId: String)

    // --- Recurring Expenses ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(recurring: RecurringExpense)

    @Query("SELECT * FROM recurring_expenses")
    fun getAllRecurringExpenses(): Flow<List<RecurringExpense>>

    @Query("SELECT * FROM recurring_expenses WHERE groupId = :groupId")
    fun getRecurringExpensesForGroup(groupId: String): Flow<List<RecurringExpense>>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1")
    suspend fun getActiveRecurringExpensesSync(): List<RecurringExpense>
}
