package com.opensplit.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.data.ai.AiService
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens

/** Provided once in MainActivity so any screen can reach the AI layer without threading it through. */
val LocalAiService = staticCompositionLocalOf<AiService> { error("AiService not provided") }

/** Small pill that marks a value as AI-generated. Every AI touchpoint shows one. */
@Composable
fun AiBadge(label: String = "AI", modifier: Modifier = Modifier) {
    Surface(shape = OpenSplitTokens.PillShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            SymbolIcon(OpenSplitIcons.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

/**
 * Gate for an AI-only surface. When [unlocked] is false the content is blurred, dimmed and
 * made non-interactive, with a lock card explaining how to enable it.
 */
@Composable
fun AiLocked(unlocked: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (unlocked) { content(); return }
    androidx.compose.foundation.layout.Box(modifier) {
        androidx.compose.foundation.layout.Box(Modifier.blur(6.dp).alpha(0.35f)) { content() }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.matchParentSize().pointerInput(Unit) {
                awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
            },
            contentAlignment = Alignment.Center
        ) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    SymbolIcon(OpenSplitIcons.Lock, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    Text("AI features are locked", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(
                        "Add your API key under Settings › AI to use them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
