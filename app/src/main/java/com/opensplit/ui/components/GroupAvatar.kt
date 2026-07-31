package com.opensplit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens

/** A preset group avatar: an emoji on a color from the app's existing category palette. */
data class GroupAvatarPreset(val key: String, val emoji: String, val color: Color)

object GroupAvatarPresets {
    private val categoryPalette: List<Color> = listOf(
        OpenSplitTokens.CategoryFood, OpenSplitTokens.CategoryTransport, OpenSplitTokens.CategoryBills,
        OpenSplitTokens.CategoryShopping, OpenSplitTokens.CategoryRent, OpenSplitTokens.CategoryHealth,
        OpenSplitTokens.CategoryTravel, OpenSplitTokens.CategoryEntertainment, OpenSplitTokens.CategoryEducation,
        OpenSplitTokens.CategoryGroceries, OpenSplitTokens.CategoryCoffee, OpenSplitTokens.CategoryDrinks,
        OpenSplitTokens.CategoryGifts, OpenSplitTokens.CategoryFitness, OpenSplitTokens.CategorySubscriptions,
        OpenSplitTokens.CategoryPets
    )

    val all: List<GroupAvatarPreset> = listOf(
        "travel" to "✈️",      // ✈️
        "home" to "🏠",        // 🏠
        "food" to "🍕",        // 🍕
        "party" to "🎉",       // 🎉
        "work" to "💼",        // 💼
        "school" to "🎓",      // 🎓
        "beach" to "🏖️", // 🏖️
        "gaming" to "🎮",      // 🎮
        "car" to "🚗",         // 🚗
        "movie" to "🎬",       // 🎬
        "sports" to "⚽",            // ⚽
        "pets" to "🐶",        // 🐶
        "music" to "🎵",       // 🎵
        "books" to "📚",       // 📚
        "coffee" to "☕",            // ☕
        "shopping" to "🛒",    // 🛒
        "money" to "💰",       // 💰
        "birthday" to "🎂",    // 🎂
        "fitness" to "🏋️", // 🏋️
        "world" to "🌍"        // 🌍
    ).mapIndexed { index, (key, emoji) ->
        GroupAvatarPreset(key, emoji, categoryPalette[index % categoryPalette.size])
    }

    fun find(key: String?): GroupAvatarPreset? = all.firstOrNull { it.key == key }
}

/** Renders a group's avatar: the picked preset if set, otherwise the group name's first letter. */
@Composable
fun GroupAvatar(
    name: String,
    avatarKey: String?,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val preset = GroupAvatarPresets.find(avatarKey)
    Surface(
        shape = CircleShape,
        color = preset?.color?.copy(alpha = 0.22f) ?: MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (preset != null) {
                Text(text = preset.emoji, fontSize = (size.value * 0.5f).sp)
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Preset avatar picker styled like a photo-picker grid: a bottom sheet of tappable tiles,
 * selecting one applies immediately and closes (no separate confirm step).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupAvatarPickerSheet(
    currentKey: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OpenSplitTokens.SpaceLG)
                .padding(bottom = OpenSplitTokens.SpaceXL),
            verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)
        ) {
            Text("Choose an avatar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD),
                verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD),
                modifier = Modifier.heightIn(max = 420.dp)
            ) {
                items(GroupAvatarPresets.all) { preset ->
                    val isSelected = preset.key == currentKey
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.large)
                            .background(preset.color.copy(alpha = 0.22f))
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, preset.color, MaterialTheme.shapes.large)
                                } else Modifier
                            )
                            .clickable {
                                onSelect(preset.key)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = preset.emoji, fontSize = 28.sp)
                        if (isSelected) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                                Icon(
                                    OpenSplitIcons.Check,
                                    contentDescription = "Selected",
                                    tint = preset.color,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
