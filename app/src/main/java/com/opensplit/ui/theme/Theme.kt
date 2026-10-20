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
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    surfaceBright = SurfaceBrightDark,
    surfaceDim = SurfaceDimDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    surfaceBright = SurfaceBrightLight,
    surfaceDim = SurfaceDimLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight
  )

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
// Emerald Palette (Mint / Forest Green)
// ==========================================
private val EmeraldLightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF0D9488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF0369A1),
    surface = Color(0xFFF6FBF7),
    onSurface = Color(0xFF0F291E),
    surfaceVariant = Color(0xFFDCECE1),
    onSurfaceVariant = Color(0xFF3D5348),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F8F3),
    surfaceContainer = Color(0xFFE7F3EB),
    surfaceContainerHigh = Color(0xFFDEEDE3),
    surfaceContainerHighest = Color(0xFFD5E7DB),
    surfaceBright = Color(0xFFF6FBF7),
    surfaceDim = Color(0xFFD5E7DB),
    background = Color(0xFFF6FBF7),
    onBackground = Color(0xFF0F291E),
    outline = Color(0xFF6E9480),
    outlineVariant = Color(0xFFB8D5C4)
)

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = Color(0xFF6EE7B7),
    onPrimary = Color(0xFF064E3B),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF134E4A),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = Color(0xFF0369A1),
    onTertiaryContainer = Color(0xFFBAE6FD),
    surface = Color(0xFF0B1712),
    onSurface = Color(0xFFE1EFE7),
    surfaceVariant = Color(0xFF1F352B),
    onSurfaceVariant = Color(0xFF9EB5A8),
    surfaceContainerLowest = Color(0xFF060F0B),
    surfaceContainerLow = Color(0xFF10211A),
    surfaceContainer = Color(0xFF162B22),
    surfaceContainerHigh = Color(0xFF1E352B),
    surfaceContainerHighest = Color(0xFF264035),
    surfaceBright = Color(0xFF2E4B3E),
    surfaceDim = Color(0xFF0B1712),
    background = Color(0xFF0B1712),
    onBackground = Color(0xFFE1EFE7),
    outline = Color(0xFF688777),
    outlineVariant = Color(0xFF274235)
)

// ==========================================
// Ocean Palette (Sapphire / Azure)
// ==========================================
private val OceanLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF2563EB),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E40AF),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF115E59),
    surface = Color(0xFFF7FAFD),
    onSurface = Color(0xFF0E2538),
    surfaceVariant = Color(0xFFDCE7F2),
    onSurfaceVariant = Color(0xFF3C4D5D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5FA),
    surfaceContainer = Color(0xFFE7EFF7),
    surfaceContainerHigh = Color(0xFFDEE9F3),
    surfaceContainerHighest = Color(0xFFD4E2EE),
    surfaceBright = Color(0xFFF7FAFD),
    surfaceDim = Color(0xFFD4E2EE),
    background = Color(0xFFF7FAFD),
    onBackground = Color(0xFF0E2538),
    outline = Color(0xFF6C8296),
    outlineVariant = Color(0xFFBDD0E2)
)

private val OceanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF7DD3FC),
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = Color(0xFF93C5FD),
    onSecondary = Color(0xFF172554),
    secondaryContainer = Color(0xFF1E40AF),
    onSecondaryContainer = Color(0xFFBFDBFE),
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF134E4A),
    tertiaryContainer = Color(0xFF115E59),
    onTertiaryContainer = Color(0xFF99F6E4),
    surface = Color(0xFF0A141D),
    onSurface = Color(0xFFE0ECF6),
    surfaceVariant = Color(0xFF1C2F40),
    onSurfaceVariant = Color(0xFF98ACB9),
    surfaceContainerLowest = Color(0xFF050C12),
    surfaceContainerLow = Color(0xFF0E1C27),
    surfaceContainer = Color(0xFF142432),
    surfaceContainerHigh = Color(0xFF1B2E3E),
    surfaceContainerHighest = Color(0xFF23384A),
    surfaceBright = Color(0xFF2D4559),
    surfaceDim = Color(0xFF0A141D),
    background = Color(0xFF0A141D),
    onBackground = Color(0xFFE0ECF6),
    outline = Color(0xFF62798A),
    outlineVariant = Color(0xFF223647)
)

// ==========================================
// Sunset Palette (Warm Amber / Coral)
// ==========================================
private val SunsetLightColorScheme = lightColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF92400E),
    secondary = Color(0xFFEA580C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFF9A3412),
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFDE68A),
    onTertiaryContainer = Color(0xFF78350F),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF2E1C0A),
    surfaceVariant = Color(0xFFEFE2D2),
    onSurfaceVariant = Color(0xFF534536),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF4EB),
    surfaceContainer = Color(0xFFF4ECE0),
    surfaceContainerHigh = Color(0xFFEFE4D4),
    surfaceContainerHighest = Color(0xFFE8DCCB),
    surfaceBright = Color(0xFFFFFDF8),
    surfaceDim = Color(0xFFE8DCCB),
    background = Color(0xFFFFFDF8),
    onBackground = Color(0xFF2E1C0A),
    outline = Color(0xFF867462),
    outlineVariant = Color(0xFFDACBB9)
)

private val SunsetDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFBBF24),
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF92400E),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = Color(0xFFFB923C),
    onSecondary = Color(0xFF431407),
    secondaryContainer = Color(0xFF9A3412),
    onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFDE68A),
    surface = Color(0xFF191209),
    onSurface = Color(0xFFF7EFE4),
    surfaceVariant = Color(0xFF35291C),
    onSurfaceVariant = Color(0xFFAFA091),
    surfaceContainerLowest = Color(0xFF0F0A04),
    surfaceContainerLow = Color(0xFF21190E),
    surfaceContainer = Color(0xFF2A2013),
    surfaceContainerHigh = Color(0xFF342819),
    surfaceContainerHighest = Color(0xFF3E3120),
    surfaceBright = Color(0xFF4D3D2A),
    surfaceDim = Color(0xFF191209),
    background = Color(0xFF191209),
    onBackground = Color(0xFFF7EFE4),
    outline = Color(0xFF7A6D5E),
    outlineVariant = Color(0xFF382C1F)
)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun OpenSplitTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  amoled: Boolean = false,
  dynamicColor: Boolean = true,
  palette: String = if (dynamicColor) "dynamic" else "lavender",
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val baseScheme = when (palette) {
    "dynamic" -> {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
      }
    }
    "monochrome" -> if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme
    "emerald" -> if (darkTheme) EmeraldDarkColorScheme else EmeraldLightColorScheme
    "ocean" -> if (darkTheme) OceanDarkColorScheme else OceanLightColorScheme
    "sunset" -> if (darkTheme) SunsetDarkColorScheme else SunsetLightColorScheme
    else -> if (darkTheme) DarkColorScheme else LightColorScheme
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

  MaterialExpressiveTheme(
    colorScheme = colorScheme,
    motionScheme = MotionScheme.expressive(),
    typography = Typography,
    content = content
  )
}

