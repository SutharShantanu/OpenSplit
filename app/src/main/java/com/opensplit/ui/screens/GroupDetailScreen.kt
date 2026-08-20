package com.opensplit.ui.screens

import com.opensplit.ui.components.LocalSnackbarController

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.ui.components.*
import androidx.compose.animation.AnimatedContent
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitMotion
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.GroupDetailViewModel
import com.opensplit.ui.viewmodel.ScreenState
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToExpenseDetail: (String, String) -> Unit,
    onNavigateToSettleUp: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddMember by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showFilterSortSheet by remember { mutableStateOf(false) }
    var sortOrder by rememberSaveable { mutableStateOf(ExpenseSortOrder.DATE_NEWEST) }
    var filterPaidByYou by rememberSaveable { mutableStateOf(false) }
    var filterInvolvedYou by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabTitles = listOf("Expenses", "Balances", "Members")

    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()

    val isExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 10 }
    }

    val groupName = if (uiState is ScreenState.Success) (uiState as ScreenState.Success).data.group.name else "Group Details"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(OpenSplitIcons.Back, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSortSheet = true }) {
                        Icon(OpenSplitIcons.Filter, contentDescription = "Filter and Sort")
                    }
                    IconButton(onClick = { showExportSheet = true }) {
                        Icon(OpenSplitIcons.Export, contentDescription = "Export Group Expenses")
                    }
                    IconButton(onClick = { showAddMember = true }) {
                        Icon(OpenSplitIcons.Invite, contentDescription = "Add Member")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(OpenSplitIcons.More, contentDescription = "Group settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                ),
                modifier = Modifier.appHazeHeader(hazeState)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                expanded = isExpanded,
                onClick = onNavigateToAddExpense,
                icon = { Icon(OpenSplitIcons.AddExpense, contentDescription = null) },
                text = { Text("Add Expense") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
        ) {
            StateLayout(state = uiState) { data ->
                val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val filteredExpenses = data.expenses.filter { exp ->
                    val matchesQuery = exp.description.contains(searchQuery, ignoreCase = true) || exp.category.contains(searchQuery, ignoreCase = true)
                    val matchesCategory = selectedCategory == "All" || exp.category.equals(selectedCategory, ignoreCase = true)
                    val matchesPaidBy = !filterPaidByYou || exp.paidBy == currentUid
                    val matchesInvolved = !filterInvolvedYou || exp.splits.any { it.uid == currentUid } || exp.paidBy == currentUid
                    matchesQuery && matchesCategory && matchesPaidBy && matchesInvolved
                }.let { list ->
                    when (sortOrder) {
                        ExpenseSortOrder.DATE_NEWEST -> list.sortedByDescending { it.date.seconds }
                        ExpenseSortOrder.AMOUNT_HIGHEST -> list.sortedByDescending { it.amount }
                        ExpenseSortOrder.CATEGORY -> list.sortedBy { it.category }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .appHazeSource(hazeState)
                ) {
                    // Secondary Tab Row
                    SecondaryTabRow(selectedTabIndex = selectedTab) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    if (index == 0 && data.expenses.isNotEmpty()) {
                                        BadgedBox(
                                            badge = { Badge { Text("${data.expenses.size}") } }
                                        ) {
                                            Text(title)
                                        }
                                    } else {
                                        Text(title)
                                    }
                                }
                            )
                        }
                    }

                    // Expenses / Balances / Members are peer views — fade through between them.
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { OpenSplitMotion.fadeThrough() },
                        label = "groupDetailTab"
                    ) { tab ->
                    when (tab) {
                        0 -> { // Expenses Tab
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceMD)
                                ) {
                                    // Total Spending Banner
                                    val totalSpending = remember(data.expenses) { data.expenses.sumOf { it.amount } }
                                    val currencySymbol = remember(data.group.currency) { com.opensplit.util.CurrencyFormatter.getCurrencySymbol(data.group.currency) }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = OpenSplitTokens.SpaceMD),
                                        shape = RoundedCornerShape(28.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(OpenSplitTokens.SpaceLG),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Group Total Spending",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "$currencySymbol${com.opensplit.util.CurrencyFormatter.format(totalSpending, showSymbol = false)}",
                                                    style = MaterialTheme.typography.headlineLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "This month",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    if (data.members.size > 1) {
                                                        onNavigateToSettleUp()
                                                    } else {
                                                        snackbar.showMessage("Add another member to this group before settling up")
                                                    }
                                                },
                                                shape = CircleShape,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            ) {
                                                Icon(OpenSplitIcons.Settle, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Settle Up", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // AI Suggestion Nudge Card
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = OpenSplitTokens.SpaceMD),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = OpenSplitIcons.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "AI Suggestion",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Looks like someone hasn't paid for recent shared expenses. Want to send a gentle nudge?",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(
                                                        onClick = { snackbar.showMessage("Reminder nudge sent to group members!") },
                                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                                        shape = CircleShape,
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.surface,
                                                            contentColor = MaterialTheme.colorScheme.secondary
                                                        ),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                                    ) {
                                                        Text("Send Nudge", style = MaterialTheme.typography.labelMedium)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (data.expenses.isNotEmpty()) {
                                        AppSearchBar(
                                            query = searchQuery,
                                            onQueryChange = { searchQuery = it },
                                            placeholderText = "Search expenses..."
                                        )

                                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                                        CategoryChipRow(
                                            selectedCategory = selectedCategory,
                                            onCategorySelected = { selectedCategory = it }
                                        )

                                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                                    }

                                if (filteredExpenses.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(OpenSplitTokens.SpaceXL)
                                        ) {
                                            ReceiptIllustration(size = 120.dp)
                                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
                                            Text(
                                                text = "No expenses yet \u2014 add the first one and OpenSplit handles the math",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
                                            Button(onClick = onNavigateToAddExpense) {
                                                Icon(OpenSplitIcons.AddExpense, contentDescription = null)
                                                Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                                                Text("Add Expense")
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        state = listState,
                                        verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(filteredExpenses, key = { it.id }) { exp ->
                                            val categoryIcon = getCategoryIcon(exp.category)
                                            val categoryColor = getCategoryColor(exp.category)
                                            val dateStr = remember(exp.date) {
                                                try {
                                                    SimpleDateFormat("MMM d", Locale.getDefault()).format(exp.date.toDate())
                                                } catch (e: Exception) { "" }
                                            }

                                            ListItem(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(MaterialTheme.shapes.medium)
                                                    .clickable { onNavigateToExpenseDetail(data.group.id, exp.id) },
                                                headlineContent = {
                                                    Text(
                                                        text = exp.description,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                supportingContent = {
                                                    val payerName = data.members.find { it.uid == exp.paidBy }?.displayName ?: "Unknown"
                                                    Text(
                                                        text = "Paid by $payerName • $dateStr",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                leadingContent = {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = categoryColor.copy(alpha = 0.15f),
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = categoryIcon,
                                                                contentDescription = exp.category,
                                                                tint = categoryColor,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                trailingContent = {
                                                    Text(
                                                        text = com.opensplit.util.CurrencyFormatter.format(exp.amount, exp.currency),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }

                        1 -> { // Balances Tab
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(OpenSplitTokens.SpaceLG)
                            ) {
                                val nameOf: (String) -> String = { uid ->
                                    data.members.find { it.uid == uid }?.displayName ?: uid.take(6)
                                }
                                if (data.members.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(OpenSplitTokens.SpaceXL)
                                        ) {
                                            com.opensplit.ui.components.HandshakeIllustration(size = 120.dp)
                                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
                                            Text(
                                                text = "No balances yet",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                                            Text(
                                                text = "Balances show up once this group has members.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                Button(
                                    onClick = {
                                        if (data.members.size > 1) {
                                            onNavigateToSettleUp()
                                        } else {
                                            snackbar.showMessage("Add another member to this group before settling up")
                                        }
                                    },
                                    enabled = data.members.size > 1,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(OpenSplitIcons.Settle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                                    Text("Settle Up")
                                }
                                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                                if (data.simplifiedSettlements.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.large,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(OpenSplitTokens.SpaceLG)) {
                                            Text(
                                                text = "Suggested payments",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                                            data.simplifiedSettlements.forEach { s ->
                                                Text(
                                                    text = "${nameOf(s.fromUid)} → ${nameOf(s.toUid)}: ${com.opensplit.util.CurrencyFormatter.format(s.amount, data.group.currency)}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(OpenSplitTokens.SpaceLG)) {
                                        Text(
                                            text = "Group Balances Summary",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                                        data.members.forEach { user ->
                                            val bal = data.balances[user.uid] ?: 0.0
                                            val color = if (bal > 0.01) OpenSplitTokens.OwedPositive else if (bal < -0.01) OpenSplitTokens.OwedNegative else OpenSplitTokens.OwedNeutral
                                            val currency = data.group.currency
                                            val text = if (bal > 0.01) {
                                                "Gets back ${com.opensplit.util.CurrencyFormatter.format(bal, currency)}"
                                            } else if (bal < -0.01) {
                                                "Owes ${com.opensplit.util.CurrencyFormatter.format(-bal, currency)}"
                                            } else {
                                                "Settled up"
                                            }

                                            ListItem(
                                                headlineContent = { Text(user.displayName, fontWeight = FontWeight.Medium) },
                                                supportingContent = { Text(text, color = color, fontWeight = FontWeight.SemiBold) },
                                                leadingContent = {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = user.displayName.take(1).uppercase(),
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                }
                            }
                        }

                        2 -> { // Members Tab
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(OpenSplitTokens.SpaceLG)
                            ) {
                                // Simplify Debts Toggle Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = OpenSplitTokens.SpaceMD),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = OpenSplitIcons.AutoAwesome,
                                                        contentDescription = "Simplify Debts",
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Simplify Debts",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Minimize total number of transactions",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = data.group.simplifyDebts,
                                            onCheckedChange = {
                                                viewModel.setSimplifyDebts(it)
                                                snackbar.showMessage(if (it) "Debt simplification enabled" else "Debt simplification disabled")
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.surface,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }

                                if (data.members.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(OpenSplitTokens.SpaceXL),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            com.opensplit.ui.components.HandshakeIllustration(size = 120.dp)
                                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
                                            Text(
                                                text = "No members yet",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                                            Text(
                                                text = "Use \"Add New Member\" below to bring people into this group.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)
                                    ) {
                                        item {
                                            Text(
                                                text = "${data.members.size} Members",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS)
                                            )
                                        }

                                        items(data.members, key = { it.uid }) { user ->
                                            ListItem(
                                                headlineContent = { Text(user.displayName, fontWeight = FontWeight.SemiBold) },
                                                supportingContent = { Text(user.email, style = MaterialTheme.typography.bodySmall) },
                                                leadingContent = {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        modifier = Modifier.size(44.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = user.displayName.take(1).uppercase(),
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        }

                                        if (data.pendingInvites.isNotEmpty()) {
                                            item {
                                                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                                                Text(
                                                    text = "Pending Invites (${data.pendingInvites.size})",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS)
                                                )
                                            }

                                            items(data.pendingInvites, key = { it.id }) { invite ->
                                                val expiryStr = remember(invite.expiresAt) {
                                                    try {
                                                        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(invite.expiresAt.toDate())
                                                    } catch (e: Exception) { "7 days" }
                                                }
                                                ListItem(
                                                    headlineContent = { Text(invite.email, fontWeight = FontWeight.Medium) },
                                                    supportingContent = {
                                                        Text(
                                                            text = "Invite expires $expiryStr",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    },
                                                    leadingContent = {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                                            modifier = Modifier.size(40.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    OpenSplitIcons.Invite,
                                                                    contentDescription = "Pending",
                                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    },
                                                    trailingContent = {
                                                        TextButton(
                                                            onClick = {
                                                                viewModel.revokeInvite(invite.id)
                                                                snackbar.showUndo("Invite revoked for ${invite.email}") {
                                                                    viewModel.addMemberByEmail(invite.email)
                                                                }
                                                            }
                                                        ) {
                                                            Text("Revoke", color = MaterialTheme.colorScheme.error)
                                                        }
                                                    }
                                                )
                                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                                    // Add New Member Button
                                    Button(
                                        onClick = { showAddMember = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(OpenSplitIcons.Invite, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                                        Text("Add New Member", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    }
                }

                if (showFilterSortSheet) {
                    FilterAndSortBottomSheet(
                        currentSort = sortOrder,
                        filterPaidByYou = filterPaidByYou,
                        filterInvolvedYou = filterInvolvedYou,
                        resultCount = filteredExpenses.size,
                        onApply = { sort, paid, involved ->
                            sortOrder = sort
                            filterPaidByYou = paid
                            filterInvolvedYou = involved
                        },
                        onReset = {
                            sortOrder = ExpenseSortOrder.DATE_NEWEST
                            filterPaidByYou = false
                            filterInvolvedYou = false
                        },
                        onDismiss = { showFilterSortSheet = false }
                    )
                }

                if (showExportSheet) {
                    ExportBottomSheet(
                        scopeName = data.group.name,
                        expenses = data.expenses,
                        groups = listOf(data.group),
                        onDismiss = { showExportSheet = false }
                    )
                }
            }
        }
    }

    if (showAddMember) {
        InviteMemberDialog(
            title = "Add member",
            description = "Pick them from your contacts, or invite over WhatsApp or SMS.",
            onDismiss = { showAddMember = false },
            onSubmitEmail = { email ->
                viewModel.addMemberByEmail(email)
                snackbar.showMessage("Member / Invite added")
            }
        )
    }

    if (showSettings) {
        val success = uiState as? ScreenState.Success
        if (success != null) {
            val group = success.data.group
            val members = success.data.members
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            var renameText by remember(group.name) { mutableStateOf(group.name) }
            var confirmDelete by remember { mutableStateOf(false) }
            var showAvatarPicker by remember { mutableStateOf(false) }
            val currencies = listOf("INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD")

            ModalBottomSheet(onDismissRequest = { showSettings = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(OpenSplitTokens.SpaceLG),
                    verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)
                ) {
                    Text("Group settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    // Avatar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clickable { showAvatarPicker = true }) {
                            GroupAvatar(name = group.name, avatarKey = group.avatarKey, size = 56.dp)
                        }
                        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceMD))
                        TextButton(onClick = { showAvatarPicker = true }) {
                            Text(if (group.avatarKey == null) "Choose an avatar" else "Change avatar")
                        }
                    }

                    // Rename
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Group name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            viewModel.renameGroup(renameText)
                            snackbar.showMessage("Group renamed")
                        },
                        enabled = renameText.isNotBlank() && renameText.trim() != group.name,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Rename") }

                    // Currency
                    Text("Currency", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                        currencies.forEach { c ->
                            val flag = com.opensplit.util.CurrencyFormatter.getCurrencyFlag(c)
                            val symbol = com.opensplit.util.CurrencyFormatter.getCurrencySymbol(c)
                            FilterChip(
                                selected = group.currency == c,
                                onClick = {
                                    viewModel.setGroupCurrency(c)
                                    snackbar.showMessage("Currency set to $c")
                                },
                                label = { Text("$flag $c ($symbol)") }
                            )
                        }
                    }

                    // Simplify debts toggle
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simplify debts", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Minimize the number of payments to settle up",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = group.simplifyDebts, onCheckedChange = {
                            viewModel.setSimplifyDebts(it)
                            snackbar.showMessage(if (it) "Simplify debts on" else "Simplify debts off")
                        })
                    }

                    // Members (remove; can't remove the creator)
                    Text("Members", fontWeight = FontWeight.SemiBold)
                    members.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(m.displayName, modifier = Modifier.weight(1f))
                            if (m.uid != group.createdBy) {
                                TextButton(onClick = {
                                    viewModel.removeMember(m.uid)
                                    snackbar.showUndo("Removed ${m.displayName}") {
                                        viewModel.restoreMember(m.uid)
                                    }
                                }) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            } else {
                                Text("Owner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                            viewModel.leaveGroup { showSettings = false; onNavigateBack() }
                            snackbar.showMessage("Left ${group.name}")
                        },
                            modifier = Modifier.weight(1f)
                        ) { Text("Leave group") }

                        if (currentUid == group.createdBy) {
                            Button(
                                onClick = { confirmDelete = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) { Text("Delete group") }
                        }
                    }
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
                }
            }

            if (confirmDelete) {
                AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    title = { Text("Delete group?") },
                    text = { Text("This permanently deletes '${group.name}' and its expenses for everyone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                confirmDelete = false
                                viewModel.deleteGroup { showSettings = false; onNavigateBack() }
                                snackbar.showMessage("Deleted ${group.name}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
                    }
                )
            }

            if (showAvatarPicker) {
                val aiSuggestedKey = remember(group.name) {
                    com.opensplit.data.ai.GeminiGroupIconSuggester.getLocalHeuristicSuggestion(group.name)
                }
                GroupAvatarPickerSheet(
                    currentKey = group.avatarKey,
                    aiSuggestedKey = aiSuggestedKey,
                    onDismiss = { showAvatarPicker = false },
                    onSelect = {
                        viewModel.setGroupAvatar(it)
                        snackbar.showMessage("Avatar updated")
                    }
                )
            }
        }
    }
}
