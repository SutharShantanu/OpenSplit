package com.opensplit.data.local

import com.google.firebase.Timestamp
import com.opensplit.domain.model.Activity
import com.opensplit.domain.model.ActivityType
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.ExpenseSplit
import com.opensplit.domain.model.Friend
import com.opensplit.domain.model.Group
import com.opensplit.domain.model.Settlement
import com.opensplit.domain.model.SplitType
import com.opensplit.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object InMemoryDataStore {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _settlements = MutableStateFlow<List<Settlement>>(emptyList())
    val settlements: StateFlow<List<Settlement>> = _settlements.asStateFlow()

    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities.asStateFlow()

    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends.asStateFlow()

    fun seedForUser(currentUid: String) {
        val now = Timestamp.now()
        val oneDayAgo = Timestamp(now.seconds - 86400, 0)
        val twoDaysAgo = Timestamp(now.seconds - 172800, 0)
        val threeDaysAgo = Timestamp(now.seconds - 259200, 0)

        val friendList = listOf(
            User(uid = "friend_sarah", displayName = "Sarah Jenkins", email = "sarah.j@example.com"),
            User(uid = "friend_michael", displayName = "Michael Chen", email = "michael.c@example.com"),
            User(uid = "friend_david", displayName = "David Rodriguez", email = "david.r@example.com"),
            User(uid = "friend_alex", displayName = "Alex Miller", email = "alex.m@example.com"),
            User(uid = "friend_emily", displayName = "Emily Thorne", email = "emily.t@example.com")
        )
        _friends.value = friendList

        val group1 = Group(
            id = "group_apt",
            name = "Apartment 🏠",
            createdBy = currentUid,
            memberIds = listOf(currentUid, "friend_sarah", "friend_david", "friend_alex"),
            currency = "USD",
            createdAt = threeDaysAgo
        )

        val group2 = Group(
            id = "group_tahoe",
            name = "Tahoe Trip 2024 🌲",
            createdBy = currentUid,
            memberIds = listOf(currentUid, "friend_sarah", "friend_michael", "friend_alex"),
            currency = "USD",
            createdAt = twoDaysAgo
        )

        val group3 = Group(
            id = "group_foodies",
            name = "Weekend Foodies 🍕",
            createdBy = currentUid,
            memberIds = listOf(currentUid, "friend_michael", "friend_emily"),
            currency = "USD",
            createdAt = oneDayAgo
        )

        _groups.value = listOf(group1, group2, group3)

        val exp1 = Expense(
            id = "exp_electric",
            groupId = group1.id,
            description = "Electric Bill",
            amount = 142.00,
            currency = "USD",
            paidBy = "friend_sarah",
            splitType = SplitType.EQUAL,
            splits = listOf(
                ExpenseSplit(uid = currentUid, amount = 47.33),
                ExpenseSplit(uid = "friend_sarah", amount = 47.34),
                ExpenseSplit(uid = "friend_david", amount = 47.33)
            ),
            category = "Utilities",
            date = now,
            createdBy = "friend_sarah"
        )

        val exp2 = Expense(
            id = "exp_groceries_apt",
            groupId = group1.id,
            description = "Trader Joe's Groceries",
            amount = 85.40,
            currency = "USD",
            paidBy = currentUid,
            splitType = SplitType.EQUAL,
            splits = listOf(
                ExpenseSplit(uid = currentUid, amount = 28.47),
                ExpenseSplit(uid = "friend_sarah", amount = 28.47),
                ExpenseSplit(uid = "friend_david", amount = 28.46)
            ),
            category = "Groceries",
            date = oneDayAgo,
            createdBy = currentUid
        )

        val exp3 = Expense(
            id = "exp_internet",
            groupId = group1.id,
            description = "Internet (Comcast)",
            amount = 60.00,
            currency = "USD",
            paidBy = "friend_david",
            splitType = SplitType.EQUAL,
            splits = listOf(
                ExpenseSplit(uid = currentUid, amount = 20.00),
                ExpenseSplit(uid = "friend_sarah", amount = 20.00),
                ExpenseSplit(uid = "friend_david", amount = 20.00)
            ),
            category = "Utilities",
            date = twoDaysAgo,
            createdBy = "friend_david"
        )

        val exp4 = Expense(
            id = "exp_tahoe_airbnb",
            groupId = group2.id,
            description = "Airbnb Split",
            amount = 450.00,
            currency = "USD",
            paidBy = "friend_sarah",
            splitType = SplitType.EQUAL,
            splits = listOf(
                ExpenseSplit(uid = currentUid, amount = 150.00),
                ExpenseSplit(uid = "friend_sarah", amount = 150.00),
                ExpenseSplit(uid = "friend_michael", amount = 150.00)
            ),
            category = "Rent",
            date = threeDaysAgo,
            createdBy = "friend_sarah"
        )

        val exp5 = Expense(
            id = "exp_tahoe_groceries",
            groupId = group2.id,
            description = "Weekend Groceries (Safeway)",
            amount = 124.50,
            currency = "USD",
            paidBy = currentUid,
            splitType = SplitType.EQUAL,
            splits = listOf(
                ExpenseSplit(uid = currentUid, amount = 41.50),
                ExpenseSplit(uid = "friend_sarah", amount = 41.50),
                ExpenseSplit(uid = "friend_michael", amount = 41.50)
            ),
            category = "Groceries",
            date = twoDaysAgo,
            createdBy = currentUid
        )

        val exp6 = Expense(
            id = "exp_dinner_luigi",
            groupId = group2.id,
            description = "Dinner at Luigi's",
            amount = 124.50,
            currency = "USD",
            paidBy = "friend_alex",
            splitType = SplitType.EQUAL,
            splits = listOf(
                ExpenseSplit(uid = currentUid, amount = 41.50),
                ExpenseSplit(uid = "friend_sarah", amount = 41.50),
                ExpenseSplit(uid = "friend_alex", amount = 41.50)
            ),
            category = "Food & Drinks",
            date = oneDayAgo,
            createdBy = "friend_alex"
        )

        _expenses.value = listOf(exp1, exp2, exp3, exp4, exp5, exp6)

        val set1 = Settlement(
            id = "set_1",
            fromUid = currentUid,
            toUid = "friend_alex",
            amount = 45.00,
            currency = "USD",
            date = oneDayAgo,
            note = "Settled up for dinner"
        )
        _settlements.value = listOf(set1)

        val actList = listOf(
            Activity(
                id = "act_1",
                type = ActivityType.GROUP_CREATED,
                actorUid = currentUid,
                message = "created group 'Apartment 🏠'",
                timestamp = threeDaysAgo
            ),
            Activity(
                id = "act_2",
                type = ActivityType.EXPENSE_ADDED,
                actorUid = "friend_sarah",
                message = "added 'Electric Bill' ($142.00) in 'Apartment 🏠'",
                timestamp = now
            ),
            Activity(
                id = "act_3",
                type = ActivityType.EXPENSE_ADDED,
                actorUid = currentUid,
                message = "added 'Trader Joe's Groceries' ($85.40) in 'Apartment 🏠'",
                timestamp = oneDayAgo
            ),
            Activity(
                id = "act_4",
                type = ActivityType.SETTLEMENT_ADDED,
                actorUid = currentUid,
                message = "paid $45.00 to Alex M. in 'Apartment 🏠'",
                timestamp = oneDayAgo
            )
        )
        _activities.value = actList
    }

    fun addGroup(group: Group) {
        _groups.update { current -> current + group }
    }

    fun addExpense(expense: Expense) {
        _expenses.update { current -> listOf(expense) + current }
    }

    fun addSettlement(settlement: Settlement) {
        _settlements.update { current -> listOf(settlement) + current }
    }

    fun recordSettlement(settlement: Settlement, groupName: String, fromName: String, toName: String) {
        _settlements.update { current -> listOf(settlement) + current }
        val formatted = com.opensplit.util.CurrencyFormatter.format(settlement.amount, settlement.currency)
        val actMsg = "$fromName paid $formatted to $toName in '$groupName'"
        val act = Activity(
            id = "act_${System.currentTimeMillis()}",
            type = ActivityType.SETTLEMENT_ADDED,
            actorUid = settlement.fromUid,
            message = actMsg,
            timestamp = settlement.date
        )
        _activities.update { current -> listOf(act) + current }
    }
}
