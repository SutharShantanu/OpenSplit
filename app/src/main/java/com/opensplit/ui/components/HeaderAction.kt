@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.components

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.Symbol

/** Round tonal 48dp icon button used in screen headers; each action gets its own container tint. */
@Composable
fun HeaderAction(
    icon: Symbol,
    contentDescription: String,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        shapes = com.opensplit.ui.theme.appIconButtonShapes(),
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = container, contentColor = content),
        modifier = Modifier.padding(horizontal = 4.dp).size(48.dp)
    ) { SymbolIcon(icon, contentDescription = contentDescription) }
}

/** One action in a [HeaderButtonGroup]. */
data class MenuAction(val icon: Symbol, val label: String, val onClick: () -> Unit)

/**
 * Header actions as one connected (segmented) button group: tonal icon buttons side by side with
 * round outer ends and tight inner corners, each squeezing a little when pressed. All actions stay
 * visible, unlike a split button's menu.
 */
@Composable
fun HeaderButtonGroup(actions: List<MenuAction>, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
            androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween
        )
    ) {
        actions.forEachIndexed { i, action ->
            fun corners(outer: androidx.compose.ui.unit.Dp, inner: androidx.compose.ui.unit.Dp) =
                androidx.compose.foundation.shape.RoundedCornerShape(
                    topStart = if (i == 0) outer else inner, bottomStart = if (i == 0) outer else inner,
                    topEnd = if (i == actions.lastIndex) outer else inner, bottomEnd = if (i == actions.lastIndex) outer else inner
                )
            FilledTonalIconButton(
                onClick = action.onClick,
                shapes = androidx.compose.material3.IconButtonShapes(shape = corners(24.dp, 8.dp), pressedShape = corners(24.dp, 4.dp)),
                modifier = Modifier.size(width = 52.dp, height = 48.dp)
            ) { SymbolIcon(action.icon, contentDescription = action.label) }
        }
    }
}

/** Round tonal back button for sub-screen headers. */
@Composable
fun RoundBackButton(onClick: () -> Unit) {
    HeaderAction(OpenSplitIcons.ArrowBack, "Back", onClick = onClick)
}

/** Shape of row [index] of [count] in a segmented list: large outer corners, tight between rows. */
fun segmentShape(index: Int, count: Int): androidx.compose.ui.graphics.Shape {
    val top = if (index == 0) 28.dp else 6.dp
    val bottom = if (index == count - 1) 28.dp else 6.dp
    return androidx.compose.foundation.shape.RoundedCornerShape(top, top, bottom, bottom)
}

/** Rotating tonal container/content pair for leading icon circles, so lists aren't one colour. */
@Composable
fun toneFor(index: Int): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (index % 3) {
        0 -> c.primaryContainer to c.onPrimaryContainer
        1 -> c.tertiaryContainer to c.onTertiaryContainer
        else -> c.secondaryContainer to c.onSecondaryContainer
    }
}
