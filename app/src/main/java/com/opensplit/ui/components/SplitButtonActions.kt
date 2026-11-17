@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.Symbol

/** One selectable option in a split button trailing dropdown menu. */
data class SplitOption<T>(val value: T, val label: String, val icon: Symbol? = null)

/**
 * Reusable Material 3 Expressive Split Button with primary leading action
 * and trailing dropdown menu for secondary selections.
 */
@Composable
fun <T> ExpressiveSplitButton(
    leadingText: String,
    leadingIcon: Symbol? = null,
    onPrimaryClick: () -> Unit,
    options: List<SplitOption<T>>,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    trailingDescription: String = "More options"
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(onClick = onPrimaryClick) {
                    if (leadingIcon != null) {
                        SymbolIcon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(leadingText, fontWeight = FontWeight.Bold)
                }
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    checked = expanded,
                    onCheckedChange = { expanded = it },
                    modifier = Modifier.semantics { contentDescription = trailingDescription }
                ) {
                    SymbolIcon(
                        OpenSplitIcons.Dropdown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = if (expanded) 180f else 0f }
                    )
                }
            }
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    leadingIcon = opt.icon?.let { sym -> { SymbolIcon(sym, contentDescription = null, modifier = Modifier.size(18.dp)) } },
                    text = { Text(opt.label) },
                    onClick = {
                        expanded = false
                        onOptionSelected(opt.value)
                    }
                )
            }
        }
    }
}
