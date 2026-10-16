package com.opensplit.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * Fades and scales content in the first time it appears (empty states, illustrations), per the
 * Compose animation guide: AnimatedVisibility with an initial `false` state.
 */
@Composable
fun AppearOnEnter(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(
        visible = shown,
        modifier = modifier,
        enter = fadeIn(spring(stiffness = 300f)) + scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)),
        exit = fadeOut()
    ) { content() }
}

/**
 * Money or any headline number that changes: the old value slides up and fades while the new one
 * slides in from below (AnimatedContent with a vertical slide), so totals never just "jump".
 */
@Composable
fun AnimatedNumberText(
    text: String,
    style: TextStyle,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    maxLines: Int = 1
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            val up = targetState.length >= initialState.length
            (slideInVertically { h -> if (up) h / 2 else -h / 2 } + fadeIn(spring(stiffness = 400f)))
                .togetherWith(slideOutVertically { h -> if (up) -h / 2 else h / 2 } + fadeOut(spring(stiffness = 400f)))
        },
        label = "number"
    ) { value ->
        Text(text = value, style = style, color = color, modifier = modifier, textAlign = textAlign, maxLines = maxLines)
    }
}
