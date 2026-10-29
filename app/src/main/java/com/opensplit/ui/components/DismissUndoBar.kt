package com.opensplit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons

/**
 * Pixel-style floating undo pill bar with an animated countdown progress bar,
 * undo action button, and close dismiss icon.
 */
@Composable
fun DismissUndoBar(
    message: String,
    onUndo: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Long = 4000L
) {
    val progress = remember { Animatable(1f) }

    LaunchedEffect(key1 = message) {
        progress.snapTo(1f)
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = durationMillis.toInt(),
                easing = LinearEasing
            )
        )
        onClose()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp,
        tonalElevation = 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated progress countdown background
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(fraction = progress.value.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        shape = CircleShape
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp, end = 8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        onClick = onUndo,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Undo",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    FilledIconButton(
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = onClose,
                        modifier = Modifier.size(36.dp)
                    ) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
