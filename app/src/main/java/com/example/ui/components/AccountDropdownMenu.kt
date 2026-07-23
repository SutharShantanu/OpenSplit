package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.OpenSplitIcons
import com.example.ui.theme.OpenSplitTokens
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.width(260.dp)
    ) {
        // 1. Identity header row (non-clickable)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OpenSplitTokens.SpaceLG, vertical = OpenSplitTokens.SpaceMD)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val photoUrl = currentUser?.photoUrl
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initials = currentUser?.displayName?.firstOrNull()?.toString()?.uppercase()
                        ?: currentUser?.email?.firstOrNull()?.toString()?.uppercase()
                        ?: "?"
                    val hash = currentUser?.uid?.hashCode() ?: 0
                    val hue = abs(hash % 360).toFloat()
                    val avatarColor = Color.hsv(hue, 0.5f, 0.8f)

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceMD))

                Column {
                    Text(
                        text = currentUser?.displayName ?: "OpenSplit User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider()

        // 2. My Account
        DropdownMenuItem(
            text = { Text("My account") },
            leadingIcon = { Icon(OpenSplitIcons.Account, contentDescription = null) },
            onClick = {
                onDismissRequest()
                onNavigateToAccount()
            }
        )

        // 3. Theme
        DropdownMenuItem(
            text = { Text("Theme") },
            leadingIcon = { Icon(OpenSplitIcons.More, contentDescription = null) },
            onClick = {
                onDismissRequest()
                showThemeDialog = true
            }
        )

        HorizontalDivider()

        // 4. Help & feedback
        DropdownMenuItem(
            text = { Text("Help & feedback") },
            leadingIcon = { Icon(OpenSplitIcons.CategoryOther, contentDescription = null) },
            onClick = {
                onDismissRequest()
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@opensplit.app")
                    putExtra(Intent.EXTRA_SUBJECT, "OpenSplit App Feedback")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // 5. About
        DropdownMenuItem(
            text = { Text("About OpenSplit") },
            leadingIcon = { Icon(OpenSplitIcons.ReceiptScan, contentDescription = null) },
            onClick = {
                onDismissRequest()
                showAboutDialog = true
            }
        )

        HorizontalDivider()

        // 6. Sign out
        DropdownMenuItem(
            text = { Text("Sign out", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(OpenSplitIcons.Leave, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismissRequest()
                showSignOutDialog = true
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
                    onSignOut()
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

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About OpenSplit") },
            text = {
                Text("OpenSplit v1.0.0\n\nA modern, real-time expense splitter built with Jetpack Compose, Material 3, and Firebase.\n\nKeep track of shared expenses, balances, settlements, and export financial summaries seamlessly.")
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Text("Manage theme preferences under My Account -> Preferences for light, dark, or system default mode.")
            },
            confirmButton = {
                Button(onClick = {
                    showThemeDialog = false
                    onNavigateToAccount()
                }) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
