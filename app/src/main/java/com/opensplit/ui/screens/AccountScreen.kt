package com.opensplit.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
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
import kotlinx.coroutines.launch

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
                                    val notifEnabled by viewModel.notificationsEnabledFlow.collectAsState(initial = true)
                                    Switch(
                                        checked = notifEnabled,
                                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
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
                            PermissionListItem(
                                title = "Camera Access",
                                subtitle = "Scan receipts with AI OCR",
                                icon = OpenSplitIcons.Camera,
                                permission = android.Manifest.permission.CAMERA
                            )

                            HorizontalDivider()

                            PermissionListItem(
                                title = "Contacts Sync",
                                subtitle = "Find friends by phone or email",
                                icon = OpenSplitIcons.Contacts,
                                permission = android.Manifest.permission.READ_CONTACTS
                            )

                            HorizontalDivider()

                            PermissionListItem(
                                title = "Notifications",
                                subtitle = "Get notified about new activity",
                                icon = OpenSplitIcons.Activity,
                                permission = android.Manifest.permission.POST_NOTIFICATIONS
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
                                supportingContent = { Text("Verify by email, then set a new password in-app") },
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
        var isSending by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPasswordResetDialog = false },
            title = { Text("Change Password") },
            text = {
                Text(
                    "We'll email a secure link to ${appContainer.authRepository.currentUser?.email}. " +
                        "Opening it brings you back into the app to set a new password (with a strength check) — the link expires after a while, so use it soon."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = appContainer.authRepository.currentUser?.email
                        if (!email.isNullOrEmpty()) {
                            isSending = true
                            viewModel.sendPasswordResetEmail(email) { result ->
                                isSending = false
                                Toast.makeText(
                                    context,
                                    if (result.isSuccess) "Email sent — check your inbox" else "Failed: ${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        showPasswordResetDialog = false
                    },
                    enabled = !isSending
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
        var isGoogleVerified by remember { mutableStateOf(false) }
        val deleteScope = rememberCoroutineScope()
        val isGoogleUser = appContainer.authRepository.currentUser?.providerData?.any { it.providerId == "google.com" } == true

        fun verifyWithGoogle() {
            deleteScope.launch {
                isReauthenticating = true
                val serverClientId = runCatching {
                    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                    if (resId == 0) null else context.getString(resId)
                }.getOrNull()
                if (serverClientId.isNullOrBlank()) {
                    isReauthenticating = false
                    Toast.makeText(context, "Google sign-in isn't configured for this build.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val credentialManager = CredentialManager.create(context)
                suspend fun request(option: CredentialOption): GoogleIdTokenCredential? {
                    val req = GetCredentialRequest.Builder().addCredentialOption(option).build()
                    val credential = credentialManager.getCredential(context, req).credential
                    return when {
                        credential is GoogleIdTokenCredential -> credential
                        credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ->
                            GoogleIdTokenCredential.createFrom(credential.data)
                        else -> null
                    }
                }
                try {
                    val option = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(true)
                        .setAutoSelectEnabled(false)
                        .setServerClientId(serverClientId)
                        .build()
                    val credential = try {
                        request(option)
                    } catch (e: NoCredentialException) {
                        request(GetSignInWithGoogleOption.Builder(serverClientId).build())
                    }
                    if (credential != null) {
                        viewModel.reauthenticateWithGoogle(credential.idToken) { success ->
                            isReauthenticating = false
                            if (success) {
                                isGoogleVerified = true
                            } else {
                                Toast.makeText(context, "Verification failed. Try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        isReauthenticating = false
                        Toast.makeText(context, "Unexpected credential type from Google.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    isReauthenticating = false
                    Toast.makeText(context, "Google verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account") },
            text = {
                Column {
                    Text(
                        "This permanently deletes your account and personal data — this can't be undone. " +
                            "Group data stays intact since it's owned by the group, but you'll lose access to it.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceMD))
                    OutlinedButton(
                        onClick = {
                            showDeleteAccountDialog = false
                            showExportBottomSheet = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(OpenSplitIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                        Text("Export your data first")
                    }
                    Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceLG))

                    if (isGoogleUser) {
                        if (isGoogleVerified) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    OpenSplitIcons.Success,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceXS))
                                Text("Identity verified", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Text(
                                "You signed in with Google, so confirm it's you that way:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
                            OutlinedButton(
                                onClick = { verifyWithGoogle() },
                                enabled = !isReauthenticating,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isReauthenticating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                                    Text("Verifying...")
                                } else {
                                    Text("Verify with Google")
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Current Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isGoogleUser) {
                            viewModel.deleteAccount()
                            showDeleteAccountDialog = false
                            rootNavController.navigate("login") {
                                popUpTo(0)
                            }
                        } else {
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
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = if (isGoogleUser) isGoogleVerified else password.isNotBlank() && !isReauthenticating
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

@Composable
private fun PermissionListItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    permission: String
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> granted = isGranted }
    var showRevokeDialog by remember { mutableStateOf(false) }

    // Android has no API for an app to revoke its own permission, and the user may flip it
    // in system Settings and come straight back here — re-check on every resume.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, permission) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            if (granted) {
                AssistChip(
                    onClick = { showRevokeDialog = true },
                    label = { Text("Granted") },
                    leadingIcon = { Icon(OpenSplitIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            } else {
                AssistChip(
                    onClick = { launcher.launch(permission) },
                    label = { Text("Grant") }
                )
            }
        }
    )

    if (showRevokeDialog) {
        AlertDialog(
            onDismissRequest = { showRevokeDialog = false },
            title = { Text("Revoke $title?") },
            text = {
                Text("If you revoke this permission, you won't be able to: $subtitle.\n\nAndroid doesn't let apps turn off their own permissions — you'll be taken to system Settings to do it there.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRevokeDialog = false
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

