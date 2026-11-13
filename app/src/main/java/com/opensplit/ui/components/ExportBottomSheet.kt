@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.opensplit.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.opensplit.data.export.CsvExportGenerator
import com.opensplit.data.export.ExportUtils
import com.opensplit.data.export.JsonExportGenerator
import com.opensplit.data.export.PdfExportGenerator
import com.opensplit.domain.model.Expense
import com.opensplit.domain.model.Group
import com.opensplit.domain.model.Settlement
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.theme.Symbol
import com.opensplit.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ExpenseReportBasis(val title: String, val subtitle: String) {
    CHRONOLOGICAL("By Date", "Ordered newest first"),
    CATEGORY("By Category", "Grouped by expense category"),
    PAYER("By Payer", "Grouped by who paid")
}

enum class ExpenseReportScope(val title: String) {
    ALL("All Expenses"),
    PAID_BY_ME("Paid by You"),
    MY_SHARE("Involved You")
}

enum class ExpenseReportPeriod(val title: String, val days: Int?, val thisMonth: Boolean = false, val thisYear: Boolean = false) {
    ALL("All Time", null),
    THIS_MONTH("This Month", null, thisMonth = true),
    LAST_30_DAYS("Last 30 Days", 30),
    LAST_90_DAYS("Last 90 Days", 90),
    THIS_YEAR("This Year", null, thisYear = true)
}

enum class ExpenseExportDocType(val title: String, val description: String, val mimeType: String, val icon: Symbol) {
    PDF("PDF Document", "Print-ready summary report", "application/pdf", OpenSplitIcons.PictureAsPdf),
    CSV("CSV Spreadsheet", "Standard tabular format for Excel / Sheets", "text/csv", OpenSplitIcons.TableChart),
    JSON("JSON Backup", "Structured data for backup & restore", "application/json", OpenSplitIcons.Code)
}

/**
 * Revamped Export Bottom Sheet using the full filter options pattern from settlement history:
 * - Basis (Grouping): By Date, By Category, By Payer
 * - Scope: All Expenses, Paid by You, Involved You
 * - Time Period: All Time, This Month, Last 30 Days, Last 90 Days, This Year
 * - Document Format: PDF, CSV, JSON
 * - Live summary card with matching count & volume
 * - FilledTonalButton for export
 */
