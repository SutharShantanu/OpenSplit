package com.opensplit.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    onNavigateBack: () -> Unit
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

            var description by remember { mutableStateOf("") }
            var amountText by remember { mutableStateOf("") }
            var category by remember { mutableStateOf("Food") }
            var paidBy by remember { mutableStateOf(members.firstOrNull()?.uid ?: "") }
            var isSaving by remember { mutableStateOf(false) }

            // 0: Equal, 1: Exact, 2: Percentage, 3: Shares, 4: Itemized
            var selectedSplitIndex by remember { mutableStateOf(0) }
            val splitTypes = listOf("Equally", "Exact", "Percent", "Shares", "Itemized")

            // Maps for custom split inputs
            val exactAmounts = remember { mutableStateMapOf<String, String>() }
            val percentages = remember { mutableStateMapOf<String, String>() }
            val shares = remember { mutableStateMapOf<String, Int>() }

            // Itemized list state
            var itemizedName by remember { mutableStateOf("") }
            var itemizedPrice by remember { mutableStateOf("") }
            val itemizedList = remember { mutableStateListOf<ExpenseItem>() }
            val selectedUidsForItem = remember { mutableStateListOf<String>() }

            val totalAmount = amountText.toDoubleOrNull() ?: 0.0

            fun validateAndBuildSplits(): Pair<SplitType, List<ExpenseSplit>>? {
                if (totalAmount <= 0) return null
                val participantUids = members.map { it.uid }

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

            val splitResult = remember(selectedSplitIndex, totalAmount, exactAmounts.toMap(), percentages.toMap(), shares.toMap(), itemizedList.toList()) {
                validateAndBuildSplits()
            }

            val isFormValid = description.isNotBlank() && totalAmount > 0 && splitResult != null && !isSaving

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
                        text = "Add Expense",
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

                // Paid By Selection
                Column {
                    Text(
                        text = "Paid by",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
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
                            val perPerson = totalAmount / maxOf(1, members.size)
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
                            members.forEach { m ->
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
                            members.forEach { m ->
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
                            members.forEach { m ->
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
                                members.forEach { m ->
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
                        if (description.isNotBlank() && totalAmount > 0 && evaluated != null) {
                            isSaving = true
                            val (sType, splitsList) = evaluated
                            val newExpense = Expense(
                                groupId = group.id,
                                description = description.trim(),
                                amount = totalAmount,
                                paidBy = paidBy,
                                category = category,
                                currency = group.currency,
                                splitType = sType,
                                splits = splitsList,
                                items = if (sType == SplitType.ITEMIZED) itemizedList.toList() else null,
                                createdBy = paidBy,
                                date = Timestamp.now()
                            )

                            viewModel.addExpense(newExpense) {
                                isSaving = false
                                Toast.makeText(context, "Expense saved successfully!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
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
                        Text("Save Expense", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
