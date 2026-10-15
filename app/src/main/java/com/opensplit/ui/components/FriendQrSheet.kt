@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.opensplit.domain.model.InviteLink
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens

/**
 * "My code" shows the user's own add-me QR; "Scan" reads a friend or group code with the
 * Play-services scanner (no camera permission prompt) or a pasted link.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendQrSheet(
    myLinkUrl: String?,
    onDismiss: () -> Unit,
    onOpenLink: (kind: String, token: String) -> Unit
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    var tab by remember { mutableStateOf(0) }
    var pasted by remember { mutableStateOf("") }

    fun handle(text: String) {
        val kind = InviteLink.kindOf(text); val token = InviteLink.tokenFrom(text)
        if (kind != null && token != null) { onDismiss(); onOpenLink(kind, token) }
        else snackbar.showMessage("That's not an OpenSplit friend or group link.")
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OpenSplitTokens.SpaceLG).padding(bottom = OpenSplitTokens.SpaceXL), horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.PrimaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("My code") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Scan") })
            }
            Spacer(Modifier.height(OpenSplitTokens.SpaceLG))
            if (tab == 0) {
                if (myLinkUrl == null) {
                    AppLoadingIndicator()
                } else {
                    val qr = remember(myLinkUrl) { qrBitmap(myLinkUrl, 640) }
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.size(220.dp)) {
                        Image(bitmap = qr.asImageBitmap(), contentDescription = "QR code that adds you as a friend", modifier = Modifier.padding(12.dp))
                    }
                    Spacer(Modifier.height(OpenSplitTokens.SpaceMD))
                    Text("Anyone who scans this and signs in becomes your friend.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(OpenSplitTokens.SpaceMD))
                    Surface(shape = OpenSplitTokens.PillShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp)) {
                            Text(myLinkUrl, style = MaterialTheme.typography.bodySmall, maxLines = 1, modifier = Modifier.weight(1f))
                            IconButton(shapes = IconButtonDefaults.shapes(), onClick = {
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("OpenSplit friend link", myLinkUrl))
                                snackbar.showMessage("Link copied")
                            }) { SymbolIcon(OpenSplitIcons.Copy, contentDescription = "Copy link") }
                            IconButton(shapes = IconButtonDefaults.shapes(), onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Add me on OpenSplit: $myLinkUrl") }
                                context.startActivity(Intent.createChooser(send, "Share your link"))
                            }) { SymbolIcon(OpenSplitIcons.Share, contentDescription = "Share link") }
                        }
                    }
                }
            } else {
                Button(shapes = ButtonDefaults.shapes(), 
                    onClick = {
                        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                        GmsBarcodeScanning.getClient(context, options).startScan()
                            .addOnSuccessListener { code -> code.rawValue?.let { handle(it) } }
                            .addOnFailureListener { snackbar.showMessage("Scanner unavailable: ${it.message ?: "try pasting the link"}") }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    SymbolIcon(OpenSplitIcons.QrCode, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Scan a QR code", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(OpenSplitTokens.SpaceLG))
                Text("or paste a link", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
                OutlinedTextField(value = pasted, onValueChange = { pasted = it }, singleLine = true, placeholder = { Text("https://…/friend/… or /join/…") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
                OutlinedButton(shapes = ButtonDefaults.shapes(), enabled = InviteLink.tokenFrom(pasted) != null, onClick = { handle(pasted) }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
            }
        }
    }
}
