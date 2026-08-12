package com.opensplit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SoftLavender,
    background = ExpressiveDarkBg,
    surface = ExpressiveDarkSurface,
    onPrimary = Color(0xFF2D1673),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    primaryContainer = Color(0xFF422E8A),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondaryContainer = Color(0xFF2C2C35),
    onSecondaryContainer = Color(0xFFE6E1E5),
    secondary = GraySecondaryDark,
    tertiary = MintGreenDark,
    error = CoralRedDark,
    outline = Color(0xFF938F99)
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
    onPrimaryContainer = Color(0xFF21005D),
    secondary = GraySecondaryLight,
    tertiary = MintGreenLight,
    error = CoralRedLight,
    outline = Color(0xFF79747E)
  )

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun OpenSplitTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Enable dynamic color for Material You system wallpaper color syncing
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

  // MaterialExpressiveTheme (M3 Expressive) rather than MaterialTheme: it supplies the
  // expressive MotionScheme and shape defaults that the newer components — LoadingIndicator,
  // FloatingActionButtonMenu — are designed against, so they animate as specified.
  MaterialExpressiveTheme(
    colorScheme = colorScheme,
    motionScheme = MotionScheme.expressive(),
    typography = Typography,
    content = content
  )
}
