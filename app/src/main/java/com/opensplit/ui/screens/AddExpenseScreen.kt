@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.theme.Symbol
import com.opensplit.ui.components.SymbolIcon
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
import com.opensplit.data.ai.AiHeuristics
import com.opensplit.data.ai.AiFeature
import com.opensplit.ui.components.AiBadge
import com.opensplit.ui.components.LocalAiService
import androidx.compose.foundation.horizontalScroll
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
import com.opensplit.ui.viewmodel.ScreenState
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.GroupDetailViewModel
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    viewModel: GroupDetailViewModel,
    onNavigateBack: () -> Unit,
    editingExpense: Expense? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    val busy = com.opensplit.ui.components.LocalBusy.current
    // Add Expense is the one full-height sheet in the app; a half-open form reads as cut off.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onNavigateBack,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        // Inline state handling (StateLayout fills the whole sheet, which forced it full-height).
        when (val st = uiState) {
            is ScreenState.Loading -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.ContainedLoadingIndicator() }
            is ScreenState.Empty -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { Text(st.message.ifBlank { "Nothing here yet" }) }
            is ScreenState.Error -> Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(st.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                TextButton(shapes = ButtonDefaults.shapes(), onClick = st.retry) { Text("Retry") }
            }
            is ScreenState.Success -> { val data = st.data
            val group = data.group
            val members = data.members

            val isEditing = editingExpense != null

            var description by remember { mutableStateOf(editingExpense?.description ?: "") }
            var isDescriptionEditing by remember { mutableStateOf(false) }
            val descriptionFocusRequester = remember { FocusRequester() }
            var amountText by remember { mutableStateOf(editingExpense?.amount?.takeIf { it > 0 }?.let { if (it % 1.0 == 0.0) it.toLong().toString() else "%.2f".format(java.util.Locale.US, it) } ?: "") }
            var category by remember { mutableStateOf(editingExpense?.category ?: "Food") }
            // True once the user picks a category by hand; suggestions then stop overriding it.
            var categoryTouched by remember { mutableStateOf(isEditing) }
            var categoryFromAi by remember { mutableStateOf(false) }
            val aiService = LocalAiService.current
            val categorizeAvailable by aiService.availability(AiFeature.CATEGORIZE).collectAsState(initial = false)
            val aiUnlocked by aiService.unlocked.collectAsState(initial = true)
            LaunchedEffect(description, categorizeAvailable) {
                if (categoryTouched || description.isBlank()) return@LaunchedEffect
                kotlinx.coroutines.delay(500)
                val local = AiHeuristics.categoryFor(description)
                if (local != "Other") { category = local; categoryFromAi = false }
                if (categorizeAvailable && description.length >= 3) {
                    kotlinx.coroutines.delay(400)
                    aiService.suggestCategory(description)?.let { category = it; categoryFromAi = true }
                }
            }
            var paidBy by remember { mutableStateOf(editingExpense?.paidBy ?: members.firstOrNull()?.uid ?: "") }
            var isSaving by remember { mutableStateOf(false) }
            var saveError by remember { mutableStateOf<String?>(null) }

            // 0: Equal, 1: Exact, 2: Percentage, 3: Shares, 4: Itemized
            var selectedSplitIndex by remember {
                mutableStateOf(
                    when (editingExpense?.splitType ?: group.defaultSplitType) {
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

            // Entry Tab State: 0 = AI Entry, 1 = Manual Entry
            var selectedEntryTab by remember { mutableStateOf(if (isEditing) 1 else 0) }

            // Receipt scanning via the user's AI provider; prefills merchant, total and items.
            val scope = rememberCoroutineScope()
            var isScanning by remember { mutableStateOf(false) }
            var scanStatus by remember { mutableStateOf<String?>(null) }
            // One scan path for every source: camera shot, gallery photo or a picked file. The image
            // is also attached to the expense so the receipt is kept, not just read.
            val scanReceipt: (android.net.Uri?) -> Unit = { uri ->
                android.util.Log.i("ReceiptScan", "scan uri=$uri")
                if (uri != null) {
                    pickedReceiptUri = uri
                    pickedReceiptName = "Receipt"
                    existingReceiptUrl = null
                    isScanning = true; scanStatus = null
                    scope.launch {
                        val bytes = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }
                        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val receipt = if (bytes != null) aiService.parseReceipt(bytes, mime) else null
                        android.util.Log.i("ReceiptScan", "bytes=${bytes?.size} mime=$mime parsed=${receipt != null} aiAvailable=${aiService.isAvailable(AiFeature.RECEIPT_SCAN)}")
                        isScanning = false
                        if (receipt != null) {
                            if (description.isBlank()) receipt.merchant?.let { description = it }
                            if (amountText.isBlank()) receipt.total?.let { amountText = it.toString() }
                            if (receipt.items.isNotEmpty()) {
                                itemizedList.addAll(receipt.items)
                                selectedSplitIndex = 4
                            }
                            selectedEntryTab = 1
                            scanStatus = if (receipt.lowConfidence) "Low confidence: the photo was hard to read. Check every field."
                                else "Read ${receipt.items.size} items from receipt. Review and save."
                            snackbar.showMessage(scanStatus!!)
                        } else if (!aiService.isAvailable(AiFeature.RECEIPT_SCAN)) {
                            scanStatus = "Receipt attached. Add an AI key in Settings to read it automatically."
                        } else {
                            scanStatus = "Couldn't read the receipt" + (aiService.lastError?.let { ": $it" } ?: ". Try a clearer photo.")
                        }
                    }
                }
            }
            val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { scanReceipt(it) }
            val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { scanReceipt(it) }
            // Saveable: the camera app can push this activity out of memory; the URI must survive.
            var cameraReceiptUri by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<android.net.Uri?>(null) }
            val cameraCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
                android.util.Log.i("ReceiptScan", "camera result ok=$ok uri=$cameraReceiptUri")
                if (ok) scanReceipt(cameraReceiptUri) else snackbar.showMessage("No photo taken.")
            }
            val openCamera = {
                val file = java.io.File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                cameraReceiptUri = uri
                cameraCapture.launch(uri)
            }
            val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) openCamera() else snackbar.showMessage("Camera permission is needed to scan a receipt.")
            }
            var showReceiptSource by remember { mutableStateOf(false) }
            if (showReceiptSource) {
                androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showReceiptSource = false }) {
                    Text("Add receipt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                    listOf(
                        Triple(OpenSplitIcons.CameraAlt, "Take a photo", {
                            showReceiptSource = false
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) openCamera()
                            else cameraPermission.launch(android.Manifest.permission.CAMERA)
                        }),
                        Triple(OpenSplitIcons.PhotoLibrary, "Choose from gallery", {
                            showReceiptSource = false
                            galleryPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }),
                        Triple(OpenSplitIcons.AttachFile, "Browse files", { showReceiptSource = false; filePicker.launch("image/*") })
                    ).forEach { (icon, label, action) ->
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(label) },
                            leadingContent = { SymbolIcon(icon, contentDescription = null) },
                            modifier = Modifier.clickable(onClick = action)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
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

            var aiNaturalInput by remember { mutableStateOf("") }
            var isParsingText by remember { mutableStateOf(false) }

            com.opensplit.ui.components.FormLock(locked = isSaving) {
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
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = onNavigateBack, enabled = !isSaving) {
                        SymbolIcon(OpenSplitIcons.Close, contentDescription = "Close")
                    }
                }

                // AI Entry vs Manual Entry Segmented Tab Row
                androidx.compose.material3.PrimaryTabRow(
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
                                SymbolIcon(OpenSplitIcons.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            SymbolIcon(OpenSplitIcons.Groups, contentDescription = null, modifier = Modifier.size(14.dp))
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

                if (selectedEntryTab == 0) { com.opensplit.ui.components.AiLocked(unlocked = aiUnlocked) { Column {
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
                                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { snackbar.showMessage("Listening for voice input...") }) {
                                    SymbolIcon(OpenSplitIcons.Activity, contentDescription = "Voice Input")
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
                        onClick = { showReceiptSource = true },
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
                                    SymbolIcon(
                                        symbol = OpenSplitIcons.ReceiptLong,
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
                            if (isScanning) {
                                Spacer(modifier = Modifier.height(12.dp))
                                com.opensplit.ui.components.InlineBusy("Reading receipt…")
                            } else scanStatus?.let {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (it.startsWith("Couldn't")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (aiNaturalInput.isNotBlank() && !isParsingText) {
                                isParsingText = true
                                scope.launch {
                                    val names = members.map { it.displayName.ifBlank { it.email } }
                                    val parsed = aiService.parseExpenseText(aiNaturalInput, names, group.currency)
                                        ?: AiHeuristics.parseExpenseText(aiNaturalInput, names)
                                    isParsingText = false
                                    if (parsed == null) {
                                        snackbar.showMessage("Couldn't understand that. Try: 'Rahul paid 1200 for dinner'.")
                                        return@launch
                                    }
                                    description = parsed.description
                                    parsed.amount?.let { amountText = it.toString().removeSuffix(".0") }
                                    parsed.category?.let { category = it; categoryTouched = true; categoryFromAi = parsed.source == com.opensplit.data.ai.ParsedExpense.Source.AI }
                                    parsed.paidByName?.let { n -> members.firstOrNull { it.displayName.ifBlank { it.email } == n }?.let { paidBy = it.uid } }
                                    if (parsed.participantNames.isNotEmpty()) {
                                        val uids = members.filter { it.displayName.ifBlank { it.email } in parsed.participantNames }.map { it.uid }
                                        if (uids.isNotEmpty()) { selectedParticipants.clear(); selectedParticipants.addAll((uids + paidBy).distinct()) }
                                    }
                                    parsed.splitType?.let { st ->
                                        selectedSplitIndex = when (st) { SplitType.EXACT -> 1; SplitType.PERCENTAGE -> 2; SplitType.SHARES -> 3; SplitType.ITEMIZED -> 4; else -> 0 }
                                    }
                                    selectedEntryTab = 1
                                    snackbar.showMessage(
                                        if (parsed.source == com.opensplit.data.ai.ParsedExpense.Source.AI) "AI filled in the expense. Review and save."
                                        else "Filled in what could be read offline. Add an AI key in Settings for smarter parsing."
                                    )
                                }
                            } else {
                                snackbar.showMessage("Enter an expense description or scan a receipt first.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape
                    ) {
                        if (isParsingText) {
                            AppLoadingIndicator(size = 20.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            SymbolIcon(OpenSplitIcons.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Process with AI", fontWeight = FontWeight.Bold)
                    }
                } } } else {
                    // ---- MANUAL ENTRY VIEW ----


                // Amount: symbol + big number, auto-focused, digits with at most one dot and two decimals.
                val amountFocus = remember { FocusRequester() }
                LaunchedEffect(selectedEntryTab) { if (selectedEntryTab == 1 && amountText.isBlank()) runCatching { amountFocus.requestFocus() } }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = OpenSplitTokens.SpaceMD, vertical = OpenSplitTokens.SpaceLG),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Amount · ${group.currency}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                        val moneyStyle = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = com.opensplit.ui.theme.MoneyFontFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = com.opensplit.util.CurrencyFormatter.getCurrencySymbol(group.currency),
                                style = MaterialTheme.typography.displaySmall.copy(fontFamily = com.opensplit.ui.theme.MoneyFontFamily),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = amountText,
                                onValueChange = { new ->
                                    val cleaned = buildString {
                                        var dot = false
                                        for (c in new) {
                                            if (c.isDigit()) { if (dot && length - indexOf('.') > 2) continue; append(c) }
                                            else if (c == '.' && !dot && length > 0) { dot = true; append(c) }
                                        }
                                    }
                                    amountText = cleaned.take(12)
                                },
                                textStyle = moneyStyle,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                visualTransformation = remember(group.currency) { CurrencyAmountVisualTransformation(group.currency) },
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.focusRequester(amountFocus).width(IntrinsicSize.Min).widthIn(min = 72.dp),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (amountText.isEmpty()) Text("0", style = moneyStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                                        inner()
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                        Text(
                            text = if (totalAmount > 0) com.opensplit.util.CurrencyFormatter.format(totalAmount, group.currency) else "Type the total; the rest appears as you go",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        OutlinedButton(shapes = ButtonDefaults.shapes(), 
                            onClick = { isDescriptionEditing = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = OpenSplitTokens.SpaceMD, vertical = OpenSplitTokens.SpaceMD)
                        ) {
                            SymbolIcon(OpenSplitIcons.EditExpense, contentDescription = null, modifier = Modifier.size(18.dp))
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (categoryFromAi && !categoryTouched) AiBadge(label = "AI suggested")
                    }
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                    CategoryChipRow(
                        selectedCategory = category,
                        onCategorySelected = { category = it; categoryTouched = true; categoryFromAi = false }
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
                    OutlinedButton(shapes = ButtonDefaults.shapes(), onClick = { showDatePicker = true }) {
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
                    OutlinedButton(shapes = ButtonDefaults.shapes(), 
                        onClick = { isNotesEditing = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = OpenSplitTokens.SpaceMD, vertical = OpenSplitTokens.SpaceMD)
                    ) {
                        SymbolIcon(OpenSplitIcons.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            SymbolIcon(
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
                            IconButton(shapes = IconButtonDefaults.shapes(), onClick = {
                                pickedReceiptUri = null
                                pickedReceiptName = null
                                existingReceiptUrl = null
                            }) {
                                SymbolIcon(OpenSplitIcons.Close, contentDescription = "Remove receipt")
                            }
                        }
                    } else {
                        OutlinedButton(shapes = ButtonDefaults.shapes(), 
                            onClick = { receiptAttachmentPicker.launch(arrayOf("image/*", "application/pdf")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SymbolIcon(OpenSplitIcons.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            val label = com.opensplit.domain.logic.RecurrenceScheduler.label(freq)
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
                                            SymbolIcon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
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
                                        SymbolIcon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
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
                                    SymbolIcon(OpenSplitIcons.Pending, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        AssistChip(
                            onClick = { showInvitePersonDialog = true },
                            label = { Text("Invite") },
                            leadingIcon = {
                                SymbolIcon(OpenSplitIcons.AddMember, contentDescription = null, modifier = Modifier.size(16.dp))
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
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        splitTypes.forEachIndexed { index, title ->
                            androidx.compose.material3.ToggleButton(
                                checked = selectedSplitIndex == index,
                                onCheckedChange = { selectedSplitIndex = index }
                            ) { Text(title) }
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
                                        OutlinedButton(shapes = ButtonDefaults.shapes(), 
                                            onClick = { if (currentShares > 1) shares[m.uid] = currentShares - 1 },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(36.dp)
                                        ) { Text("-") }
                                        Text(
                                            text = "$currentShares share(s)",
                                            modifier = Modifier.padding(horizontal = OpenSplitTokens.SpaceSM),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        OutlinedButton(shapes = ButtonDefaults.shapes(), 
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
                            OutlinedButton(shapes = ButtonDefaults.shapes(), 
                                onClick = { showReceiptSource = true },
                                enabled = !isScanning && aiUnlocked,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isScanning) {
                                    AppLoadingIndicator(size = 16.dp)
                                    Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                                    Text("Scanning receipt...")
                                } else {
                                    SymbolIcon(OpenSplitIcons.Camera, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            Button(shapes = ButtonDefaults.shapes(), 
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
                Button(shapes = ButtonDefaults.shapes(), 
                    onClick = {
                        if (isSaving) return@Button
                        val evaluated = splitResult
                        if (description.isNotBlank() && totalAmount > 0 && evaluated != null && payersValid) {
                            isSaving = true
                            busy.show(if (isEditing) "Updating expense…" else "Saving expense…")
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
                                busy.hide()
                                onNavigateBack()
                                // After the sheet is gone, so the host underneath is visible.
                                snackbar.showMessage(if (isEditing) "Expense updated" else "Expense saved")
                            }
                            val onError: (String) -> Unit = { msg ->
                                isSaving = false
                                busy.hide()
                                saveError = msg
                            }
                            if (isEditing) viewModel.updateExpense(builtExpense, pickedReceiptUri, onDone, onError)
                            else viewModel.addExpense(builtExpense, pickedReceiptUri, onDone, onError)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    // Stays in the primary colour while saving so the spinner reads clearly; the click guard above prevents double saves.
                    enabled = isFormValid || isSaving
                ) {
                    if (isSaving) {
                        AppLoadingIndicator(size = 22.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                        Text(if (isEditing) "Updating…" else "Saving…", fontWeight = FontWeight.Bold)
                    } else {
                        Text(if (isEditing) "Update Expense" else "Save Expense", fontWeight = FontWeight.Bold)
                    }
                }
                saveError?.let {
                    Text(
                        text = "Couldn't save: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = OpenSplitTokens.SpaceSM)
                    )
                }
                } // end Split-mode/Save stage Column
                } // end AnimatedVisibility(showAfterSplitBetween)
                } // end else (Manual Entry tab)

                if (showDatePicker) {
                    val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(shapes = ButtonDefaults.shapes(), onClick = {
                                dateState.selectedDateMillis?.let { selectedDateMillis = it }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(shapes = ButtonDefaults.shapes(), onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = dateState)
                    }
                }

                if (showInvitePersonDialog) {
                    val links by viewModel.inviteLinks.collectAsState()
                    com.opensplit.ui.components.GroupInviteSheet(
                        groupId = group.id,
                        groupName = group.name,
                        inviteLink = links.firstOrNull(),
                        onDismiss = { showInvitePersonDialog = false },
                        onSubmitEmail = { email ->
                            viewModel.addMemberByEmail(email)
                            snackbar.showMessage("Invite sent — they'll appear as pending")
                        },
                        onGenerateLink = { expires, maxUses ->
                            viewModel.createInviteLink(expires, maxUses) { ok ->
                                snackbar.showMessage(if (ok) "Invite link ready" else "Couldn't create link")
                            }
                        }
                    )
                }
            }
        } } }
    }
}

/** Next occurrence timestamp for a recurring expense, one period after [fromMillis]. */
private fun nextRecurrence(fromMillis: Long, frequency: RecurrenceFrequency): com.google.firebase.Timestamp =
    com.opensplit.domain.logic.RecurrenceScheduler.next(com.google.firebase.Timestamp(fromMillis / 1000, 0), frequency)

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
