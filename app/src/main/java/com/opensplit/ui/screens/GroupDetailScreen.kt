@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.theme.Symbol
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.LocalSnackbarController

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.animateContentSize
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
    onNavigateToSettleUp: (fromUid: String?, toUid: String?, amount: Double?) -> Unit = { _, _, _ -> },
    onNavigateToEditExpense: (String, String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val busy = com.opensplit.ui.components.LocalBusy.current
    var showAddMember by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showFilterSortSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
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
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = onNavigateBack) {
                        SymbolIcon(OpenSplitIcons.Back, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = { showFilterSortSheet = true }) {
                        SymbolIcon(OpenSplitIcons.Filter, contentDescription = "Filter and Sort")
                    }
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = { showExportSheet = true }) {
                        SymbolIcon(OpenSplitIcons.Export, contentDescription = "Export Group Expenses")
                    }
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = { showSettings = true }) {
                        SymbolIcon(OpenSplitIcons.More, contentDescription = "Group settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                ),
                modifier = Modifier.appHazeHeader(hazeState)
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> {
                    ExtendedFloatingActionButton(
                        expanded = isExpanded,
                        onClick = onNavigateToAddExpense,
                        icon = { SymbolIcon(OpenSplitIcons.AddExpense, contentDescription = null) },
                        text = { Text("Add Expense") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                2 -> {
                    ExtendedFloatingActionButton(
                        expanded = true,
                        onClick = { showAddMember = true },
                        icon = { SymbolIcon(OpenSplitIcons.Invite, contentDescription = null) },
                        text = { Text("Add Member") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                else -> {}
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
        ) {
            StateLayout(state = uiState) { data ->
                val currentUid = viewModel.currentUserId
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(title)
                                        if (index == 0 && data.expenses.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ) {
                                                Text(
                                                    "${data.expenses.size}",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
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
                                            .animateContentSize()
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
                                                com.opensplit.ui.components.AnimatedNumberText(text = "$currencySymbol${com.opensplit.util.CurrencyFormatter.format(totalSpending, showSymbol = false)}", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
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
                                                        val s = data.simplifiedSettlements.firstOrNull()
                                                        onNavigateToSettleUp(s?.fromUid, s?.toUid, s?.amount)
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
                                                SymbolIcon(OpenSplitIcons.Settle, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Settle Up", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Settle-up suggestion: only when someone actually owes the signed-in user.
                                    val me = viewModel.currentUserId
                                    val owedToMe = data.simplifiedSettlements.filter { it.toUid == me && it.amount > 0.01 }
                                    if (owedToMe.isNotEmpty()) {
                                        val top = owedToMe.maxBy { it.amount }
                                        val who = data.members.find { it.uid == top.fromUid }?.displayName ?: "Someone"
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = OpenSplitTokens.SpaceMD),
                                            shape = RoundedCornerShape(24.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                        ) {
                                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        SymbolIcon(OpenSplitIcons.Settle, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("$who owes you ${com.opensplit.util.CurrencyFormatter.format(top.amount, data.group.currency)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        if (owedToMe.size > 1) "${owedToMe.size} people owe you in this group" else "Record it once they pay you",
                                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                TextButton(shapes = ButtonDefaults.shapes(), onClick = { onNavigateToSettleUp(top.fromUid, top.toUid, top.amount) }) { Text("Settle up") }
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
                                            Button(shapes = ButtonDefaults.shapes(), onClick = onNavigateToAddExpense) {
                                                SymbolIcon(OpenSplitIcons.AddExpense, contentDescription = null)
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

                                            com.opensplit.ui.components.SwipeActionRow(
                                                modifier = Modifier.animateItem(),
                                                startAction = com.opensplit.ui.components.SwipeAction(
                                                    icon = OpenSplitIcons.Edit, label = "Edit",
                                                    container = MaterialTheme.colorScheme.primaryContainer,
                                                    onContainer = MaterialTheme.colorScheme.onPrimaryContainer
                                                ) { onNavigateToEditExpense(data.group.id, exp.id) },
                                                endAction = com.opensplit.ui.components.SwipeAction(
                                                    icon = OpenSplitIcons.Delete, label = "Delete",
                                                    container = MaterialTheme.colorScheme.errorContainer,
                                                    onContainer = MaterialTheme.colorScheme.onErrorContainer,
                                                    primary = false
                                                ) {
                                                    busy.show("Deleting expense…")
                                                    viewModel.deleteExpense(exp.id) { ok ->
                                                        busy.hide()
                                                        if (ok) snackbar.showUndo("Deleted ${exp.description}") { viewModel.restoreExpense(exp.id) }
                                                        else snackbar.showMessage("Couldn't delete ${exp.description}")
                                                    }
                                                }
                                            ) {
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
                                                            SymbolIcon(
                                                                symbol = categoryIcon,
                                                                contentDescription = exp.category,
                                                                tint = categoryColor,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                trailingContent = {
                                                    Text(
                                                        text = com.opensplit.util.CurrencyFormatter.format(exp.amount, exp.currency) +
                                                            if (exp.currency != data.group.currency) " ${exp.currency}" else "",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            )
                                            }
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
                                Button(shapes = ButtonDefaults.shapes(), 
                                    onClick = {
                                        if (data.members.size > 1) {
                                            val s = data.simplifiedSettlements.firstOrNull()
                                            onNavigateToSettleUp(s?.fromUid, s?.toUid, s?.amount)
                                        } else {
                                            snackbar.showMessage("Add another member to this group before settling up")
                                        }
                                    },
                                    enabled = data.members.size > 1,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    SymbolIcon(OpenSplitIcons.Settle, contentDescription = null)
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
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { onNavigateToSettleUp(s.fromUid, s.toUid, s.amount) }
                                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${nameOf(s.fromUid)} → ${nameOf(s.toUid)}: ${com.opensplit.util.CurrencyFormatter.format(s.amount, data.group.currency)}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = "Settle",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
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
                                                    SymbolIcon(
                                                        symbol = OpenSplitIcons.AutoAwesome,
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
                                        contentPadding = PaddingValues(bottom = 88.dp),
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
                                                },
                                                trailingContent = {
                                                    if (user.uid == data.group.createdBy) {
                                                        Surface(
                                                            shape = OpenSplitTokens.PillShape,
                                                            color = MaterialTheme.colorScheme.primaryContainer
                                                        ) {
                                                            Text(
                                                                "Owner",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        }

                                        if (data.formerMembers.isNotEmpty()) {
                                            item {
                                                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                                                Text(
                                                    text = "Left the group",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS)
                                                )
                                            }
                                            items(data.formerMembers, key = { "former_" + it.uid }) { user ->
                                                ListItem(
                                                    headlineContent = { Text(user.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                    supportingContent = { Text("Past expenses still show their share", style = MaterialTheme.typography.bodySmall) },
                                                    trailingContent = {
                                                        Surface(shape = OpenSplitTokens.PillShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                                                            Text("Left", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                )
                                            }
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
                                                                SymbolIcon(
                                                                    OpenSplitIcons.Invite,
                                                                    contentDescription = "Pending",
                                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    },
                                                    trailingContent = {
                                                        TextButton(shapes = ButtonDefaults.shapes(), 
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
                        currentUid = viewModel.currentUserId,
                        onDismiss = { showExportSheet = false }
                    )
                }
            }
        }
    }

    if (showAddMember) {
        val links by viewModel.inviteLinks.collectAsState()
        val success = uiState as? ScreenState.Success
        val gName = success?.data?.group?.name ?: groupName
        val gId = success?.data?.group?.id ?: viewModel.groupId
        GroupInviteSheet(
            groupId = gId,
            groupName = gName,
            inviteLink = links.firstOrNull(),
            onDismiss = { showAddMember = false },
            onSubmitEmail = { email ->
                viewModel.addMemberByEmail(email)
                snackbar.showMessage("Member / Invite added")
            },
            onGenerateLink = { expires, maxUses ->
                busy.show("Creating invite link…")
                viewModel.createInviteLink(expires, maxUses) { ok ->
                    busy.hide()
                    snackbar.showMessage(if (ok) "Invite link ready" else "Couldn't create link")
                }
            }
        )
    }

    if (showSettings) {
        val success = uiState as? ScreenState.Success
        if (success != null) {
            val group = success.data.group
            val members = success.data.members
            val currentUid = viewModel.currentUserId
            var renameText by remember(group.name) { mutableStateOf(group.name) }
            val currencies = listOf("INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD")

            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(OpenSplitTokens.SpaceLG),
                    verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)
                ) {
                    Text("Group settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    // Avatar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clickable {
                            showSettings = false
                            showAvatarPicker = true
                        }) {
                            GroupAvatar(name = group.name, avatarKey = group.avatarKey, size = 56.dp)
                        }
                        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceMD))
                        TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                            showSettings = false
                            showAvatarPicker = true
                        }) {
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
                    Button(shapes = ButtonDefaults.shapes(), 
                        onClick = {
                            viewModel.renameGroup(renameText)
                            snackbar.showMessage("Group renamed")
                        },
                        enabled = renameText.isNotBlank() && renameText.trim() != group.name,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Rename") }

                    // Currency: locked once the group has expenses, so balances stay consistent.
                    val currencyLocked = success.data.expenses.isNotEmpty()
                    Text("Currency", fontWeight = FontWeight.SemiBold)
                    if (currencyLocked) {
                        Text(
                            "Can't be changed once a group has expenses. Each expense can still use its own currency.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                        currencies.forEach { c ->
                            val flag = com.opensplit.util.CurrencyFormatter.getCurrencyFlag(c)
                            val symbol = com.opensplit.util.CurrencyFormatter.getCurrencySymbol(c)
                            FilterChip(
                                selected = group.currency == c,
                                enabled = !currencyLocked || group.currency == c,
                                onClick = {
                                    viewModel.setGroupCurrency(c)
                                    snackbar.showMessage("Currency set to $c")
                                },
                                label = { Text("$flag $c ($symbol)") }
                            )
                        }
                    }

                    // Default split for new expenses
                    Text("Default split", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                        com.opensplit.domain.model.SplitType.entries.forEach { st ->
                            FilterChip(
                                selected = group.defaultSplitType == st,
                                onClick = { viewModel.setDefaultSplitType(st) },
                                label = { Text(st.name.lowercase().replaceFirstChar { it.uppercase() }) }
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
                                TextButton(shapes = ButtonDefaults.shapes(), onClick = {
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
                        OutlinedButton(shapes = ButtonDefaults.shapes(), 
                            onClick = {
                                val myBalance = currentUid?.let { viewModel.balanceOf(it) } ?: 0.0
                                if (kotlin.math.abs(myBalance) > 0.01) {
                                    snackbar.showMessage("Settle up first: you still have a balance in this group.")
                                } else {
                                    viewModel.leaveGroup { showSettings = false; onNavigateBack() }
                                    snackbar.showMessage("Left ${group.name}. Your past expenses stay in the history.")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Leave group") }

                        if (currentUid == group.createdBy) {
                            Button(shapes = ButtonDefaults.shapes(), 
                                onClick = {
                                    showSettings = false
                                    confirmDelete = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) { Text("Delete group") }
                        }
                    }
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXXL))
                }
            }
        }
    }

    val activeGroup = (uiState as? ScreenState.Success)?.data?.group
    if (confirmDelete && activeGroup != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete group?") },
            text = { Text("This permanently deletes '${activeGroup.name}' and its expenses for everyone.") },
            confirmButton = {
                Button(shapes = ButtonDefaults.shapes(), 
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteGroup { onNavigateBack() }
                        snackbar.showMessage("Deleted ${activeGroup.name}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(shapes = ButtonDefaults.shapes(), onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    if (showAvatarPicker && activeGroup != null) {
        val aiSuggestedKey = remember(activeGroup.name) {
            com.opensplit.data.ai.AiHeuristics.avatarKeyFor(activeGroup.name)
        }
        GroupAvatarPickerSheet(
            currentKey = activeGroup.avatarKey,
            aiSuggestedKey = aiSuggestedKey,
            onDismiss = { showAvatarPicker = false },
            onSelect = {
                viewModel.setGroupAvatar(it)
                snackbar.showMessage("Avatar updated")
            }
        )
    }
}
