package com.opensplit.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * The single loading affordance for the whole app — the Material 3 Expressive
 * [LoadingIndicator] (the shape-morphing one), not a plain circular spinner.
 *
 * Every screen routes through this wrapper so the experimental opt-in lives in one place
 * and the look stays consistent app-wide.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp? = null
) {
    LoadingIndicator(
        modifier = if (size != null) modifier.size(size) else modifier,
        color = color
    )
}
