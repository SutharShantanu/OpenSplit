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
// Monochrome Palette (Minimalist B&W / Slate)
// ==========================================
private val MonochromeLightColorScheme = lightColorScheme(
    primary = Color(0xFF18181B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF27272A),
    onPrimaryContainer = Color(0xFFF4F4F5),
    secondary = Color(0xFF52525B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4E4E7),
    onSecondaryContainer = Color(0xFF18181B),
    tertiary = Color(0xFF71717A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF4F4F5),
    onTertiaryContainer = Color(0xFF27272A),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFE4E4E7),
    onSurfaceVariant = Color(0xFF52525B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F4F5),
    surfaceContainer = Color(0xFFECECEE),
    surfaceContainerHigh = Color(0xFFE4E4E7),
    surfaceContainerHighest = Color(0xFFD4D4D8),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFD4D4D8),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF18181B),
    outline = Color(0xFF71717A),
    outlineVariant = Color(0xFFD4D4D8)
)

private val MonochromeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF4F4F5),
    onPrimary = Color(0xFF18181B),
    primaryContainer = Color(0xFF3F3F46),
    onPrimaryContainer = Color(0xFFF4F4F5),
    secondary = Color(0xFFA1A1AA),
    onSecondary = Color(0xFF18181B),
    secondaryContainer = Color(0xFF27272A),
    onSecondaryContainer = Color(0xFFE4E4E7),
    tertiary = Color(0xFFD4D4D8),
    onTertiary = Color(0xFF18181B),
    tertiaryContainer = Color(0xFF3F3F46),
    onTertiaryContainer = Color(0xFFFAFAFA),
    surface = Color(0xFF121214),
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = Color(0xFF27272A),
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainerLowest = Color(0xFF09090B),
    surfaceContainerLow = Color(0xFF18181B),
    surfaceContainer = Color(0xFF1F1F23),
    surfaceContainerHigh = Color(0xFF27272A),
    surfaceContainerHighest = Color(0xFF333338),
    surfaceBright = Color(0xFF3F3F46),
    surfaceDim = Color(0xFF121214),
    background = Color(0xFF121214),
    onBackground = Color(0xFFF4F4F5),
    outline = Color(0xFF71717A),
    outlineVariant = Color(0xFF3F3F46)
)

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

/**
 * Per-theme component styling that Material's Shapes don't cover: the "pill" used by buttons,
 * chips, tabs and the nav bar, and segmented-list corners. Material = round and soft;
 * Monochrome = squarer, Nothing-style.
 */
data class AppStyle(
    val monochrome: Boolean,
    val pill: androidx.compose.ui.graphics.Shape,
    val pillPressed: androidx.compose.ui.graphics.Shape,
    val segmentOuter: androidx.compose.ui.unit.Dp,
    val segmentInner: androidx.compose.ui.unit.Dp
)

private val MaterialStyle = AppStyle(
    monochrome = false,
    pill = androidx.compose.foundation.shape.CircleShape,
    pillPressed = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    segmentOuter = 28.dp,
    segmentInner = 6.dp
)

private val MonochromeStyle = AppStyle(
    monochrome = true,
    pill = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    pillPressed = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    segmentOuter = 14.dp,
    segmentInner = 3.dp
)

val LocalAppStyle = androidx.compose.runtime.staticCompositionLocalOf { MaterialStyle }

/** Button shapes that follow the active theme style (pill in Material, squarer in Monochrome). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun appButtonShapes() = LocalAppStyle.current.let {
    androidx.compose.material3.ButtonShapes(shape = it.pill, pressedShape = it.pillPressed)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun appIconButtonShapes() = LocalAppStyle.current.let {
    androidx.compose.material3.IconButtonShapes(shape = it.pill, pressedShape = it.pillPressed)
}

/** Monochrome: tight corners for a flatter, technical look. */
private val MonoShapes = androidx.compose.material3.Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
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
  amoled: Boolean = false,
  palette: String = "material",
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val baseScheme = when (palette) {
    "monochrome" -> if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme
    // "material" (and any legacy palette id): wallpaper colours when they carry some hue,
    // otherwise Sage. Monochrome wallpapers (e.g. Nothing OS) would yield a flat grey scheme.
    else -> {
      val dynamic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      } else null
      if (dynamic != null && isColourful(dynamic.primary)) dynamic
      else if (darkTheme) SageDarkColorScheme else SageLightColorScheme
    }
  }


  // AMOLED only replaces surface tokens, so brand or dynamic accents stay intact.
  val colorScheme =
    if (darkTheme && amoled) {
      baseScheme.copy(
        surface = SurfaceAmoled,
        background = SurfaceAmoled,
        surfaceDim = SurfaceDimAmoled,
        surfaceBright = SurfaceBrightAmoled,
        surfaceContainerLowest = SurfaceContainerLowestAmoled,
        surfaceContainerLow = SurfaceContainerLowAmoled,
        surfaceContainer = SurfaceContainerAmoled,
        surfaceContainerHigh = SurfaceContainerHighAmoled,
        surfaceContainerHighest = SurfaceContainerHighestAmoled,
        surfaceVariant = SurfaceVariantAmoled
      )
    } else baseScheme

  OpenSplitStatusBarStyle(
      color = colorScheme.background,
      navigationColor = colorScheme.background
  )

  val mono = palette == "monochrome"
  androidx.compose.runtime.CompositionLocalProvider(LocalAppStyle provides if (mono) MonochromeStyle else MaterialStyle) {
    MaterialExpressiveTheme(
      colorScheme = colorScheme,
      motionScheme = MotionScheme.expressive(),
      typography = if (mono) MonoTypography else Typography,
      shapes = if (mono) MonoShapes else OpenSplitShapes,
      content = content
    )
  }
}


/** True when [color] has enough saturation to read as a hue rather than a grey. */
private fun isColourful(color: Color): Boolean {
  val hsv = FloatArray(3)
  android.graphics.Color.colorToHSV(color.toArgb(), hsv)
  return hsv[1] >= 0.18f
}
