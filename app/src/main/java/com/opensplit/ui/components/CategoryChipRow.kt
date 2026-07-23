package com.opensplit.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food", "food & drink", "dining" -> OpenSplitTokens.CategoryFood
        "transport", "transportation" -> OpenSplitTokens.CategoryTransport
        "bills", "utilities", "services" -> OpenSplitTokens.CategoryBills
        "shopping", "groceries" -> OpenSplitTokens.CategoryShopping
        "rent", "housing" -> OpenSplitTokens.CategoryRent
        "health", "medical" -> OpenSplitTokens.CategoryHealth
        "travel", "flights", "vacation" -> OpenSplitTokens.CategoryTravel
        "entertainment", "movies", "fun" -> OpenSplitTokens.CategoryEntertainment
        "education", "books" -> OpenSplitTokens.CategoryEducation
        else -> OpenSplitTokens.CategoryOther
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food", "food & drink", "dining" -> OpenSplitIcons.CategoryFood
        "transport", "transportation" -> OpenSplitIcons.CategoryTransport
        "bills", "utilities" -> OpenSplitIcons.CategoryBills
        "shopping", "groceries" -> OpenSplitIcons.CategoryShopping
        "rent", "housing" -> OpenSplitIcons.CategoryRent
        "health", "medical" -> OpenSplitIcons.CategoryHealth
        "travel", "flights" -> OpenSplitIcons.CategoryTravel
        "entertainment", "movies" -> OpenSplitIcons.CategoryEntertainment
        "education", "books" -> OpenSplitIcons.CategoryEducation
        else -> OpenSplitIcons.CategoryOther
    }
}

val ALL_CATEGORIES = listOf(
    "All", "Food", "Transport", "Bills", "Shopping", "Rent", "Health", "Travel", "Entertainment", "Education", "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChipRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    categories: List<String> = ALL_CATEGORIES
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
    ) {
        categories.forEach { category ->
            val isSelected = category.equals(selectedCategory, ignoreCase = true)
            val icon = if (category == "All") OpenSplitIcons.Filter else getCategoryIcon(category)
            val color = if (category == "All") MaterialTheme.colorScheme.primary else getCategoryColor(category)

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else color
                    )
                },
                shape = MaterialTheme.shapes.extraLarge
            )
        }
    }
}
