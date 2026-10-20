@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.responsiveContentWidth
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.Symbol

/* =========================================================================================
 * SUB-SCREEN: APPEARANCE & THEME (SYSTEM, LIGHT, DARK, AMOLED & COLOR PALETTES)
 * Responsive across phone and tablet / wide screens.
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsSubScreen(
    theme: String,
    onThemeChange: (String) -> Unit,
    palette: String = "dynamic",
    onPaletteChange: (String) -> Unit = {},
    amoled: Boolean = false,
    onAmoledChange: (Boolean) -> Unit = {},
    onBack: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTabletOrWide = configuration.screenWidthDp >= 720

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .responsiveContentWidth(maxWidth = 1040.dp)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Appearance & Theme",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        SymbolIcon(OpenSplitIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            if (isTabletOrWide) {
                // Tablet / Expanded screen layout: 2-column side-by-side
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column: Live Preview & Tokens Inspector
                    Column(
                        modifier = Modifier
                            .weight(0.46f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        ThemeLivePreviewCard(theme = theme, palette = palette)
                        ThemeActiveTokensCard()
                    }

                    // Right Column: Theme Mode & Palette Selection
                    Column(
                        modifier = Modifier
                            .weight(0.54f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        ThemeModeSelectionCard(
                            theme = theme,
                            onThemeChange = onThemeChange,
                            amoled = amoled,
                            onAmoledChange = onAmoledChange
                        )
                        ColorPalettesSelectionCard(
                            palette = palette,
                            onPaletteChange = onPaletteChange
                        )
                    }
                }
            } else {
                // Phone / Compact screen layout: single scrollable column with safe bottom padding
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        ThemeLivePreviewCard(theme = theme, palette = palette)
                    }
                    item {
                        ThemeModeSelectionCard(
                            theme = theme,
                            onThemeChange = onThemeChange,
                            amoled = amoled,
                            onAmoledChange = onAmoledChange
                        )
                    }
                    item {
                        ColorPalettesSelectionCard(
                            palette = palette,
                            onPaletteChange = onPaletteChange
                        )
                    }
                    item {
                        ThemeActiveTokensCard()
                    }
                }
            }
        }
    }
}

/* =========================================================================================
 * 1. LIVE THEME PREVIEW CARD
 * ========================================================================================= */
