package com.opensplit.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensplit.R
import com.opensplit.ui.theme.Symbol

/**
 * Draws one Material Symbols Rounded glyph from the bundled variable font.
 *
 * Drop-in for `Icon(imageVector, …)`: 24dp by default, the glyph scales to whatever size the
 * caller's [modifier] imposes, and [tint] follows [LocalContentColor]. [filled] overrides the
 * symbol's own fill (e.g. the active navigation destination).
 */
@Composable
fun SymbolIcon(
    symbol: Symbol,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean = symbol.filled
) {
    val family = remember(filled) { symbolFontFamily(filled) }
    val measurer = rememberTextMeasurer()
    val text = remember(symbol) { String(Character.toChars(symbol.codepoint)) }
    val semantics = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription; role = Role.Image }
    } else Modifier
    androidx.compose.foundation.layout.Box(
        modifier
            .then(Modifier.size(24.dp))
            .then(semantics)
            .drawWithCache {
                val px = minOf(size.width, size.height)
                val layout = measurer.measure(
                    text = text,
                    style = TextStyle(fontFamily = family, fontSize = px.toSp()),
                )
                val left = (size.width - layout.size.width) / 2f
                val top = (size.height - layout.size.height) / 2f
                onDrawBehind {
                    drawText(layout, color = tint, topLeft = androidx.compose.ui.geometry.Offset(left, top))
                }
            }
    )
}

// Variable-font axes: FILL 0/1, wght 400, GRAD 0, opsz 24. Applied on API 26+; older devices
// render the font's default instance (outlined, regular weight), which is still legible.
private fun symbolFontFamily(filled: Boolean): FontFamily = FontFamily(
    Font(
        resId = R.font.material_symbols_rounded,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("FILL", if (filled) 1f else 0f),
            FontVariation.weight(400),
            FontVariation.grade(0),
            FontVariation.Setting("opsz", 24f)
        )
    )
)
