package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.GroupDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: GroupDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val group by viewModel.group.collectAsState()
    val members by viewModel.members.collectAsState()

    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    // In a real app we'd let user select who paid. For now we assume current user.
    // However, since we don't have current user directly available here, we'll just pick the first member
    var paidBy by remember { mutableStateOf(members.firstOrNull()?.uid ?: "") }

    LaunchedEffect(members) {
        if (paidBy.isEmpty() && members.isNotEmpty()) {
            paidBy = members.first().uid
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (amount != null && description.isNotBlank() && paidBy.isNotBlank()) {
                                viewModel.addExpense(description, amount, paidBy)
                                onNavigateBack()
                            }
                        },
                        enabled = description.isNotBlank() && amountText.toDoubleOrNull() != null
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (${group?.currency ?: "USD"})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Paid by", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            // simple dropdown or list of members
            members.forEach { user ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = paidBy == user.uid,
                        onClick = { paidBy = user.uid }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(user.displayName)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Split equally between all members", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
