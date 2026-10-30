@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensplit.ui.components.SettingsGroupCard
import com.opensplit.ui.components.SettingsGroupDivider
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
import com.opensplit.ui.components.responsiveContentWidth
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
    val disabled by (prefs?.disabledNotificationTypesFlow ?: kotlinx.coroutines.flow.flowOf(emptySet<String>())).collectAsStateWithLifecycle(initialValue = emptySet())
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
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    com.opensplit.ui.components.RoundBackButton(onClick = onNavigateBack)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .responsiveContentWidth(maxWidth = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            Text(
                text = "Control how and when OpenSplit alerts you about shared expenses and group activity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingsGroupCard(title = "Expense Alerts", icon = OpenSplitIcons.ReceiptLong) {
                NotificationToggleRow(
                    title = "New Expenses",
                    subtitle = "When someone adds a new expense involving you.",
                    checked = newExpensesEnabled,
                    onCheckedChange = {
                        set(NotificationType.EXPENSE_ADDED, it)
                        snackbar.showMessage(if (it) "New expense alerts enabled" else "New expense alerts disabled")
                    }
                )

                SettingsGroupDivider()

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

            SettingsGroupCard(title = "Settlement Reminders", icon = OpenSplitIcons.Payments) {
                NotificationToggleRow(
                    title = "Payment Requests",
                    subtitle = "When someone sends you a reminder to settle up.",
                    checked = paymentRequestsEnabled,
                    onCheckedChange = {
                        set(NotificationType.PAYMENT, it)
                        snackbar.showMessage(if (it) "Payment request reminders enabled" else "Payment request reminders disabled")
                    }
                )

                SettingsGroupDivider()

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

            Spacer(modifier = Modifier.height(24.dp))
        }
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
