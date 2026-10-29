package com.opensplit.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import kotlinx.coroutines.delay

/**
 * Hosts top-level tabs and slides between them along the tab order (M3 shared axis X).
 *
 * Unlike AnimatedContent it keeps every visited tab composed, and builds the remaining tabs one
 * by one shortly after start. Switching therefore never composes a whole screen in the frame the
 * animation starts, which on heavy screens froze the main thread long enough to skip the slide.
 * Hidden tabs stay composed (state, scroll position) but are neither measured, drawn nor touchable.
 */
@Composable
fun <T> SlidingTabHost(
    selected: T,
    tabs: List<T>,
    modifier: Modifier = Modifier,
    durationMs: Int = 300,
    content: @Composable (T) -> Unit
) {
    val composed = remember { mutableStateListOf(selected) }
    if (selected !in composed) composed.add(selected)

    var shown by remember { mutableStateOf(selected) }
    var leaving by remember { mutableStateOf<T?>(null) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(selected) {
        if (selected == shown) return@LaunchedEffect
        leaving = shown
        shown = selected
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
        leaving = null
    }

    // Warm the remaining tabs while idle so their first visit is smooth too.
    LaunchedEffect(Unit) {
        delay(800)
        for (tab in tabs) {
            if (tab !in composed) {
                composed.add(tab)
                delay(120)
            }
        }
    }

    // Travel a fifth of the width (the 30dp M3 default read as a plain cross-fade on phones);
    // the outgoing tab fades over the first 40%, the incoming one from 15% on.
    val outgoingShare = 0.4f
    val incomingStart = 0.15f

    Layout(
        modifier = modifier,
        content = { composed.forEach { tab -> key(tab) { Box(Modifier.layoutId(TabId(tab))) { content(tab) } } } }
    ) { measurables, constraints ->
        // Measure every composed tab, not just the visible ones: a hidden tab's first measure
        // (lists, text layout) otherwise lands in the first frame of the slide and stalls it.
        // Unchanged tabs are cached by Compose, so this costs nothing while idle.
        // Children are matched by layoutId, not index: when the warm-up adds a tab, this pass can
        // run before the new child is composed.
        @Suppress("UNCHECKED_CAST")
        val placeables = measurables.mapNotNull { m ->
            val tab = (m.layoutId as? TabId<*>)?.tab as? T ?: return@mapNotNull null
            tab to m.measure(constraints)
        }.filter { (tab, _) -> tab == shown || tab == leaving }
        val width = placeables.maxOfOrNull { it.second.width } ?: constraints.minWidth
        val height = placeables.maxOfOrNull { it.second.height } ?: constraints.minHeight
        layout(width, height) {
            val t = progress.value
            val eased = FastOutSlowInEasing.transform(t)
            val from = leaving
            val dir = if (from != null && tabs.indexOf(shown) < tabs.indexOf(from)) -1 else 1
            val slidePx = width * 0.2f
            placeables.forEach { (tab, placeable) ->
                placeable.placeWithLayer(0, 0) {
                    if (from == null) return@placeWithLayer
                    if (tab == shown) {
                        translationX = dir * slidePx * (1f - eased)
                        alpha = ((t - incomingStart) / (1f - incomingStart)).coerceIn(0f, 1f)
                    } else {
                        translationX = -dir * slidePx * eased
                        alpha = (1f - t / outgoingShare).coerceIn(0f, 1f)
                    }
                }
            }
        }
    }
}

/** Wrapper so a tab value can't collide with other layoutIds. */
private data class TabId<T>(val tab: T)
