@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.opensplit.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.data.update.AppUpdater
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import kotlinx.coroutines.launch

/** Provided once in MainActivity so Settings can trigger a manual check. */
val LocalAppUpdater = staticCompositionLocalOf<AppUpdater> {
    error("No AppUpdater provided")
}

/**
 * Modal bottom sheet shown whenever [AppUpdater.state] holds a release: offers to download and
 * install it, shows an expressive wavy progress bar while downloading, then an Install button.
 */
@Composable
fun UpdateSheet(updater: AppUpdater) {
    val state by updater.state.collectAsState()
    val release = when (val s = state) {
        is AppUpdater.State.Available -> s.release
        is AppUpdater.State.Downloading -> s.release
        is AppUpdater.State.Downloaded -> s.release
        else -> null
    } ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloading = state is AppUpdater.State.Downloading

    ModalBottomSheet(
        onDismissRequest = { if (!downloading) updater.dismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = OpenSplitTokens.ShapeBrandCard, topEnd = OpenSplitTokens.ShapeBrandCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .responsiveContentWidth(maxWidth = 560.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "Update available",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val size = if (release.apkBytes > 0) " · ${release.apkBytes / 1_048_576} MB" else ""
                    Text(
                        "${release.tag}$size",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (release.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(OpenSplitTokens.ShapeLarge),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        release.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    )
                }
            }

            when (val s = state) {
                is AppUpdater.State.Downloading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val progress = s.progress
                        if (progress == null) {
                            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            if (progress == null) "Downloading…" else "Downloading… ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is AppUpdater.State.Downloaded -> {
                    Button(
                        shapes = ButtonDefaults.shapes(),
                        onClick = { updater.install(s.apk) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Install") }
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(shapes = ButtonDefaults.shapes(), onClick = { updater.dismiss() }) { Text("Later") }
                        Button(
                            shapes = ButtonDefaults.shapes(),
                            onClick = { scope.launch { updater.downloadAndInstall(release) } },
                            modifier = Modifier.weight(1f)
                        ) { Text("Download & install") }
                    }
                }
            }

            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl))) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("View release on GitHub") }
        }
    }
}
