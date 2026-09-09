package com.opensplit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object OpenSplitTokens {

    // Color semantics — ME3 v2.0
    val OwedPositive   = Color(0xFF1B5E20) // You are owed — positive green
    val OwedNegative   = Color(0xFFBA1A1A) // You owe — negative red
    val OwedNeutral    = Color(0xFF494551) // Settled / zero
    val OnlineDot      = Color(0xFF4ADE80)
    val AwayDot        = Color(0xFFFACC15)

    // Category color palette (10 categories, consistent everywhere)
    val CategoryFood          = Color(0xFFFF6B35)
    val CategoryTransport     = Color(0xFF4A90E2)
    val CategoryBills         = Color(0xFFF5C518)
    val CategoryShopping      = Color(0xFFE91E8C)
    val CategoryRent          = Color(0xFF6B4EFF)
    val CategoryHealth        = Color(0xFF00BCD4)
    val CategoryTravel        = Color(0xFF4CAF50)
    val CategoryEntertainment = Color(0xFFFF9800)
    val CategoryEducation     = Color(0xFF795548)
    val CategoryOther         = Color(0xFF9E9E9E)
    val CategoryGroceries     = Color(0xFF2E7D32)
    val CategoryCoffee        = Color(0xFF8D6E63)
    val CategoryDrinks        = Color(0xFF7E57C2)
    val CategoryGifts         = Color(0xFFEC407A)
    val CategoryFitness       = Color(0xFFEF5350)
    val CategorySubscriptions = Color(0xFF26A69A)
    val CategoryPets          = Color(0xFFFFB300)

    // Shape tokens — Material Expressive 3 (ME3)
    val ShapeExtraSmall = 4.dp     // 4dp: text field internal radius, micro-elements
    val ShapeSmall      = 8.dp     // 8dp: badges, small chips
    val ShapeMedium     = 12.dp    // 12dp: input fields, compact cards
    val ShapeLarge      = 16.dp    // 16dp: list item rows inside cards
    val ShapeBrandCard  = 28.dp    // 28dp: brand shape — all primary cards, sheets, dialogs
    val ShapeExtraLarge = 28.dp
    val ShapeFull       = 9999.dp  // pill: buttons, filter chips, avatars, nav active pills

    val BrandCardShape  = RoundedCornerShape(28.dp)
    val BottomSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    val ListItemShape   = RoundedCornerShape(16.dp)
    val InputShape      = RoundedCornerShape(12.dp)
    val MicroShape      = RoundedCornerShape(4.dp)
    val PillShape       = RoundedCornerShape(9999.dp)

    // Elevation & Tonal Layering
    val ElevationNone    = 0.dp
    val ElevationLow     = 1.dp
    val ElevationMedium  = 3.dp
    val ElevationHigh    = 6.dp

    // Spacing — 8dp base grid with 4dp unit
    val SpaceXS  = 4.dp
    val SpaceSM  = 8.dp
    val SpaceMD  = 12.dp
    val SpaceLG  = 16.dp
    val SpaceXL  = 24.dp
    val SpaceXXL = 32.dp

    // Motion & Spring Physics (ME3)
    val SpringStiffness = 400f
    val SpringDamping   = 35f
    val SpringMass      = 1.0f

    val MotionFast        = 100
    val MotionIcon        = 150
    val MotionChip        = 200
    val MotionStandard    = 300
    val MotionContainer   = 400
    val MotionExpressive  = 400
    val MotionSuccess     = 600
}
