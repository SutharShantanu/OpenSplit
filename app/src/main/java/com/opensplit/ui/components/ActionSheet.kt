@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.opensplit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.Symbol

/** Colour role of an [ActionSheet] button. */
enum class ActionTone { Primary, Strong, Secondary, Tertiary, Neutral, Danger }

class SheetAction(
    val label: String,
    val icon: Symbol,
    val tone: ActionTone = ActionTone.Neutral,
    /** Relative width within its row. */
    val weight: Float = 1f,
    val onClick: () -> Unit
)

/**
 * Bottom sheet of big tonal pill buttons laid out in [rows], each row sharing the width.
 * The sheet closes itself before running an action.
 */
@Composable
fun ActionSheet(
    title: String,
    onDismiss: () -> Unit,
    rows: List<List<SheetAction>>,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .responsiveContentWidth(maxWidth = 560.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(16.dp))
                }
                Column {
                    Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { action ->
                        val (bg, fg) = toneColors(action.tone)
                        Button(
                            onClick = { onDismiss(); action.onClick() },
                            shapes = com.opensplit.ui.theme.appButtonShapes(),
                            colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
                            contentPadding = ButtonDefaults.ContentPadding,
                            modifier = Modifier.weight(action.weight).height(64.dp)
                        ) {
                            SymbolIcon(action.icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(action.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun toneColors(tone: ActionTone): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (tone) {
        ActionTone.Primary -> c.primaryContainer to c.onPrimaryContainer
        ActionTone.Strong -> c.primary to c.onPrimary
        ActionTone.Secondary -> c.secondaryContainer to c.onSecondaryContainer
        ActionTone.Tertiary -> c.tertiaryContainer to c.onTertiaryContainer
        ActionTone.Neutral -> c.surfaceContainerHighest to c.onSurface
        ActionTone.Danger -> c.errorContainer to c.onErrorContainer
    }
}
