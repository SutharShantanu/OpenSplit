@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.components.SymbolIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import com.opensplit.domain.model.NotificationType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.theme.OpenSplitIcons

/**
 * SCREEN 24 — Notification Settings
 * Faithfully implemented from Stitch design `Notification_Settings.html`.
 * Sections:
 * - TopAppBar with back navigation and title
 * - Header Section ("Notifications", description)
 * - Expense Alerts Card (New Expenses, Expense Updates)
 * - Settlement Reminders Card (Payment Requests, Automatic Reminders)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    prefs: com.opensplit.domain.repository.UserPreferencesRepository? = null
) {
    val snackbar = LocalSnackbarController.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val disabled by (prefs?.disabledNotificationTypesFlow ?: kotlinx.coroutines.flow.flowOf(emptySet<String>())).collectAsState(initial = emptySet())
    fun on(type: com.opensplit.domain.model.NotificationType) = type.id !in disabled
    fun set(type: com.opensplit.domain.model.NotificationType, enabled: Boolean) {
        scope.launch { prefs?.setNotificationTypeEnabled(type.id, enabled) }
    }
    val newExpensesEnabled = on(NotificationType.EXPENSE_ADDED)
    val expenseUpdatesEnabled = on(NotificationType.EXPENSE_EDITED)
    val paymentRequestsEnabled = on(NotificationType.PAYMENT)
    val automaticRemindersEnabled = on(NotificationType.REMINDER)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = onNavigateBack) {
                        SymbolIcon(OpenSplitIcons.Back, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Control how and when openSplit alerts you about shared expenses and group activity.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expense Alerts Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Expense Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            NotificationToggleRow(
                                title = "New Expenses",
                                subtitle = "When someone adds a new expense involving you.",
                                checked = newExpensesEnabled,
                                onCheckedChange = {
                                    set(NotificationType.EXPENSE_ADDED, it)
                                    snackbar.showMessage(if (it) "New expense alerts enabled" else "New expense alerts disabled")
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            NotificationToggleRow(
                                title = "Expense Updates",
                                subtitle = "Changes to amount, date, or split logic on your expenses.",
                                checked = expenseUpdatesEnabled,
                                onCheckedChange = {
                                    set(NotificationType.EXPENSE_EDITED, it)
                                    snackbar.showMessage(if (it) "Expense update alerts enabled" else "Expense update alerts disabled")
                                }
                            )
                        }
                    }
                }
            }

            // Settlement Reminders Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Settlement Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            NotificationToggleRow(
                                title = "Payment Requests",
                                subtitle = "When someone sends you a reminder to settle up.",
                                checked = paymentRequestsEnabled,
                                onCheckedChange = {
                                    set(NotificationType.PAYMENT, it)
                                    snackbar.showMessage(if (it) "Payment request reminders enabled" else "Payment request reminders disabled")
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            NotificationToggleRow(
                                title = "Automatic Reminders",
                                subtitle = "Weekly digest of outstanding balances you owe.",
                                checked = automaticRemindersEnabled,
                                onCheckedChange = {
                                    set(NotificationType.REMINDER, it)
                                    snackbar.showMessage(if (it) "Weekly digest reminders enabled" else "Weekly digest reminders disabled")
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
