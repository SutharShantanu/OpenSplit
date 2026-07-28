package com.opensplit.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import kotlinx.coroutines.launch

/**
 * Single "add / invite a person" dialog used everywhere in the app (group members, expense
 * split-between, friends) so the visual language stays identical. Always offers email entry +
 * picking a contact; [showShareChannels] additionally offers WhatsApp/SMS for contexts that
 * aren't bound to a specific group (those channels can't carry a group-scoped pending invite,
 * only a generic "come join OpenSplit" message).
 */
@Composable
fun InviteMemberDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onSubmitEmail: (String) -> Unit,
    confirmLabel: String = "Add member",
    showShareChannels: Boolean = false,
    shareMessage: String = "Join me on OpenSplit to split expenses easily!"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val resolved = resolveContactEmail(context, uri)
                if (resolved != null) {
                    onSubmitEmail(resolved)
                    onDismiss()
                } else {
                    Toast.makeText(context, "That contact has no email address on file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contactPickerLauncher.launch(null)
        else Toast.makeText(context, "Contacts permission is needed to pick a person", Toast.LENGTH_SHORT).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(OpenSplitIcons.AddMember, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                InviteChannelRow(
                    icon = OpenSplitIcons.Contacts,
                    label = "Contacts",
                    subtitle = "Pick a person from your contacts"
                ) {
                    contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                }
                if (showShareChannels) {
                    InviteChannelRow(
                        icon = OpenSplitIcons.Whatsapp,
                        label = "WhatsApp",
                        subtitle = "Share an invite message"
                    ) {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    setPackage("com.whatsapp")
                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                }
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                    InviteChannelRow(
                        icon = OpenSplitIcons.Sms,
                        label = "SMS",
                        subtitle = "Send a text invite"
                    ) {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                                    putExtra("sms_body", shareMessage)
                                }
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "No messaging app found", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitEmail(email.trim())
                    onDismiss()
                },
                enabled = email.trim().contains("@")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun InviteChannelRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = { Text(label, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}

/** Looks up the primary email address for a contact picked via ContactsContract.CONTENT_TYPE. */
private suspend fun resolveContactEmail(context: android.content.Context, contactUri: Uri): String? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val contactId = context.contentResolver.query(
            contactUri, arrayOf(ContactsContract.Contacts._ID), null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: return@withContext null

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }
