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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * A number that counts up from zero when it first appears and rolls from the old value to the
 * new one when it changes (about 0.7 s, fast then settling). It snaps with Reduce motion, and
 * TalkBack always reads the final value. Pair with the mono money font so digits don't jitter.
 */
@Composable
fun AnimatedAmountText(
    value: Double,
    format: (Double) -> String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = 1
) {
    val reduce = com.opensplit.ui.theme.LocalReduceMotion.current
    val animated = remember { androidx.compose.animation.core.Animatable(if (reduce) value.toFloat() else 0f) }
    LaunchedEffect(value, reduce) {
        if (reduce) animated.snapTo(value.toFloat())
        else animated.animateTo(value.toFloat(), androidx.compose.animation.core.tween(700, easing = com.opensplit.ui.theme.OpenSplitMotion.EmphasizedDecelerateEasing))
    }
    // Float steps are only for the roll; the resting text is the exact value.
    val shown = if (animated.isRunning) animated.value.toDouble() else value
    val final = format(value)
    Text(
        text = format(shown),
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        modifier = modifier.clearAndSetSemantics { contentDescription = final }
    )
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
