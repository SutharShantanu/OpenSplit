package com.opensplit.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.theme.Symbol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One way to share an invite; [intent] builds the launch intent for a message. */
private class ShareTarget(val icon: Symbol, val label: String, val intent: (String) -> Intent)

private fun textIntent(message: String) = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message)

/**
 * "Share via" label and a row of share targets (SMS, WhatsApp, Email, Telegram, More). Each target
 * shows the real icon of the app that handles it on this phone, falling back to a symbol when the
 * app isn't installed or no default is set. Outcomes are reported through the snackbar.
 */
@Composable
fun ShareViaRow(message: String, subject: String, chooserTitle: String) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    val targets = remember(subject, chooserTitle) {
        listOf(
            // Straight to the default SMS app; otherwise other "smsto:" handlers (WhatsApp, carrier apps) force a chooser.
            ShareTarget(OpenSplitIcons.Sms, "SMS") {
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", it)
                    .setPackage(android.provider.Telephony.Sms.getDefaultSmsPackage(context))
            },
            ShareTarget(OpenSplitIcons.Whatsapp, "WhatsApp") { textIntent(it).setPackage("com.whatsapp") },
            ShareTarget(OpenSplitIcons.Mail, "Email") { Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, it) },
            ShareTarget(OpenSplitIcons.Send, "Telegram") { textIntent(it).setPackage("org.telegram.messenger") },
            ShareTarget(OpenSplitIcons.Share, "More") { Intent.createChooser(textIntent(it), chooserTitle) }
        )
    }
    val appIcons by produceState(List<ImageBitmap?>(targets.size) { null }, targets) {
        value = withContext(Dispatchers.IO) { targets.map { appIconFor(context, it.intent("")) } }
    }

    Text("Share via", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
        targets.forEachIndexed { i, t ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                try { context.startActivity(t.intent(message)); snackbar.showMessage("Opening ${t.label}…") }
                catch (e: ActivityNotFoundException) { snackbar.showMessage("${t.label} isn't installed on this phone") }
                catch (e: Exception) { snackbar.showMessage("Couldn't share via ${t.label}") }
            }.padding(6.dp)) {
                val icon = appIcons[i]
                if (icon != null) {
                    Image(bitmap = icon, contentDescription = "Share via ${t.label}", modifier = Modifier.size(52.dp))
                } else {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(52.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            SymbolIcon(t.icon, contentDescription = "Share via ${t.label}", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(t.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Launcher icon of the default app for [intent]; null for the system chooser or when nothing handles it. */
private fun appIconFor(context: Context, intent: Intent): ImageBitmap? = runCatching {
    if (intent.action == Intent.ACTION_CHOOSER) return null
    val pm = context.packageManager
    val info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
    // "android" is the disambiguation dialog shown when no default app is set.
    if (info.activityInfo.packageName == "android") return null
    info.activityInfo.applicationInfo.loadIcon(pm).toBitmap(144, 144).asImageBitmap()
}.getOrNull()
