package com.opensplit.ui.theme

import android.app.Activity
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import kotlin.math.abs
import kotlin.math.hypot



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
  // The system palette when it has hue. A monochrome system style (e.g. Nothing OS) makes it
  // grey whatever the wallpaper is, so build the scheme from the wallpaper's own colours instead.
  val dynamic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
  } else null
  val wallpaper = rememberWallpaperColors()
  val colorScheme = when {
    dynamic != null && isColourful(dynamic.primary) -> dynamic
    wallpaper != null -> remember(wallpaper, darkTheme) {
      val primary = wallpaper.primaryColor.toArgb()
      val secondary = wallpaper.secondaryColor?.toArgb() ?: primary
      val tertiary = wallpaper.tertiaryColor?.toArgb() ?: secondary
      wallpaperColorScheme(primary, secondary, tertiary, darkTheme)
    }
    darkTheme -> SageDarkColorScheme
    else -> SageLightColorScheme
  }

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

/** The home wallpaper's colours, updated when the wallpaper changes. Null before API 27. */
@Composable
private fun rememberWallpaperColors(): WallpaperColors? {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return null
  val context = LocalContext.current
  val manager = remember(context) { WallpaperManager.getInstance(context) }
  var colors by remember(manager) { mutableStateOf(manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)) }
  DisposableEffect(manager) {
    val listener = WallpaperManager.OnColorsChangedListener { changed, which ->
      if (which and WallpaperManager.FLAG_SYSTEM != 0) colors = changed
    }
    manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
    onDispose { manager.removeOnColorsChangedListener(listener) }
  }
  return colors
}

/**
 * A seed's hue and chroma at any Material tone. CIELAB L* is the tone axis Material uses, so the
 * standard tone pairs below keep their designed contrast.
 */
private class TonalPalette(seed: Int, maxChroma: Double) {
  private val a: Double
  private val b: Double

  init {
    val lab = DoubleArray(3)
    ColorUtils.colorToLAB(seed, lab)
    val scale = minOf(1.0, maxChroma / hypot(lab[1], lab[2]))
    a = lab[1] * scale
    b = lab[2] * scale
  }

  /** Lowers chroma until the colour fits sRGB, because clipping would shift its tone. */
  fun tone(tone: Int): Color {
    val check = DoubleArray(3)
    var scale = 1.0
    while (true) {
      val argb = ColorUtils.LABToColor(tone.toDouble(), a * scale, b * scale)
      ColorUtils.colorToLAB(argb, check)
      if (abs(check[0] - tone) < 1.0 || scale < 0.05) return Color(argb)
      scale *= 0.85
    }
  }
}

/** Material tonal scheme where each accent role comes from its own wallpaper colour. */
internal fun wallpaperColorScheme(primary: Int, secondary: Int, tertiary: Int, dark: Boolean): ColorScheme {
  val p = TonalPalette(primary, maxChroma = 48.0)
  val s = TonalPalette(secondary, maxChroma = 32.0)
  val t = TonalPalette(tertiary, maxChroma = 40.0)
  val n = TonalPalette(primary, maxChroma = 6.0)
  val v = TonalPalette(primary, maxChroma = 12.0)
  return if (dark) darkColorScheme(
    primary = p.tone(80), onPrimary = p.tone(20),
    primaryContainer = p.tone(30), onPrimaryContainer = p.tone(90),
    secondary = s.tone(80), onSecondary = s.tone(20),
    secondaryContainer = s.tone(30), onSecondaryContainer = s.tone(90),
    tertiary = t.tone(80), onTertiary = t.tone(20),
    tertiaryContainer = t.tone(30), onTertiaryContainer = t.tone(90),
    surface = n.tone(6), onSurface = n.tone(90),
    surfaceVariant = v.tone(30), onSurfaceVariant = v.tone(80),
    surfaceContainerLowest = n.tone(4), surfaceContainerLow = n.tone(10),
    surfaceContainer = n.tone(12), surfaceContainerHigh = n.tone(17),
    surfaceContainerHighest = n.tone(22),
    surfaceBright = n.tone(24), surfaceDim = n.tone(6),
    background = n.tone(6), onBackground = n.tone(90),
    outline = v.tone(60), outlineVariant = v.tone(30),
    inverseSurface = n.tone(90), inverseOnSurface = n.tone(20),
    inversePrimary = p.tone(40)
  ) else lightColorScheme(
    primary = p.tone(40), onPrimary = p.tone(100),
    primaryContainer = p.tone(90), onPrimaryContainer = p.tone(30),
    secondary = s.tone(40), onSecondary = s.tone(100),
    secondaryContainer = s.tone(90), onSecondaryContainer = s.tone(30),
    tertiary = t.tone(40), onTertiary = t.tone(100),
    tertiaryContainer = t.tone(90), onTertiaryContainer = t.tone(30),
    surface = n.tone(98), onSurface = n.tone(10),
    surfaceVariant = v.tone(90), onSurfaceVariant = v.tone(30),
    surfaceContainerLowest = n.tone(100), surfaceContainerLow = n.tone(96),
    surfaceContainer = n.tone(94), surfaceContainerHigh = n.tone(92),
    surfaceContainerHighest = n.tone(90),
    surfaceBright = n.tone(98), surfaceDim = n.tone(87),
    background = n.tone(98), onBackground = n.tone(10),
    outline = v.tone(50), outlineVariant = v.tone(80),
    inverseSurface = n.tone(20), inverseOnSurface = n.tone(95),
    inversePrimary = p.tone(80)
  )
}

/** True when [color] has enough saturation to read as a hue rather than a grey. */
private fun isColourful(color: Color): Boolean {
  val hsv = FloatArray(3)
  android.graphics.Color.colorToHSV(color.toArgb(), hsv)
  return hsv[1] >= 0.18f
}
