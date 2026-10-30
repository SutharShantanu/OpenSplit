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
        Column(
            modifier = Modifier
                .responsiveDialogWidth(maxWidth = 560.dp)
                .padding(horizontal = OpenSplitTokens.SpaceLG)
                .padding(bottom = OpenSplitTokens.SpaceXL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Add a friend", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
            PillTabs(
                selected = tab,
                labels = listOf("My invite", "Scan theirs"),
                icons = listOf(OpenSplitIcons.QrCode, OpenSplitIcons.CameraAlt),
                onSelect = { tab = it }
            )
            Spacer(Modifier.height(OpenSplitTokens.SpaceLG))
            PillTabContent(tab) { t ->
                if (t == 0) MyInviteTab(link, linkError, onRetry) else ScanTab(
                    onScanned = { handleScanned(it) },
                    pasted = pasted, onPasted = { pasted = it }, onContinue = { handleScanned(pasted) }
                )
            }
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
            OutlinedButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = onRetry) { Text("Try again") }
        }
        link == null -> InlineBusy("Preparing your invite…")
        else -> {
            val qr = remember(link.url) { qrBitmap(link.url, 640) }
            Surface(shape = MaterialTheme.shapes.large, color = Color.White, modifier = Modifier.size(232.dp)) {
                Image(bitmap = qr.asImageBitmap(), contentDescription = "QR code that adds you as a friend", modifier = Modifier.padding(14.dp))
            }
            Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
            val expiry = remember(link.expiresAt) { java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(link.expiresAt.toDate()) }
            Text("Let them scan this, or send the link. Valid until $expiry.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(OpenSplitTokens.SpaceMD))
            Surface(shape = OpenSplitTokens.PillShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp)) {
                    Text(link.url.removePrefix("https://"), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(shapes = com.opensplit.ui.theme.appIconButtonShapes(), onClick = {
                        runCatching {
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("OpenSplit friend link", link.url))
                        }.onSuccess { snackbar.showMessage("Link copied") }.onFailure { snackbar.showMessage("Couldn't copy the link") }
                    }) { SymbolIcon(OpenSplitIcons.ContentCopy, contentDescription = "Copy link") }
                }
            }
            Spacer(Modifier.height(OpenSplitTokens.SpaceLG))
            ShareViaRow(
                message = "Add me on OpenSplit so we can split expenses: ${link.url}",
                subject = "Join me on OpenSplit",
                chooserTitle = "Share your invite"
            )
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
    OutlinedButton(shapes = com.opensplit.ui.theme.appButtonShapes(), enabled = InviteLink.tokenFrom(pasted) != null, onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
}
