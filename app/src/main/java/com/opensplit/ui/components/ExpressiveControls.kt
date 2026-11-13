@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTheme
import com.opensplit.ui.theme.Symbol

/**
 * Single-choice connected button group (M3 Expressive): equal-width toggle buttons whose outer
 * corners stay round and inner corners square, and the selected one morphs fully round.
 * Use for 2–4 mutually exclusive options such as a period or a mode.
 */
@Composable
fun ConnectedChoiceGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<Symbol?> = emptyList()
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { i, label ->
            ToggleButton(
                checked = i == selectedIndex,
                onCheckedChange = { onSelect(i) },
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier.weight(1f).semantics {
                    role = Role.RadioButton
                    selected = i == selectedIndex
                }
            ) {
                icons.getOrNull(i)?.let {
                    SymbolIcon(it, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Shape-morphing indicator sized for a button or a status line, in place of a small spinner. */
@Composable
fun InlineLoadingIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    LoadingIndicator(modifier = modifier.size(24.dp), color = color)
}

/** One entry in [ActionFabMenu]. */
data class FabAction(val label: String, val icon: Symbol, val onClick: () -> Unit)

/**
 * FAB that expands into labelled actions (M3 Expressive FAB menu). The button morphs from a
 * circle into a close button; Back or picking an action collapses it.
 */
@Composable
fun ActionFabMenu(
    actions: List<FabAction>,
    icon: Symbol,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(expanded) { expanded = false }
    FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { expanded = it },
                modifier = Modifier.semantics {
                    this.contentDescription = contentDescription
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                }
            ) {
                SymbolIcon(
                    if (checkedProgress > 0.5f) OpenSplitIcons.Close else icon,
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ checkedProgress })
                )
            }
        }
    ) {
        actions.forEach { action ->
            FloatingActionButtonMenuItem(
                onClick = { expanded = false; action.onClick() },
                icon = { SymbolIcon(action.icon, contentDescription = null) },
                text = { Text(action.label) }
            )
        }
    }
}

@Preview
@Composable
private fun ConnectedChoiceGroupPreview() = OpenSplitTheme {
    ConnectedChoiceGroup(listOf("Month", "Year", "All time"), selectedIndex = 0, onSelect = {})
}
