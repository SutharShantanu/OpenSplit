# 10 — Design System Recommendations & Reusable Component Backlog

This document establishes the architectural blueprint for refining OpenSplit's design system tokens and outlines the backlog of reusable composables to implement across the application.

---

## 1. Design Token Improvement Proposal

### 1.1 Semantic Financial Color Tokens
To resolve WCAG AA contrast failures in dark mode ([M3E-P0-01]), semantic financial colors must be elevated into dynamic extension properties on `ColorScheme`:

```kotlin
// ui/theme/Color.kt

/**
 * Dynamic financial color tokens ensuring WCAG 2.2 AA (>= 4.5:1) compliance in both themes.
 */
val ColorScheme.financialPositive: Color
    @Composable
    get() = if (surface.luminance() > 0.5f) Color(0xFF1B6C3A) else Color(0xFF85DF9B)

val ColorScheme.financialPositiveContainer: Color
    @Composable
    get() = if (surface.luminance() > 0.5f) Color(0xFFD4F5D8) else Color(0xFF0F3E1E)

val ColorScheme.financialNegative: Color
    @Composable
    get() = if (surface.luminance() > 0.5f) Color(0xFFBA1A1A) else Color(0xFFFFB4AB)

val ColorScheme.financialNegativeContainer: Color
    @Composable
    get() = if (surface.luminance() > 0.5f) Color(0xFFFFDAD6) else Color(0xFF690005)

val ColorScheme.financialNeutral: Color
    @Composable
    get() = onSurfaceVariant
```

### 1.2 Category Tonal Palette Architecture
Replace static hex constants in `OpenSplitTokens.kt` with a dynamic tonal generator providing accessible container and on-container pairs:

```kotlin
data class CategoryToken(
    val container: Color,
    val onContainer: Color
)

@Composable
fun getCategoryTonalPair(category: String): CategoryToken {
    val dark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f
    return when (category.lowercase()) {
        "food" -> if (dark) CategoryToken(Color(0xFF5D2800), Color(0xFFFFDBCB)) else CategoryToken(Color(0xFFFFDBCB), Color(0xFF331200))
        "transport" -> if (dark) CategoryToken(Color(0xFF003D66), Color(0xFFCBE6FF)) else CategoryToken(Color(0xFFCBE6FF), Color(0xFF001E33))
        "bills" -> if (dark) CategoryToken(Color(0xFF474000), Color(0xFFFFE870)) else CategoryToken(Color(0xFFFFF0A0), Color(0xFF232000))
        "shopping" -> if (dark) CategoryToken(Color(0xFF591040), Color(0xFFFFD8EC)) else CategoryToken(Color(0xFFFFD8EC), Color(0xFF3B002A))
        else -> if (dark) CategoryToken(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)
                else CategoryToken(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
```

### 1.3 Unified Shape Scale
Standardize corner radii across the entire application into a 5-tier system:

```kotlin
object OpenSplitShapeTokens {
    val Micro       = RoundedCornerShape(4.dp)   // Micro-badges, tag indicators
    val Input       = RoundedCornerShape(12.dp)  // Text fields, search fields
    val RowItem     = RoundedCornerShape(16.dp)  // List rows, segmented tiles
    val BrandCard   = RoundedCornerShape(28.dp)  // Primary cards, dialogs, sheets
    val Pill        = RoundedCornerShape(50.dp)  // Buttons, chips, active nav indicators
}
```

---

## 2. Reusable Component Backlog & Blueprints

Below are the 5 core composables to build in `ui/components/` to replace legacy patterns.

### Component 1: `SplitButtonAction`
**Location**: `ui/components/SplitButtonActions.kt`  
**Purpose**: Replaces deprecated `SplitButtonLayout` and plain action buttons with stable M3 Expressive `SplitButton`.

```kotlin
package com.opensplit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.Symbol

data class SplitMenuItem(
    val icon: Symbol,
    val label: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SplitButtonAction(
    label: String,
    primaryIcon: Symbol,
    onPrimaryClick: () -> Unit,
    menuItems: List<SplitMenuItem>,
    modifier: Modifier = Modifier,
    trailingDescription: String = "More options"
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        SplitButton(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(onClick = onPrimaryClick) {
                    SymbolIcon(primaryIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    checked = expanded,
                    onCheckedChange = { expanded = it }
                ) {
                    SymbolIcon(
                        OpenSplitIcons.Dropdown,
                        contentDescription = trailingDescription,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = if (expanded) 180f else 0f }
                    )
                }
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            menuItems.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    leadingIcon = { SymbolIcon(item.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        expanded = false
                        item.onClick()
                    }
                )
            }
        }
    }
}
```

---

### Component 2: `ConnectedButtonGroup`
**Location**: `ui/components/ConnectedButtonGroup.kt`  
**Purpose**: Native M3 Expressive connected button group replacing hand-rolled `Row` corner calculations.

```kotlin
package com.opensplit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConnectedButtonGroup(
    items: List<T>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    iconProvider: ((T) -> Symbol?)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onSelectIndex(index) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    items.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        role = Role.RadioButton
                        selected = isSelected
                    }
            ) {
                iconProvider?.invoke(item)?.let { symbol ->
                    SymbolIcon(symbol, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                }
                Text(labelProvider(item), maxLines = 1)
            }
        }
    }
}
```

---

### Component 3: `AccessibleLinearWavyProgressIndicator`
**Location**: `ui/components/WavyProgressIndicators.kt`  
**Purpose**: Organic wavy progress indicator for budget and debt ratios, with full accessibility range info.

```kotlin
package com.opensplit.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccessibleWavyRatioBar(
    progress: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    val clamped = progress.coerceIn(0f, 1f)
    LinearWavyProgressIndicator(
        progress = { clamped },
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
            },
        color = color,
        trackColor = trackColor
    )
}
```

---

### Component 4: `AccessibleMoneyText`
**Location**: `ui/components/AccessibleMoneyText.kt`  
**Purpose**: Stably formatted, non-recomposing tabular currency display with font-scale safety.

```kotlin
package com.opensplit.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.MoneyFontFamily

@Composable
fun AccessibleMoneyText(
    amount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    prefixSign: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.displaySmall,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val formattedNumber = com.opensplit.util.CurrencyFormatter.format(kotlin.math.abs(amount), showSymbol = false)
    val displaySign = prefixSign ?: if (amount > 0.01) "+" else if (amount < -0.01) "-" else ""
    val fullText = "$displaySign$currencySymbol$formattedNumber"

    Row(
        verticalAlignment = Alignment.Baseline,
        modifier = modifier.semantics(mergeDescendants = true) { }
    ) {
        if (displaySign.isNotEmpty()) {
            Text(
                text = displaySign,
                style = textStyle.copy(fontFamily = MoneyFontFamily, color = color),
                modifier = Modifier.padding(end = 2.dp)
            )
        }
        Text(
            text = currencySymbol,
            style = textStyle.copy(
                fontSize = textStyle.fontSize * 0.75f,
                fontFamily = MoneyFontFamily,
                color = color.copy(alpha = 0.85f)
            ),
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = formattedNumber,
            style = textStyle.copy(fontFamily = MoneyFontFamily, color = color, fontWeight = FontWeight.Bold),
            maxLines = 1,
            softWrap = false
        )
    }
}
```
