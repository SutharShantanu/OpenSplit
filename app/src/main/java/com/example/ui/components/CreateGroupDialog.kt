package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, currency: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("USD") }
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    val currencies = listOf("USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Box {
                    OutlinedTextField(
                        value = selectedCurrency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currencyMenuExpanded = true }
                    )

                    DropdownMenu(
                        expanded = currencyMenuExpanded,
                        onDismissRequest = { currencyMenuExpanded = false }
                    ) {
                        currencies.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text(curr) },
                                onClick = {
                                    selectedCurrency = curr
                                    currencyMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), selectedCurrency)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
