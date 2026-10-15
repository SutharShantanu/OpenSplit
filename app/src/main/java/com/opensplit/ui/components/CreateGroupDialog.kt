@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.opensplit.data.ai.AiHeuristics
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.theme.OpenSplitTokens
import com.opensplit.util.CurrencyFormatter
import kotlinx.coroutines.delay

private val GROUP_TYPES = listOf("Home" to "home", "Trip" to "travel", "Food" to "food", "Work" to "work", "Party" to "party", "Other" to "money")
private val CURRENCIES = listOf("INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "SGD", "AED")

/**
 * New-group form. One screen, no dead space: name first (focused), type chips that also pick
 * the avatar, currency as a dropdown, and Create in the header the way full-screen dialogs do.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, currency: String, avatarKey: String?) -> Unit,
    defaultCurrency: String = "INR"
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<String?>(null) }
    var avatarKey by remember { mutableStateOf<String?>(null) }
    var avatarPickedByHand by remember { mutableStateOf(false) }
    var avatarSuggested by remember { mutableStateOf(false) }
    var showAvatars by remember { mutableStateOf(false) }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var currencyMenu by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val aiService = LocalAiService.current
    val canCreate = name.trim().length >= 2

    LaunchedEffect(Unit) { delay(150); runCatching { focus.requestFocus() } }

    // Avatar follows the name (local heuristic instantly, AI after a pause) unless chosen by hand.
    LaunchedEffect(name) {
        if (avatarPickedByHand) return@LaunchedEffect
        val trimmed = name.trim()
        if (trimmed.isBlank()) { if (type == null) { avatarKey = null; avatarSuggested = false }; return@LaunchedEffect }
        AiHeuristics.avatarKeyFor(trimmed)?.let { avatarKey = it; avatarSuggested = true }
        delay(700)
        if (name.trim() == trimmed) aiService.suggestAvatarKey(trimmed, GroupAvatarPresets.all.map { it.key })?.let { avatarKey = it; avatarSuggested = true }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OpenSplitTokens.SpaceLG)
                .padding(bottom = OpenSplitTokens.SpaceXL),
            verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceLG)
        ) {
            // Header: close · title · primary action (full-screen dialog pattern)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = onDismiss) { SymbolIcon(OpenSplitIcons.Close, contentDescription = "Close") }
                Text("New group", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(enabled = canCreate, shapes = androidx.compose.material3.ButtonDefaults.shapes(), onClick = { onCreate(name.trim(), currency, avatarKey) }) { Text("Create") }
            }

            // Avatar + name side by side
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceMD)) {
                Box(modifier = Modifier.size(76.dp)) {
                    GroupAvatar(
                        name = name.ifBlank { "G" }, avatarKey = avatarKey, size = 72.dp,
                        modifier = Modifier.align(Alignment.TopStart).clip(CircleShape).clickable { showAvatars = !showAvatars }
                    )
                    // Edit badge overlaps the avatar's corner; it is outside the avatar's clip so it never gets cut.
                    Surface(
                        onClick = { showAvatars = !showAvatars },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                        modifier = Modifier.size(28.dp).align(Alignment.BottomEnd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            SymbolIcon(OpenSplitIcons.Edit, contentDescription = "Choose avatar", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    label = { Text("Group name") },
                    placeholder = { Text("Goa trip, Flat 4B…") },
                    singleLine = true,
                    supportingText = if (avatarSuggested && !avatarPickedByHand) ({ Row { AiBadge(label = "Avatar suggested") } }) else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f).focusRequester(focus)
                )
            }

            AnimatedVisibility(visible = showAvatars) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GroupAvatarPresets.all.forEach { preset ->
                        val selected = preset.key == avatarKey
                        Surface(
                            onClick = { avatarKey = preset.key; avatarPickedByHand = true; avatarSuggested = false; showAvatars = false },
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else preset.color.copy(alpha = 0.15f),
                            border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text(preset.emoji, style = MaterialTheme.typography.titleMedium) }
                        }
                    }
                }
            }

            // Type: one tap sets a sensible avatar too
            Column(verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceXS)) {
                Text("What's it for?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Expressive toggle buttons: one connected row that wraps on narrow widths.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween), verticalArrangement = Arrangement.spacedBy(OpenSplitTokens.SpaceSM)) {
                    GROUP_TYPES.forEach { (label, key) ->
                        val preset = GroupAvatarPresets.all.firstOrNull { it.key == key }
                        androidx.compose.material3.ToggleButton(
                            checked = type == label,
                            onCheckedChange = {
                                type = label
                                if (!avatarPickedByHand) { avatarKey = key; avatarSuggested = false }
                            }
                        ) {
                            preset?.let { Text(it.emoji); Spacer(Modifier.width(6.dp)) }
                            Text(label)
                        }
                    }
                }
            }

            // Currency
            ExposedDropdownMenuBox(expanded = currencyMenu, onExpandedChange = { currencyMenu = it }) {
                OutlinedTextField(
                    value = "${CurrencyFormatter.getCurrencyFlag(currency)} $currency · ${CurrencyFormatter.getCurrencySymbol(currency)}",
                    onValueChange = {}, readOnly = true, singleLine = true,
                    label = { Text("Currency") },
                    supportingText = { Text("Locked once the group has expenses") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                    CURRENCIES.forEach { c ->
                        DropdownMenuItem(
                            text = { Text("${CurrencyFormatter.getCurrencyFlag(c)} $c · ${CurrencyFormatter.getCurrencySymbol(c)}") },
                            onClick = { currency = c; currencyMenu = false }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SymbolIcon(OpenSplitIcons.QrCode, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(OpenSplitTokens.SpaceSM))
                Text("You're the first member. Invite others with a link or QR code once the group exists.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(OpenSplitTokens.SpaceSM))
        }
    }
}
