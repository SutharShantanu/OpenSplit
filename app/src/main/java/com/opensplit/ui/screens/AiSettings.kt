@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.opensplit.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.opensplit.data.ai.AI_PROVIDERS_CATALOG
import com.opensplit.data.ai.AiFeature
import com.opensplit.data.ai.AiProviderInfo
import com.opensplit.ui.components.responsiveContentWidth
import com.opensplit.data.ai.AiService
import com.opensplit.ui.components.SettingsGroupCard
import com.opensplit.ui.components.SettingsGroupDivider
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.theme.OpenSplitIcons

/**
 * AI settings: saved keys (with fallback order), an "Add new API key" flow that only asks for the
 * service and the key (the model is picked automatically by the test), then feature switches and usage.
 * Keys are stored encrypted on device.
 */
@Composable
fun AiProvidersSubScreen(
    activeProvider: String,
    providerApiKeys: Map<String, String>,
    customEndpoint: String,
    customModel: String,
    onSelectProvider: (String) -> Unit,
    onSaveProviderKey: (String, String) -> Unit,
    onSaveCustomSettings: (String, String) -> Unit,
    aiEnabled: Boolean,
    disabledFeatures: Set<String>,
    providerModels: Map<String, String>,
    usage: Map<String, Int>,
    tokens: Map<String, Int> = emptyMap(),
    onToggleAi: (Boolean) -> Unit,
    onToggleFeature: (String, Boolean) -> Unit,
    onSetModel: (String, String) -> Unit,
    onTestKey: (providerId: String, key: String, endpoint: String?, model: String?, onResult: (Result<AiService.AiTestResult>) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val saved: List<AiProviderInfo> = AI_PROVIDERS_CATALOG.filter { p ->
        (p.id == "custom" && p.id == activeProvider) || (p.id != "custom" && !providerApiKeys[p.id].isNullOrBlank())
    }.sortedBy { if (it.id == activeProvider) 0 else 1 }

    // Add-key form; hidden until asked for, or when nothing is configured yet.
    var formOpen by remember { mutableStateOf(saved.isEmpty()) }
    var formProvider by remember { mutableStateOf(AI_PROVIDERS_CATALOG.first { it.id != "custom" }) }
    var keyInput by remember { mutableStateOf("") }
    var endpointInput by remember { mutableStateOf(customEndpoint) }
    var showKey by remember { mutableStateOf(false) }
    var providerMenu by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    val openForm: (AiProviderInfo) -> Unit = { p -> formProvider = p; keyInput = ""; testMessage = null; showKey = false; formOpen = true }
    // Keys are irreplaceable from inside the app, so removal always asks first.
    var pendingRemove by remember { mutableStateOf<AiProviderInfo?>(null) }
    pendingRemove?.let { p ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("Remove ${p.name} key?") },
            text = { Text("AI features will fall back to your other saved keys. You can add this key again any time.") },
            confirmButton = {
                TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error), onClick = {
                    pendingRemove = null
                    onSaveProviderKey(p.id, "")
                    // Removing the active key hands over to the next saved one.
                    if (p.id == activeProvider) saved.firstOrNull { it.id != p.id }?.let { onSelectProvider(it.id) }
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { pendingRemove = null }) { Text("Cancel") } }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .responsiveContentWidth(maxWidth = 760.dp)
        ) {
            androidx.compose.material3.TopAppBar(
                title = { Text("AI", fontWeight = FontWeight.Bold) },
                navigationIcon = { com.opensplit.ui.components.RoundBackButton(onClick = onBack) },
                actions = { Switch(checked = aiEnabled, onCheckedChange = onToggleAi, modifier = Modifier.padding(end = 12.dp)) },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            item {
                SettingsGroupCard(title = "API Keys", icon = OpenSplitIcons.Key) {
                    Text(
                        if (saved.isEmpty()) "Pick a service, paste its API key and run the test. Keys stay on this device, encrypted."
                        else "The active key answers first. If it fails (quota, outage, retired model) the next one is tried automatically.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                    saved.forEachIndexed { index, p ->
                        val model = if (p.id == "custom") customModel else (providerModels[p.id] ?: p.defaultModel)
                        SettingsGroupDivider()
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = { Text("${index + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                            headlineContent = { Text(p.name) },
                            supportingContent = { Text(if (p.id == activeProvider) "Active · $model" else "Fallback · $model", maxLines = 1) },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (p.id != activeProvider) TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { onSelectProvider(p.id) }) { Text("Use") }
                                    IconButton(shapes = com.opensplit.ui.theme.appIconButtonShapes(), onClick = { openForm(p) }) { SymbolIcon(OpenSplitIcons.Edit, contentDescription = "Replace ${p.name} key") }
                                    if (p.id != "custom") IconButton(shapes = com.opensplit.ui.theme.appIconButtonShapes(), onClick = { pendingRemove = p }) {
                                        SymbolIcon(OpenSplitIcons.Delete, contentDescription = "Remove ${p.name} key", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )
                    }
                    if (!formOpen) {
                        SettingsGroupDivider()
                        OutlinedButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { openForm(AI_PROVIDERS_CATALOG.first { it.id !in saved.map { s -> s.id } && it.id != "custom" } ) }, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            SymbolIcon(OpenSplitIcons.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add new API key")
                        }
                    }
                }
            }

            if (formOpen) item {
                SettingsGroupCard(title = "New API Key", icon = OpenSplitIcons.VpnKey) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExposedDropdownMenuBox(expanded = providerMenu, onExpandedChange = { providerMenu = it }) {
                            OutlinedTextField(
                                value = formProvider.name, onValueChange = {}, readOnly = true, singleLine = true,
                                label = { Text("Service") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenu) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(expanded = providerMenu, onDismissRequest = { providerMenu = false }) {
                                AI_PROVIDERS_CATALOG.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Column { Text(p.name); Text(p.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) } },
                                        onClick = { formProvider = p; providerMenu = false; testMessage = null }
                                    )
                                }
                            }
                        }
                        if (formProvider.id == "custom") {
                            OutlinedTextField(value = endpointInput, onValueChange = { endpointInput = it }, label = { Text("Endpoint (OpenAI-compatible)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        OutlinedTextField(
                            value = keyInput, onValueChange = { keyInput = it; testMessage = null },
                            label = { Text(if (formProvider.id == "custom") "API key (optional)" else "API key") },
                            singleLine = true,
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(shapes = com.opensplit.ui.theme.appIconButtonShapes(), onClick = { showKey = !showKey }) {
                                    SymbolIcon(if (showKey) OpenSplitIcons.VisibilityOff else OpenSplitIcons.Visibility, contentDescription = if (showKey) "Hide key" else "Show key")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(formProvider.helpUrl))) }) { Text("Get a key") }
                            Spacer(Modifier.weight(1f))
                            // Cancel moved here from the old header row; the group card owns the title now.
                            if (saved.isNotEmpty()) TextButton(shapes = com.opensplit.ui.theme.appButtonShapes(), onClick = { formOpen = false }) { Text("Cancel") }
                            Button(
                                shapes = com.opensplit.ui.theme.appButtonShapes(),
                                enabled = !testing && (keyInput.isNotBlank() || formProvider.id == "custom"),
                                onClick = {
                                    testing = true; testMessage = null
                                    val p = formProvider
                                    onTestKey(p.id, keyInput.trim(), if (p.id == "custom") endpointInput.trim() else null, null) { r ->
                                        testing = false
                                        r.fold(
                                            { res ->
                                                // Success is the save: key, the model the test settled on, and make it active when it's the first key.
                                                onSaveProviderKey(p.id, keyInput.trim())
                                                if (p.id == "custom") onSaveCustomSettings(endpointInput.trim(), res.model) else onSetModel(p.id, res.model)
                                                if (saved.isEmpty() || p.id == activeProvider) onSelectProvider(p.id)
                                                testMessage = "Connected to ${p.name} (${res.model}). Model replied \"${res.reply.take(20)}\"."
                                                keyInput = ""; formOpen = false
                                            },
                                            { testMessage = "Failed: ${it.message ?: "unknown error"}" }
                                        )
                                    }
                                }
                            ) { Text("Test & save") }
                        }
                        if (testing) com.opensplit.ui.components.InlineBusy("Testing key…")
                        else testMessage?.takeIf { it.startsWith("Failed") }?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            if (!formOpen) testMessage?.takeIf { it.startsWith("Connected") }?.let { msg ->
                item { Text(msg, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp)) }
            }

            item {
                SettingsGroupCard(title = "Features", icon = OpenSplitIcons.AutoAwesome) {
                    AiFeature.entries.forEachIndexed { index, feature ->
                        if (index > 0) SettingsGroupDivider()
                        val on = feature.name !in disabledFeatures
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(feature.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(feature.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(checked = on && aiEnabled, enabled = aiEnabled, onCheckedChange = { onToggleFeature(feature.name, it) })
                        }
                    }
                }
            }

            val total = usage.values.sum()
            if (total > 0) item {
                SettingsGroupCard(title = "Usage", icon = OpenSplitIcons.Insights) {
                    Text(
                        "Used from this device: " + usage.entries.joinToString(" · ") { (id, n) -> "${AI_PROVIDERS_CATALOG.find { it.id == id }?.name ?: id} $n calls, ${tokens[id] ?: 0} tokens" } +
                            ". Remaining quota isn't exposed by providers; check your provider dashboard.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
}
