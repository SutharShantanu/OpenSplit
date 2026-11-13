package com.opensplit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.opensplit.R

/**
 * Google Sans Flex (OFL, third_party/google-sans-flex), bundled as one variable font subset to
 * Latin + currency symbols. Each weight is an instance of the same file; API 24-25 can't apply
 * variations and falls back to the regular instance.
 */
private const val RoundedSansFlexRond = 100f

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun flex(weight: FontWeight, width: Float = 100f, round: Float = RoundedSansFlexRond) = Font(
    R.font.google_sans_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.width(width),
        FontVariation.Setting("ROND", round)
    )
)

private val weights = listOf(
    FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold
)

val AppFontFamily = FontFamily(weights.map { flex(it) })

/** Slightly wider cut for big screen titles and hero numbers with Pixel roundness. */
val DisplayFontFamily = FontFamily(weights.map { flex(it, width = 112f) })

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun mono(weight: FontWeight) = Font(
    R.font.jetbrains_mono,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

/** JetBrains Mono (OFL, third_party/jetbrains-mono): variable weight, fixed-width digits. */
val MonoFontFamily = FontFamily(weights.map { mono(it) })

/** Amounts always use the mono family so digits line up, whatever base font the user picks. */
val MoneyFontFamily = MonoFontFamily

/**
 * Pixel-style expanded display typography for hero numbers, balance headers, and dashboard titles.
 * Uses horizontal geometric scaling (scaleX = 1.25f - 1.35f) and negative letter spacing.
 */
val ExpTitleTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 54.sp,
        textGeometricTransform = androidx.compose.ui.text.style.TextGeometricTransform(scaleX = 1.3f),
        letterSpacing = (-0.02).em,
        lineHeight = 58.sp,
        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        textGeometricTransform = androidx.compose.ui.text.style.TextGeometricTransform(scaleX = 1.25f),
        letterSpacing = (-0.02).em,
        lineHeight = 46.sp,
        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        textGeometricTransform = androidx.compose.ui.text.style.TextGeometricTransform(scaleX = 1.15f),
        letterSpacing = (-0.01).em,
        lineHeight = 30.sp,
        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
    )
)

private fun style(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    line: Int,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

val Typography = Typography(
    displayLarge = style(DisplayFontFamily, FontWeight.ExtraBold, 57, 64, -0.5),
    displayMedium = style(DisplayFontFamily, FontWeight.ExtraBold, 45, 52, -0.25),
    displaySmall = style(DisplayFontFamily, FontWeight.Bold, 36, 44),
    headlineLarge = style(DisplayFontFamily, FontWeight.Bold, 34, 42),
    headlineMedium = style(DisplayFontFamily, FontWeight.Bold, 28, 36),
    headlineSmall = style(AppFontFamily, FontWeight.Bold, 24, 32),
    titleLarge = style(AppFontFamily, FontWeight.SemiBold, 22, 28),
    titleMedium = style(AppFontFamily, FontWeight.SemiBold, 16, 24, 0.1),
    titleSmall = style(AppFontFamily, FontWeight.SemiBold, 14, 20, 0.1),
    bodyLarge = style(AppFontFamily, FontWeight.Normal, 16, 24, 0.15),
    bodyMedium = style(AppFontFamily, FontWeight.Normal, 14, 20, 0.2),
    bodySmall = style(AppFontFamily, FontWeight.Normal, 12, 16, 0.3),
    labelLarge = style(AppFontFamily, FontWeight.SemiBold, 14, 20, 0.1),
    labelMedium = style(AppFontFamily, FontWeight.SemiBold, 12, 16, 0.4),
    labelSmall = style(AppFontFamily, FontWeight.SemiBold, 11, 16, 0.4)
)

/** [Typography] with [font] ("default", "system", "serif" or "mono") as the base family. */
/**
 * Optional fonts, one per style, fetched from Google Fonts through Play services the first time
 * they are picked (cached by the system after that, so the APK carries none of them). Until the
 * download finishes, text shows in the system font.
 */
private val googleFonts = androidx.compose.ui.text.googlefonts.GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private fun googleFamily(name: String) = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map {
        androidx.compose.ui.text.googlefonts.Font(androidx.compose.ui.text.googlefonts.GoogleFont(name), googleFonts, it)
    }
)

/** Key, label, family for every base font the user can pick (Settings › Appearance › Font). */
val FontChoices: List<Triple<String, String, FontFamily>> by lazy {
    listOf(
        Triple("default", "OpenSplit (Google Sans Flex)", AppFontFamily),
        Triple("system", "System (Roboto)", FontFamily.Default),
        Triple("source_sans", "Professional (Source Sans 3)", googleFamily("Source Sans 3")),
        Triple("nunito", "Friendly (Nunito)", googleFamily("Nunito")),
        Triple("poppins", "Geometric (Poppins)", googleFamily("Poppins")),
        Triple("inter", "Minimal (Inter)", googleFamily("Inter")),
        Triple("montserrat", "Display (Montserrat)", googleFamily("Montserrat")),
        Triple("serif", "Serif", FontFamily.Serif),
        Triple("mono", "Mono (JetBrains Mono)", MonoFontFamily)
    )
}

fun openSplitTypography(font: String): androidx.compose.material3.Typography {
    if (font == "default") return Typography
    val family = FontChoices.firstOrNull { it.first == font }?.third ?: return Typography
    fun TextStyle.f() = copy(fontFamily = family)
    return with(Typography) {
        copy(
            displayLarge = displayLarge.f(), displayMedium = displayMedium.f(), displaySmall = displaySmall.f(),
            headlineLarge = headlineLarge.f(), headlineMedium = headlineMedium.f(), headlineSmall = headlineSmall.f(),
            titleLarge = titleLarge.f(), titleMedium = titleMedium.f(), titleSmall = titleSmall.f(),
            bodyLarge = bodyLarge.f(), bodyMedium = bodyMedium.f(), bodySmall = bodySmall.f(),
            labelLarge = labelLarge.f(), labelMedium = labelMedium.f(), labelSmall = labelSmall.f()
        )
    }
}
