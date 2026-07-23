package com.opensplit.ui.screens

import android.content.Context
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.GroupDetailViewModel
import com.opensplit.ui.viewmodel.ScreenState
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization

@OptIn(ExperimentalMaterial3Api::class)
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
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabTitles = listOf("Expenses", "Balances", "Members")

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()

    val isExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 10 }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val emailOrPhone = extractEmailOrPhoneFromContact(context, uri)
                if (emailOrPhone != null) {
                    viewModel.addMemberFromContact(emailOrPhone, context)
                } else {
                    Toast.makeText(context, "No email or phone found for contact.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permission required to read contacts", Toast.LENGTH_SHORT).show()
        }
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
                    IconButton(onClick = { showExportSheet = true }) {
                        Icon(OpenSplitIcons.Export, contentDescription = "Export Group Expenses")
                    }
                    IconButton(onClick = { showAddMember = true }) {
                        Icon(OpenSplitIcons.Invite, contentDescription = "Add Member")
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
                val filteredExpenses = data.expenses.filter { exp ->
                    val matchesQuery = exp.description.contains(searchQuery, ignoreCase = true) || exp.category.contains(searchQuery, ignoreCase = true)
                    val matchesCategory = selectedCategory == "All" || exp.category.equals(selectedCategory, ignoreCase = true)
                    matchesQuery && matchesCategory
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

                    when (selectedTab) {
                        0 -> { // Expenses Tab
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceMD)
                            ) {
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
                                Button(
                                    onClick = onNavigateToSettleUp,
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

                        2 -> { // Members Tab
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(OpenSplitTokens.SpaceLG)
                            ) {
                                Button(
                                    onClick = { showAddMember = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(OpenSplitIcons.Invite, contentDescription = null)
                                    Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                                    Text("Add Member to Group")
                                }

                                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)
                                ) {
                                    item {
                                        Text(
                                            text = "Active Members (${data.members.size})",
                                            style = MaterialTheme.typography.titleSmall,
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
                                                    modifier = Modifier.size(40.dp)
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
                                                            Toast.makeText(context, "Invite revoked for ${invite.email}", Toast.LENGTH_SHORT).show()
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
        var emailQuery by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text("Add member") },
            text = {
                Column {
                    Text("Enter the email address of the person you want to add.")
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                    OutlinedTextField(
                        value = emailQuery,
                        onValueChange = { emailQuery = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))
                    Text("Or", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                    OutlinedButton(
                        onClick = {
                            showAddMember = false
                            permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(OpenSplitIcons.Contacts, contentDescription = null)
                        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                        Text("Add from contacts")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addMemberByEmail(emailQuery)
                        Toast.makeText(context, "Member / Invite added", Toast.LENGTH_SHORT).show()
                        showAddMember = false
                    },
                    enabled = emailQuery.isNotBlank()
                ) {
                    Text("Add member")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMember = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


suspend fun extractEmailOrPhoneFromContact(context: Context, contactUri: android.net.Uri): String? {
    return kotlinx.coroutines.Dispatchers.IO.let {
        kotlinx.coroutines.withContext(it) {
            try {
                var result: String? = null
                var contactId: String? = null
                context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    }
                }

                if (contactId != null) {
                    // Try to get email first
                    context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            result = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DATA))
                        }
                    }

                    // If no email, try phone
                    if (result == null) {
                        context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                result = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            }
                        }
                    }
                }
                result
            } catch (e: Exception) {
                null
            }
        }
    }
}
