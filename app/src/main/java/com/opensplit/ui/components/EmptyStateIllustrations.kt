package com.opensplit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun WalletIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val outlineColor = color.copy(alpha = 0.6f)
    val fillColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 3.dp.toPx()

        // Main body rectangle
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(w * 0.1f, h * 0.25f),
            size = Size(w * 0.8f, h * 0.55f),
            cornerRadius = CornerRadius(w * 0.08f, h * 0.08f)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(w * 0.1f, h * 0.25f),
            size = Size(w * 0.8f, h * 0.55f),
            cornerRadius = CornerRadius(w * 0.08f, h * 0.08f),
            style = Stroke(width = strokeWidth)
        )

        // Wallet flap
        val flapPath = Path().apply {
            moveTo(w * 0.65f, h * 0.38f)
            lineTo(w * 0.9f, h * 0.38f)
            lineTo(w * 0.9f, h * 0.67f)
            lineTo(w * 0.65f, h * 0.67f)
            close()
        }
        drawPath(path = flapPath, color = fillColor)
        drawPath(path = flapPath, color = outlineColor, style = Stroke(width = strokeWidth))

        // Clasp button
        drawCircle(
            color = outlineColor,
            radius = w * 0.04f,
            center = Offset(w * 0.77f, h * 0.525f)
        )
    }
}

@Composable
fun ReceiptIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val outlineColor = color.copy(alpha = 0.6f)
    val fillColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 3.dp.toPx()

        val receiptPath = Path().apply {
            moveTo(w * 0.25f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.82f)
            // Zigzag bottom
            val teeth = 5
            val teethW = (w * 0.5f) / teeth
            for (i in teeth downTo 1) {
                val x = w * 0.25f + (i - 0.5f) * teethW
                val y = if (i % 2 == 0) h * 0.87f else h * 0.82f
                lineTo(x, y)
            }
            lineTo(w * 0.25f, h * 0.82f)
            close()
        }

        drawPath(path = receiptPath, color = fillColor)
        drawPath(path = receiptPath, color = outlineColor, style = Stroke(width = strokeWidth))

        // Lines on receipt
        drawLine(
            color = outlineColor,
            start = Offset(w * 0.35f, h * 0.3f),
            end = Offset(w * 0.65f, h * 0.3f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = outlineColor,
            start = Offset(w * 0.35f, h * 0.42f),
            end = Offset(w * 0.55f, h * 0.42f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = outlineColor,
            start = Offset(w * 0.35f, h * 0.54f),
            end = Offset(w * 0.65f, h * 0.54f),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun HandshakeIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val outlineColor = color.copy(alpha = 0.6f)
    val fillColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 3.dp.toPx()

        // Heart/check settled motif
        val circleCenter = Offset(w * 0.5f, h * 0.48f)
        val circleRadius = w * 0.32f

        drawCircle(
            color = fillColor,
            center = circleCenter,
            radius = circleRadius
        )
        drawCircle(
            color = outlineColor,
            center = circleCenter,
            radius = circleRadius,
            style = Stroke(width = strokeWidth)
        )

        // Checkmark inside
        val checkPath = Path().apply {
            moveTo(w * 0.36f, h * 0.48f)
            lineTo(w * 0.46f, h * 0.58f)
            lineTo(w * 0.66f, h * 0.38f)
        }
        drawPath(path = checkPath, color = color, style = Stroke(width = strokeWidth * 1.3f))
    }
}

@Composable
fun BellWavesIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val outlineColor = color.copy(alpha = 0.6f)
    val fillColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 3.dp.toPx()

        // Bell shape
        val bellPath = Path().apply {
            moveTo(w * 0.5f, h * 0.2f)
            cubicTo(w * 0.38f, h * 0.2f, w * 0.35f, h * 0.4f, w * 0.3f, h * 0.58f)
            lineTo(w * 0.25f, h * 0.65f)
            lineTo(w * 0.75f, h * 0.65f)
            lineTo(w * 0.7f, h * 0.58f)
            cubicTo(w * 0.65f, h * 0.4f, w * 0.62f, h * 0.2f, w * 0.5f, h * 0.2f)
            close()
        }

        drawPath(path = bellPath, color = fillColor)
        drawPath(path = bellPath, color = outlineColor, style = Stroke(width = strokeWidth))

        // Clapper
        drawArc(
            color = outlineColor,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.43f, h * 0.65f),
            size = Size(w * 0.14f, h * 0.12f),
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun ChartBarsIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val outlineColor = color.copy(alpha = 0.6f)
    val fillColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 3.dp.toPx()

        // 3 bar charts
        val bar1 = Size(w * 0.16f, h * 0.3f)
        val bar2 = Size(w * 0.16f, h * 0.5f)
        val bar3 = Size(w * 0.16f, h * 0.38f)

        // Bar 1
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(w * 0.22f, h * 0.5f),
            size = bar1,
            cornerRadius = CornerRadius(w * 0.03f, h * 0.03f)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(w * 0.22f, h * 0.5f),
            size = bar1,
            cornerRadius = CornerRadius(w * 0.03f, h * 0.03f),
            style = Stroke(width = strokeWidth)
        )

        // Bar 2
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(w * 0.42f, h * 0.3f),
            size = bar2,
            cornerRadius = CornerRadius(w * 0.03f, h * 0.03f)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(w * 0.42f, h * 0.3f),
            size = bar2,
            cornerRadius = CornerRadius(w * 0.03f, h * 0.03f),
            style = Stroke(width = strokeWidth)
        )

        // Bar 3
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(w * 0.62f, h * 0.42f),
            size = bar3,
            cornerRadius = CornerRadius(w * 0.03f, h * 0.03f)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(w * 0.62f, h * 0.42f),
            size = bar3,
            cornerRadius = CornerRadius(w * 0.03f, h * 0.03f),
            style = Stroke(width = strokeWidth)
        )

        // Baseline
        drawLine(
            color = outlineColor,
            start = Offset(w * 0.15f, h * 0.8f),
            end = Offset(w * 0.85f, h * 0.8f),
            strokeWidth = strokeWidth
        )
    }
}
