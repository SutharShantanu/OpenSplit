package com.opensplit.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard Material Design 3 Window Width Size Classes.
 * - COMPACT: Screen width < 600dp (standard portrait phones)
 * - MEDIUM: Screen width between 600dp and 839dp (unfolded foldables, small tablets, phones in landscape)
 * - EXPANDED: Screen width >= 840dp (full-size tablets, desktops, large foldables)
 */
enum class WindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Composable
fun rememberWindowWidthClass(): WindowWidthClass {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    return when {
        screenWidthDp < 600 -> WindowWidthClass.COMPACT
        screenWidthDp < 840 -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.EXPANDED
    }
}

@Composable
fun isCompactScreen(): Boolean = rememberWindowWidthClass() == WindowWidthClass.COMPACT

@Composable
fun isMediumScreen(): Boolean = rememberWindowWidthClass() == WindowWidthClass.MEDIUM

@Composable
fun isExpandedScreen(): Boolean = rememberWindowWidthClass() == WindowWidthClass.EXPANDED

@Composable
fun isTabletOrWide(): Boolean = rememberWindowWidthClass() != WindowWidthClass.COMPACT

/**
 * Responsive modifier that centers content and limits maximum width on wide screens (tablets, foldables, landscape),
 * while expanding edge-to-edge on compact phones.
 */
fun Modifier.responsiveContentWidth(maxWidth: Dp = 840.dp): Modifier {
    return this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = maxWidth)
}

/**
 * Responsive modifier for modals, bottom sheets, and dialogs so they don't stretch excessively on tablets.
 */
fun Modifier.responsiveDialogWidth(maxWidth: Dp = 560.dp): Modifier {
    return this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = maxWidth)
}

/**
 * Responsive modifier for forms and edit screens (e.g. Add Expense, Settle Up, Profile).
 */
fun Modifier.responsiveFormWidth(maxWidth: Dp = 680.dp): Modifier {
    return this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = maxWidth)
}
