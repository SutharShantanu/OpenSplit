package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldPrimary,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = GraySecondary,
    tertiary = MintGreen,
    error = CoralRed
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldPrimary,
    background = SoftGrayBg,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF1D1B1E),
    onSurface = Color(0xFF1D1B1E),
    primaryContainer = EmeraldLight,
    onPrimaryContainer = Color(0xFF21005D), // Deep purple sleek label
    secondary = GraySecondary,
    tertiary = MintGreen,
    error = CoralRed,
    outline = Color(0xFFCAC4D0)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Enable dynamic color for Material You system color syncing
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
