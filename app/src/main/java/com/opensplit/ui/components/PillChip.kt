package com.opensplit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * App-wide filter chip: borderless tonal pill, filled primary when selected. Same parameters as
 * the Material one so call sites only swap the import (same package or an explicit import wins
 * over `material3.*`).
 */
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = com.opensplit.ui.theme.LocalAppStyle.current.pill,
    colors: SelectableChipColors = pillChipColors(),
    border: BorderStroke? = null
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        border = border
    )
}

@Composable
fun pillChipColors(): SelectableChipColors {
    val c = MaterialTheme.colorScheme
    return FilterChipDefaults.filterChipColors(
        containerColor = c.surfaceContainerHigh,
        labelColor = c.onSurfaceVariant,
        iconColor = c.onSurfaceVariant,
        selectedContainerColor = c.primary,
        selectedLabelColor = c.onPrimary,
        selectedLeadingIconColor = c.onPrimary,
        selectedTrailingIconColor = c.onPrimary
    )
}

/**
 * Segmented pill tabs: a tonal track with the selected tab as a filled primary pill.
 * Used instead of underline tab rows.
 */
@Composable
fun PillTabs(
    selected: Int,
    labels: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<com.opensplit.ui.theme.Symbol?> = emptyList()
) {
    val c = MaterialTheme.colorScheme
    androidx.compose.material3.Surface(
        shape = com.opensplit.ui.theme.LocalAppStyle.current.pill,
        color = c.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            labels.forEachIndexed { i, label ->
                val on = i == selected
                androidx.compose.material3.Surface(
                    onClick = { onSelect(i) },
                    shape = com.opensplit.ui.theme.LocalAppStyle.current.pill,
                    color = if (on) c.primary else androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = if (on) c.onPrimary else c.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { role = Role.Tab; this.selected = on }
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        icons.getOrNull(i)?.let {
                            SymbolIcon(it, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        androidx.compose.material3.Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Content under [PillTabs]: slides toward the tapped tab (shared axis X) and animates the height
 * change between tabs instead of jumping.
 */
@Composable
fun PillTabContent(selected: Int, content: @Composable (Int) -> Unit) {
    androidx.compose.animation.AnimatedContent(
        targetState = selected,
        transitionSpec = {
            com.opensplit.ui.theme.OpenSplitMotion.sharedAxisX(forward = targetState > initialState)
                .using(androidx.compose.animation.SizeTransform(clip = false))
        },
        label = "pillTabContent"
    ) { tab -> content(tab) }
}
