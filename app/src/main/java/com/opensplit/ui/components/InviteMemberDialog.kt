package com.opensplit.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import kotlinx.coroutines.launch

/**
 * The one "add / invite a person" dialog used everywhere in the app — group members, expense
 * split-between, and friends — so the flow is identical wherever you invite someone.
 */
@Composable
fun InviteMemberDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onSubmitEmail: (String) -> Unit,
    shareMessage: String = "Join me on OpenSplit to split expenses easily!",
    inviteUrl: String = "https://opensplit.app/invite"
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    val scope = rememberCoroutineScope()

    val fullInviteMessage = remember(shareMessage, inviteUrl) {
        "$shareMessage\n\nJoin link: $inviteUrl"
    }

    fun copyToClipboard(text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("OpenSplit Invite Link", text)
            clipboard.setPrimaryClip(clip)
            snackbar.showMessage("✨ Invite link copied to clipboard!")
        } catch (e: Exception) {
            snackbar.showMessage("Failed to copy link")
        }
    }

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
                    snackbar.showMessage("That contact has no email address on file")
                }
            }
        }
    }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contactPickerLauncher.launch(null)
        else snackbar.showMessage("Contacts permission is needed to pick a person")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = OpenSplitIcons.AddMember,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Default Invite Link Box with Copy Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = OpenSplitIcons.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = inviteUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { copyToClipboard(inviteUrl) }
                        ) {
                            Icon(
                                imageVector = OpenSplitIcons.Copy,
                                contentDescription = "Copy Invite Link",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                InviteChannelRow(
                    icon = OpenSplitIcons.Copy,
                    label = "Copy Link",
                    subtitle = "Copy invite link to clipboard"
                ) {
                    copyToClipboard(inviteUrl)
                }

                InviteChannelRow(
                    icon = OpenSplitIcons.Contacts,
                    label = "Contacts",
                    subtitle = "Pick a person from your contacts"
                ) {
                    contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                }

                InviteChannelRow(
                    icon = OpenSplitIcons.Whatsapp,
                    label = "WhatsApp",
                    subtitle = "Share invite message with link"
                ) {
                    val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, fullInviteMessage)
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(whatsappIntent)
                    } catch (e: Exception) {
                        try {
                            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, fullInviteMessage)
                            }
                            context.startActivity(Intent.createChooser(fallbackIntent, "Share invite link via"))
                        } catch (_: Exception) {
                            snackbar.showMessage("WhatsApp or messaging app not installed")
                        }
                    }
                    onDismiss()
                }

                InviteChannelRow(
                    icon = OpenSplitIcons.Sms,
                    label = "SMS",
                    subtitle = "Send a text invite with link"
                ) {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                                putExtra("sms_body", fullInviteMessage)
                            }
                        )
                    } catch (e: Exception) {
                        snackbar.showMessage("No messaging app found")
                    }
                    onDismiss()
                }
            }
        },
        confirmButton = {},
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
