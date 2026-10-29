package com.opensplit.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin

/**
 * Material 3 Expressive transition patterns matching Google Pixel standards.
 *
 * Utilizes emphasized decelerate and accelerate easing curves, half-width horizontal
 * slides, subtle scale-ins, and layered fades to mirror Pixel native screen choreography.
 */
object OpenSplitMotion {

    /** Emphasized standard easing (0.2, 0.0, 0.0, 1.0) */
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Emphasized decelerate easing for incoming elements (0.2, 0.85, 0.7, 1.0) */
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.2f, 0.85f, 0.7f, 1f)

    /** Emphasized accelerate easing for outgoing elements (0.3, 0.0, 0.8, 0.15) */
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Total duration of an expressive transition (ms). */
    const val DurationMs = 400

    /**
     * **Fade through** — for switching between peers with no spatial relationship, such as
     * bottom-navigation destinations or filter tabs.
     */
    fun fadeThrough(): ContentTransform =
        (
            fadeIn(
                animationSpec = tween(
                    durationMillis = DurationMs / 2,
                    delayMillis = DurationMs / 4,
                    easing = EmphasizedDecelerateEasing
                )
            ) + scaleIn(
                initialScale = 0.94f,
                animationSpec = tween(
                    durationMillis = (DurationMs * 0.75f).toInt(),
                    delayMillis = DurationMs / 4,
                    easing = EmphasizedDecelerateEasing
                )
            )
        ) togetherWith fadeOut(
            animationSpec = tween(durationMillis = DurationMs / 3, easing = EmphasizedAccelerateEasing)
        )

    /**
     * **Shared axis (X)** — for hierarchical navigation with forward/backward flow.
     * Incorporates layered horizontal translation and subtle scale zoom.
     */
    fun sharedAxisX(forward: Boolean = true): ContentTransform {
        val enterOffsetMultiplier = if (forward) 0.40f else -0.20f
        val exitOffsetMultiplier = if (forward) -0.20f else 0.40f
        val enterScale = if (forward) 0.94f else 0.96f
        val exitScale = if (forward) 0.96f else 0.94f

        return (
            slideInHorizontally(
                animationSpec = tween(DurationMs, easing = EmphasizedDecelerateEasing),
                initialOffsetX = { (it * enterOffsetMultiplier).toInt() }
            ) + scaleIn(
                animationSpec = tween(DurationMs, easing = EmphasizedDecelerateEasing),
                initialScale = enterScale,
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            ) + fadeIn(
                animationSpec = tween(DurationMs / 2, easing = EmphasizedDecelerateEasing)
            )
        ) togetherWith (
            slideOutHorizontally(
                animationSpec = tween(DurationMs, easing = EmphasizedAccelerateEasing),
                targetOffsetX = { (it * exitOffsetMultiplier).toInt() }
            ) + scaleOut(
                animationSpec = tween(DurationMs, easing = EmphasizedAccelerateEasing),
                targetScale = exitScale,
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            ) + fadeOut(
                animationSpec = tween(DurationMs / 2, easing = EmphasizedAccelerateEasing)
            )
        )
    }

    /**
     * **Spring Spec (ME3)**: Stiffness 400, Damping 35, natural settle with ~3-5% overshoot.
     */
    val ExpressiveSpring = androidx.compose.animation.core.spring<Float>(
        dampingRatio = 0.85f,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
    )
}
