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
            User(uid = "friend_rahul", displayName = "Rahul Sharma", email = "rahul.s@example.com"),
            User(uid = "friend_priya", displayName = "Priya Patel", email = "priya.p@example.com"),
            User(uid = "friend_amit", displayName = "Amit Verma", email = "amit.v@example.com"),
            User(uid = "friend_ananya", displayName = "Ananya Singh", email = "ananya.s@example.com")
        )
        _friends.value = friendList

        val group1 = Group(
            id = "group_goa",
            name = "Trip to Goa 🌴",
            createdBy = currentUid,
            memberIds = listOf(currentUid, "friend_rahul", "friend_priya", "friend_amit"),
            currency = "INR",
            createdAt = threeDaysAgo
        )

        val group2 = Group(
            id = "group_apt302",
            name = "Apartment 302 🏠",
            createdBy = currentUid,
            memberIds = listOf(currentUid, "friend_priya", "friend_ananya"),
            currency = "INR",
            createdAt = twoDaysAgo
        )

        val group3 = Group(
            id = "group_foodies",
            name = "Weekend Foodies 🍕",
            createdBy = currentUid,
            memberIds = listOf(currentUid, "friend_rahul", "friend_amit"),
            currency = "INR",
            createdAt = oneDayAgo
        )

        _groups.value = listOf(group1, group2, group3)

        val exp1 = Expense(
            id = "exp_villa",
            groupId = group1.id,
            description = "Beach Villa Stay",
            amount = 16000.0,
            currency = "INR",
            paidBy = currentUid,
            splitType = SplitType.EQUAL,
            splits = listOf(
                ExpenseSplit(uid = currentUid, amount = 4000.0),
                ExpenseSplit(uid = "friend_rahul", amount = 4000.0),
                ExpenseSplit(uid = "friend_priya", amount = 4000.0),
                ExpenseSplit(uid = "friend_amit", amount = 4000.0)
            ),
            category = "Rent",
            date = threeDaysAgo,
            createdBy = currentUid
        )

        val exp2 = Expense(
            id = "exp_dinner",
            groupId = group1.id,
            description = "Seafood Dinner & Drinks",
            amount = 4800.0,
            currency = "INR",
            paidBy = "friend_rahul",
            splitType = SplitType.EXACT,
            splits = listOf(
                ExpenseSplit(uid = currentUid, amount = 1500.0),
                ExpenseSplit(uid = "friend_rahul", amount = 1300.0),
                ExpenseSplit(uid = "friend_priya", amount = 1000.0),
                ExpenseSplit(uid = "friend_amit", amount = 1000.0)
            ),
            category = "Food & Drinks",
            date = twoDaysAgo,
            createdBy = "friend_rahul"
        )

        val exp3 = Expense(
            id = "exp_scuba",
            groupId = group1.id,
            description = "Scuba Diving & Watersports",
            amount = 6000.0,
            currency = "INR",
            paidBy = "friend_priya",
            splitType = SplitType.PERCENTAGE,
            splits = listOf(
                ExpenseSplit(uid = currentUid, amount = 1800.0, percentage = 30.0),
                ExpenseSplit(uid = "friend_rahul", amount = 1800.0, percentage = 30.0),
                ExpenseSplit(uid = "friend_priya", amount = 1200.0, percentage = 20.0),
                ExpenseSplit(uid = "friend_amit", amount = 1200.0, percentage = 20.0)
            ),
            category = "Entertainment",
            date = oneDayAgo,
            createdBy = "friend_priya"
        )

        val exp4 = Expense(
            id = "exp_grocery",
            groupId = group2.id,
            description = "Monthly Grocery Stockup",
            amount = 3600.0,
            currency = "INR",
            paidBy = currentUid,
            splitType = SplitType.EQUAL,
            splits = listOf(
                ExpenseSplit(uid = currentUid, amount = 1200.0),
                ExpenseSplit(uid = "friend_priya", amount = 1200.0),
                ExpenseSplit(uid = "friend_ananya", amount = 1200.0)
            ),
            category = "Groceries",
            date = twoDaysAgo,
            createdBy = currentUid
        )

        val exp5 = Expense(
            id = "exp_wifi",
            groupId = group2.id,
            description = "High-Speed WiFi Bill",
            amount = 1500.0,
            currency = "INR",
            paidBy = "friend_ananya",
            splitType = SplitType.EQUAL,
            splits = listOf(
                ExpenseSplit(uid = currentUid, amount = 500.0),
                ExpenseSplit(uid = "friend_priya", amount = 500.0),
                ExpenseSplit(uid = "friend_ananya", amount = 500.0)
            ),
            category = "Utilities",
            date = oneDayAgo,
            createdBy = "friend_ananya"
        )

        val exp6 = Expense(
            id = "exp_pizza",
            groupId = group3.id,
            description = "Artisanal Pizza & Desserts",
            amount = 2200.0,
            currency = "INR",
            paidBy = currentUid,
            splitType = SplitType.EQUAL,
            splits = listOf(
                ExpenseSplit(uid = currentUid, amount = 733.34),
                ExpenseSplit(uid = "friend_rahul", amount = 733.33),
                ExpenseSplit(uid = "friend_amit", amount = 733.33)
            ),
            category = "Food & Drinks",
            date = now,
            createdBy = currentUid
        )

        _expenses.value = listOf(exp1, exp2, exp3, exp4, exp5, exp6)

        val set1 = Settlement(
            id = "set_1",
            fromUid = "friend_rahul",
            toUid = currentUid,
            amount = 2500.0,
            currency = "INR",
            date = oneDayAgo,
            note = "Partial settlement for Villa stay"
        )
        _settlements.value = listOf(set1)

        val actList = listOf(
            Activity(
                id = "act_1",
                type = ActivityType.GROUP_CREATED,
                actorUid = currentUid,
                message = "created group 'Trip to Goa 🌴'",
                timestamp = threeDaysAgo
            ),
            Activity(
                id = "act_2",
                type = ActivityType.EXPENSE_ADDED,
                actorUid = currentUid,
                message = "added 'Beach Villa Stay' (₹16,000.00) in 'Trip to Goa 🌴'",
                timestamp = threeDaysAgo
            ),
            Activity(
                id = "act_3",
                type = ActivityType.EXPENSE_ADDED,
                actorUid = "friend_rahul",
                message = "added 'Seafood Dinner & Drinks' (₹4,800.00) in 'Trip to Goa 🌴'",
                timestamp = twoDaysAgo
            ),
            Activity(
                id = "act_4",
                type = ActivityType.SETTLEMENT_ADDED,
                actorUid = "friend_rahul",
                message = "paid ₹2,500.00 to You",
                timestamp = oneDayAgo
            ),
            Activity(
                id = "act_5",
                type = ActivityType.EXPENSE_ADDED,
                actorUid = currentUid,
                message = "added 'Artisanal Pizza & Desserts' (₹2,200.00) in 'Weekend Foodies 🍕'",
                timestamp = now
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
