@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.opensplit.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opensplit.domain.model.InviteLink
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.theme.Symbol
import kotlinx.coroutines.launch

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

private class GroupShareTarget(val icon: Symbol, val label: String, val send: (Context, String) -> Unit)

/**
 * Modern dual-tab QR-based sheet for group invites.
 * Tab 0: "Group QR & Link" with large QR code, copy pill, quick-share buttons, email/contact invite.
 * Tab 1: "Scan to Join" with CameraX QR scanner and link paste.
 */
@Composable
fun GroupInviteSheet(
    groupId: String,
    groupName: String,
    inviteLink: InviteLink? = null,
    onDismiss: () -> Unit,
    onSubmitEmail: ((String) -> Unit)? = null,
    onGenerateLink: ((expiresInSeconds: Long?, maxUses: Int) -> Unit)? = null,
    onOpenLink: ((kind: String, token: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    var tab by remember { mutableStateOf(0) }
    var pasted by remember { mutableStateOf("") }

    // Auto-generate invite link if missing
    LaunchedEffect(inviteLink) {
        if (inviteLink == null && onGenerateLink != null) {
            onGenerateLink(null, 0)
        }
    }

    fun handleScanned(text: String) {
        val kind = InviteLink.kindOf(text)
        val token = InviteLink.tokenFrom(text)
        when {
            kind == null || token == null -> snackbar.showMessage("That code isn't an OpenSplit invite.")
            inviteLink != null && token == inviteLink.token -> snackbar.showMessage("That's this group's code. Let another member scan it.")
            else -> {
                snackbar.showMessage(if (kind == "join") "Group invite found, opening…" else "Friend code found, adding…")
                onDismiss()
                onOpenLink?.invoke(kind, token)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = OpenSplitTokens.SpaceLG)
                .padding(bottom = OpenSplitTokens.SpaceXL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Invite to ${groupName.ifBlank { "Group" }}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(OpenSplitTokens.SpaceSM))

            PrimaryTabRow(
                selectedTabIndex = tab,
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("Group QR & Link") },
                    icon = { SymbolIcon(OpenSplitIcons.QrCode, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("Scan code") },
                    icon = { SymbolIcon(OpenSplitIcons.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
            }

            Spacer(Modifier.height(OpenSplitTokens.SpaceLG))

            if (tab == 0) {
                GroupQrTab(
                    groupName = groupName,
                    inviteLink = inviteLink,
                    onGenerateLink = onGenerateLink,
                    onSubmitEmail = onSubmitEmail,
                    onDismiss = onDismiss
                )
            } else {
                GroupScanTab(
                    onScanned = { handleScanned(it) },
                    pasted = pasted,
                    onPasted = { pasted = it },
                    onContinue = { handleScanned(pasted) }
                )
            }
        }
    }
}

@Composable
private fun GroupQrTab(
    groupName: String,
    inviteLink: InviteLink?,
    onGenerateLink: ((expiresInSeconds: Long?, maxUses: Int) -> Unit)?,
    onSubmitEmail: ((String) -> Unit)?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var showAdvancedOptions by remember { mutableStateOf(false) }
    val expiryOptions = listOf("1 hour" to 3600L, "1 day" to 86400L, "7 days" to 7 * 86400L, "30 days" to 30 * 86400L, "Never" to null)
    var expiryChoice by remember { mutableStateOf(4) }
    var singleUse by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null && onSubmitEmail != null) {
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
        else snackbar.showMessage("Contacts permission needed to pick a contact")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (inviteLink == null) {
            InlineBusy("Preparing group invite link…")
            Spacer(Modifier.height(OpenSplitTokens.SpaceMD))
        } else {
            val qr = remember(inviteLink.url) { qrBitmap(inviteLink.url, 640) }
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier.size(232.dp)
            ) {
                Image(
                    bitmap = qr.asImageBitmap(),
                    contentDescription = "QR code for group invite",
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(Modifier.height(OpenSplitTokens.SpaceSM))

            val expiryText = remember(inviteLink.expiresAt, inviteLink.maxUses) {
                val expiryPart = inviteLink.expiresAt?.let {
                    "Valid until " + java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.getDefault()).format(it.toDate())
                } ?: "Never expires"
                val usePart = if (inviteLink.maxUses == 1) " · Single use" else ""
                "$expiryPart$usePart"
            }
            Text(
                text = "Let them scan this code to join $groupName, or share the link.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = expiryText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(OpenSplitTokens.SpaceMD))

            // Link copy pill
            Surface(
                shape = OpenSplitTokens.PillShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = inviteLink.url.removePrefix("https://"),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        shapes = IconButtonDefaults.shapes(),
                        onClick = {
                            runCatching {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("OpenSplit group invite", inviteLink.url))
                            }.onSuccess {
                                snackbar.showMessage("✨ Invite link copied to clipboard!")
                            }.onFailure {
                                snackbar.showMessage("Couldn't copy link")
                            }
                        }
                    ) {
                        SymbolIcon(OpenSplitIcons.ContentCopy, contentDescription = "Copy link")
                    }
                }
            }

            Spacer(Modifier.height(OpenSplitTokens.SpaceMD))

            // Share Targets
            Text(
                text = "Share via",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(OpenSplitTokens.SpaceSM))

            val shareMessage = "Join our group \"$groupName\" on OpenSplit to easily split expenses: ${inviteLink.url}"
            val targets = remember(shareMessage) {
                listOf(
                    GroupShareTarget(OpenSplitIcons.Sms, "SMS") { c, m ->
                        c.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", m))
                    },
                    GroupShareTarget(OpenSplitIcons.Whatsapp, "WhatsApp") { c, m ->
                        c.startActivity(Intent(Intent.ACTION_SEND).setType("text/plain").setPackage("com.whatsapp").putExtra(Intent.EXTRA_TEXT, m))
                    },
                    GroupShareTarget(OpenSplitIcons.Mail, "Email") { c, m ->
                        c.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT, "Join $groupName on OpenSplit").putExtra(Intent.EXTRA_TEXT, m))
                    },
                    GroupShareTarget(OpenSplitIcons.Send, "Telegram") { c, m ->
                        c.startActivity(Intent(Intent.ACTION_SEND).setType("text/plain").setPackage("org.telegram.messenger").putExtra(Intent.EXTRA_TEXT, m))
                    },
                    GroupShareTarget(OpenSplitIcons.Share, "More") { c, m ->
                        c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, m), "Share invite link"))
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                targets.forEach { t ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                try {
                                    t.send(context, shareMessage)
                                    snackbar.showMessage("Opening ${t.label}…")
                                } catch (e: ActivityNotFoundException) {
                                    snackbar.showMessage("${t.label} is not installed")
                                } catch (e: Exception) {
                                    snackbar.showMessage("Couldn't share via ${t.label}")
                                }
                            }
                            .padding(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SymbolIcon(
                                    t.icon,
                                    contentDescription = "Share via ${t.label}",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(t.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Direct Email Invite
        if (onSubmitEmail != null) {
            Spacer(Modifier.height(OpenSplitTokens.SpaceMD))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(Modifier.height(OpenSplitTokens.SpaceSM))

            var emailInput by remember { mutableStateOf("") }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Invite directly by email", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val trimmed = emailInput.trim()
                            if (trimmed.isNotBlank() && trimmed.contains("@")) {
                                onSubmitEmail(trimmed)
                                emailInput = ""
                                onDismiss()
                            } else {
                                snackbar.showMessage("Please enter a valid email address")
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    shapes = IconButtonDefaults.shapes(),
                    onClick = { contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS) }
                ) {
                    SymbolIcon(
                        symbol = OpenSplitIcons.Contacts,
                        contentDescription = "Pick Contact",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    shapes = IconButtonDefaults.shapes(),
                    onClick = {
                        val trimmed = emailInput.trim()
                        if (trimmed.isNotBlank() && trimmed.contains("@")) {
                            onSubmitEmail(trimmed)
                            emailInput = ""
                            onDismiss()
                        } else {
                            snackbar.showMessage("Please enter a valid email address")
                        }
                    },
                    enabled = emailInput.isNotBlank()
                ) {
                    SymbolIcon(
                        symbol = OpenSplitIcons.Send,
                        contentDescription = "Send Invite",
                        tint = if (emailInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
        }

        // Optional Link Expiry / Customization
        if (onGenerateLink != null) {
            Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = { showAdvancedOptions = !showAdvancedOptions }
            ) {
                SymbolIcon(
                    if (showAdvancedOptions) OpenSplitIcons.ArrowUpward else OpenSplitIcons.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (showAdvancedOptions) "Hide link settings" else "Customize link expiry & uses")
            }

            if (showAdvancedOptions) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Link expires in",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        expiryOptions.forEachIndexed { i, (label, _) ->
                            FilterChip(
                                selected = expiryChoice == i,
                                onClick = { expiryChoice = i },
                                label = { Text(label) }
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Single use link", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = singleUse, onCheckedChange = { singleUse = it })
                    }
                    Button(
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            onGenerateLink(expiryOptions[expiryChoice].second, if (singleUse) 1 else 0)
                            showAdvancedOptions = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SymbolIcon(OpenSplitIcons.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Generate New Link")
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupScanTab(
    onScanned: (String) -> Unit,
    pasted: String,
    onPasted: (String) -> Unit,
    onContinue: () -> Unit
) {
    QrScannerView(onScanned = onScanned, modifier = Modifier.fillMaxWidth().height(300.dp))
    Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
    Text(
        "Point the camera at an OpenSplit invite code",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(OpenSplitTokens.SpaceLG))
    Text(
        "Got an invite link instead?",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
    OutlinedTextField(
        value = pasted,
        onValueChange = onPasted,
        singleLine = true,
        placeholder = { Text("Paste invite link here") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
    OutlinedButton(
        shapes = ButtonDefaults.shapes(),
        enabled = InviteLink.tokenFrom(pasted) != null,
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Continue")
    }
}

private suspend fun resolveContactEmail(context: Context, contactUri: Uri): String? =
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
