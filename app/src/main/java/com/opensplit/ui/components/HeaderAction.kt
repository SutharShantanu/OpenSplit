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

/** One action in a [HeaderSplitButton]. */
data class MenuAction(val icon: Symbol, val label: String, val onClick: () -> Unit)

/**
 * Header split button (M3 Expressive): the screen's main action as a tonal icon button joined to
 * a trailing toggle that opens the related actions, instead of a row of separate icon buttons.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HeaderSplitButton(primary: MenuAction, more: List<MenuAction>, modifier: Modifier = Modifier) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.foundation.layout.Box(modifier.padding(horizontal = 4.dp)) {
        androidx.compose.material3.SplitButtonLayout(
            leadingButton = {
                androidx.compose.material3.SplitButtonDefaults.TonalLeadingButton(
                    onClick = primary.onClick,
                    modifier = Modifier.size(width = 56.dp, height = 48.dp)
                        .semantics { contentDescription = primary.label }
                ) { SymbolIcon(primary.icon, contentDescription = null) }
            },
            trailingButton = {
                androidx.compose.material3.SplitButtonDefaults.TonalTrailingButton(
                    checked = open,
                    onCheckedChange = { open = it },
                    modifier = Modifier.size(width = 44.dp, height = 48.dp)
                        .semantics { contentDescription = "More actions" }
                ) {
                    SymbolIcon(
                        OpenSplitIcons.Dropdown, contentDescription = null,
                        modifier = Modifier.graphicsLayer { rotationZ = if (open) 180f else 0f }
                    )
                }
            }
        )
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            more.forEach { action ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { androidx.compose.material3.Text(action.label) },
                    leadingIcon = { SymbolIcon(action.icon, contentDescription = null) },
                    onClick = { open = false; action.onClick() }
                )
            }
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
