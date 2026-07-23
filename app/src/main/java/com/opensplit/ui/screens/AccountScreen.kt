package com.opensplit.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.opensplit.di.AppContainer
import com.opensplit.ui.components.ExportBottomSheet
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.components.appHazeHeader
import com.opensplit.ui.components.appHazeSource
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.viewmodel.AccountViewModel
import com.google.firebase.auth.FirebaseAuth
import dev.chrisbanes.haze.HazeState
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    appContainer: AppContainer,
    rootNavController: NavController,
    viewModel: AccountViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val theme by viewModel.themeFlow.collectAsState(initial = "system")

    var showNameDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showExportBottomSheet by remember { mutableStateOf(false) }
    var showCurrencyMenu by remember { mutableStateOf(false) }
    var showPasswordResetDialog by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf("") }
    val hazeState = remember { HazeState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account & Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { rootNavController.popBackStack() }) {
                        Icon(OpenSplitIcons.Back, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                ),
                modifier = Modifier.appHazeHeader(hazeState)
            )
        }
    ) { innerPadding ->
        StateLayout(state = uiState) { accountData ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .appHazeSource(hazeState),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + OpenSplitTokens.SpaceMD,
                    bottom = innerPadding.calculateBottomPadding() + OpenSplitTokens.SpaceXL,
                    start = OpenSplitTokens.SpaceLG,
                    end = OpenSplitTokens.SpaceLG
                ),
                verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceLG)
            ) {
                // 1. Profile hero card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(OpenSplitTokens.SpaceLG)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            com.opensplit.ui.components.UserAvatar(
                                photoUrl = accountData.user.photoUrl?.toString(),
                                displayName = accountData.user.displayName,
                                size = 64.dp
                            )
                            Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceMD))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = accountData.user.displayName ?: "User",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                                    IconButton(
                                        onClick = {
                                            editName = accountData.user.displayName ?: ""
                                            showNameDialog = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            OpenSplitIcons.Edit,
                                            contentDescription = "Edit Name",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Text(
                                    text = accountData.user.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }

                // 2. Stat strip
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
                    ) {
                        StatChip(
                            modifier = Modifier.weight(1f),
                            label = "Groups",
                            value = accountData.groupCount.toString()
                        )
                        StatChip(
                            modifier = Modifier.weight(1f),
                            label = "Friends",
                            value = accountData.friendCount.toString()
                        )

                        // Net balance in the user's default currency (balances are never
                        // summed across currencies). Other currencies, if any, show a "+n" hint.
                        val nonZeroNet = accountData.netByCurrency.filterValues { abs(it) > 0.01 }
                        val primaryNet = accountData.netByCurrency[accountData.defaultCurrency]
                            ?: nonZeroNet.entries.maxByOrNull { abs(it.value) }?.value
                            ?: 0.0
                        val balanceColor = when {
                            primaryNet > 0.01 -> OpenSplitTokens.OwedPositive
                            primaryNet < -0.01 -> OpenSplitTokens.OwedNegative
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val otherCurrencies = nonZeroNet.keys.count { it != accountData.defaultCurrency }
                        val balanceStr = com.opensplit.util.CurrencyFormatter.format(
                            primaryNet, accountData.defaultCurrency, showSign = true
                        ) + if (otherCurrencies > 0) " +$otherCurrencies" else ""

                        StatChip(
                            modifier = Modifier.weight(1f),
                            label = "Net balance",
                            value = balanceStr,
                            valueColor = balanceColor
                        )
                    }
                }

                // 3. Preferences Section Card
                item {
                    SectionHeader("Preferences")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column {
                            // Default Currency with anchored DropdownMenu
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val currentFlag = com.opensplit.util.CurrencyFormatter.getCurrencyFlag(accountData.defaultCurrency)
                                val currentSymbol = com.opensplit.util.CurrencyFormatter.getCurrencySymbol(accountData.defaultCurrency)

                                ListItem(
                                    headlineContent = { Text("Default currency", fontWeight = FontWeight.Medium) },
                                    supportingContent = { Text("Default currency for new expenses") },
                                    trailingContent = {
                                        Box {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable { showCurrencyMenu = true }
                                            ) {
                                                Text(
                                                    text = "$currentFlag ${accountData.defaultCurrency} ($currentSymbol)",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceXS))
                                                Icon(OpenSplitIcons.Dropdown, contentDescription = "Select Currency")
                                            }

                                            DropdownMenu(
                                                expanded = showCurrencyMenu,
                                                onDismissRequest = { showCurrencyMenu = false }
                                            ) {
                                                val currencies = listOf("INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD")
                                                currencies.forEach { curr ->
                                                    val flag = com.opensplit.util.CurrencyFormatter.getCurrencyFlag(curr)
                                                    val symbol = com.opensplit.util.CurrencyFormatter.getCurrencySymbol(curr)
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                text = "$flag $curr ($symbol)",
                                                                fontWeight = if (curr == accountData.defaultCurrency) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        },
                                                        onClick = {
                                                            viewModel.updateDefaultCurrency(curr)
                                                            showCurrencyMenu = false
                                                        },
                                                        leadingIcon = if (curr == accountData.defaultCurrency) {
                                                            { Icon(OpenSplitIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                                        } else null
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            HorizontalDivider()

                            // Theme segmented button row
                            ListItem(
                                headlineContent = { Text("Theme", fontWeight = FontWeight.Medium) },
                                trailingContent = {
                                    val options = listOf("Light", "Dark", "System")
                                    val selectedIndex = when (theme.lowercase()) {
                                        "light" -> 0
                                        "dark" -> 1
                                        else -> 2
                                    }
                                    SingleChoiceSegmentedButtonRow {
                                        options.forEachIndexed { index, label ->
                                            SegmentedButton(
                                                selected = index == selectedIndex,
                                                onClick = { viewModel.setTheme(label.lowercase()) },
                                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                                            ) {
                                                Text(label, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            )

                            HorizontalDivider()

                            // Notifications switch
                            ListItem(
                                headlineContent = { Text("Push Notifications", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("Notify on new expenses & settlements") },
                                trailingContent = {
                                    var notifEnabled by remember { mutableStateOf(true) }
                                    Switch(
                                        checked = notifEnabled,
                                        onCheckedChange = {
                                            notifEnabled = it
                                            Toast.makeText(context, if (it) "Notifications enabled" else "Notifications disabled", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                // 4. Permissions Section Card
                item {
                    SectionHeader("System Permissions")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text("Camera Access", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("Scan receipts with AI OCR") },
                                leadingContent = { Icon(OpenSplitIcons.Camera, contentDescription = null) },
                                trailingContent = {
                                    AssistChip(
                                        onClick = { },
                                        label = { Text("Granted") },
                                        leadingIcon = { Icon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            )

                            HorizontalDivider()

                            ListItem(
                                headlineContent = { Text("Storage & Export", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("Save CSV, PDF reports") },
                                leadingContent = { Icon(OpenSplitIcons.Download, contentDescription = null) },
                                trailingContent = {
                                    AssistChip(
                                        onClick = { },
                                        label = { Text("Granted") },
                                        leadingIcon = { Icon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            )

                            HorizontalDivider()

                            ListItem(
                                headlineContent = { Text("Contacts Sync", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("Find friends by phone or email") },
                                leadingContent = { Icon(OpenSplitIcons.Person, contentDescription = null) },
                                trailingContent = {
                                    AssistChip(
                                        onClick = { },
                                        label = { Text("Granted") },
                                        leadingIcon = { Icon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            )
                        }
                    }
                }

                // 5. Your Data Section Card
                item {
                    SectionHeader("Your Data")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text("Export expenses & backup", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("Export to CSV, PDF, or JSON backup format") },
                                leadingContent = { Icon(OpenSplitIcons.Download, contentDescription = null) },
                                trailingContent = { Icon(OpenSplitIcons.ChevronRight, contentDescription = null) },
                                modifier = Modifier.clickable { showExportBottomSheet = true }
                            )

                            HorizontalDivider()

                            ListItem(
                                headlineContent = { Text("Manage contacts & invites", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("${accountData.pendingInvites.size} pending group invitations") },
                                leadingContent = { Icon(OpenSplitIcons.AddMember, contentDescription = null) },
                                trailingContent = { Icon(OpenSplitIcons.ChevronRight, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    Toast.makeText(context, "No pending invitations", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                // 6. Security Section Card
                item {
                    SectionHeader("Security")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text("Change password", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("Send password reset link to your email") },
                                leadingContent = { Icon(OpenSplitIcons.Security, contentDescription = null) },
                                trailingContent = { Icon(OpenSplitIcons.ChevronRight, contentDescription = null) },
                                modifier = Modifier.clickable { showPasswordResetDialog = true }
                            )

                            HorizontalDivider()

                            ListItem(
                                headlineContent = { Text("Linked accounts", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text(if (accountData.user.providerData.any { it.providerId == "google.com" }) "Google Sign-In connected" else "Email / Password account") },
                                leadingContent = { Icon(OpenSplitIcons.Person, contentDescription = null) }
                            )
                        }
                    }
                }

                // 7. Support & About Section Card
                item {
                    SectionHeader("Support & About")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text("Help & feedback", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("Send feedback or bug report") },
                                leadingContent = { Icon(OpenSplitIcons.Info, contentDescription = null) },
                                trailingContent = { Icon(OpenSplitIcons.ChevronRight, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:support@opensplit.app")
                                        putExtra(Intent.EXTRA_SUBJECT, "OpenSplit App Feedback")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            HorizontalDivider()

                            ListItem(
                                headlineContent = { Text("About OpenSplit", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("Version 1.0.0 • Open Source Expense Splitter") },
                                leadingContent = { Icon(OpenSplitIcons.Info, contentDescription = null) }
                            )
                        }
                    }
                }

                // 8. Actions: Sign out & Delete account
                item {
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                    OutlinedButton(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(OpenSplitIcons.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                        Text("Sign out", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))

                    TextButton(
                        onClick = { showDeleteAccountDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete account", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Export Modal Bottom Sheet
            if (showExportBottomSheet) {
                ExportBottomSheet(
                    scopeName = "All Personal Data",
                    expenses = accountData.allExpenses,
                    onDismiss = { showExportBottomSheet = false }
                )
            }
        }
    }

    // Dialogs
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Edit Display Name") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateDisplayName(editName)
                        }
                        showNameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPasswordResetDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordResetDialog = false },
            title = { Text("Reset Password") },
            text = {
                Text("We will send a password reset link to your email address:\n\n${appContainer.authRepository.currentUser?.email}")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = appContainer.authRepository.currentUser?.email
                        if (!email.isNullOrEmpty()) {
                            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_LONG).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                        showPasswordResetDialog = false
                    }
                ) {
                    Text("Send Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out of OpenSplit?") },
            text = { Text("You will need to sign in again to access your groups and balances.") },
            confirmButton = {
                Button(onClick = {
                    showSignOutDialog = false
                    viewModel.signOut()
                    rootNavController.navigate("login") {
                        popUpTo(0)
                    }
                }) {
                    Text("Sign out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        var password by remember { mutableStateOf("") }
        var isReauthenticating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account") },
            text = {
                Column {
                    Text("Warning: Your account and personal data will be removed. Group data is not deleted as it is owned by the group.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Current Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isReauthenticating = true
                        viewModel.reauthenticate(password) { success ->
                            if (success) {
                                viewModel.deleteAccount()
                                showDeleteAccountDialog = false
                                rootNavController.navigate("login") {
                                    popUpTo(0)
                                }
                            } else {
                                isReauthenticating = false
                                Toast.makeText(context, "Re-authentication failed. Please check password.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = password.isNotBlank() && !isReauthenticating
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun StatChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .padding(OpenSplitTokens.SpaceMD)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
            AnimatedContent(
                targetState = value,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "statChipAnimation"
            ) { targetValue ->
                Text(
                    text = targetValue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
            }
        }
    }
}

