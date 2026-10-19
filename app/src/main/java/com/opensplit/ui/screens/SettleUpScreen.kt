@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.components.SymbolIcon
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
    suggestedFromUid: String? = null,
    suggestedToUid: String? = null,
    suggestedAmount: Double? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val members by viewModel.members.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val snackbar = LocalSnackbarController.current
    val busy = com.opensplit.ui.components.LocalBusy.current

    var fromUid by rememberSaveable { mutableStateOf(suggestedFromUid ?: currentUserId ?: "") }
    var toUid by rememberSaveable { mutableStateOf(suggestedToUid ?: "") }
    var amountText by rememberSaveable { mutableStateOf(suggestedAmount?.let { CurrencyFormatter.format(it, showSymbol = false) } ?: "") }
    var note by rememberSaveable { mutableStateOf("") }
    
    var method by rememberSaveable { mutableStateOf(SettlementMethod.UPI) }

    var isSuccessScreenShown by remember { mutableStateOf(false) }
    var recordedSettlementAmount by remember { mutableStateOf(0.0) }
    var memberPickerTarget by remember { mutableStateOf<String?>(null) } // "from" or "to"

    val displayMembers = remember(members, currentUserId) {
        if (members.isNotEmpty()) members else listOf(
            User(uid = currentUserId ?: "user1", displayName = "You"),
            User(uid = "user2", displayName = "Rahul Sharma"),
            User(uid = "user3", displayName = "Priya Patel")
        )
    }

    LaunchedEffect(members, currentUserId, suggestedFromUid, suggestedToUid, suggestedAmount) {
        if (members.isNotEmpty()) {
            if (suggestedFromUid != null && members.any { it.uid == suggestedFromUid }) {
                fromUid = suggestedFromUid
            } else if (fromUid.isEmpty() || members.none { it.uid == fromUid }) {
                fromUid = currentUserId?.takeIf { id -> members.any { it.uid == id } } ?: members.first().uid
            }
            if (suggestedToUid != null && suggestedToUid != fromUid && members.any { it.uid == suggestedToUid }) {
                toUid = suggestedToUid
            } else if (toUid.isEmpty() || toUid == fromUid || members.none { it.uid == toUid }) {
                toUid = members.firstOrNull { it.uid != fromUid }?.uid ?: ""
            }
        }
        if (amountText.isEmpty() && suggestedAmount != null && suggestedAmount > 0) {
            amountText = CurrencyFormatter.format(suggestedAmount, showSymbol = false)
        }
    }

    val fromUser = remember(displayMembers, fromUid) { displayMembers.firstOrNull { it.uid == fromUid } ?: displayMembers.firstOrNull() }
    val toUser = remember(displayMembers, toUid) { displayMembers.firstOrNull { it.uid == toUid } ?: displayMembers.getOrNull(1) }

    val currencySymbol = remember(currency) { CurrencyFormatter.getCurrencySymbol(currency) }

    val rawAmount = amountText.replace(",", "").toDoubleOrNull() ?: 0.0
    var isRecording by remember { mutableStateOf(false) }
    val canSubmit = fromUid.isNotBlank() && toUid.isNotBlank() && fromUid != toUid && rawAmount > 0 && !isRecording

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
            // Trigger UPI Payment Intent ONLY when UPI method is chosen
            if (method == SettlementMethod.UPI) {
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
            }

            isRecording = true
            busy.show("Recording payment…")
            viewModel.addSettlement(
                fromUid = fromUid,
                toUid = toUid,
                amount = rawAmount,
                method = method,
                note = note.ifBlank { null },
                onSuccess = {
                    isRecording = false
                    busy.hide()
                    recordedSettlementAmount = rawAmount
                    isSuccessScreenShown = true
                }
            )
        }
    }

    if (isSuccessScreenShown) {
        // Successful Settlement Screen
        val successSubtitle = when {
            fromUid == currentUserId -> "You paid ${toUser?.displayName ?: "Recipient"}"
            toUid == currentUserId -> "${fromUser?.displayName ?: "Payer"} paid you"
            else -> "${fromUser?.displayName ?: "Payer"} paid ${toUser?.displayName ?: "Recipient"}"
        }
        SettlementSuccessOverlay(
            amount = recordedSettlementAmount,
            currencySymbol = currencySymbol,
            subtitle = successSubtitle,
            fromName = fromUser?.displayName ?: "Payer",
            toName = toUser?.displayName ?: "Recipient",
            method = method,
            onDone = onNavigateBack
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Record Payment", fontWeight = FontWeight.Bold)
                            Text("Settle your balances instantly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(shapes = IconButtonDefaults.shapes(), onClick = onNavigateBack) {
                            SymbolIcon(OpenSplitIcons.Close, contentDescription = "Close")
                        }
                    }
                )
            }
        ) { padding ->
            com.opensplit.ui.components.FormLock(locked = isRecording, modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(OpenSplitTokens.SpaceLG),
                verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceLG)
            ) {
                // 1. Transaction Direction Card (Tonal Layering Level 1)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(OpenSplitTokens.SpaceLG),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Payer -> Paying Arrow -> Payee
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Payer Block (Clickable)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { memberPickerTarget = "from" }
                                    .padding(8.dp)
                            ) {
                                UserAvatar(
                                    name = fromUser?.displayName ?: "Payer",
                                    photoUrl = fromUser?.photoUrl,
                                    size = 56
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
                                    text = "Payer (tap to change)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Direction Indicator Pill ("Paying" with swap on tap)
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val temp = fromUid
                                        fromUid = toUid
                                        toUid = temp
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SymbolIcon(
                                        symbol = OpenSplitIcons.Sync,
                                        contentDescription = "Swap Payer and Payee",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Paying",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    SymbolIcon(
                                        symbol = OpenSplitIcons.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Payee Block (Clickable)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { memberPickerTarget = "to" }
                                    .padding(8.dp)
                            ) {
                                UserAvatar(
                                    name = toUser?.displayName ?: "Recipient",
                                    photoUrl = toUser?.photoUrl,
                                    size = 56
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
                                    text = "Recipient (tap to change)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                        // Amount Input Section
                        Text(
                            text = "AMOUNT TO SETTLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
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
                                    if (newValue.all { it.isDigit() || it == '.' || it == ',' }) {
                                        amountText = newValue
                                    }
                                },
                                textStyle = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = dynamicFontSize,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = com.opensplit.ui.theme.MoneyFontFamily,
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
                                                text = "0.00",
                                                style = MaterialTheme.typography.displayMedium.copy(
                                                    fontSize = dynamicFontSize,
                                                    fontFamily = com.opensplit.ui.theme.MoneyFontFamily,
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
                                icon = { SymbolIcon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) },
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

                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                        // Payment Method Selector
                        Text(
                            text = "PAYMENT METHOD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                SettlementMethod.UPI to "UPI",
                                SettlementMethod.CASH to "Cash",
                                SettlementMethod.BANK_TRANSFER to "Bank",
                                SettlementMethod.OTHER to "Other"
                            ).forEach { (m, label) ->
                                FilterChip(
                                    selected = method == m,
                                    onClick = { method = m },
                                    label = { Text(label) },
                                    leadingIcon = if (method == m) {
                                        { SymbolIcon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // Info Warning Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Marking as settled doesn't transfer money. Please ensure you've paid via your banking or UPI app first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Save Settlement Button
                Button(
                    onClick = handleRecordSettlement,
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    SymbolIcon(
                        symbol = OpenSplitIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Settlement",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } }
        }

        if (memberPickerTarget != null) {
            val isFrom = memberPickerTarget == "from"
            AlertDialog(
                onDismissRequest = { memberPickerTarget = null },
                title = { Text(if (isFrom) "Select Payer" else "Select Recipient") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayMembers.forEach { m ->
                            val isSelected = if (isFrom) m.uid == fromUid else m.uid == toUid
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isFrom) {
                                            fromUid = m.uid
                                            if (toUid == m.uid) {
                                                toUid = displayMembers.firstOrNull { it.uid != m.uid }?.uid ?: ""
                                            }
                                        } else {
                                            toUid = m.uid
                                            if (fromUid == m.uid) {
                                                fromUid = displayMembers.firstOrNull { it.uid != m.uid }?.uid ?: ""
                                            }
                                        }
                                        memberPickerTarget = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(name = m.displayName, photoUrl = m.photoUrl, size = 40)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = m.displayName + if (m.uid == currentUserId) " (You)" else "",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (m.email.isNotBlank()) {
                                            Text(
                                                text = m.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        SymbolIcon(OpenSplitIcons.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { memberPickerTarget = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettlementSuccessOverlay(
    amount: Double,
    currencySymbol: String,
    subtitle: String,
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
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated Success Checkmark Badge with ring
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SymbolIcon(
                                    symbol = OpenSplitIcons.CheckCircle,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Settle up complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Amount Sent Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AMOUNT SENT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$currencySymbol${CurrencyFormatter.format(amount, showSymbol = false)}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "via ${if (method == SettlementMethod.UPI) "UPI Transfer" else "Cash / Manual"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Back to group",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape
                ) {
                    Text(
                        text = "View receipt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * SCREEN 22 — Settlement Failed Overlay
 * Faithfully implemented from Stitch design `Settlement_Failed.html`.
 */
@Composable
fun SettlementFailedOverlay(
    amount: Double,
    currencySymbol: String,
    toName: String,
    errorMessage: String = "Something went wrong while recording the settlement. Please try again.",
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SymbolIcon(
                        symbol = OpenSplitIcons.ErrorIcon,
                        contentDescription = "Settlement Failed",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Settlement Failed",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Summary Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
                        text = "ATTEMPTED PAYMENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$currencySymbol${CurrencyFormatter.format(amount, showSymbol = false)} to $toName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                SymbolIcon(OpenSplitIcons.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape
            ) {
                Text("Cancel", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
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
