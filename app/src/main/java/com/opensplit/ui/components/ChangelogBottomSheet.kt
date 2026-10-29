@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.ShapeCache

data class ChangelogSection(
    val title: String,
    val items: List<String>
)

data class ChangelogRelease(
    val version: String,
    val date: String,
    val badge: String? = null,
    val sections: List<ChangelogSection>
)

val OpenSplitChangelog: List<ChangelogRelease> = listOf(
    ChangelogRelease(
        version = "1.0.0",
        date = "October 2026",
        badge = "Latest",
        sections = listOf(
            ChangelogSection(
                title = "What's New",
                items = listOf(
                    "Pixel Design Parity: Rounded typography (ROND=100 variable font), Expressive squircle containers, and tactile haptic response.",
                    "Fast Scroller with Category Bubble: Interactive draggable side scrollbar on Groups, Friends, Activity, and Expense details.",
                    "Dynamic System Bars: Contrast-aware luminance calculation dynamically matching system bar icons against top surfaces.",
                    "In-App Updater: Zero-friction GitHub Release detection with Android 8–15 background package install handoff.",
                    "Expense Fast Settle: One-tap simplified debt resolution with UPI deep-link generation."
                )
            ),
            ChangelogSection(
                title = "Polish & Architecture",
                items = listOf(
                    "Predictive Navigation & Emphasized Motion: Decelerate/accelerate physics curve with depth-scale transitions.",
                    "Pre-warmed Tab Host: Zero frame drops during bottom navigation switching via background composition delay.",
                    "Offline Persistence: Uncached network calls gracefully fall back to local encrypted disk caches."
                )
            )
        )
    )
)

@Composable
fun ChangelogBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = ShapeCache.smooth16,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            SymbolIcon(
                                symbol = OpenSplitIcons.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "What's New",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "OpenSplit Release Notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/SutharShantanu/OpenSplit/releases"))
                        context.startActivity(intent)
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Releases", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(OpenSplitChangelog) { release ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Version ${release.version}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (release.badge != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        ) {
                                            Text(
                                                text = release.badge,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = release.date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            release.sections.forEach { section ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                section.items.forEach { bullet ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = bullet,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
