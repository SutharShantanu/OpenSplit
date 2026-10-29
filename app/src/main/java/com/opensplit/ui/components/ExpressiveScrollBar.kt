package com.opensplit.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pixel-style interactive fast scrollbar for LazyLists with an animated expandable handle
 * and floating circular indicator bubble.
 */
@Composable
fun ExpressiveScrollBar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    labelResolver: ((index: Int) -> String?)? = null
) {
    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    if (totalItems <= 4) return

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(-1f) }
    var currentLabel by remember { mutableStateOf<String?>(null) }

    val firstVisible = listState.firstVisibleItemIndex
    val scrollProgress = remember(firstVisible, totalItems) {
        if (totalItems > 1) {
            (firstVisible.toFloat() / (totalItems - 1)).coerceIn(0f, 1f)
        } else 0f
    }

    val displayedProgress = remember { Animatable(scrollProgress) }
    LaunchedEffect(scrollProgress) {
        if (!isDragging) {
            displayedProgress.animateTo(scrollProgress, tween(100, easing = FastOutSlowInEasing))
        }
    }

    // Auto-hide when idle
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(firstVisible, isDragging) {
        isVisible = true
        if (!isDragging) {
            delay(1500)
            isVisible = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "ScrollBarAlpha"
    )

    val thumbWidth by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 4.dp,
        animationSpec = tween(150),
        label = "ScrollBarWidth"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        val totalHeightPx = with(density) { maxHeight.toPx() }
        val thumbHeightPx = with(density) { 56.dp.toPx() }
        val scrollableHeight = (totalHeightPx - thumbHeightPx).coerceAtLeast(1f)

        val progressToUse = if (isDragging && dragProgress >= 0f) dragProgress else displayedProgress.value
        val thumbY = progressToUse * scrollableHeight

        // Drag gestures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(totalItems) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val p = (offset.y / totalHeightPx).coerceIn(0f, 1f)
                            dragProgress = p
                            val targetIndex = (p * (totalItems - 1)).toInt()
                            currentLabel = labelResolver?.invoke(targetIndex)
                            scope.launch { listState.scrollToItem(targetIndex) }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val p = (change.position.y / totalHeightPx).coerceIn(0f, 1f)
                            dragProgress = p
                            val targetIndex = (p * (totalItems - 1)).toInt()
                            currentLabel = labelResolver?.invoke(targetIndex)
                            scope.launch { listState.scrollToItem(targetIndex) }
                        },
                        onDragEnd = {
                            isDragging = false
                            dragProgress = -1f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragProgress = -1f
                        }
                    )
                }
        ) {
            // Draw thumb handle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val thumbWidthPx = thumbWidth.toPx()
                val x = size.width - thumbWidthPx - 4.dp.toPx()

                // Subtle track guide
                drawLine(
                    color = trackColor,
                    start = Offset(x + thumbWidthPx / 2f, 0f),
                    end = Offset(x + thumbWidthPx / 2f, size.height),
                    strokeWidth = 2.dp.toPx()
                )

                // Draggable Thumb Handle
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(x, thumbY),
                    size = Size(thumbWidthPx, thumbHeightPx),
                    cornerRadius = CornerRadius(thumbWidthPx / 2f, thumbWidthPx / 2f)
                )
            }

            // Floating letter / category bubble indicator
            if (isDragging && !currentLabel.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = 0,
                                y = (thumbY + thumbHeightPx / 2f - with(density) { 24.dp.toPx() }).toInt()
                            )
                        }
                        .size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 3.dp
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = currentLabel ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
