@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.components

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
