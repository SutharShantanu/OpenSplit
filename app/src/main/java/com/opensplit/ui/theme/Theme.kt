package com.opensplit.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat



// ==========================================
// Sage (default): Material tonal scheme from a green seed, so surfaces carry a
// soft tint instead of flat grey, like Pixel apps.
// ==========================================
private val SageLightColorScheme = lightColorScheme(
    primary = Color(0xFF4C662B), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEDA3), onPrimaryContainer = Color(0xFF354E16),
    secondary = Color(0xFF586249), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE7C8), onSecondaryContainer = Color(0xFF404A33),
    tertiary = Color(0xFF386663), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCECE7), onTertiaryContainer = Color(0xFF1F4E4B),
    surface = Color(0xFFF9FAEF), onSurface = Color(0xFF1A1C16),
    surfaceVariant = Color(0xFFE1E4D5), onSurfaceVariant = Color(0xFF44483D),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF3F4E9),
    surfaceContainer = Color(0xFFEEEFE3), surfaceContainerHigh = Color(0xFFE8E9DE),
    surfaceContainerHighest = Color(0xFFE2E3D8),
    surfaceBright = Color(0xFFF9FAEF), surfaceDim = Color(0xFFDADBD0),
    background = Color(0xFFF9FAEF), onBackground = Color(0xFF1A1C16),
    outline = Color(0xFF75796C), outlineVariant = Color(0xFFC5C8BA),
    inverseSurface = Color(0xFF2F312A), inverseOnSurface = Color(0xFFF1F2E6),
    inversePrimary = Color(0xFFB1D18A)
)

private val SageDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB1D18A), onPrimary = Color(0xFF1F3701),
    primaryContainer = Color(0xFF354E16), onPrimaryContainer = Color(0xFFCDEDA3),
    secondary = Color(0xFFBFCBAD), onSecondary = Color(0xFF2A331E),
    secondaryContainer = Color(0xFF404A33), onSecondaryContainer = Color(0xFFDCE7C8),
    tertiary = Color(0xFFA0D0CB), onTertiary = Color(0xFF003735),
    tertiaryContainer = Color(0xFF1F4E4B), onTertiaryContainer = Color(0xFFBCECE7),
    surface = Color(0xFF12140E), onSurface = Color(0xFFE2E3D8),
    surfaceVariant = Color(0xFF44483D), onSurfaceVariant = Color(0xFFC5C8BA),
    surfaceContainerLowest = Color(0xFF0C0F09), surfaceContainerLow = Color(0xFF1A1C16),
    surfaceContainer = Color(0xFF1E201A), surfaceContainerHigh = Color(0xFF282B24),
    surfaceContainerHighest = Color(0xFF33362E),
    surfaceBright = Color(0xFF383A32), surfaceDim = Color(0xFF12140E),
    background = Color(0xFF12140E), onBackground = Color(0xFFE2E3D8),
    outline = Color(0xFF8F9285), outlineVariant = Color(0xFF44483D),
    inverseSurface = Color(0xFFE2E3D8), inverseOnSurface = Color(0xFF2F312A),
    inversePrimary = Color(0xFF4C662B)
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Pixel-style edge-to-edge system bar controller with automatic luminance contrast calculation.
 * Ensures transparent status and navigation bars with legible icon colors.
 */
@Composable
fun OpenSplitStatusBarStyle(
    color: Color,
    useDarkIcons: Boolean = ColorUtils.calculateLuminance(color.toArgb()) > 0.55,
    navigationColor: Color? = null,
    useDarkNavigationIcons: Boolean = navigationColor
        ?.let { ColorUtils.calculateLuminance(it.toArgb()) > 0.55 }
        ?: useDarkIcons
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val updateNavigationBar = navigationColor != null
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = useDarkIcons

            if (updateNavigationBar) {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                isAppearanceLightNavigationBars = useDarkNavigationIcons
            }
        }
    }
}

/** Button shapes: pill at rest, softer square when pressed. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun appButtonShapes() = androidx.compose.material3.ButtonShapes(
    shape = androidx.compose.foundation.shape.CircleShape,
    pressedShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun appIconButtonShapes() = androidx.compose.material3.IconButtonShapes(
    shape = androidx.compose.foundation.shape.CircleShape,
    pressedShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
)

/** Large, soft corners throughout, as in Pixel / M3 Expressive apps. */
val OpenSplitShapes = androidx.compose.material3.Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun OpenSplitTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  // Wallpaper colours when they carry some hue, otherwise Sage: grey wallpapers
  // (e.g. Nothing OS) would yield a flat grey scheme.
  val dynamic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
  } else null
  val colorScheme = if (dynamic != null && isColourful(dynamic.primary)) dynamic
    else if (darkTheme) SageDarkColorScheme else SageLightColorScheme

  OpenSplitStatusBarStyle(
      color = colorScheme.background,
      navigationColor = colorScheme.background
  )

  MaterialExpressiveTheme(
    colorScheme = colorScheme,
    motionScheme = MotionScheme.expressive(),
    typography = Typography,
    shapes = OpenSplitShapes,
    content = content
  )
}

/** True when [color] has enough saturation to read as a hue rather than a grey. */
private fun isColourful(color: Color): Boolean {
  val hsv = FloatArray(3)
  android.graphics.Color.colorToHSV(color.toArgb(), hsv)
  return hsv[1] >= 0.18f
}
