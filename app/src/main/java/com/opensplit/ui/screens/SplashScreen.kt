package com.opensplit.ui.screens

import com.opensplit.ui.components.SymbolIcon
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensplit.ui.theme.OpenSplitIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SCREEN 01 — Splash Screen
 * Full-screen launch splash for openSplit.
 * Colours come from the active theme (palette + light/dark): primary-container background with a
 * soft primary glow, the logo in a primary circle, wordmark and tagline in on-primary-container.
 * Spring scale animation from 0.8 to 1.0. Auto advances after 1.5s.
 */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val scaleAnim = remember { Animatable(0.8f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        launch {
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 400)
            )
        }
        delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        // Soft radial glow in center
        Box(
            modifier = Modifier
                .size(360.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
        ) {
            // 96dp circle in surface/20% containing pie_chart icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                SymbolIcon(
                    symbol = OpenSplitIcons.PieChart,
                    contentDescription = "openSplit Logo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wordmark
            Text(
                text = "openSplit",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Split smarter. Stay free.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
        }
    }
}
