package com.opensplit.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@Composable
fun Modifier.appHazeSource(hazeState: HazeState): Modifier {
    return this.hazeSource(state = hazeState)
}

@Composable
fun Modifier.appHazeHeader(hazeState: HazeState): Modifier {
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
    return this.hazeEffect(
        state = hazeState,
        style = HazeDefaults.style(backgroundColor = surfaceColor)
    )
}
