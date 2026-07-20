package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.GroupDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddExpense: () -> Unit
) {
    val group by viewModel.group.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val members by viewModel.members.collectAsState()
    val balances by viewModel.balances.collectAsState()

    var showAddMember by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Group Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddMember = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Person, contentDescription = "Add Member")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddExpense) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (group == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    text = "Members",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                // Display balances
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    members.forEach { user ->
                        val bal = balances[user.uid] ?: 0.0
                        val color = if (bal > 0.01) MaterialTheme.colorScheme.primary else if (bal < -0.01) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        val currency = group?.currency ?: "USD"
                        val text = if (bal > 0.01) "gets back $currency ${"%.2f".format(bal)}" else if (bal < -0.01) "owes $currency ${"%.2f".format(-bal)}" else "settled up"
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(user.displayName, style = MaterialTheme.typography.bodyLarge)
                                Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()

                Text(
                    text = "Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )

                if (expenses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No expenses yet.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(expenses) { exp ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(exp.description, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${exp.amount} ${exp.currency}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Paid by ${members.find { it.uid == exp.paidBy }?.displayName ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMember) {
        var emailQuery by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text("Add Member") },
            text = {
                Column {
                    Text("Enter the email address of the user you want to add.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailQuery,
                        onValueChange = { emailQuery = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addMemberByEmail(emailQuery)
                        showAddMember = false
                    },
                    enabled = emailQuery.isNotBlank()
                ) {
                    Text("Add")
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
