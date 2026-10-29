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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .responsiveContentWidth(maxWidth = 1040.dp)
        ) {
            androidx.compose.material3.LargeTopAppBar(
                title = {
                    Text(
                        text = "Appearance & Theme",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    com.opensplit.ui.components.RoundBackButton(onClick = onBack)
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .responsiveContentWidth(maxWidth = 640.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
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
        shape = MaterialTheme.shapes.large,
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
                        shape = MaterialTheme.shapes.medium,
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
                shape = MaterialTheme.shapes.medium,
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
        shape = MaterialTheme.shapes.large,
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
                    id = "material",
                    title = "Material",
                    subtitle = "Colours from your wallpaper (Material You); soft green when the wallpaper has none",
                    icon = OpenSplitIcons.Palette,
                    swatches = listOf(Color(0xFF4C662B), Color(0xFF386663), Color(0xFFCDEDA3))
                ),
                PaletteInfo(
                    id = "monochrome",
                    title = "Monochrome",
                    subtitle = "Black, white and greys, Inter type and squarer corners",
                    icon = OpenSplitIcons.Contrast,
                    swatches = listOf(Color(0xFF18181B), Color(0xFF71717A), Color(0xFFE4E4E7))
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                palettes.forEach { p ->
                    val isSelected = palette == p.id
                    Card(
                        onClick = { onPaletteChange(p.id) },
                        shape = MaterialTheme.shapes.medium,
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

