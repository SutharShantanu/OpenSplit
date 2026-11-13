@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensplit.data.payment.DetectedPayment
import com.opensplit.data.payment.DetectedPaymentStore
import com.opensplit.data.payment.Direction
import com.opensplit.domain.repository.UserPreferencesRepository
import com.opensplit.service.PaymentDetector
import com.opensplit.ui.components.SettingsGroupCard
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.theme.MoneyFontFamily
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.success
import com.opensplit.util.CurrencyFormatter
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Settings › Payment detection: notification access, what to detect, and the payments waiting
 * for review. Add expense / Settle up hand the amount to the normal forms via [onOpenAction].
 */
@Composable
fun PaymentDetectionScreen(
    prefs: UserPreferencesRepository,
    store: DetectedPaymentStore,
    onOpenAction: (String) -> Unit,
    onNavigateBack: () -> Unit,
    updater: com.opensplit.data.update.AppUpdater? = null
) {
    val context = LocalContext.current
    if (!PaymentDetector.isIncluded(context)) {
        NotInThisBuild(updater, onNavigateBack)
        return
    }
    val scope = rememberCoroutineScope()
    // Re-check on resume: the user grants access in system settings and comes back.
    var hasAccess by remember { mutableStateOf(PaymentDetector.hasAccess(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasAccess = PaymentDetector.hasAccess(context) }
    val enabled by prefs.paymentDetectionFlow.collectAsStateWithLifecycle(initialValue = true)
    val credits by prefs.detectCreditsFlow.collectAsStateWithLifecycle(initialValue = true)
    val refunds by prefs.detectRefundsFlow.collectAsStateWithLifecycle(initialValue = false)
    val items by store.items.collectAsStateWithLifecycle()
    val pending = items.filter { it.status == DetectedPayment.Status.PENDING }
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment detection", fontWeight = FontWeight.Bold) },
                navigationIcon = { com.opensplit.ui.components.RoundBackButton(onClick = onNavigateBack) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                SettingsGroupCard(title = "Access", icon = OpenSplitIcons.Notifications) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (hasAccess) "On. OpenSplit reads notifications from UPI and bank apps only."
                            else "Off. Allow notification access so OpenSplit can spot payments from GPay, PhonePe, Paytm and bank apps.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!hasAccess) {
                            Text(
                                "If Android says the setting is restricted, open App info › ⋮ › Allow restricted settings, then try again.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = { runCatching { context.startActivity(PaymentDetector.accessSettingsIntent(context)) } }) {
                            Text(if (hasAccess) "Notification access settings" else "Allow notification access")
                        }
                    }
                }
            }
            item {
                SettingsGroupCard(title = "Detect", icon = OpenSplitIcons.Payments) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        SwitchRow("Detect payments", "Turn off to stop without removing access.", enabled) { v -> scope.launch { prefs.setPaymentDetection(v) } }
                        SwitchRow("Money received", "Also catch payments to you.", credits) { v -> scope.launch { prefs.setDetectCredits(v) } }
                        SwitchRow("Refunds", "Also catch refunds.", refunds) { v -> scope.launch { prefs.setDetectRefunds(v) } }
                    }
                }
            }
            item {
                Text(
                    if (pending.isEmpty()) "Nothing to review" else "To review · ${pending.size}",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                )
            }
            items(pending, key = { it.id }) { p ->
                DetectedPaymentCard(
                    p,
                    onAddExpense = { onOpenAction(PaymentDetector.ACTION_EXPENSE_PREFIX + p.id) },
                    onSettle = { onOpenAction(PaymentDetector.ACTION_SETTLE_PREFIX + p.id) },
                    onIgnore = { store.setStatus(p.id, DetectedPayment.Status.IGNORED) }
                )
            }
            item {
                Text(
                    "Only the amount, direction, payee, reference and app name are kept, on this phone only. " +
                        "Message text is read once to find those and never saved, synced or logged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (items.isNotEmpty()) {
                item {
                    TextButton(onClick = { confirmClear = true }) {
                        Text("Clear detected history", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear detected history?") },
            text = { Text("Removes every detected payment from this phone. Expenses you already added stay.") },
            confirmButton = { TextButton(onClick = { store.clear(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun DetectedPaymentCard(p: DetectedPayment, onAddExpense: () -> Unit, onSettle: () -> Unit, onIgnore: () -> Unit) {
    val debit = p.direction == Direction.DEBIT
    val tint = if (debit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.success
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SymbolIcon(if (debit) OpenSplitIcons.ArrowUpward else OpenSplitIcons.ArrowDownward, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    (if (debit) "−" else "+") + CurrencyFormatter.format(p.amount, "INR"),
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = MoneyFontFamily), color = tint
                )
                Spacer(Modifier.width(8.dp))
                Text(if (debit) "Paid" else "Received", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                listOfNotNull(p.counterparty, p.source, p.reference?.let { "Ref $it" }).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(p.detectedAt)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                Button(onClick = onAddExpense) { Text("Add expense") }
                OutlinedButton(onClick = onSettle) { Text("Settle up") }
                TextButton(onClick = onIgnore) { Text("Ignore") }
            }
        }
    }
}

/**
 * Default build: the notification listener isn't declared (Play Protect blocks sideloading APKs
 * that declare one), so explain that and offer the payment-detection build from GitHub releases.
 */
@Composable
private fun NotInThisBuild(updater: com.opensplit.data.update.AppUpdater?, onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbar = com.opensplit.ui.components.LocalSnackbarController.current
    var checking by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment detection", fontWeight = FontWeight.Bold) },
                navigationIcon = { com.opensplit.ui.components.RoundBackButton(onClick = onNavigateBack) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SettingsGroupCard(title = "Not in this build", icon = OpenSplitIcons.Info) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Spotting payments from GPay, PhonePe and bank app notifications needs notification access. " +
                            "Google Play Protect blocks installing any app that asks for it from a browser or chat, " +
                            "so the standard OpenSplit leaves it out.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "The payment-detection build is the same app with this feature. It installs over this one and keeps your data. " +
                            "If Play Protect still blocks it, install it over USB (adb install) instead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val height = ButtonDefaults.MediumContainerHeight
                    Button(
                        onClick = {
                            if (updater == null || checking) return@Button
                            checking = true
                            scope.launch {
                                updater.findPaymentDetectionBuild()
                                    .onSuccess { r -> if (r != null) updater.offer(r) else snackbar.showMessage("No newer payment-detection build yet") }
                                    .onFailure { snackbar.showMessage("Couldn't reach GitHub. Try again.") }
                                checking = false
                            }
                        },
                        enabled = updater != null && !checking,
                        shapes = ButtonDefaults.shapesFor(height),
                        modifier = Modifier.fillMaxWidth().heightIn(min = height)
                    ) {
                        if (checking) com.opensplit.ui.components.InlineLoadingIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Get the payment-detection build", style = ButtonDefaults.textStyleFor(height))
                    }
                }
            }
        }
    }
}
