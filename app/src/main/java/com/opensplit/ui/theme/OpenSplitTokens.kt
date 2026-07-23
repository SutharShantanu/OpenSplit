package com.opensplit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object OpenSplitTokens {

    // Color semantics — enforce these meanings globally, no exceptions
    // Green/red must ONLY appear for financial directionality (owed/owe)
    // Never reuse these for success/error states — use M3 system colors for those
    val OwedPositive   = Color(0xFF1B8C57) // You are owed — green
    val OwedNegative   = Color(0xFFB3261E) // You owe — red (M3 error-family)
    val OwedNeutral    = Color(0xFF79747E) // Settled / zero

    // Category color palette (10 categories, consistent everywhere — list, chip, chart, icon)
    val CategoryFood        = Color(0xFFFF6B35)
    val CategoryTransport   = Color(0xFF4A90E2)
    val CategoryBills       = Color(0xFFF5C518)
    val CategoryShopping    = Color(0xFFE91E8C)
    val CategoryRent        = Color(0xFF6B4EFF)
    val CategoryHealth      = Color(0xFF00BCD4)
    val CategoryTravel      = Color(0xFF4CAF50)
    val CategoryEntertainment = Color(0xFFFF9800)
    val CategoryEducation   = Color(0xFF795548)
    val CategoryOther       = Color(0xFF9E9E9E)

    // Shape tokens — M3 Expressive shape system
    // Use these named tokens everywhere; never hardcode dp corner values inline
    val ShapeExtraSmall = 4.dp
    val ShapeSmall      = 8.dp
    val ShapeMedium     = 12.dp
    val ShapeLarge      = 16.dp
    val ShapeExtraLarge = 28.dp
    val ShapeFull       = 50.dp   // chips, avatars, FABs

    // Elevation
    val ElevationNone    = 0.dp
    val ElevationLow     = 1.dp
    val ElevationMedium  = 3.dp
    val ElevationHigh    = 6.dp

    // Spacing — use these named values; do not inline arbitrary dp padding
    val SpaceXS  = 4.dp
    val SpaceSM  = 8.dp
    val SpaceMD  = 12.dp
    val SpaceLG  = 16.dp
    val SpaceXL  = 24.dp
    val SpaceXXL = 32.dp

    // Animation durations (M3 Expressive motion)
    val MotionFast     = 150
    val MotionStandard = 300
    val MotionExpressive = 500
}
