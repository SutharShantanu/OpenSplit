@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.components

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.Symbol
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** One revealed action: a round icon button in the tray. [primary] renders filled, the rest tonal. */
class SwipeAction(
    val icon: Symbol,
    val label: String,
    val container: Color = Color.Unspecified,
    val onContainer: Color = Color.Unspecified,
    val primary: Boolean = false,
    val onAction: () -> Unit
)

private enum class Reveal { Start, Settled, End }

/**
 * M3 Expressive "swipe to reveal" list row. Dragging the row sideways uncovers a tray of round
 * action buttons on that side and the row stays open until an action is tapped or it is swiped
 * back. [startAction]/[endAction] keep single-action call sites working; [startActions]/[endActions]
 * take a full tray.
 */
@Composable
fun SwipeActionRow(
    startAction: SwipeAction? = null,
    endAction: SwipeAction? = null,
    startActions: List<SwipeAction> = listOfNotNull(startAction),
    endActions: List<SwipeAction> = listOfNotNull(endAction),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val buttonSize = 48.dp
    val gap = 8.dp
    val sidePad = 12.dp
    fun trayWidth(n: Int) = if (n == 0) 0.dp else buttonSize * n + gap * (n - 1) + sidePad * 2
    val startPx = with(density) { trayWidth(startActions.size).toPx() }
    val endPx = with(density) { trayWidth(endActions.size).toPx() }
    val state = remember(startPx, endPx) {
        AnchoredDraggableState(
            initialValue = Reveal.Settled,
            anchors = DraggableAnchors {
                if (startActions.isNotEmpty()) Reveal.Start at startPx
                Reveal.Settled at 0f
                if (endActions.isNotEmpty()) Reveal.End at -endPx
            }
        )
    }
    val scope = rememberCoroutineScope()
    val latestStart by rememberUpdatedState(startActions)
    val latestEnd by rememberUpdatedState(endActions)
    val close: () -> Unit = { scope.launch { state.animateTo(Reveal.Settled) } }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    androidx.compose.runtime.LaunchedEffect(state) {
        androidx.compose.runtime.snapshotFlow { state.settledValue }.collect {
            if (it != Reveal.Settled) {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.GestureThresholdActivate)
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)) {
        val offset = state.requireOffset()
        // Trays sit under the row; only the one being uncovered is composed.
        if (offset > 0f && latestStart.isNotEmpty()) {
            Tray(latestStart, Modifier.align(Alignment.CenterStart).padding(horizontal = sidePad), close)
        }
        if (offset < 0f && latestEnd.isNotEmpty()) {
            Tray(latestEnd, Modifier.align(Alignment.CenterEnd).padding(horizontal = sidePad), close)
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .anchoredDraggable(state, Orientation.Horizontal)
        ) { content() }
    }
}

@Composable
private fun Tray(actions: List<SwipeAction>, modifier: Modifier, close: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        actions.forEach { a ->
            val run = { close(); a.onAction() }
            if (a.primary) {
                FilledIconButton(onClick = run, shapes = com.opensplit.ui.theme.appIconButtonShapes(), modifier = Modifier.size(48.dp)) {
                    SymbolIcon(a.icon, contentDescription = a.label)
                }
            } else {
                FilledTonalIconButton(
                    onClick = run, shapes = com.opensplit.ui.theme.appIconButtonShapes(), modifier = Modifier.size(48.dp),
                    colors = if (a.container != Color.Unspecified) IconButtonDefaults.filledTonalIconButtonColors(containerColor = a.container, contentColor = a.onContainer) else IconButtonDefaults.filledTonalIconButtonColors()
                ) { SymbolIcon(a.icon, contentDescription = a.label) }
            }
        }
    }
}
