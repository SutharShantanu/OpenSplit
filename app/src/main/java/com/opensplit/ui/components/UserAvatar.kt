package com.opensplit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.abs

@Composable
fun UserAvatar(
    photoUrl: String?,
    displayName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val cleanPhotoUrl = photoUrl?.takeIf { it.isNotBlank() }

    Surface(
        shape = CircleShape,
        modifier = modifier.size(size),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        if (!cleanPhotoUrl.isNullOrBlank()) {
            AsyncImage(
                model = cleanPhotoUrl,
                contentDescription = displayName ?: "Profile Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val name = displayName?.takeIf { it.isNotBlank() } ?: "User"
            val initial = name.trim().take(1).uppercase()
            val hue = abs(name.hashCode() % 360).toFloat()
            val avatarColor = Color.hsv(hue, 0.6f, 0.75f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = if (size >= 50.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
