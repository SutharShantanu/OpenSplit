package com.opensplit.ui.screens

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
import com.opensplit.ui.theme.PrimaryContainerLight
import com.opensplit.ui.theme.PrimaryFixedLight
import com.opensplit.ui.theme.OnPrimaryContainerLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SCREEN 01 — Splash Screen
 * Full-screen launch splash for openSplit.
 * Background: solid primary-container (#6750a4) with soft radial glow using primary-fixed at 30% opacity.
 * Centered: 96dp circle in surface/20% containing filled pie_chart icon in on-primary-container (#e0d2ff) at 48dp.
 * Wordmark "openSplit" in 28sp bold on-primary (#ffffff).
 * Tagline "Split smarter. Settle faster. Forever free." in font-body-md (#ffffff at 70% opacity).
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
            .background(PrimaryContainerLight),
        contentAlignment = Alignment.Center
    ) {
        // Soft radial glow in center
        Box(
            modifier = Modifier
                .size(360.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryFixedLight.copy(alpha = 0.30f),
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
                    .background(Color.White.copy(alpha = 0.20f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = OpenSplitIcons.PieChart,
                    contentDescription = "openSplit Logo",
                    tint = OnPrimaryContainerLight,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wordmark
            Text(
                text = "openSplit",
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Split smarter. Stay free.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.70f)
            )
        }
    }
}
