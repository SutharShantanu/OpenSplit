package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.Expense
import com.example.domain.model.ExpenseSplit
import com.example.ui.components.CategoryChipRow
import com.example.ui.components.StateLayout
import com.example.ui.theme.OpenSplitIcons
import com.example.ui.theme.OpenSplitTokens
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
        var category by remember { mutableStateOf("Food") }
        var paidBy by remember { mutableStateOf(members.firstOrNull()?.uid ?: "") }
        var selectedSplitType by remember { mutableStateOf(0) } // 0: Equal, 1: Custom

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add expense", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(OpenSplitIcons.Close, contentDescription = "Close")
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
                    .padding(OpenSplitTokens.SpaceLG),
                verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceLG)
            ) {
                // Prominent Large Amount Input Field
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OpenSplitTokens.SpaceLG),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AMOUNT (${group.currency})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            placeholder = { Text("0.00", style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center) },
                            textStyle = MaterialTheme.typography.displayMedium.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    }
                }

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Dinner, Groceries, Flight") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection
                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                    CategoryChipRow(
                        selectedCategory = category,
                        onCategorySelected = { category = it }
                    )
                }

                // Paid By Selection
                Column {
                    Text(
                        text = "Paid by",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        members.forEach { user ->
                            val isSelected = paidBy == user.uid
                            FilterChip(
                                selected = isSelected,
                                onClick = { paidBy = user.uid },
                                label = { Text(user.displayName) },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }
                }

                // Split Options Segmented Buttons
                Column {
                    Text(
                        text = "Split Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedSplitType == 0,
                            onClick = { selectedSplitType = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Equally")
                        }
                        SegmentedButton(
                            selected = selectedSplitType == 1,
                            onClick = { selectedSplitType = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Exact")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                // Bottom Save Action
                Button(
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = description.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text("Save Expense")
                }
            }
        }
    }
}

