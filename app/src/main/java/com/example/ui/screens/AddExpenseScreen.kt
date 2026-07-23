package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.Expense
import com.example.domain.model.ExpenseSplit
import com.example.ui.components.StateLayout
import com.example.ui.viewmodel.GroupDetailViewModel
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: GroupDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    StateLayout(state = uiState) { data ->
        val group = data.group
        val members = data.members

        var description by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("General") }
        var paidBy by remember { mutableStateOf(members.firstOrNull()?.uid ?: "") }
        var categoryMenuExpanded by remember { mutableStateOf(false) }

        val categories = listOf("General", "Food & Drink", "Transportation", "Entertainment", "Utilities", "Rent", "Groceries")

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add Expense", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val amt = amountText.toDoubleOrNull()
                                if (description.isNotBlank() && amt != null && amt > 0) {
                                    val equalSplitAmount = amt / maxOf(1, members.size)
                                    val splits = members.map { member ->
                                        ExpenseSplit(uid = member.uid, amount = equalSplitAmount)
                                    }

                                    val newExpense = Expense(
                                        groupId = group.id,
                                        description = description.trim(),
                                        amount = amt,
                                        paidBy = paidBy,
                                        category = category,
                                        currency = group.currency,
                                        splits = splits,
                                        createdBy = paidBy,
                                        date = Timestamp.now()
                                    )

                                    viewModel.addExpense(newExpense, onSuccess = onNavigateBack)
                                }
                            },
                            enabled = description.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Dinner, Rent, Groceries") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (${group.currency})") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryMenuExpanded = true }
                    )

                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Paid by", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Column {
                    members.forEach { user ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { paidBy = user.uid }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = paidBy == user.uid,
                                onClick = { paidBy = user.uid }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(user.displayName)
                        }
                    }
                }
            }
        }
    }
}
