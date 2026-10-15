@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.components

import com.opensplit.ui.theme.Symbol
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
import androidx.compose.ui.graphics.asImageBitmap
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
    /** Current group invite link, when inviting into a group. */
    inviteLink: com.opensplit.domain.model.InviteLink? = null,
    /** Creates a link that lasts [expiresInSeconds] (null = never) with [maxUses] (0 = unlimited). */
    onGenerateLink: ((expiresInSeconds: Long?, maxUses: Int) -> Unit)? = null,
    /** Personal "add me" link when inviting a friend rather than into a group. */
    friendInviteUrl: String? = null
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    val scope = rememberCoroutineScope()
    val inviteUrl = inviteLink?.url ?: friendInviteUrl ?: ""

    val fullInviteMessage = remember(shareMessage, inviteUrl) {
        "$shareMessage\n\nJoin link: $inviteUrl"
    }
    val expiryOptions = listOf("1 hour" to 3600L, "1 day" to 86400L, "7 days" to 7 * 86400L, "30 days" to 30 * 86400L, "Never" to null)
    var expiryChoice by remember { mutableStateOf(2) }
    var singleUse by remember { mutableStateOf(false) }

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
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        icon = {
            SymbolIcon(
                symbol = OpenSplitIcons.Invite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (onGenerateLink != null) {
                    // Expiry picker + generate; the QR encodes the live link.
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Link expires in", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            expiryOptions.forEachIndexed { i, (label, _) ->
                                FilterChip(selected = expiryChoice == i, onClick = { expiryChoice = i }, label = { Text(label) })
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Single use", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(checked = singleUse, onCheckedChange = { singleUse = it })
                        }
                        Button(shapes = ButtonDefaults.shapes(), 
                            onClick = { onGenerateLink(expiryOptions[expiryChoice].second, if (singleUse) 1 else 0) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SymbolIcon(OpenSplitIcons.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (inviteLink == null) "Create invite link" else "Create a new link")
                        }
                    }
                }

                if (inviteUrl.isNotBlank()) {
                    val qr = remember(inviteUrl) { qrBitmap(inviteUrl, 512) }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(180.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = "QR code for the invite link",
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    val expiryText = when {
                        inviteLink == null -> "Scan to add you as a friend"
                        inviteLink.expiresAt != null -> "Expires " + java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(inviteLink.expiresAt.toDate())
                        else -> "Never expires"
                    }
                    Text(
                        expiryText + if (inviteLink?.maxUses == 1) " · single use" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Default Invite Link Box with Copy Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            SymbolIcon(
                                symbol = OpenSplitIcons.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = inviteUrl,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(shapes = IconButtonDefaults.shapes(), 
                            onClick = { copyToClipboard(inviteUrl) }
                        ) {
                            SymbolIcon(
                                symbol = OpenSplitIcons.Copy,
                                contentDescription = "Copy Invite Link",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Quick Share Channels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // WhatsApp
                    IconButton(shapes = IconButtonDefaults.shapes(), 
                        onClick = {
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
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                SymbolIcon(OpenSplitIcons.Whatsapp, contentDescription = "WhatsApp", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }

                    // SMS
                    IconButton(shapes = IconButtonDefaults.shapes(), 
                        onClick = {
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
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                SymbolIcon(OpenSplitIcons.Sms, contentDescription = "SMS", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }

                    // Contacts
                    IconButton(shapes = IconButtonDefaults.shapes(), 
                        onClick = { contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS) }
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                SymbolIcon(OpenSplitIcons.Contacts, contentDescription = "Contacts", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }

                    // System Share
                    IconButton(shapes = IconButtonDefaults.shapes(), 
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, fullInviteMessage)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share invite link"))
                            onDismiss()
                        }
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                SymbolIcon(OpenSplitIcons.Share, contentDescription = "Share", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun InviteChannelRow(
    icon: Symbol,
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
                    SymbolIcon(
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

/** Renders [text] as a QR code bitmap of [size] px using ZXing. */
fun qrBitmap(text: String, size: Int): android.graphics.Bitmap {
    val hints = mapOf(com.google.zxing.EncodeHintType.MARGIN to 1)
    val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(text, com.google.zxing.BarcodeFormat.QR_CODE, size, size, hints)
    val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size) {
        bmp.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    }
    return bmp
}
