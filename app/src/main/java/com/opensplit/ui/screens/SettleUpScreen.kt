package com.opensplit.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.opensplit.domain.model.SettlementMethod
import com.opensplit.domain.model.User
import com.opensplit.domain.model.toDisplayName
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.util.CurrencyFormatter
import com.opensplit.ui.viewmodel.SettleUpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpScreen(
    viewModel: SettleUpViewModel,
    suggestedToUid: String?,
    suggestedAmount: Double?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val members by viewModel.members.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val snackbar = LocalSnackbarController.current

    var fromUid by rememberSaveable { mutableStateOf(currentUserId ?: "") }
    var toUid by rememberSaveable { mutableStateOf(suggestedToUid ?: "") }
    var amountText by rememberSaveable { mutableStateOf(suggestedAmount?.let { CurrencyFormatter.format(it, showSymbol = false) } ?: "") }
    var note by rememberSaveable { mutableStateOf("") }
    
    var method by rememberSaveable { mutableStateOf(SettlementMethod.UPI) }

    var isSuccessScreenShown by remember { mutableStateOf(false) }
    var recordedSettlementAmount by remember { mutableStateOf(0.0) }

    val displayMembers = remember(members, currentUserId) {
        if (members.isNotEmpty()) members else listOf(
            User(uid = currentUserId ?: "user1", displayName = "You"),
            User(uid = "user2", displayName = "Rahul Sharma"),
            User(uid = "user3", displayName = "Priya Patel")
        )
    }

    LaunchedEffect(displayMembers, currentUserId) {
        if (fromUid.isEmpty() || displayMembers.none { it.uid == fromUid }) {
            fromUid = currentUserId?.takeIf { id -> displayMembers.any { it.uid == id } } ?: displayMembers.first().uid
        }
        if (toUid.isEmpty() || toUid == fromUid || displayMembers.none { it.uid == toUid }) {
            toUid = displayMembers.firstOrNull { it.uid != fromUid }?.uid ?: ""
        }
    }

    val fromUser = remember(displayMembers, fromUid) { displayMembers.firstOrNull { it.uid == fromUid } ?: displayMembers.firstOrNull() }
    val toUser = remember(displayMembers, toUid) { displayMembers.firstOrNull { it.uid == toUid } ?: displayMembers.getOrNull(1) }

    val currencySymbol = remember(currency) { CurrencyFormatter.getCurrencySymbol(currency) }

    val rawAmount = amountText.toDoubleOrNull() ?: 0.0
    val canSubmit = fromUid.isNotBlank() && toUid.isNotBlank() && fromUid != toUid && rawAmount > 0

    // Dynamic Indian Rupee formatting with proper commas
    val formattedDisplayAmount = remember(amountText) {
        if (rawAmount > 0) {
            CurrencyFormatter.format(rawAmount, showSymbol = false)
        } else {
            "0"
        }
    }

    // Non-collapsing dynamic font sizing based on formatted string length
    val dynamicFontSize = remember(formattedDisplayAmount) {
        when {
            formattedDisplayAmount.length > 10 -> 20.sp
            formattedDisplayAmount.length > 7 -> 26.sp
            else -> 34.sp
        }
    }

    val handleRecordSettlement = {
        if (canSubmit) {
            // Trigger UPI Payment Intent so UPI apps catch recipient, amount, description
            try {
                val recipientVpa = "opensplit@upi"
                val uriString = "upi://pay?pa=$recipientVpa&pn=${Uri.encode(toUser?.displayName ?: "Recipient")}&am=$rawAmount&tn=${Uri.encode(note.ifBlank { "OpenSplit Settlement" })}&cu=INR"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                val chooser = Intent.createChooser(intent, "Pay via UPI App")
                chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(chooser)
            } catch (_: Exception) {
                // If no UPI app installed or intent fails, fall back gracefully
            }

            viewModel.addSettlement(
                fromUid = fromUid,
                toUid = toUid,
                amount = rawAmount,
                method = method,
                note = note.ifBlank { null },
                onSuccess = {
                    recordedSettlementAmount = rawAmount
                    isSuccessScreenShown = true
                }
            )
        }
    }

    if (isSuccessScreenShown) {
        // Successful Settlement Screen
        SettlementSuccessOverlay(
            amount = recordedSettlementAmount,
            currencySymbol = currencySymbol,
            fromName = fromUser?.displayName ?: "Payer",
            toName = toUser?.displayName ?: "Recipient",
            method = method,
            onDone = onNavigateBack
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settle Up", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(OpenSplitIcons.Close, contentDescription = "Close")
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
                // 1. Top Card: Fixed Payer & Recipient Details + Amount + Description
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OpenSplitTokens.SpaceLG),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Transfer Flow Header (Fixed Member Settle - No Recipient Switching)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Payer Block (Fixed)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                UserAvatar(
                                    name = fromUser?.displayName ?: "Payer",
                                    photoUrl = fromUser?.photoUrl,
                                    size = 52
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = fromUser?.displayName + if (fromUser?.uid == currentUserId) " (You)" else "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Payer",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Directional Arrow Icon
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowForward,
                                    contentDescription = "Paid to",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(24.dp)
                                )
                            }

                            // Recipient Block (Fixed to target member)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                UserAvatar(
                                    name = toUser?.displayName ?: "Recipient",
                                    photoUrl = toUser?.photoUrl,
                                    size = 52
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = toUser?.displayName + if (toUser?.uid == currentUserId) " (You)" else "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Recipient",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                        // Amount Input Section (Single '0' placeholder, no rupee symbol or /- in placeholder)
                        Text(
                            text = "SETTLEMENT AMOUNT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            BasicTextField(
                                value = amountText,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() || it == '.' }) {
                                        amountText = newValue
                                    }
                                },
                                textStyle = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = dynamicFontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (amountText.isEmpty()) {
                                            Text(
                                                text = "0",
                                                style = MaterialTheme.typography.displayMedium.copy(
                                                    fontSize = dynamicFontSize,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    textAlign = TextAlign.Center
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        // Full Balance Shortcut Chip
                        if (suggestedAmount != null && suggestedAmount > 0) {
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                            SuggestionChip(
                                onClick = { amountText = CurrencyFormatter.format(suggestedAmount, showSymbol = false) },
                                label = {
                                    Text(
                                        text = "Full Balance: ${CurrencyFormatter.format(suggestedAmount, showSymbol = false)}",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                icon = { Icon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                        // Description Option
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Add description (Optional)") },
                            placeholder = { Text("e.g. Lunch money, Cab fare") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Show Pay Button ONLY when user fills the amount to pay
                AnimatedVisibility(
                    visible = canSubmit,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Button(
                        onClick = handleRecordSettlement,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = OpenSplitIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pay",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettlementSuccessOverlay(
    amount: Double,
    currencySymbol: String,
    fromName: String,
    toName: String,
    method: SettlementMethod,
    onDone: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated Success Checkmark Badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Settlement Recorded!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$currencySymbol${CurrencyFormatter.format(amount, showSymbol = false)}/-",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$fromName paid $toName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Method: ${if (method == SettlementMethod.UPI) "UPI App" else "Number / UPI ID"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(
    name: String,
    photoUrl: String?,
    size: Int
) {
    if (!photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
        )
    } else {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(size.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(1).uppercase(),
                    style = if (size > 36) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