@Composable
fun ExportBottomSheet(
    scopeName: String,
    expenses: List<Expense>,
    settlements: List<Settlement> = emptyList(),
    groups: List<Group> = emptyList(),
    currentUid: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    val coroutineScope = rememberCoroutineScope()
    val busy = LocalBusy.current
    val resolvedUid = remember(currentUid) {
        currentUid.ifEmpty { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    }

    var selectedBasis by rememberSaveable { mutableStateOf(ExpenseReportBasis.CHRONOLOGICAL) }
    var selectedScope by rememberSaveable { mutableStateOf(ExpenseReportScope.ALL) }
    var selectedPeriod by rememberSaveable { mutableStateOf(ExpenseReportPeriod.ALL) }
    var selectedFormat by rememberSaveable { mutableStateOf(ExpenseExportDocType.PDF) }
    var isExporting by remember { mutableStateOf(false) }

    // Filter and sort expenses based on user's configuration
    val filteredExpenses = remember(expenses, selectedScope, selectedPeriod, selectedBasis, resolvedUid) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val curMonth = cal.get(Calendar.MONTH)
        val curYear = cal.get(Calendar.YEAR)

        val filtered = expenses.filter { exp ->
            // Scope match
            val isPaidByMe = exp.paidBy == resolvedUid
            val isInvolved = isPaidByMe || exp.splits.any { it.uid == resolvedUid }
            val matchesScope = when (selectedScope) {
                ExpenseReportScope.ALL -> true
                ExpenseReportScope.PAID_BY_ME -> isPaidByMe
                ExpenseReportScope.MY_SHARE -> isInvolved
            }

            // Period match
            val expTime = exp.date.toDate().time
            cal.time = exp.date.toDate()
            val matchesPeriod = when {
                selectedPeriod.days != null -> (now - expTime) <= (selectedPeriod.days!! * 86400000L)
                selectedPeriod.thisMonth -> cal.get(Calendar.MONTH) == curMonth && cal.get(Calendar.YEAR) == curYear
                selectedPeriod.thisYear -> cal.get(Calendar.YEAR) == curYear
                else -> true
            }

            matchesScope && matchesPeriod
        }

        // Apply grouping / sorting basis
        when (selectedBasis) {
            ExpenseReportBasis.CHRONOLOGICAL -> filtered.sortedByDescending { it.date.toDate().time }
            ExpenseReportBasis.CATEGORY -> filtered.sortedWith(compareBy({ it.category.lowercase() }, { -it.date.toDate().time }))
            ExpenseReportBasis.PAYER -> filtered.sortedWith(compareBy({ it.paidBy }, { -it.date.toDate().time }))
        }
    }

    val primaryCurrency = remember(filteredExpenses) { filteredExpenses.firstOrNull()?.currency ?: "INR" }
    // Mixed-currency selections are converted, never added raw (₹ + $).
    val totalVolume = remember(filteredExpenses, primaryCurrency) {
        filteredExpenses.sumOf { CurrencyFormatter.convert(it.amount, it.currency.ifBlank { primaryCurrency }, primaryCurrency) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = com.opensplit.ui.theme.OpenSplitTokens.BottomSheetShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 640.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Export expenses", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(scopeName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                HeaderAction(OpenSplitIcons.Close, "Close", onClick = onDismiss)
            }

            val scopeLabels = listOf("All", "Paid by you", "With you")
            // Single choices are connected button groups; the selected one morphs round.
            ExportSection("Group by") {
                ConnectedChoiceGroup(
                    options = listOf("Date", "Category", "Payer"),
                    selectedIndex = selectedBasis.ordinal,
                    onSelect = { selectedBasis = ExpenseReportBasis.entries[it] }
                )
            }
            ExportSection("Include") {
                ConnectedChoiceGroup(
                    options = scopeLabels,
                    selectedIndex = selectedScope.ordinal,
                    onSelect = { selectedScope = ExpenseReportScope.entries[it] }
                )
            }
            ExportSection("Period") {
                // Five options don't fit one row, so they wrap as toggle buttons (one selected).
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseReportPeriod.entries.forEach { period ->
                        androidx.compose.material3.OutlinedToggleButton(
                            checked = selectedPeriod == period,
                            onCheckedChange = { selectedPeriod = period }
                        ) { Text(period.title) }
                    }
                }
            }
            ExportSection("Format") {
                ConnectedChoiceGroup(
                    options = ExpenseExportDocType.entries.map { it.name },
                    icons = ExpenseExportDocType.entries.map { it.icon },
                    selectedIndex = selectedFormat.ordinal,
                    onSelect = { selectedFormat = ExpenseExportDocType.entries[it] }
                )
                Text(selectedFormat.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Live summary: count and total roll as the filters change.
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AnimatedAmountText(
                            value = filteredExpenses.size.toDouble(),
                            format = { n -> n.toInt().let { if (it == 1) "1 expense" else "$it expenses" } },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text("${scopeLabels[selectedScope.ordinal]} · ${selectedPeriod.title}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AnimatedAmountText(
                        value = totalVolume,
                        format = { CurrencyFormatter.format(it, primaryCurrency) },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = com.opensplit.ui.theme.MoneyFontFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Primary call to action: filled, medium (56dp) expressive button.
            val height = ButtonDefaults.MediumContainerHeight
            Button(
                onClick = {
                    isExporting = true
                    busy.show("Generating ${selectedFormat.title}…")
                    coroutineScope.launch {
                        try {
                            val generator = when (selectedFormat) {
                                ExpenseExportDocType.CSV -> CsvExportGenerator()
                                ExpenseExportDocType.PDF -> PdfExportGenerator()
                                ExpenseExportDocType.JSON -> JsonExportGenerator()
                            }
                            val file = generator.generate(
                                context = context,
                                scopeName = scopeName,
                                expenses = filteredExpenses,
                                settlements = settlements,
                                groups = groups
                            )
                            busy.hide()
                            isExporting = false
                            ExportUtils.shareFile(context, file, selectedFormat.mimeType)
                            onDismiss()
                        } catch (e: Exception) {
                            busy.hide()
                            isExporting = false
                            snackbar.showMessage("Export failed: ${e.message}")
                        }
                    }
                },
                enabled = !isExporting && filteredExpenses.isNotEmpty(),
                shapes = ButtonDefaults.shapesFor(height),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier = Modifier.fillMaxWidth().heightIn(min = height)
            ) {
                if (isExporting) {
                    InlineLoadingIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                    Text("Exporting…", style = ButtonDefaults.textStyleFor(height))
                } else {
                    SymbolIcon(selectedFormat.icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)))
                    Spacer(modifier = Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                    Text(
                        if (filteredExpenses.isEmpty()) "Nothing to export" else "Export ${selectedFormat.name}",
                        style = ButtonDefaults.textStyleFor(height)
                    )
                }
            }
        }
    }
}

/** Labelled block in an export sheet. */
@Composable
internal fun ExportSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
