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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
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
import com.opensplit.ui.components.SettingsGroupCard
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.responsiveContentWidth
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.Symbol

/* =========================================================================================
 * SUB-SCREEN: APPEARANCE & THEME (SYSTEM, LIGHT, DARK)
 * Responsive across phone and tablet / wide screens.
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsSubScreen(
    theme: String,
    onThemeChange: (String) -> Unit,
    font: String,
    onFontChange: (String) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    reduceMotion: Boolean,
    onReduceMotionChange: (Boolean) -> Unit,
    haptics: Boolean,
    onHapticsChange: (Boolean) -> Unit,
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
            androidx.compose.material3.TopAppBar(
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
                        onThemeChange = onThemeChange
                    )
                }
                item { FontSelectionCard(font = font, onFontChange = onFontChange) }
                item {
                    SettingsGroupCard(title = "Colour", icon = OpenSplitIcons.AutoAwesome) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            SettingsSwitchRow(
                                title = "Wallpaper colours",
                                subtitle = "Match your wallpaper. Off uses the OpenSplit green palette.",
                                checked = dynamicColor,
                                onCheckedChange = onDynamicColorChange
                            )
                        }
                    }
                }
                item {
                    SettingsGroupCard(title = "Accessibility", icon = OpenSplitIcons.Account) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "Text size follows your phone's font size setting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            SettingsSwitchRow(
                                title = "Haptic feedback",
                                subtitle = "Vibrate on tab changes, swipes and confirmations.",
                                checked = haptics,
                                onCheckedChange = onHapticsChange
                            )
                            SettingsSwitchRow(
                                title = "Reduce motion",
                                subtitle = "Skip transitions and animations. Also on when system animations are off.",
                                checked = reduceMotion,
                                onCheckedChange = onReduceMotionChange
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Base font picker; each option previews itself. Amounts stay mono regardless. */
@Composable
private fun FontSelectionCard(font: String, onFontChange: (String) -> Unit) {
    val selected = com.opensplit.ui.theme.FontChoices.firstOrNull { it.first == font }?.first ?: "default"
    SettingsGroupCard(title = "Font", icon = OpenSplitIcons.Palette) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Amounts always use a fixed-width font so digits line up.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            com.opensplit.ui.theme.FontChoices.forEach { (id, label, family) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = selected == id, role = Role.RadioButton, onClick = { onFontChange(id) })
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected == id, onClick = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = family))
                        Text(
                            "Aa Bb Cc · 1,234.50",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = family),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** A full-row switch: the whole row toggles, and TalkBack reads it as one switch. */
@Composable
private fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/* =========================================================================================
 * 2. THEME MODE SELECTOR (SYSTEM, LIGHT, DARK)
 * ========================================================================================= */
@Composable
private fun ThemeModeSelectionCard(
    theme: String,
    onThemeChange: (String) -> Unit
) {
    SettingsGroupCard(title = "Theme Mode", icon = OpenSplitIcons.Palette) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        "system" -> theme == "system" || (theme != "light" && theme != "dark")
                        "light" -> theme == "light"
                        "dark" -> theme == "dark"
                        else -> false
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(androidx.compose.material3.MaterialTheme.shapes.medium)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onThemeChange(mode) }),
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
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
