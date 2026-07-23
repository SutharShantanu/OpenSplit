package com.opensplit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opensplit.domain.model.SettlementMethod
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.SettleUpViewModel

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
    
    var fromUid by rememberSaveable { mutableStateOf(currentUserId ?: "") }
    var toUid by rememberSaveable { mutableStateOf(suggestedToUid ?: "") }
    var amountText by rememberSaveable { mutableStateOf(suggestedAmount?.toString() ?: "") }
    var note by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf(SettlementMethod.CASH) }
    
    LaunchedEffect(currentUserId) {
        if (fromUid.isEmpty() && currentUserId != null) {
            fromUid = currentUserId!!
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settle Up", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(OpenSplitIcons.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (amount != null && amount > 0 && fromUid.isNotBlank() && toUid.isNotBlank() && fromUid != toUid) {
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
                        enabled = fromUid.isNotBlank() && toUid.isNotBlank() && fromUid != toUid &&
                            (amountText.toDoubleOrNull()?.let { it > 0 } == true)
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
            // Prominent Amount Input Field
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
                        text = "SETTLEMENT AMOUNT ($currency)",
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

            // Payer (From)
            Column {
                Text("Payer (Who paid?)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    members.forEach { user ->
                        val isSelected = fromUid == user.uid
                        FilterChip(
                            selected = isSelected,
                            onClick = { fromUid = user.uid },
                            label = { Text(user.displayName + if (user.uid == currentUserId) " (You)" else "") },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }
            }

            // Recipient (To)
            Column {
                Text("Recipient (Who received?)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    members.forEach { user ->
                        val isSelected = toUid == user.uid
                        FilterChip(
                            selected = isSelected,
                            onClick = { toUid = user.uid },
                            label = { Text(user.displayName + if (user.uid == currentUserId) " (You)" else "") },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }
            }

            // Method
            Column {
                Text("Payment Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
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
            }

            // Optional Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                placeholder = { Text("e.g. Venmo transfer, Cash at dinner") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            // Submit Button
            Button(
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
                modifier = Modifier.fillMaxWidth(),
                enabled = fromUid.isNotBlank() && toUid.isNotBlank() && amountText.toDoubleOrNull() != null
            ) {
                Text("Record Settlement")
            }
        }
    }
}

