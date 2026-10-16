@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.opensplit.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opensplit.domain.model.FriendLink
import com.opensplit.domain.model.InviteLink
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.theme.Symbol

/** One way to share an invite: a fixed-package app, or the system chooser. */
private class ShareTarget(val icon: Symbol, val label: String, val send: (Context, String) -> Unit)

/**
 * The single "invite a friend" surface. Tab 1 shows the user's QR code, link (copy) and a short
 * share row; tab 2 scans another phone's code with the camera. Every action reports success or
 * failure through the snackbar, and scanned links land on [onOpenLink] (AcceptFriendScreen shows
 * the outcome).
 */
@Composable
fun InviteFriendSheet(
    link: FriendLink?,
    linkError: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOpenLink: (kind: String, token: String) -> Unit
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    var tab by remember { mutableStateOf(0) }
    var pasted by remember { mutableStateOf("") }

    fun handleScanned(text: String) {
        val kind = InviteLink.kindOf(text); val token = InviteLink.tokenFrom(text)
        when {
            kind == null || token == null -> snackbar.showMessage("That code isn't an OpenSplit invite.")
            link != null && token == link.token -> snackbar.showMessage("That's your own code. Let the other person scan it.")
            else -> { snackbar.showMessage(if (kind == "join") "Group invite found, opening…" else "Friend code found, adding…"); onDismiss(); onOpenLink(kind, token) }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OpenSplitTokens.SpaceLG).padding(bottom = OpenSplitTokens.SpaceXL), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Add a friend", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
            androidx.compose.material3.PrimaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("My invite") }, icon = { SymbolIcon(OpenSplitIcons.QrCode, contentDescription = null, modifier = Modifier.size(20.dp)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Scan theirs") }, icon = { SymbolIcon(OpenSplitIcons.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp)) })
            }
            Spacer(Modifier.height(OpenSplitTokens.SpaceLG))
            if (tab == 0) MyInviteTab(link, linkError, onRetry) else ScanTab(
                onScanned = { handleScanned(it) },
                pasted = pasted, onPasted = { pasted = it }, onContinue = { handleScanned(pasted) }
            )
        }
    }
}

@Composable
private fun MyInviteTab(link: FriendLink?, linkError: String?, onRetry: () -> Unit) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    when {
        linkError != null -> {
            Text("Couldn't create your invite link: $linkError", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(Modifier.height(OpenSplitTokens.SpaceMD))
            OutlinedButton(shapes = ButtonDefaults.shapes(), onClick = onRetry) { Text("Try again") }
        }
        link == null -> InlineBusy("Preparing your invite…")
        else -> {
            val qr = remember(link.url) { qrBitmap(link.url, 640) }
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.size(232.dp)) {
                Image(bitmap = qr.asImageBitmap(), contentDescription = "QR code that adds you as a friend", modifier = Modifier.padding(14.dp))
            }
            Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
            val expiry = remember(link.expiresAt) { java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(link.expiresAt.toDate()) }
            Text("Let them scan this, or send the link. Valid until $expiry.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(OpenSplitTokens.SpaceMD))
            Surface(shape = OpenSplitTokens.PillShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp)) {
                    Text(link.url.removePrefix("https://"), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = {
                        runCatching {
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("OpenSplit friend link", link.url))
                        }.onSuccess { snackbar.showMessage("Link copied") }.onFailure { snackbar.showMessage("Couldn't copy the link") }
                    }) { SymbolIcon(OpenSplitIcons.ContentCopy, contentDescription = "Copy link") }
                }
            }
            Spacer(Modifier.height(OpenSplitTokens.SpaceLG))
            Text("Share via", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
            val message = "Add me on OpenSplit so we can split expenses: ${link.url}"
            val targets = remember {
                listOf(
                    ShareTarget(OpenSplitIcons.Sms, "SMS") { c, m -> c.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", m)) },
                    ShareTarget(OpenSplitIcons.Whatsapp, "WhatsApp") { c, m -> c.startActivity(Intent(Intent.ACTION_SEND).setType("text/plain").setPackage("com.whatsapp").putExtra(Intent.EXTRA_TEXT, m)) },
                    ShareTarget(OpenSplitIcons.Mail, "Email") { c, m -> c.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT, "Join me on OpenSplit").putExtra(Intent.EXTRA_TEXT, m)) },
                    ShareTarget(OpenSplitIcons.Send, "Telegram") { c, m -> c.startActivity(Intent(Intent.ACTION_SEND).setType("text/plain").setPackage("org.telegram.messenger").putExtra(Intent.EXTRA_TEXT, m)) },
                    ShareTarget(OpenSplitIcons.Share, "More") { c, m -> c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, m), "Share your invite")) }
                )
            }
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                targets.forEach { t ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                        try { t.send(context, message); snackbar.showMessage("Opening ${t.label}…") }
                        catch (e: ActivityNotFoundException) { snackbar.showMessage("${t.label} isn't installed on this phone") }
                        catch (e: Exception) { snackbar.showMessage("Couldn't share via ${t.label}") }
                    }.padding(6.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(52.dp)) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                SymbolIcon(t.icon, contentDescription = "Share via ${t.label}", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(t.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanTab(onScanned: (String) -> Unit, pasted: String, onPasted: (String) -> Unit, onContinue: () -> Unit) {
    // Live camera preview inside the sheet; decodes as soon as a QR is in view.
    QrScannerView(onScanned = onScanned, modifier = Modifier.fillMaxWidth().height(300.dp))
    Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
    Text("Point the camera at your friend's invite code", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(OpenSplitTokens.SpaceLG))
    Text("Got a link instead?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
    OutlinedTextField(value = pasted, onValueChange = onPasted, singleLine = true, placeholder = { Text("Paste the invite link") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
    OutlinedButton(shapes = ButtonDefaults.shapes(), enabled = InviteLink.tokenFrom(pasted) != null, onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
}
