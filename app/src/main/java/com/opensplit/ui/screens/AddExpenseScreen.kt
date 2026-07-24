package com.opensplit.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.rememberCoroutineScope
import com.opensplit.data.ai.GeminiReceiptParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: GroupDetailViewModel,
    onNavigateBack: () -> Unit,
    editingExpense: Expense? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            var selectedDateMillis by remember { mutableStateOf(editingExpense?.date?.toDate()?.time ?: System.currentTimeMillis()) }
            var showDatePicker by remember { mutableStateOf(false) }
            var multiplePayers by remember { mutableStateOf(editingExpense?.multiPayer != null) }
            val payerAmounts = remember {
                mutableStateMapOf<String, String>().apply {
                    editingExpense?.multiPayer?.forEach { (uid, amt) -> put(uid, amt.toString()) }
                }
            }
            var recurrenceFreq by remember { mutableStateOf(editingExpense?.recurrence?.frequency ?: RecurrenceFrequency.NONE) }

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
                            Toast.makeText(context, "Added ${items.size} items from receipt", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Couldn't read receipt (check Gemini API key)", Toast.LENGTH_SHORT).show()
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

                // Description Field with Capitalization
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Dinner, Groceries, Flight") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
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
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )

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
                    }
                    if (selectedParticipants.isEmpty()) {
                        Text(
                            text = "Select at least one person",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

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
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
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
                                date = Timestamp(java.util.Date(selectedDateMillis)),
                                recurrence = if (recurrenceFreq != RecurrenceFrequency.NONE) {
                                    val existing = editingExpense?.recurrence
                                    if (existing != null && existing.frequency == recurrenceFreq) existing
                                    else RecurrenceRule(recurrenceFreq, nextRecurrence(selectedDateMillis, recurrenceFreq))
                                } else null
                            )

                            val onDone: () -> Unit = {
                                isSaving = false
                                Toast.makeText(context, if (isEditing) "Expense updated!" else "Expense saved successfully!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                            if (isEditing) viewModel.updateExpense(builtExpense, onDone)
                            else viewModel.addExpense(builtExpense, onDone)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormValid
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                        Text("Saving...")
                    } else {
                        Text(if (isEditing) "Update Expense" else "Save Expense", fontWeight = FontWeight.Bold)
                    }
                }

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