@Composable
private fun ThemeLivePreviewCard(theme: String, palette: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SymbolIcon(
                            symbol = OpenSplitIcons.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                val modeName = when (theme) {
                    "light" -> "Light"
                    "dark" -> "Dark"
                    "amoled" -> "AMOLED"
                    else -> "System"
                }
                val palName = when (palette) {
                    "monochrome" -> "Monochrome"
                    "emerald" -> "Emerald"
                    "ocean" -> "Ocean"
                    "sunset" -> "Sunset"
                    "lavender" -> "Lavender"
                    else -> "Dynamic"
                }
                Text(
                    text = "$modeName · $palName",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mini screen preview mockup
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SymbolIcon(
                                    symbol = OpenSplitIcons.Groups,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Goa Vacation Trip",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "4 members · All settled up",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "+₹2,450",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            SymbolIcon(
                                symbol = OpenSplitIcons.Settle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Settle", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            SymbolIcon(
                                symbol = OpenSplitIcons.AddExpense,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Expense", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/* =========================================================================================
 * 2. THEME MODE SELECTOR (SYSTEM, LIGHT, DARK, AMOLED)
 * ========================================================================================= */
@Composable
private fun ThemeModeSelectionCard(
    theme: String,
    onThemeChange: (String) -> Unit,
    amoled: Boolean,
    onAmoledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Theme Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose how OpenSplit responds to light and dark environments.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Way Mode Switcher Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    Triple("system", "System", OpenSplitIcons.AutoAwesome),
                    Triple("light", "Light", OpenSplitIcons.WbSunny),
                    Triple("dark", "Dark", OpenSplitIcons.DarkMode)
                ).forEach { (mode, label, icon) ->
                    val isSelected = when (mode) {
                        "system" -> theme == "system" || (theme != "light" && theme != "dark" && theme != "amoled")
                        "light" -> theme == "light"
                        "dark" -> theme == "dark" || theme == "amoled"
                        else -> false
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onThemeChange(mode) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        },
                        border = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    SymbolIcon(
                                        symbol = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            RadioButton(
                                selected = isSelected,
                                onClick = { onThemeChange(mode) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AMOLED Pure Black Toggle
            val isAmoledActive = amoled || theme == "amoled"
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val next = !isAmoledActive
                            onAmoledChange(next)
                            if (next && theme == "light") {
                                onThemeChange("dark")
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isAmoledActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            SymbolIcon(
                                symbol = OpenSplitIcons.Contrast,
                                contentDescription = null,
                                tint = if (isAmoledActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AMOLED Pure Black",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Uses true #000000 black surfaces for OLED power savings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isAmoledActive,
                        onCheckedChange = { checked ->
                            onAmoledChange(checked)
                            if (checked && theme == "light") {
                                onThemeChange("dark")
                            }
                        }
                    )
                }
            }
        }
    }
}

/* =========================================================================================
 * 3. COLOR PALETTES & ACCENTS (MONOCHROME, DYNAMIC, EMERALD, OCEAN, SUNSET, LAVENDER)
 * ========================================================================================= */
private data class PaletteInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: Symbol,
    val swatches: List<Color>
)

@Composable
private fun ColorPalettesSelectionCard(
    palette: String,
    onPaletteChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Color Style & Palette",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Customize the primary accent color and visual mood of the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            val palettes = listOf(
                PaletteInfo(
                    id = "monochrome",
                    title = "Monochrome",
                    subtitle = "Pure minimalist black, white, and slate grayscale",
                    icon = OpenSplitIcons.Contrast,
                    swatches = listOf(Color(0xFF18181B), Color(0xFF71717A), Color(0xFFE4E4E7))
                ),
                PaletteInfo(
                    id = "dynamic",
                    title = "Dynamic (Material You)",
                    subtitle = "Harmonizes with device wallpaper on Android 12+",
                    icon = OpenSplitIcons.AutoAwesome,
                    swatches = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                ),
                PaletteInfo(
                    id = "emerald",
                    title = "Emerald Mint",
                    subtitle = "Fresh mint and emerald green finance accents",
                    icon = OpenSplitIcons.Eco,
                    swatches = listOf(Color(0xFF059669), Color(0xFF0D9488), Color(0xFFD1FAE5))
                ),
                PaletteInfo(
                    id = "ocean",
                    title = "Ocean Sapphire",
                    subtitle = "Vibrant sapphire, sky blue, and deep azure",
                    icon = OpenSplitIcons.WaterDrop,
                    swatches = listOf(Color(0xFF0284C7), Color(0xFF2563EB), Color(0xFFE0F2FE))
                ),
                PaletteInfo(
                    id = "sunset",
                    title = "Sunset Amber",
                    subtitle = "Warm terracotta, amber glow, and golden peach",
                    icon = OpenSplitIcons.WbSunny,
                    swatches = listOf(Color(0xFFD97706), Color(0xFFEA580C), Color(0xFFFEF3C7))
                ),
                PaletteInfo(
                    id = "lavender",
                    title = "OpenSplit Violet",
                    subtitle = "Classic royal purple and soft lilac identity",
                    icon = OpenSplitIcons.Palette,
                    swatches = listOf(Color(0xFF381E72), Color(0xFF63597C), Color(0xFFE1D4FD))
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                palettes.forEach { p ->
                    val isSelected = palette == p.id
                    Card(
                        onClick = { onPaletteChange(p.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            }
                        ),
                        border = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    SymbolIcon(
                                        symbol = p.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = p.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (p.id == "monochrome") {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "POPULAR",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = p.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Swatch preview dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy((-6).dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                p.swatches.forEach { c ->
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            RadioButton(
                                selected = isSelected,
                                onClick = { onPaletteChange(p.id) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/* =========================================================================================
 * 4. ACTIVE SYSTEM COLOR TOKENS
 * ========================================================================================= */
@Composable
private fun ThemeActiveTokensCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Active Color Tokens",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Live Material 3 tokens dynamically computed for your active combination.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Primary" to MaterialTheme.colorScheme.primary,
                    "Secondary" to MaterialTheme.colorScheme.secondary,
                    "Tertiary" to MaterialTheme.colorScheme.tertiary,
                    "Container" to MaterialTheme.colorScheme.primaryContainer
                ).forEach { (name, color) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
