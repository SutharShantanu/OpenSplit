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

    val totalVolume = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }
    val primaryCurrency = remember(filteredExpenses) { filteredExpenses.firstOrNull()?.currency ?: "INR" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 640.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Export Expenses — $scopeName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Configure basis, scope, period, and format",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = onDismiss) {
                    SymbolIcon(OpenSplitIcons.Close, contentDescription = "Close")
                }
            }

            // 1. Report Basis / Grouping
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Report Basis (Grouping)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseReportBasis.values().forEach { basis ->
                        val isSelected = selectedBasis == basis
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable { selectedBasis = basis },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = basis.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 2. Transaction Scope
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Expense Scope",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseReportScope.values().forEach { scope ->
                        val isSelected = selectedScope == scope
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable { selectedScope = scope },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = scope.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 3. Time Period
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Time Period",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseReportPeriod.values().forEach { period ->
                        val isSelected = selectedPeriod == period
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.title) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // 4. File Format
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Export Format",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseExportDocType.values().forEach { format ->
                        val isSelected = selectedFormat == format
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clickable { selectedFormat = format },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary) else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                SymbolIcon(
                                    symbol = format.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = format.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 5. Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${filteredExpenses.size} expenses selected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Scope: ${selectedScope.title} · ${selectedPeriod.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = CurrencyFormatter.format(totalVolume, primaryCurrency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 6. Action Button
            FilledTonalButton(
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    if (filteredExpenses.isEmpty()) {
                        snackbar.showMessage("No expenses match the selected filters")
                        return@FilledTonalButton
                    }
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
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Exporting…")
                } else {
                    SymbolIcon(
                        symbol = selectedFormat.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Export ${selectedFormat.name} Report",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
