@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensplit.ui.components.SymbolIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.viewmodel.ExpenseDetailViewModel
import com.opensplit.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    viewModel: ExpenseDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var commentText by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val snackbar = LocalSnackbarController.current
    val busy = com.opensplit.ui.components.LocalBusy.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val exp = (state as? com.opensplit.ui.viewmodel.ScreenState.Success)?.data?.expense
                    Text(
                        text = exp?.description?.ifBlank { "Expense Details" } ?: "Expense Details",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    com.opensplit.ui.components.RoundBackButton(onClick = onNavigateBack)
                },
                actions = {
                    IconButton(shapes = com.opensplit.ui.theme.appIconButtonShapes(), onClick = onNavigateToEdit) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.Edit,
                            contentDescription = "Edit Expense",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(shapes = com.opensplit.ui.theme.appIconButtonShapes(), onClick = { showDeleteConfirmDialog = true }) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.Delete,
                            contentDescription = "Delete Expense",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StateLayout(state = state) { detailState ->
                val exp = detailState.expense
                if (exp == null) {
                    Text("Expense not found", modifier = Modifier.padding(16.dp))
                } else {
                    val dateFormatted = remember(exp.date) {
                        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(exp.date.toDate())
                    }
                    val nameOf: (String) -> String = { uid ->
                        if (uid == viewModel.currentUserId) "You"
                        else detailState.memberNames[uid]?.ifBlank { null } ?: uid.take(6)
                    }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .widthIn(max = 720.dp)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // SCREEN 13 Hero Card (28dp corners in surface-container)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 2.dp
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Category circle (48dp in secondary-container)
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SymbolIcon(
                                            symbol = OpenSplitIcons.CategoryFood,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = exp.description,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(exp.amount, exp.currency),
                                            style = MaterialTheme.typography.displaySmall.copy(
                                                fontFamily = com.opensplit.ui.theme.MoneyFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Date & Category chips row
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Text(
                                            text = dateFormatted,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = com.opensplit.ui.theme.MoneyFontFamily),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Text(
                                            text = exp.category.ifEmpty { "General" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Paid by & Split Among Section (28dp surface-container-low card)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "PAID BY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val payers = exp.multiPayer?.takeIf { it.isNotEmpty() }
                                    ?: mapOf(exp.paidBy to exp.amount)
                                payers.forEach { (uid, amt) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = nameOf(uid),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(amt, exp.currency),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = com.opensplit.ui.theme.MoneyFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (exp.splits.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "SPLIT AMONG (${exp.splitType.name.lowercase()})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    exp.splits.forEach { split ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = nameOf(split.uid),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = CurrencyFormatter.format(split.amount, exp.currency),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = com.opensplit.ui.theme.MoneyFontFamily,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Edit history (who changed what)
                        if (exp.history.isNotEmpty()) {
                            Text(text = "History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    exp.history.takeLast(5).reversed().forEach { change ->
                                        val who = if (change.uid == viewModel.currentUserId) "You"
                                            else detailState.memberNames[change.uid]?.ifBlank { null } ?: "Someone"
                                        val whenStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(change.timestamp.toDate())
                                        Text("$who · $whenStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        change.changes.forEach { line ->
                                            Text("• $line", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }

                        // Comments / Notes
                        Text(
                            text = "Comments & Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(detailState.comments, key = { it.id }) { comment ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = comment.text,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val time = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                            .format(comment.timestamp.toDate())
                                        Text(
                                            text = time,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = com.opensplit.ui.theme.MoneyFontFamily),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Comment input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Add a comment or note...") },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                shape = MaterialTheme.shapes.small,
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(shapes = com.opensplit.ui.theme.appIconButtonShapes(), 
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        viewModel.addComment(commentText)
                                        commentText = ""
                                    }
                                },
                                enabled = commentText.isNotBlank()
                            ) {
                                SymbolIcon(OpenSplitIcons.Send, contentDescription = "Send comment", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                }
            }
        }
    }

    // SCREEN 15 — Delete Expense Confirmation Dialog
    if (showDeleteConfirmDialog) {
        val exp = (state as? com.opensplit.ui.viewmodel.ScreenState.Success)?.data?.expense
        val expTitle = exp?.let { "${CurrencyFormatter.format(it.amount, it.currency)} — ${it.description}" } ?: "this expense"

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                SymbolIcon(
                    symbol = OpenSplitIcons.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete expense?",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Removing '$expTitle' will recalculate balances for all members. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            confirmButton = {
                Button(
                    onClick = {
                        busy.show("Deleting expense…")
                        viewModel.deleteExpense { deleted ->
                            busy.hide()
                            showDeleteConfirmDialog = false
                            onNavigateBack()
                            if (deleted != null) {
                                snackbar.showUndo("Expense deleted") {
                                    viewModel.restoreExpense(deleted)
                                }
                            } else {
                                snackbar.showMessage("Expense deleted")
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
