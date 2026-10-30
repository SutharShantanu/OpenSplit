package com.opensplit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.Symbol

/** One destination in [FloatingNavBar]. [badge] wraps the icon, e.g. an unread count. */
class FloatingNavItem(
    val label: String,
    val icon: Symbol,
    val selected: Boolean,
    val onClick: () -> Unit,
    val badge: (@Composable (@Composable () -> Unit) -> Unit)? = null
)

/**
 * Floating pill navigation bar: the selected destination expands into a filled pill with its
 * label, the rest are icon-only. An optional [action] (the screen's FAB) docks to the right.
 */
@Composable
fun FloatingNavBar(
    items: List<FloatingNavItem>,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Plain tonal pill like Pixel apps: no custom shadow, no blur or tint behind it.
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.weight(1f).height(72.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { item -> NavPill(item) }
            }
        }
        AnimatedVisibility(
            visible = action != null,
            enter = fadeIn() + expandHorizontally(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)),
            exit = fadeOut() + shrinkHorizontally()
        ) { action?.invoke() }
    }
}

@Composable
private fun RowScope.NavPill(item: FloatingNavItem) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val container by animateColorAsState(
        if (item.selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "navPill"
    )
    val content = if (item.selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            item.onClick()
        },
        shape = androidx.compose.foundation.shape.CircleShape,
        color = container,
        contentColor = content,
        modifier = Modifier
            .weight(if (item.selected) 2.8f else 1f)
            .fillMaxHeight()
            .semantics {
                role = Role.Tab
                selected = item.selected
                contentDescription = item.label
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            val icon: @Composable () -> Unit = {
                SymbolIcon(item.icon, contentDescription = null, tint = content, filled = item.selected)
            }
            Box { item.badge?.invoke(icon) ?: icon() }
            AnimatedVisibility(visible = item.selected) {
                Row {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

/** Bottom padding scrollable tab content needs so its last item can scroll clear of the bar. */
val FloatingNavBarClearance = 120.dp
