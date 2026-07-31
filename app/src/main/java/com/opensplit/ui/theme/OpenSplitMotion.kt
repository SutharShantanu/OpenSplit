package com.opensplit.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/**
 * Material 3 transition patterns, in one place so every screen animates consistently.
 *
 * Follows the M3 motion spec: the outgoing element leaves over the first ~90ms while the
 * incoming one arrives over the remainder, so the two never compete for attention.
 */
object OpenSplitMotion {

    /** Total duration of a standard M3 transition. */
    const val DurationMs = 300

    /** Length of the outgoing (exit) portion; the incoming half starts after it. */
    private const val OutgoingMs = 90
    private const val IncomingMs = DurationMs - OutgoingMs

    /** How far content travels on a shared-axis transition, in px. */
    private const val SharedAxisSlidePx = 90

    /**
     * **Fade through** — for switching between peers with no spatial relationship, such as
     * bottom-navigation destinations or filter tabs. The outgoing content simply fades out;
     * the incoming fades in while scaling up slightly.
     */
    fun fadeThrough(): ContentTransform =
        (
            fadeIn(
                animationSpec = tween(
                    durationMillis = IncomingMs,
                    delayMillis = OutgoingMs,
                    easing = LinearOutSlowInEasing
                )
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(
                    durationMillis = IncomingMs,
                    delayMillis = OutgoingMs,
                    easing = LinearOutSlowInEasing
                )
            )
            ) togetherWith fadeOut(
            animationSpec = tween(durationMillis = OutgoingMs, easing = FastOutLinearInEasing)
        )

    /**
     * **Shared axis (X)** — for navigation that has a clear forward/back direction, e.g.
     * drilling into a group. [forward] flips the travel direction so going back mirrors
     * going in.
     */
    fun sharedAxisX(forward: Boolean = true): ContentTransform {
        val direction = if (forward) 1 else -1
        return (
            slideInHorizontally(
                animationSpec = tween(DurationMs, easing = FastOutSlowInEasing),
                initialOffsetX = { direction * SharedAxisSlidePx }
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = IncomingMs,
                    delayMillis = OutgoingMs,
                    easing = LinearOutSlowInEasing
                )
            )
            ) togetherWith (
            slideOutHorizontally(
                animationSpec = tween(DurationMs, easing = FastOutSlowInEasing),
                targetOffsetX = { -direction * SharedAxisSlidePx }
            ) + fadeOut(
                animationSpec = tween(durationMillis = OutgoingMs, easing = FastOutLinearInEasing)
            )
            )
    }
}
