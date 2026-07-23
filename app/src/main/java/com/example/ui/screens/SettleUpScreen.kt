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
import com.example.domain.model.SettlementMethod
import com.example.ui.viewmodel.SettleUpViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpScreen(
    viewModel: SettleUpViewModel,
    suggestedToUid: String?,
    suggestedAmount: Double?,
    onNavigateBack: () -> Unit
) {
    val members by viewModel.members.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currency by viewModel.currency.collectAsState()
    
    var fromUid by remember { mutableStateOf(currentUserId ?: "") }
    var toUid by remember { mutableStateOf(suggestedToUid ?: "") }
    var amountText by remember { mutableStateOf(suggestedAmount?.toString() ?: "") }
    var note by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(SettlementMethod.CASH) }
    
    LaunchedEffect(currentUserId) {
        if (fromUid.isEmpty() && currentUserId != null) {
            fromUid = currentUserId!!
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settle Up") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (amount != null && fromUid.isNotBlank() && toUid.isNotBlank()) {
                                viewModel.addSettlement(
                                    fromUid = fromUid,
                                    toUid = toUid,
                                    amount = amount,
                                    method = method,
                                    note = note.ifBlank { null },
                                    onSuccess = onNavigateBack
                                )
                            }
                        },
                        enabled = fromUid.isNotBlank() && toUid.isNotBlank() && amountText.toDoubleOrNull() != null
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
            Text("From", style = MaterialTheme.typography.titleMedium)
            // simple member selector for 'from'
            members.forEach { user ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = fromUid == user.uid,
                        onClick = { fromUid = user.uid }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(user.displayName + if (user.uid == currentUserId) " (You)" else "")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("To", style = MaterialTheme.typography.titleMedium)
            // simple member selector for 'to'
            members.forEach { user ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = toUid == user.uid,
                        onClick = { toUid = user.uid }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(user.displayName + if (user.uid == currentUserId) " (You)" else "")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount ($currency)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            // Segmented button for method
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SettlementMethod.values().forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = method == option,
                        onClick = { method = option },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = SettlementMethod.values().size)
                    ) {
                        Text(option.name)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
