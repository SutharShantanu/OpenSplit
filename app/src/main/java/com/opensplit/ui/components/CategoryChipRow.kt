package com.opensplit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.ui.theme.ShapeCache
import com.opensplit.ui.theme.Symbol

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

fun getCategoryIcon(category: String): Symbol {
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
    var showGridSheet by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM),
        verticalAlignment = Alignment.CenterVertically
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
                    SymbolIcon(
                        symbol = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = color
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedContainerColor = color.copy(alpha = 0.22f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    selectedLeadingIconColor = color
                )
            )
        }

        // Quick Grid Opener Chip
        AssistChip(
            onClick = { showGridSheet = true },
            label = { Text("Grid") },
            leadingIcon = {
                SymbolIcon(
                    symbol = OpenSplitIcons.CategoryBills,
                    contentDescription = "View categories grid",
                    modifier = Modifier.size(18.dp)
                )
            },
            shape = CircleShape
        )
    }

    if (showGridSheet) {
        CategoryGridSheet(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            onDismiss = { showGridSheet = false }
        )
    }
}

/**
 * Pixel-style staggered modal category selection grid with tonal container shapes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryGridSheet(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = ShapeCache.smooth32
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ALL_CATEGORIES.filter { it != "All" }) { category ->
                    val isSelected = category.equals(selectedCategory, ignoreCase = true)
                    val color = getCategoryColor(category)
                    val icon = getCategoryIcon(category)

                    Surface(
                        onClick = {
                            onCategorySelected(category)
                            onDismiss()
                        },
                        shape = ShapeCache.smooth16,
                        color = if (isSelected) color.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) BorderStroke(2.dp, color) else null,
                        modifier = Modifier.height(84.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = color.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    SymbolIcon(
                                        symbol = icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
