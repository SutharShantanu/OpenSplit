package com.opensplit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, currency: String, avatarKey: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var avatarKey by remember { mutableStateOf<String?>(null) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                TextButton(onClick = { showAvatarPicker = true }) {
                    Text(if (avatarKey == null) "Choose an avatar" else "Change avatar")
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
            onDismiss = { showAvatarPicker = false },
            onSelect = { avatarKey = it }
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
