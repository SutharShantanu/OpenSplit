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
        "shopping" -> OpenSplitTokens.CategoryShopping
        "groceries" -> OpenSplitTokens.CategoryGroceries
        "rent", "housing" -> OpenSplitTokens.CategoryRent
        "health", "medical" -> OpenSplitTokens.CategoryHealth
        "travel", "flights", "vacation" -> OpenSplitTokens.CategoryTravel
        "entertainment", "movies", "fun" -> OpenSplitTokens.CategoryEntertainment
        "education", "books" -> OpenSplitTokens.CategoryEducation
        "coffee" -> OpenSplitTokens.CategoryCoffee
        "drinks" -> OpenSplitTokens.CategoryDrinks
        "gifts" -> OpenSplitTokens.CategoryGifts
        "fitness" -> OpenSplitTokens.CategoryFitness
        "subscriptions" -> OpenSplitTokens.CategorySubscriptions
        "pets" -> OpenSplitTokens.CategoryPets
        else -> OpenSplitTokens.CategoryOther
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food", "food & drink", "dining" -> OpenSplitIcons.CategoryFood
        "transport", "transportation" -> OpenSplitIcons.CategoryTransport
        "bills", "utilities" -> OpenSplitIcons.CategoryBills
        "shopping" -> OpenSplitIcons.CategoryShopping
        "groceries" -> OpenSplitIcons.CategoryGroceries
        "rent", "housing" -> OpenSplitIcons.CategoryRent
        "health", "medical" -> OpenSplitIcons.CategoryHealth
        "travel", "flights" -> OpenSplitIcons.CategoryTravel
        "entertainment", "movies" -> OpenSplitIcons.CategoryEntertainment
        "education", "books" -> OpenSplitIcons.CategoryEducation
        "coffee" -> OpenSplitIcons.CategoryCoffee
        "drinks" -> OpenSplitIcons.CategoryDrinks
        "gifts" -> OpenSplitIcons.CategoryGifts
        "fitness" -> OpenSplitIcons.CategoryFitness
        "subscriptions" -> OpenSplitIcons.CategorySubscriptions
        "pets" -> OpenSplitIcons.CategoryPets
        else -> OpenSplitIcons.CategoryOther
    }
}

val ALL_CATEGORIES = listOf(
    "All", "Food", "Groceries", "Coffee", "Drinks", "Transport", "Bills", "Shopping",
    "Rent", "Health", "Fitness", "Travel", "Entertainment", "Subscriptions",
    "Education", "Gifts", "Pets", "Other"
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
                        // Tint follows the category color; on the selected tonal container it stays legible.
                        tint = color
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.16f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    selectedLeadingIconColor = color
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = color.copy(alpha = 0.5f),
                    selectedBorderWidth = 1.dp
                )
            )
        }
    }
}
