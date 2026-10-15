package com.opensplit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.Symbol

/** One swipe-revealed action: icon + label on a coloured backdrop, fired when the row is released past the threshold. */
class SwipeAction(
    val icon: Symbol,
    val label: String,
    val container: Color,
    val onContainer: Color,
    val onAction: () -> Unit
)

/**
 * Gmail-style swipe row. Swiping right reveals [startAction], swiping left reveals [endAction].
 * The row springs back after the action fires; data changes (delete, archive) remove it via state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeActionRow(
    startAction: SwipeAction? = null,
    endAction: SwipeAction? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            when (target) {
                SwipeToDismissBoxValue.StartToEnd -> startAction?.onAction()
                SwipeToDismissBoxValue.EndToStart -> endAction?.onAction()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false // never stay dismissed: spring back like Gmail's pin/snooze
        },
        positionalThreshold = { it * 0.35f }
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier.clip(MaterialTheme.shapes.medium),
        enableDismissFromStartToEnd = startAction != null,
        enableDismissFromEndToStart = endAction != null,
        backgroundContent = {
            val action = when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> startAction
                SwipeToDismissBoxValue.EndToStart -> endAction
                SwipeToDismissBoxValue.Settled -> null
            }
            val armed = state.targetValue != SwipeToDismissBoxValue.Settled
            val bg by animateColorAsState(
                if (action != null && armed) action.container else action?.container?.copy(alpha = 0.55f) ?: Color.Transparent,
                label = "swipeBg"
            )
            val scale by animateFloatAsState(if (armed) 1f else 0.8f, label = "swipeIconScale")
            Box(
                modifier = Modifier.fillMaxSize().background(bg).padding(horizontal = 24.dp),
                contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (action != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.scale(scale)) {
                        SymbolIcon(action.icon, contentDescription = null, tint = action.onContainer, modifier = Modifier.size(24.dp))
                        Text(action.label, style = MaterialTheme.typography.labelSmall, color = action.onContainer)
                    }
                }
            }
        }
    ) { content() }
}
