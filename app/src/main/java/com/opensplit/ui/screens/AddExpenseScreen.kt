@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.components.FilterChip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensplit.ui.theme.Symbol
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.LocalSnackbarController

import com.opensplit.ui.components.AppLoadingIndicator

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
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
import com.opensplit.ui.components.responsiveDialogWidth
import com.opensplit.ui.components.responsiveFormWidth
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
    editingExpense: Expense? = null,
    prefillAmount: Double? = null,
    prefillDescription: String? = null,
    onSaved: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    val busy = com.opensplit.ui.components.LocalBusy.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
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
                TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = st.retry) { Text("Retry") }
            }
            is ScreenState.Success -> { val data = st.data
            val group = data.group
            val members = data.members

            val isEditing = editingExpense != null

            var description by remember { mutableStateOf(editingExpense?.description ?: prefillDescription ?: "") }
            var isDescriptionEditing by remember { mutableStateOf(false) }
            val descriptionFocusRequester = remember { FocusRequester() }
            var amountText by remember { mutableStateOf((editingExpense?.amount ?: prefillAmount)?.takeIf { it > 0 }?.let { if (it % 1.0 == 0.0) it.toLong().toString() else "%.2f".format(java.util.Locale.US, it) } ?: "") }
            var category by remember { mutableStateOf(editingExpense?.category ?: "Food") }
            // True once the user picks a category by hand; suggestions then stop overriding it.
            var categoryTouched by remember { mutableStateOf(isEditing) }
            var categoryFromAi by remember { mutableStateOf(false) }
            var aiSource by remember { mutableStateOf("Cloud") }
            val aiService = LocalAiService.current
            val categorizeAvailable by aiService.availability(AiFeature.CATEGORIZE).collectAsStateWithLifecycle(initialValue = false)
            val aiUnlocked by aiService.unlocked.collectAsStateWithLifecycle(initialValue = true)
            LaunchedEffect(description, categorizeAvailable) {
                if (categoryTouched || description.isBlank()) return@LaunchedEffect
                kotlinx.coroutines.delay(500)
                val local = AiHeuristics.categoryFor(description)
                if (local != "Other") { category = local; categoryFromAi = false }
                if (categorizeAvailable && description.length >= 3) {
                    kotlinx.coroutines.delay(400)
                    aiService.suggestCategory(description)?.let { category = it; categoryFromAi = true; aiSource = aiService.lastSourceLabel() }
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
            var itemizedTaxPercent by remember { mutableStateOf(0.0) }
            var itemizedTipPercent by remember { mutableStateOf(0.0) }
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
            // No AI key: open on Manual Entry rather than a locked tab. Re-runs only if the lock changes.
            androidx.compose.runtime.LaunchedEffect(aiUnlocked) { if (!aiUnlocked) selectedEntryTab = 1 }

            var aiNaturalInput by remember { mutableStateOf("") }
            var isParsingText by remember { mutableStateOf(false) }

            // Receipt scanning via the user's AI provider; prefills merchant, total and items.
            val scope = rememberCoroutineScope()
            var isScanning by remember { mutableStateOf(false) }
            var scanStatus by remember { mutableStateOf<String?>(null) }
            // One scan path for every source: camera shot, gallery photo or a picked file. The image
            // is also attached to the expense so the receipt is kept, not just read.
            val scanReceipt: (android.net.Uri?, String?) -> Unit = { uri, splitInstructions ->
                android.util.Log.i("ReceiptScan", "scan uri=$uri instructions=$splitInstructions")
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
                        val memberNames = members.map { it.displayName.ifBlank { it.email } }
                        val receipt = if (bytes != null) aiService.parseReceipt(bytes, mime, splitInstructions, memberNames) else null
                        android.util.Log.i("ReceiptScan", "bytes=${bytes?.size} mime=$mime parsed=${receipt != null} aiAvailable=${aiService.isAvailable(AiFeature.RECEIPT_SCAN)}")
                        isScanning = false
                        if (receipt != null) {
                            if (description.isBlank()) receipt.merchant?.let { description = it }
                            if (amountText.isBlank()) receipt.total?.let { amountText = it.toString().removeSuffix(".0") }
                            if (receipt.items.isNotEmpty()) {
                                val mappedItems = receipt.items.map { item ->
                                    val matchedUids = members.filter { m ->
                                        val mName = m.displayName.ifBlank { m.email }
                                        item.assignedUids.any { assigned ->
                                            mName.contains(assigned, ignoreCase = true) ||
                                            assigned.contains(mName, ignoreCase = true)
                                        }
                                    }.map { it.uid }
                                    item.copy(assignedUids = matchedUids)
                                }
                                itemizedList.clear()
                                itemizedList.addAll(mappedItems)
                                selectedSplitIndex = 4
                            }
                            receipt.paidByName?.let { n ->
                                members.firstOrNull {
                                    val mName = it.displayName.ifBlank { it.email }
                                    mName.contains(n, ignoreCase = true) || n.contains(mName, ignoreCase = true)
                                }?.let { paidBy = it.uid }
                            }
                            selectedEntryTab = 1
                            scanStatus = if (receipt.lowConfidence) "Low confidence: photo was hard to read. Review items."
                                else "AI read receipt & mapped your split instructions!"
                            snackbar.showMessage(scanStatus!!)
                        } else if (!aiService.isAvailable(AiFeature.RECEIPT_SCAN)) {
                            scanStatus = "Receipt attached. Add an AI key in Settings to read it automatically."
                        } else {
                            scanStatus = "Couldn't read the receipt" + (aiService.lastError?.let { ": $it" } ?: ". Try a clearer photo.")
                        }
                    }
                }
            }
            val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    pickedReceiptUri = uri
                    pickedReceiptName = "Receipt"
                    existingReceiptUrl = null
                    scanReceipt(uri, aiNaturalInput.ifBlank { null })
                }
            }
            val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    pickedReceiptUri = uri
                    pickedReceiptName = "Receipt"
                    existingReceiptUrl = null
                    scanReceipt(uri, aiNaturalInput.ifBlank { null })
                }
            }
            // Saveable: the camera app can push this activity out of memory; the URI must survive.
            var cameraReceiptUri by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<android.net.Uri?>(null) }
            val cameraCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
                android.util.Log.i("ReceiptScan", "camera result ok=$ok uri=$cameraReceiptUri")
                if (ok && cameraReceiptUri != null) {
                    pickedReceiptUri = cameraReceiptUri
                    pickedReceiptName = "Receipt"
                    existingReceiptUrl = null
                    scanReceipt(cameraReceiptUri, aiNaturalInput.ifBlank { null })
                } else if (!ok) {
                    snackbar.showMessage("No photo taken.")
                }
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

            var isListening by remember { mutableStateOf(false) }
            var rmsLevel by remember { mutableFloatStateOf(0f) }
            var partialSpokenText by remember { mutableStateOf("") }

            val speechRecognizer = remember(context) {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    SpeechRecognizer.createSpeechRecognizer(context)
                } else null
            }

            DisposableEffect(speechRecognizer) {
                onDispose {
                    try {
                        speechRecognizer?.destroy()
                    } catch (_: Exception) {}
                }
            }

            val recognitionListener = remember {
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        rmsLevel = rmsdB
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        isListening = false
                        rmsLevel = 0f
                        partialSpokenText = ""
                        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            android.util.Log.w("SpeechRecognizer", "Error code: $error")
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull()
                        if (!spoken.isNullOrBlank()) {
                            aiNaturalInput = if (aiNaturalInput.isBlank()) spoken else "$aiNaturalInput $spoken"
                        }
                        isListening = false
                        rmsLevel = 0f
                        partialSpokenText = ""
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            partialSpokenText = partial
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }
            }

            val startListening: () -> Unit = {
                if (speechRecognizer != null) {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    }
                    try {
                        speechRecognizer.setRecognitionListener(recognitionListener)
                        speechRecognizer.startListening(intent)
                        isListening = true
                        partialSpokenText = ""
                        rmsLevel = 0f
                    } catch (e: Exception) {
                        isListening = false
                        snackbar.showMessage("Speech recognition error: ${e.message}")
                    }
                } else {
                    snackbar.showMessage("Speech recognition is not available on this device.")
                }
            }

            val stopListening: () -> Unit = {
                try {
                    speechRecognizer?.stopListening()
                } catch (_: Exception) {}
                isListening = false
                rmsLevel = 0f
                partialSpokenText = ""
            }

            val micPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    startListening()
                } else {
                    snackbar.showMessage("Microphone permission is needed to speak split instructions.")
                }
            }

            val toggleVoiceInput: () -> Unit = {
                if (isListening) {
                    stopListening()
                } else {
                    val hasMicPermission = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasMicPermission) {
                        startListening()
                    } else {
                        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
            var showReceiptSource by remember { mutableStateOf(false) }
            if (showReceiptSource) {
                androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showReceiptSource = false }) {
                    Column(modifier = Modifier.responsiveDialogWidth(maxWidth = 520.dp)) {
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

            com.opensplit.ui.components.FormLock(locked = isSaving) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 680.dp)
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
                    IconButton(shapes = com.opensplit.ui.theme.appIconButtonShapes(), onClick = onNavigateBack, enabled = !isSaving) {
                        SymbolIcon(OpenSplitIcons.Close, contentDescription = "Close")
                    }
                }

                // AI Entry vs Manual Entry Segmented Tab Row
                com.opensplit.ui.components.PillTabs(
                    selected = selectedEntryTab,
                    labels = listOf("AI Entry", "Manual Entry"),
                    icons = listOf(OpenSplitIcons.AutoAwesome, OpenSplitIcons.EditExpense),
                    onSelect = { selectedEntryTab = it }
                )

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
                    // ---- AI ENTRY & SMART SPLIT COMPOSER ----
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SymbolIcon(
                                        symbol = OpenSplitIcons.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Explain Split (Type or Speak)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                VoiceVisualizerIconButton(
                                    isListening = isListening,
                                    rmsLevel = rmsLevel,
                                    onClick = { toggleVoiceInput() }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Explain who paid and how to split between users or items, or tap the mic to speak.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = aiNaturalInput,
                                onValueChange = { aiNaturalInput = it },
                                placeholder = {
                                    Text(
                                        text = if (isListening && partialSpokenText.isNotBlank()) "\"$partialSpokenText\""
                                        else if (isListening) "Listening… Speak split details"
                                        else "e.g. \"Rahul paid 1400. Sarah and I split the wine, Raja doesn't drink, split food equally.\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                },
                                supportingText = if (isListening) {
                                    {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            val dotTransition = rememberInfiniteTransition(label = "live_dot")
                                            val dotAlpha by dotTransition.animateFloat(
                                                initialValue = 0.35f,
                                                targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(400, easing = FastOutSlowInEasing),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "dot_alpha"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = dotAlpha))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (partialSpokenText.isNotBlank()) "\"$partialSpokenText\"" else "Listening… Speak now · Tap voice icon to stop",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 96.dp),
                                shape = MaterialTheme.shapes.small,
                                trailingIcon = if (aiNaturalInput.isNotBlank()) {
                                    {
                                        IconButton(onClick = { aiNaturalInput = "" }) {
                                            SymbolIcon(OpenSplitIcons.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else null
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick member mention chips
                            Text(
                                text = "Quick mentions:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                members.forEach { member ->
                                    val name = member.displayName.ifBlank { member.email.substringBefore('@') }
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.clickable {
                                            aiNaturalInput = if (aiNaturalInput.isBlank()) name else "$aiNaturalInput $name"
                                        }
                                    ) {
                                        Text(
                                            text = "+ $name",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable {
                                        aiNaturalInput = if (aiNaturalInput.isBlank()) "split equally" else "$aiNaturalInput split equally"
                                    }
                                ) {
                                    Text(
                                        text = "+ Split equally",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Receipt Upload Card (Optional attachment)
                    Surface(
                        onClick = { showReceiptSource = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = if (pickedReceiptUri != null) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (pickedReceiptUri != null) 2.dp else 1.5.dp,
                            color = if (pickedReceiptUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    SymbolIcon(
                                        symbol = OpenSplitIcons.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (pickedReceiptUri != null) "Receipt Attached" else "Attach or Scan Receipt (Optional)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (pickedReceiptUri != null) "Tap to change photo or remove" else "AI will extract all line items and apply your split",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isScanning) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    com.opensplit.ui.components.InlineBusy("Reading receipt & applying instructions…")
                                }
                            }
                            if (pickedReceiptUri != null) {
                                TextButton(
                                    shapes = com.opensplit.ui.theme.appButtonShapes(),
                                    onClick = { pickedReceiptUri = null }
                                ) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        shapes = androidx.compose.material3.ButtonDefaults.shapesFor(androidx.compose.material3.ButtonDefaults.MediumContainerHeight),
                        onClick = {
                            if (pickedReceiptUri != null && !isScanning) {
                                scanReceipt(pickedReceiptUri, aiNaturalInput.ifBlank { null })
                            } else if (aiNaturalInput.isNotBlank() && !isParsingText) {
                                isParsingText = true
                                scope.launch {
                                    val names = members.map { it.displayName.ifBlank { it.email } }
                                    val parsed = aiService.parseExpenseText(aiNaturalInput, names, group.currency)
                                        ?: AiHeuristics.parseExpenseText(aiNaturalInput, names)
                                    isParsingText = false
                                    if (parsed == null) {
                                        snackbar.showMessage("Couldn't understand that. Try speaking or typing who paid and amounts.")
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
                                        else if (aiService.isDailyQuotaExhausted.value) "Daily AI limit reached. Filled in with offline parser."
                                        else "Filled in what could be read offline. Connect an AI key in Settings for cloud models."
                                    )
                                }
                            } else {
                                snackbar.showMessage("Type or speak split instructions, or scan a receipt first.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = androidx.compose.material3.ButtonDefaults.MediumContainerHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isParsingText || isScanning) {
                            AppLoadingIndicator(size = 20.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            SymbolIcon(OpenSplitIcons.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isParsingText || isScanning) "Processing with AI…" else "Process with AI",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
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
                                    val limited = cleaned.take(12)
                                    if (limited != amountText) {
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        amountText = limited
                                    }
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
                            text = if (totalAmount > 0) com.opensplit.util.CurrencyFormatter.format(totalAmount, group.currency) else "Enter total amount",
                            fontFamily = com.opensplit.ui.theme.MoneyFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Description field (always accessible)
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
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(descriptionFocusRequester)
                )

                Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)) {
                // Category Selection
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (categoryFromAi && !categoryTouched) AiBadge(label = "AI suggested · $aiSource")
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
                    OutlinedButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { showDatePicker = true }) {
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
                    OutlinedButton(shapes = com.opensplit.ui.theme.appButtonShapes(), 
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
                            IconButton(shapes = com.opensplit.ui.theme.appIconButtonShapes(), onClick = {
                                pickedReceiptUri = null
                                pickedReceiptName = null
                                existingReceiptUrl = null
                            }) {
                                SymbolIcon(OpenSplitIcons.Close, contentDescription = "Remove receipt")
                            }
                        }
                    } else {
                        OutlinedButton(shapes = com.opensplit.ui.theme.appButtonShapes(), 
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
                    com.opensplit.ui.components.ChoiceToggleRow(
                        options = RecurrenceFrequency.entries.map { com.opensplit.domain.logic.RecurrenceScheduler.label(it) },
                        selectedIndex = recurrenceFreq.ordinal,
                        onSelect = { recurrenceFreq = RecurrenceFrequency.entries[it] }
                    )
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

                // Split Mode Tabs / Chips (Always visible)
                Column {
                    Text(
                        text = "Split Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
                        verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)
                    ) {
                        splitTypes.forEachIndexed { index, title ->
                            FilterChip(
                                selected = selectedSplitIndex == index,
                                onClick = {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    selectedSplitIndex = index
                                },
                                label = { Text(title) },
                                leadingIcon = {
                                    val icon = when (index) {
                                        0 -> OpenSplitIcons.SplitEqual
                                        1 -> OpenSplitIcons.SplitExact
                                        2 -> OpenSplitIcons.SplitPercent
                                        3 -> OpenSplitIcons.SplitShares
                                        4 -> OpenSplitIcons.SplitItemized
                                        else -> null
                                    }
                                    icon?.let { SymbolIcon(it, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                },
                                shape = MaterialTheme.shapes.medium
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
                        } else {
                            Text(
                                text = "Will be split equally among ${participantMembers.size} member${if (participantMembers.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        OutlinedButton(shapes = com.opensplit.ui.theme.appButtonShapes(), 
                                            onClick = {
                                                if (currentShares > 1) {
                                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                    shares[m.uid] = currentShares - 1
                                                }
                                            },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(48.dp)
                                        ) { Text("-", style = MaterialTheme.typography.titleMedium) }
                                        Text(
                                            text = "$currentShares share(s)",
                                            modifier = Modifier.padding(horizontal = OpenSplitTokens.SpaceSM),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        OutlinedButton(shapes = com.opensplit.ui.theme.appButtonShapes(), 
                                            onClick = {
                                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                shares[m.uid] = currentShares + 1
                                            },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(48.dp)
                                        ) { Text("+", style = MaterialTheme.typography.titleMedium) }
                                    }
                                }
                            }
                        }
                    }
                    4 -> { // Itemized
                        Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                            OutlinedButton(shapes = com.opensplit.ui.theme.appButtonShapes(), 
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
                            Button(shapes = com.opensplit.ui.theme.appButtonShapes(), 
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
                                HorizontalDivider(modifier = Modifier.padding(vertical = OpenSplitTokens.SpaceXS))
                                itemizedList.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text("${item.assignedUids.size} member${if (item.assignedUids.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            text = com.opensplit.util.CurrencyFormatter.format(item.price, group.currency),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = com.opensplit.ui.theme.MoneyFontFamily),
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            shapes = com.opensplit.ui.theme.appIconButtonShapes(),
                                            onClick = { itemizedList.remove(item) }
                                        ) {
                                            SymbolIcon(OpenSplitIcons.Close, contentDescription = "Remove item", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                val itemsSubtotal = remember(itemizedList.toList()) { itemizedList.sumOf { it.price } }
                                val calculatedTotalWithTaxTip = remember(itemsSubtotal, itemizedTaxPercent, itemizedTipPercent) {
                                    itemsSubtotal * (1.0 + (itemizedTaxPercent + itemizedTipPercent) / 100.0)
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(OpenSplitTokens.SpaceMD),
                                        verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Items Subtotal", style = MaterialTheme.typography.bodyMedium)
                                            Text(com.opensplit.util.CurrencyFormatter.format(itemsSubtotal, group.currency), fontWeight = FontWeight.SemiBold)
                                        }

                                        // Proportional Tax Selection
                                        Column {
                                            Text("Proportional Tax", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)) {
                                                listOf(0.0, 5.0, 8.25, 12.0, 18.0).forEach { pct ->
                                                    FilterChip(
                                                        selected = itemizedTaxPercent == pct,
                                                        onClick = {
                                                            itemizedTaxPercent = pct
                                                            val newTotal = itemsSubtotal * (1.0 + (pct + itemizedTipPercent) / 100.0)
                                                            amountText = "%.2f".format(java.util.Locale.US, newTotal)
                                                        },
                                                        label = { Text(if (pct == 0.0) "None" else "${pct}%") }
                                                    )
                                                }
                                            }
                                        }

                                        // Proportional Tip Selection
                                        Column {
                                            Text("Proportional Tip", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)) {
                                                listOf(0.0, 10.0, 15.0, 18.0, 20.0).forEach { pct ->
                                                    FilterChip(
                                                        selected = itemizedTipPercent == pct,
                                                        onClick = {
                                                            itemizedTipPercent = pct
                                                            val newTotal = itemsSubtotal * (1.0 + (itemizedTaxPercent + pct) / 100.0)
                                                            amountText = "%.2f".format(java.util.Locale.US, newTotal)
                                                        },
                                                        label = { Text(if (pct == 0.0) "None" else "${pct.toInt()}%") }
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider()

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Total with Tax & Tip", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = com.opensplit.util.CurrencyFormatter.format(calculatedTotalWithTaxTip, group.currency),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            if (kotlin.math.abs(totalAmount - calculatedTotalWithTaxTip) > 0.01) {
                                                OutlinedButton(
                                                    shapes = com.opensplit.ui.theme.appButtonShapes(),
                                                    onClick = {
                                                        amountText = "%.2f".format(java.util.Locale.US, calculatedTotalWithTaxTip)
                                                    }
                                                ) {
                                                    Text("Apply to Total")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))

                // Save Expense Button with Duplicate Prevention
                Button(shapes = com.opensplit.ui.theme.appButtonShapes(), 
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
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
                                isSaving = false
                                busy.hide()
                                onSaved()
                                onNavigateBack()
                                // After the sheet is gone, so the host underneath is visible.
                                snackbar.showMessage(if (isEditing) "Expense updated" else "Expense saved")
                            }
                            val onError: (String) -> Unit = { msg ->
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Reject)
                                isSaving = false
                                busy.hide()
                                saveError = msg
                            }
                            if (isEditing) viewModel.updateExpense(builtExpense, pickedReceiptUri, onDone, onError)
                            else viewModel.addExpense(builtExpense, pickedReceiptUri, onDone, onError)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = androidx.compose.material3.ButtonDefaults.MediumContainerHeight),
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
                saveError?.let { err ->
                    val friendly = when {
                        err.contains("PERMISSION_DENIED", ignoreCase = true) -> "You don't have permission to save to this group."
                        err.contains("UNAVAILABLE", ignoreCase = true) -> "Network offline. Changes will sync when connected."
                        err.contains("NOT_FOUND", ignoreCase = true) -> "Group or expense was not found."
                        else -> err.substringAfter(": ").take(100).ifEmpty { err.take(100) }
                    }
                    Text(
                        text = "Couldn't save: $friendly",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = OpenSplitTokens.SpaceSM)
                    )
                }
                } // end Category..Split-mode/Save Column
                } // end else (Manual Entry tab)

                if (showDatePicker) {
                    val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = {
                                dateState.selectedDateMillis?.let { selectedDateMillis = it }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = dateState)
                    }
                }

                if (showInvitePersonDialog) {
                    val links by viewModel.inviteLinks.collectAsStateWithLifecycle()
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

@Composable
private fun VoiceVisualizerIconButton(
    isListening: Boolean,
    rmsLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val containerIdle = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
    val containerActive = MaterialTheme.colorScheme.primaryContainer

    val infiniteTransition = rememberInfiniteTransition(label = "voice_visualizer")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val animatedRms by animateFloatAsState(
        targetValue = rmsLevel.coerceIn(0f, 10f),
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "rms"
    )

    val normalizedRms = (animatedRms / 10f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(52.dp)
            .semantics {
                this.role = Role.Button
                this.contentDescription = if (isListening) "Stop voice input" else "Voice input"
            }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.center
            val baseRadius = 21.dp.toPx()
            val coreRadius = if (isListening) baseRadius * pulseScale else baseRadius

            if (isListening) {
                // Outer acoustic wave ripple 1
                val r1 = coreRadius + (3.dp.toPx() + 6.dp.toPx() * normalizedRms) *
                    (0.7f + 0.3f * kotlin.math.sin(phase))
                drawCircle(
                    color = primaryColor.copy(alpha = (0.28f + 0.32f * normalizedRms) * 0.75f),
                    radius = r1,
                    center = center,
                    style = Stroke(width = 1.8.dp.toPx())
                )

                // Outer acoustic wave ripple 2
                val r2 = coreRadius + (6.dp.toPx() + 11.dp.toPx() * normalizedRms) *
                    (0.6f + 0.4f * kotlin.math.sin(phase + 1.6f))
                drawCircle(
                    color = tertiaryColor.copy(alpha = (0.16f + 0.24f * normalizedRms) * 0.65f),
                    radius = r2,
                    center = center,
                    style = Stroke(width = 1.4.dp.toPx())
                )
            }

            // Core Button Background
            drawCircle(
                color = if (isListening) containerActive else containerIdle,
                radius = coreRadius,
                center = center
            )

            // Core Border
            drawCircle(
                color = if (isListening) primaryColor else primaryColor.copy(alpha = 0.35f),
                radius = coreRadius,
                center = center,
                style = Stroke(width = if (isListening) 1.8.dp.toPx() else 1.2.dp.toPx())
            )

            if (isListening) {
                // Audio Equalizer in the center
                val barCount = 4
                val barWidth = 3.2.dp.toPx()
                val barSpacing = 2.6.dp.toPx()
                val totalEqualizerWidth = barCount * barWidth + (barCount - 1) * barSpacing
                val startX = center.x - totalEqualizerWidth / 2f
                val minHeight = 4.dp.toPx()
                val maxHeight = 20.dp.toPx()

                for (i in 0 until barCount) {
                    val wave = kotlin.math.sin(phase * 1.6f + i * 1.1f) * 0.5f + 0.5f
                    val barActivity = (0.22f * wave + 0.78f * normalizedRms * (0.6f + 0.4f * wave)).coerceIn(0.08f, 1f)
                    val currentHeight = minHeight + (maxHeight - minHeight) * barActivity
                    val barX = startX + i * (barWidth + barSpacing)
                    val barY = center.y - currentHeight / 2f
                    val color = if (i % 2 == 0) primaryColor else tertiaryColor

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(barX, barY),
                        size = Size(barWidth, currentHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }

        if (!isListening) {
            SymbolIcon(
                symbol = OpenSplitIcons.Mic,
                contentDescription = null,
                tint = primaryColor,
                filled = true,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
