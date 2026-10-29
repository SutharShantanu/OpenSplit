@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.util.CrashLog

/** Shown on the launch after a crash: lets the user copy the report and file an issue. */
@Composable
fun CrashReportDialog() {
    val context = LocalContext.current
    var report by remember { mutableStateOf(CrashLog.pending(context)) }
    val text = report ?: return
    val dismiss = { CrashLog.clear(context); report = null }
    val snackbar = LocalSnackbarController.current

    AlertDialog(
        onDismissRequest = dismiss,
        icon = { SymbolIcon(OpenSplitIcons.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("OpenSplit closed unexpectedly") },
        text = {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(
                    text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = {
                context.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("OpenSplit crash report", text))
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ISSUES_URL)))
                }
                snackbar.showMessage("Crash report copied: paste it into the issue")
                dismiss()
            }) { Text("Copy & report") }
        },
        dismissButton = {
            TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = dismiss) { Text("Dismiss") }
        }
    )
}

private const val ISSUES_URL = "https://github.com/SutharShantanu/OpenSplit/issues/new"
