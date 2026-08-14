package com.opensplit.ui.screens

import com.opensplit.ui.components.LocalSnackbarController

import com.opensplit.ui.components.AppLoadingIndicator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.rememberCoroutineScope
import com.opensplit.data.ai.GeminiReceiptParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opensplit.domain.logic.SplitCalculator
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.ExpenseItem
import com.opensplit.domain.model.ExpenseSplit
import com.opensplit.domain.model.RecurrenceFrequency
import com.opensplit.domain.model.RecurrenceRule
import com.opensplit.domain.model.SplitType
import com.opensplit.ui.components.CategoryChipRow
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.GroupDetailViewModel
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    viewModel: GroupDetailViewModel,
    onNavigateBack: () -> Unit,
    editingExpense: Expense? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    val sheetState = rememberModalBottomSheetState()

    // This screen is reached via a NavHost `dialog()` destination, so the previous screen (Home,
    // GroupDetail, ...) stays visible behind it instead of being disposed. Blur it through the
    // outer dialog window (API 31+) so the sheet reads as "floating over" the app instead of a
    // flat scrim — the ModalBottomSheet's own scrim below still provides the dim/opacity.
    if (android.os.Build.VERSION.SDK_INT >= 31) {
        val dialogWindowProvider = androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider
        LaunchedEffect(dialogWindowProvider) {
            dialogWindowProvider?.window?.apply {
                setBackgroundBlurRadius(48)
                setDimAmount(0f)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onNavigateBack,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        StateLayout(state = uiState) { data ->
            val group = data.group
            val members = data.members

            val isEditing = editingExpense != null

            var description by remember { mutableStateOf(editingExpense?.description ?: "") }
            var isDescriptionEditing by remember { mutableStateOf(false) }
            val descriptionFocusRequester = remember { FocusRequester() }
            var amountText by remember { mutableStateOf(editingExpense?.amount?.takeIf { it > 0 }?.toString() ?: "") }
            var category by remember { mutableStateOf(editingExpense?.category ?: "Food") }
            var paidBy by remember { mutableStateOf(editingExpense?.paidBy ?: members.firstOrNull()?.uid ?: "") }
            var isSaving by remember { mutableStateOf(false) }

            // 0: Equal, 1: Exact, 2: Percentage, 3: Shares, 4: Itemized
            var selectedSplitIndex by remember {
                mutableStateOf(
                    when (editingExpense?.splitType) {
                        SplitType.EXACT -> 1
                        SplitType.PERCENTAGE -> 2
                        SplitType.SHARES -> 3
                        SplitType.ITEMIZED -> 4
                        else -> 0
                    }
                )
            }
            val splitTypes = listOf("Equally", "Exact", "Percent", "Shares", "Itemized")

            // Maps for custom split inputs (prefilled when editing).
            val exactAmounts = remember {
                mutableStateMapOf<String, String>().apply {
                    if (editingExpense?.splitType == SplitType.EXACT) {
                        editingExpense.splits.forEach { put(it.uid, it.amount.toString()) }
                    }
                }
            }
            val percentages = remember {
                mutableStateMapOf<String, String>().apply {
                    if (editingExpense?.splitType == SplitType.PERCENTAGE) {
                        editingExpense.splits.forEach { s -> s.percentage?.let { put(s.uid, it.toString()) } }
                    }
                }
            }
            val shares = remember {
                mutableStateMapOf<String, Int>().apply {
                    if (editingExpense?.splitType == SplitType.SHARES) {
                        editingExpense.splits.forEach { s -> s.shares?.let { put(s.uid, it) } }
                    }
                }
            }

            // Itemized list state
            var itemizedName by remember { mutableStateOf("") }
            var itemizedPrice by remember { mutableStateOf("") }
            val itemizedList = remember { mutableStateListOf<ExpenseItem>().apply { editingExpense?.items?.let { addAll(it) } } }
            val selectedUidsForItem = remember { mutableStateListOf<String>() }

            // Which members this expense is split among (default: everyone; editing: the split's members).
            val selectedParticipants = remember {
                mutableStateListOf<String>().apply {
                    addAll(editingExpense?.splits?.map { it.uid } ?: members.map { it.uid })
                }
            }
            val participantMembers = members.filter { selectedParticipants.contains(it.uid) }

            // Metadata: notes, date, and optional multiple payers.
            var notes by remember { mutableStateOf(editingExpense?.notes ?: "") }
            var isNotesEditing by remember { mutableStateOf(false) }
            val notesFocusRequester = remember { FocusRequester() }
            var selectedDateMillis by remember { mutableStateOf(editingExpense?.date?.toDate()?.time ?: System.currentTimeMillis()) }
            var showDatePicker by remember { mutableStateOf(false) }
            var showInvitePersonDialog by remember { mutableStateOf(false) }
            var multiplePayers by remember { mutableStateOf(editingExpense?.multiPayer != null) }
            val payerAmounts = remember {
                mutableStateMapOf<String, String>().apply {
                    editingExpense?.multiPayer?.forEach { (uid, amt) -> put(uid, amt.toString()) }
                }
            }
            var recurrenceFreq by remember { mutableStateOf(editingExpense?.recurrence?.frequency ?: RecurrenceFrequency.NONE) }

            // Receipt attachment: a picked local file pending upload, and/or an already-uploaded URL
            // (kept separate so "remove" can clear an existing receipt without re-uploading anything).
            var pickedReceiptUri by remember { mutableStateOf<android.net.Uri?>(null) }
            var pickedReceiptName by remember { mutableStateOf<String?>(null) }
            var existingReceiptUrl by remember { mutableStateOf(editingExpense?.receiptImageUrl) }
            val receiptAttachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    try {
                        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (e: Exception) { /* some providers don't support persistable grants; upload still works this session */ }
                    pickedReceiptUri = uri
                    pickedReceiptName = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                        val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (c.moveToFirst() && nameIndex >= 0) c.getString(nameIndex) else null
                    }
                    existingReceiptUrl = null
                }
            }

            // Receipt scanning (Gemini OCR) — prefills itemized list.
            val scope = rememberCoroutineScope()
            var isScanning by remember { mutableStateOf(false) }
            val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    isScanning = true
                    scope.launch {
                        val bytes = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }
                        val items = if (bytes != null) GeminiReceiptParser.parseReceipt(bytes) else null
                        isScanning = false
                        if (!items.isNullOrEmpty()) {
                            itemizedList.addAll(items)
                            selectedSplitIndex = 4
                            snackbar.showMessage("Added ${items.size} items from receipt")
                        } else {
                            snackbar.showMessage("Couldn't read receipt (check Gemini API key)")
                        }
                    }
                }
            }

            val totalAmount = amountText.toDoubleOrNull() ?: 0.0

            // Multi-payer map (uid -> amount), only when the toggle is on; must sum to total.
            val payerMap: Map<String, Double>? = if (multiplePayers) {
                members.associate { it.uid to (payerAmounts[it.uid]?.toDoubleOrNull() ?: 0.0) }
                    .filterValues { it > 0.0 }
            } else null
            val payersValid = if (multiplePayers) {
                payerMap != null && payerMap.isNotEmpty() &&
                    kotlin.math.abs(payerMap.values.sum() - totalAmount) < 0.01
            } else true

            fun validateAndBuildSplits(): Pair<SplitType, List<ExpenseSplit>>? {
                if (totalAmount <= 0) return null
                val participantUids = members.map { it.uid }.filter { selectedParticipants.contains(it) }
                if (participantUids.isEmpty()) return null

                return try {
                    when (selectedSplitIndex) {
                        0 -> {
                            val splits = SplitCalculator.calculateSplits(
                                totalAmount = totalAmount,
                                splitType = SplitType.EQUAL,
                                participants = participantUids
                            )
                            SplitType.EQUAL to splits
                        }
                        1 -> {
                            val exactMap = participantUids.associateWith { uid -> exactAmounts[uid]?.toDoubleOrNull() ?: 0.0 }
                            val splits = SplitCalculator.calculateSplits(
                                totalAmount = totalAmount,
                                splitType = SplitType.EXACT,
                                participants = participantUids,
                                exactAmounts = exactMap
                            )
                            SplitType.EXACT to splits
                        }
                        2 -> {
                            val pctMap = participantUids.associateWith { uid -> percentages[uid]?.toDoubleOrNull() ?: 0.0 }
                            val splits = SplitCalculator.calculateSplits(
                                totalAmount = totalAmount,
                                splitType = SplitType.PERCENTAGE,
                                participants = participantUids,
                                percentages = pctMap
                            )
                            SplitType.PERCENTAGE to splits
                        }
                        3 -> {
                            val shareMap = participantUids.associateWith { uid -> shares[uid] ?: 1 }
                            val splits = SplitCalculator.calculateSplits(
                                totalAmount = totalAmount,
                                splitType = SplitType.SHARES,
                                participants = participantUids,
                                shares = shareMap
                            )
                            SplitType.SHARES to splits
                        }
                        4 -> {
                            val splits = SplitCalculator.calculateSplits(
                                totalAmount = totalAmount,
                                splitType = SplitType.ITEMIZED,
                                participants = participantUids,
                                items = itemizedList.toList()
                            )
                            SplitType.ITEMIZED to splits
                        }
                        else -> null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            val splitResult = remember(selectedSplitIndex, totalAmount, exactAmounts.toMap(), percentages.toMap(), shares.toMap(), itemizedList.toList(), selectedParticipants.toList()) {
                validateAndBuildSplits()
            }

            val isFormValid = description.isNotBlank() && totalAmount > 0 && splitResult != null && selectedParticipants.isNotEmpty() && payersValid && !isSaving

            // Stepwise reveal: each stage only appears once the previous stage has something usable.
            val showAfterAmount = totalAmount > 0
            val showAfterDescription = description.isNotBlank()
            val showAfterSplitBetween = selectedParticipants.isNotEmpty()

            // Entry Tab State: 0 = AI Entry, 1 = Manual Entry
            var selectedEntryTab by remember { mutableStateOf(0) }
            var aiNaturalInput by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceMD),
                verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Expense" else "Add Expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onNavigateBack, enabled = !isSaving) {
                        Icon(OpenSplitIcons.Close, contentDescription = "Close")
                    }
                }

                // AI Entry vs Manual Entry Segmented Tab Row
                TabRow(
                    selectedTabIndex = selectedEntryTab,
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }
                ) {
                    Tab(
                        selected = selectedEntryTab == 0,
                        onClick = { selectedEntryTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(OpenSplitIcons.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Entry", fontWeight = if (selectedEntryTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedEntryTab == 1,
                        onClick = { selectedEntryTab = 1 },
                        text = {
                            Text("Manual Entry", fontWeight = if (selectedEntryTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    )
                }

                // Group Context Chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Adding to ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = OpenSplitTokens.SpaceSM, vertical = OpenSplitTokens.SpaceXS),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(OpenSplitIcons.Groups, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceXS))
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (selectedEntryTab == 0) {
                    // ---- AI ENTRY VIEW ----
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Describe expense naturally",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = aiNaturalInput,
                                onValueChange = { aiNaturalInput = it },
                                placeholder = {
                                    Text(
                                        "e.g., \"Paid $124.50 for weekend groceries at Safeway and splitting it equally with Sarah and Michael.\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { snackbar.showMessage("Listening for voice input...") }) {
                                    Icon(OpenSplitIcons.Activity, contentDescription = "Voice Input")
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "  OR  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    // Receipt Upload Button / Card
                    Surface(
                        onClick = { receiptPicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = OpenSplitIcons.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Scan or Upload Receipt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AI will extract the amount, merchant, and items.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (aiNaturalInput.isNotBlank()) {
                                // Extract digits for amount if present
                                val amountMatch = Regex("""\$?\s*(\d+(?:\.\d{1,2})?)""").find(aiNaturalInput)
                                if (amountMatch != null) {
                                    amountText = amountMatch.groupValues[1]
                                }
                                val words = aiNaturalInput.split(" ").filter { !it.contains("$") && !it.contains("Paid") }
                                description = words.take(4).joinToString(" ")
                                selectedEntryTab = 1 // Switch to Manual tab with prefilled values
                                snackbar.showMessage("AI extracted expense details! Review & save.")
                            } else {
                                snackbar.showMessage("Enter an expense description or scan a receipt first.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = CircleShape
                    ) {
                        Icon(OpenSplitIcons.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Process with AI", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // ---- MANUAL ENTRY VIEW ----


                // Amount Card
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
                            .padding(OpenSplitTokens.SpaceMD),
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
                            onValueChange = { new -> amountText = new.filter { it.isDigit() || it == '.' } },
                            placeholder = { Text("0.00", style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center) },
                            textStyle = MaterialTheme.typography.displayMedium.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            visualTransformation = remember(group.currency) { CurrencyAmountVisualTransformation(group.currency) },
                            suffix = if (amountText.isNotBlank()) {
                                {
                                    Text(
                                        text = "/- ${com.opensplit.util.CurrencyFormatter.getCurrencySymbol(group.currency)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    }
                }

                // Description: collapsed as a button, expands to an input box on click.
                AnimatedVisibility(
                    visible = showAfterAmount,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (isDescriptionEditing) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            placeholder = { Text("e.g. Dinner, Groceries, Flight") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { isDescriptionEditing = false }),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(descriptionFocusRequester)
                        )
                    } else {
                        OutlinedButton(
                            onClick = { isDescriptionEditing = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = OpenSplitTokens.SpaceMD, vertical = OpenSplitTokens.SpaceMD)
                        ) {
                            Icon(OpenSplitIcons.EditExpense, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                            Text(
                                text = description.ifBlank { "Add description" },
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start,
                                color = if (description.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current
                            )
                        }
                    }
                }
                LaunchedEffect(isDescriptionEditing) {
                    if (isDescriptionEditing) descriptionFocusRequester.requestFocus()
                }

                AnimatedVisibility(
                    visible = showAfterDescription,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)) {
                // Category Selection
                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                    CategoryChipRow(
                        selectedCategory = category,
                        onCategorySelected = { category = it }
                    )
                }

                // Date + Notes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
                ) {
                    Text("Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text(
                            java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                                .format(java.util.Date(selectedDateMillis))
                        )
                    }
                }
                if (isNotesEditing) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { isNotesEditing = false }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(notesFocusRequester)
                    )
                    LaunchedEffect(Unit) { notesFocusRequester.requestFocus() }
                } else {
                    OutlinedButton(
                        onClick = { isNotesEditing = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = OpenSplitTokens.SpaceMD, vertical = OpenSplitTokens.SpaceMD)
                    ) {
                        Icon(OpenSplitIcons.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                        Text(
                            text = notes.ifBlank { "Add notes (optional)" },
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            color = if (notes.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current
                        )
                    }
                }

                // Receipt attachment (image or PDF only)
                Column {
                    Text("Receipt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                    val attachedName = pickedReceiptName ?: existingReceiptUrl?.substringAfterLast('/')
                    if (attachedName != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (attachedName.endsWith(".pdf", ignoreCase = true)) OpenSplitIcons.Pdf else OpenSplitIcons.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                            Text(
                                text = attachedName,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                            IconButton(onClick = {
                                pickedReceiptUri = null
                                pickedReceiptName = null
                                existingReceiptUrl = null
                            }) {
                                Icon(OpenSplitIcons.Close, contentDescription = "Remove receipt")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { receiptAttachmentPicker.launch(arrayOf("image/*", "application/pdf")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(OpenSplitIcons.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                            Text("Attach receipt (image or PDF)")
                        }
                    }
                }

                // Repeat / recurrence
                Column {
                    Text("Repeat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                        RecurrenceFrequency.values().forEach { freq ->
                            val label = when (freq) {
                                RecurrenceFrequency.NONE -> "Never"
                                RecurrenceFrequency.DAILY -> "Daily"
                                RecurrenceFrequency.WEEKLY -> "Weekly"
                                RecurrenceFrequency.MONTHLY -> "Monthly"
                            }
                            FilterChip(
                                selected = recurrenceFreq == freq,
                                onClick = { recurrenceFreq = freq },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // Paid By Selection (single payer, or split across multiple payers)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paid by",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Multiple", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceXS))
                        Switch(checked = multiplePayers, onCheckedChange = { multiplePayers = it })
                    }
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                    if (!multiplePayers) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                            verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS),
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
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)) {
                            members.forEach { user ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(user.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    OutlinedTextField(
                                        value = payerAmounts[user.uid] ?: "",
                                        onValueChange = { payerAmounts[user.uid] = it },
                                        placeholder = { Text("0.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.width(120.dp)
                                    )
                                }
                            }
                            val paidSum = payerMap?.values?.sum() ?: 0.0
                            Text(
                                text = "Payers total: ${com.opensplit.util.CurrencyFormatter.format(paidSum, group.currency)} of ${com.opensplit.util.CurrencyFormatter.format(totalAmount, group.currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (payersValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Split Between (participant selection)
                Column {
                    Text(
                        text = "Split between",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                        verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        members.forEach { user ->
                            val isSelected = selectedParticipants.contains(user.uid)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedParticipants.remove(user.uid)
                                    else selectedParticipants.add(user.uid)
                                },
                                label = { Text(user.displayName) },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                        // Invited but not yet in the group — shown so it's clear who's been invited,
                        // but not selectable since they have no uid to attach a split to until they accept.
                        data.pendingInvites.forEach { invite ->
                            FilterChip(
                                selected = false,
                                onClick = { },
                                enabled = false,
                                label = { Text(invite.email.substringBefore('@')) },
                                leadingIcon = {
                                    Icon(OpenSplitIcons.Pending, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        AssistChip(
                            onClick = { showInvitePersonDialog = true },
                            label = { Text("Invite") },
                            leadingIcon = {
                                Icon(OpenSplitIcons.AddMember, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                    if (data.pendingInvites.isNotEmpty()) {
                        Text(
                            text = "Pending: joins the split automatically once they accept the invite.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selectedParticipants.isEmpty()) {
                        Text(
                            text = "Select at least one person",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                } // end Category..Split-between stage Column
                } // end AnimatedVisibility(showAfterDescription)

                AnimatedVisibility(
                    visible = showAfterDescription && showAfterSplitBetween,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)) {
                // Split Mode Tabs / Chips
                Column {
                    Text(
                        text = "Split Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                    ScrollableTabRow(
                        selectedTabIndex = selectedSplitIndex,
                        edgePadding = 0.dp
                    ) {
                        splitTypes.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedSplitIndex == index,
                                onClick = { selectedSplitIndex = index },
                                text = { Text(title, fontWeight = if (selectedSplitIndex == index) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }

                // Split Input Breakdown depending on selected type
                when (selectedSplitIndex) {
                    0 -> { // Equally
                        if (totalAmount > 0) {
                            val perPerson = totalAmount / maxOf(1, participantMembers.size)
                            Text(
                                text = "Split equally: ${com.opensplit.util.CurrencyFormatter.format(perPerson, group.currency)} / person",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    1 -> { // Exact
                        Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)) {
                            participantMembers.forEach { m ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(m.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    OutlinedTextField(
                                        value = exactAmounts[m.uid] ?: "",
                                        onValueChange = { exactAmounts[m.uid] = it },
                                        placeholder = { Text("0.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.width(120.dp)
                                    )
                                }
                            }
                        }
                    }
                    2 -> { // Percentage
                        Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)) {
                            participantMembers.forEach { m ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(m.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    OutlinedTextField(
                                        value = percentages[m.uid] ?: "",
                                        onValueChange = { percentages[m.uid] = it },
                                        placeholder = { Text("0") },
                                        trailingIcon = { Text("%") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.width(120.dp)
                                    )
                                }
                            }
                        }
                    }
                    3 -> { // Shares
                        Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)) {
                            participantMembers.forEach { m ->
                                val currentShares = shares[m.uid] ?: 1
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(m.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = { if (currentShares > 1) shares[m.uid] = currentShares - 1 },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(36.dp)
                                        ) { Text("-") }
                                        Text(
                                            text = "$currentShares share(s)",
                                            modifier = Modifier.padding(horizontal = OpenSplitTokens.SpaceSM),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        OutlinedButton(
                                            onClick = { shares[m.uid] = currentShares + 1 },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(36.dp)
                                        ) { Text("+") }
                                    }
                                }
                            }
                        }
                    }
                    4 -> { // Itemized
                        Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                            OutlinedButton(
                                onClick = { receiptPicker.launch("image/*") },
                                enabled = !isScanning,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isScanning) {
                                    AppLoadingIndicator(size = 16.dp)
                                    Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                                    Text("Scanning receipt...")
                                } else {
                                    Icon(OpenSplitIcons.Camera, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                                    Text("Scan receipt (AI)")
                                }
                            }
                            Text("Add Items", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
                            ) {
                                OutlinedTextField(
                                    value = itemizedName,
                                    onValueChange = { itemizedName = it },
                                    label = { Text("Item Name") },
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = itemizedPrice,
                                    onValueChange = { itemizedPrice = it },
                                    label = { Text("Price") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.width(100.dp)
                                )
                            }
                            Text("Assign to:", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)) {
                                participantMembers.forEach { m ->
                                    val isSelected = selectedUidsForItem.contains(m.uid)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) selectedUidsForItem.remove(m.uid) else selectedUidsForItem.add(m.uid)
                                        },
                                        label = { Text(m.displayName) }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    val p = itemizedPrice.toDoubleOrNull()
                                    if (itemizedName.isNotBlank() && p != null && p > 0 && selectedUidsForItem.isNotEmpty()) {
                                        itemizedList.add(
                                            ExpenseItem(
                                                id = java.util.UUID.randomUUID().toString(),
                                                name = itemizedName.trim(),
                                                price = p,
                                                assignedUids = selectedUidsForItem.toList()
                                            )
                                        )
                                        itemizedName = ""
                                        itemizedPrice = ""
                                        selectedUidsForItem.clear()
                                    }
                                },
                                enabled = itemizedName.isNotBlank() && (itemizedPrice.toDoubleOrNull() ?: 0.0) > 0 && selectedUidsForItem.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add Item to List")
                            }

                            if (itemizedList.isNotEmpty()) {
                                HorizontalDivider()
                                itemizedList.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${item.name} (${com.opensplit.util.CurrencyFormatter.format(item.price, group.currency)})")
                                        Text("${item.assignedUids.size} people")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))

                // Save Expense Button with Duplicate Prevention
                Button(
                    onClick = {
                        if (isSaving) return@Button
                        val evaluated = splitResult
                        if (description.isNotBlank() && totalAmount > 0 && evaluated != null && payersValid) {
                            isSaving = true
                            val (sType, splitsList) = evaluated
                            val effectivePaidBy = if (multiplePayers) (payerMap?.keys?.firstOrNull() ?: paidBy) else paidBy
                            val builtExpense = (editingExpense ?: Expense(groupId = group.id, createdBy = paidBy)).copy(
                                groupId = group.id,
                                description = description.trim(),
                                amount = totalAmount,
                                paidBy = effectivePaidBy,
                                multiPayer = if (multiplePayers) payerMap else null,
                                category = category,
                                currency = group.currency,
                                splitType = sType,
                                splits = splitsList,
                                items = if (sType == SplitType.ITEMIZED) itemizedList.toList() else null,
                                notes = notes.trim().ifBlank { null },
                                receiptImageUrl = existingReceiptUrl,
                                date = Timestamp(java.util.Date(selectedDateMillis)),
                                recurrence = if (recurrenceFreq != RecurrenceFrequency.NONE) {
                                    val existing = editingExpense?.recurrence
                                    if (existing != null && existing.frequency == recurrenceFreq) existing
                                    else RecurrenceRule(recurrenceFreq, nextRecurrence(selectedDateMillis, recurrenceFreq))
                                } else null
                            )

                            val onDone: () -> Unit = {
                                isSaving = false
                                snackbar.showMessage(if (isEditing) "Expense updated!" else "Expense saved successfully!")
                                onNavigateBack()
                            }
                            if (isEditing) viewModel.updateExpense(builtExpense, pickedReceiptUri, onDone)
                            else viewModel.addExpense(builtExpense, pickedReceiptUri, onDone)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormValid
                ) {
                    if (isSaving) {
                        AppLoadingIndicator(size = 20.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                        Text("Saving...")
                    } else {
                        Text(if (isEditing) "Update Expense" else "Save Expense", fontWeight = FontWeight.Bold)
                    }
                }
                } // end Split-mode/Save stage Column
                } // end AnimatedVisibility(showAfterSplitBetween)
                } // end else (Manual Entry tab)

                if (showDatePicker) {
                    val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dateState.selectedDateMillis?.let { selectedDateMillis = it }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = dateState)
                    }
                }

                if (showInvitePersonDialog) {
                    com.opensplit.ui.components.InviteMemberDialog(
                        title = "Invite to this group",
                        description = "They'll be added to the split once they accept. Until then they show as pending.",
                        onDismiss = { showInvitePersonDialog = false },
                        onSubmitEmail = { email ->
                            viewModel.addMemberByEmail(email)
                            snackbar.showMessage("Invite sent — they'll appear as pending")
                        }
                    )
                }
            }
        }
    }
}

/** Next occurrence timestamp for a recurring expense, one period after [fromMillis]. */
private fun nextRecurrence(fromMillis: Long, frequency: RecurrenceFrequency): com.google.firebase.Timestamp {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = fromMillis }
    when (frequency) {
        RecurrenceFrequency.DAILY -> cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        RecurrenceFrequency.WEEKLY -> cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
        RecurrenceFrequency.MONTHLY -> cal.add(java.util.Calendar.MONTH, 1)
        RecurrenceFrequency.NONE -> Unit
    }
    return com.google.firebase.Timestamp(cal.time)
}

/** Groups the integer digits of a raw amount string using the currency's thousands grouping (e.g. lakh/crore for INR). */
private fun groupIntegerDigits(digits: String, currencyCode: String): String {
    if (digits.isEmpty()) return ""
    val value = digits.toLongOrNull() ?: return digits
    val grouping = if (currencyCode.equals("INR", ignoreCase = true)) "#,##,##0" else "#,##0"
    val formatter = java.text.DecimalFormat(grouping, java.text.DecimalFormatSymbols(java.util.Locale.US))
    return formatter.format(value)
}

/**
 * Live-formats a raw amount string (digits + optional single dot) with thousands separators
 * on the integer part while leaving the decimal part untouched, mapping cursor offsets correctly.
 */
private class CurrencyAmountVisualTransformation(private val currencyCode: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val dotIndex = raw.indexOf('.')
        val intPart = if (dotIndex >= 0) raw.substring(0, dotIndex) else raw
        val fracPart = if (dotIndex >= 0) raw.substring(dotIndex) else ""

        val grouped = groupIntegerDigits(intPart, currencyCode)
        val transformed = grouped + fracPart

        // digitEndPositions[k] = index in `grouped` right after the k-th digit of intPart.
        val digitEndPositions = IntArray(intPart.length + 1)
        var digitsSeen = 0
        for (i in grouped.indices) {
            if (grouped[i].isDigit()) {
                digitsSeen++
                digitEndPositions[digitsSeen] = i + 1
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return if (offset <= intPart.length) {
                    digitEndPositions[offset]
                } else {
                    grouped.length + (offset - intPart.length)
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return if (offset <= grouped.length) {
                    var count = 0
                    for (i in 0 until offset) if (i < grouped.length && grouped[i].isDigit()) count++
                    count
                } else {
                    intPart.length + (offset - grouped.length)
                }
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}
