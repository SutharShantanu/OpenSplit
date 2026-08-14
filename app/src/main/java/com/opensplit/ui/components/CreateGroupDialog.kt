package com.opensplit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.opensplit.data.ai.GeminiGroupIconSuggester
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, currency: String, avatarKey: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var avatarKey by remember { mutableStateOf<String?>(null) }
    var isManualSelection by remember { mutableStateOf(false) }
    var isAiSuggested by remember { mutableStateOf(false) }
    var aiSuggestedKey by remember { mutableStateOf<String?>(null) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Automatically suggest avatar as user types group name
    LaunchedEffect(name) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            // Step 1: Immediate local heuristic suggestion for zero UI latency
            val localMatch = GeminiGroupIconSuggester.getLocalHeuristicSuggestion(trimmed)
            if (localMatch != null) {
                aiSuggestedKey = localMatch
                if (!isManualSelection) {
                    avatarKey = localMatch
                    isAiSuggested = true
                }
            }

            // Step 2: Debounced Gemini 2.0 Flash AI suggestion for intelligent contextual matching
            delay(300)
            if (name.trim() == trimmed) {
                val aiMatch = GeminiGroupIconSuggester.suggestAvatarKey(trimmed)
                if (aiMatch != null) {
                    aiSuggestedKey = aiMatch
                    if (!isManualSelection) {
                        avatarKey = aiMatch
                        isAiSuggested = true
                    }
                }
            }
        } else {
            aiSuggestedKey = null
            if (!isManualSelection) {
                avatarKey = null
                isAiSuggested = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = OpenSplitTokens.SpaceLG)
                .padding(bottom = OpenSplitTokens.SpaceXL),
            verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
                ) {
                    Icon(
                        OpenSplitIcons.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Create New Group",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(OpenSplitIcons.Close, contentDescription = "Close")
                }
            }

            Text(
                text = "Organize shared expenses for trips, housemates, or events.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clickable { showAvatarPicker = true }) {
                    GroupAvatar(name = name.ifBlank { "?" }, avatarKey = avatarKey, size = 56.dp)
                }
                Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceMD))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)
                    ) {
                        TextButton(
                            onClick = { showAvatarPicker = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if (avatarKey == null) "Choose an avatar" else "Change avatar")
                        }
                        if (isAiSuggested && avatarKey != null) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        OpenSplitIcons.AutoAwesome,
                                        contentDescription = "AI Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "AI Selected",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else if (isManualSelection && aiSuggestedKey != null) {
                            TextButton(
                                onClick = {
                                    isManualSelection = false
                                    avatarKey = aiSuggestedKey
                                    isAiSuggested = true
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        OpenSplitIcons.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        "Use AI Pick",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group Name") },
                placeholder = { Text("e.g. Goa Trip, Apartment 4B, Friday Dinner") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(OpenSplitTokens.SpaceXS))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(OpenSplitTokens.SpaceSM))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreate(name.trim(), "INR", avatarKey)
                        }
                    },
                    enabled = name.isNotBlank(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Create Group", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAvatarPicker) {
        GroupAvatarPickerSheet(
            currentKey = avatarKey,
            aiSuggestedKey = aiSuggestedKey,
            onDismiss = { showAvatarPicker = false },
            onSelect = { selectedKey ->
                avatarKey = selectedKey
                isManualSelection = true
                isAiSuggested = (selectedKey == aiSuggestedKey)
            }
        )
    }
}

/**
 * Backward compatibility alias
 */
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, currency: String, avatarKey: String?) -> Unit
) {
    CreateGroupBottomSheet(onDismiss = onDismiss, onCreate = onCreate)
}
