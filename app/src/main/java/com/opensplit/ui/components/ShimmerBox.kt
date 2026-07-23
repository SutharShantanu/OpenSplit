package com.opensplit.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitTokens

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shapeDp: Dp = OpenSplitTokens.ShapeMedium
) {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = OpenSplitTokens.MotionExpressive * 2, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(shapeDp))
            .background(brush)
    )
}

@Composable
fun ShimmerExpenseRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = OpenSplitTokens.SpaceSM, horizontal = OpenSplitTokens.SpaceLG),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(
            modifier = Modifier.size(44.dp),
            shapeDp = OpenSplitTokens.ShapeMedium
        )
        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceLG))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(18.dp),
                shapeDp = OpenSplitTokens.ShapeSmall
            )
            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(12.dp),
                shapeDp = OpenSplitTokens.ShapeSmall
            )
        }
        Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceLG))
        ShimmerBox(
            modifier = Modifier
                .width(60.dp)
                .height(20.dp),
            shapeDp = OpenSplitTokens.ShapeSmall
        )
    }
}

@Composable
fun ShimmerGroupCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OpenSplitTokens.ShapeLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .padding(OpenSplitTokens.SpaceLG)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(
                modifier = Modifier.size(48.dp),
                shapeDp = OpenSplitTokens.ShapeFull
            )
            Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceLG))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(20.dp),
                    shapeDp = OpenSplitTokens.ShapeSmall
                )
                Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceSM))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(14.dp),
                    shapeDp = OpenSplitTokens.ShapeSmall
                )
            }
        }
    }
}

@Composable
fun ShimmerStatStrip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
    ) {
        repeat(3) {
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp),
                shapeDp = OpenSplitTokens.ShapeMedium
            )
        }
    }
}
