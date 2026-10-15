@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.opensplit.data.ai.AI_PROVIDERS_CATALOG
import com.opensplit.data.ai.AiFeature
import com.opensplit.data.ai.AiProviderInfo
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.theme.OpenSplitIcons

/**
 * One screen, three compact blocks: provider + model + key (the thing people actually set up),
 * then the feature switches, then usage. Keys are stored encrypted on device.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    onToggleAi: (Boolean) -> Unit,
    onToggleFeature: (String, Boolean) -> Unit,
    onSetModel: (String, String) -> Unit,
    onTestKey: (providerId: String, key: String, endpoint: String?, model: String?, onResult: (Result<String>) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val provider: AiProviderInfo = AI_PROVIDERS_CATALOG.firstOrNull { it.id == activeProvider } ?: AI_PROVIDERS_CATALOG.first()
    val savedKey = providerApiKeys[provider.id].orEmpty()
    val savedModel = if (provider.id == "custom") customModel else (providerModels[provider.id] ?: provider.defaultModel)

    // Local edits; reset when the provider changes.
    var keyInput by remember(provider.id) { mutableStateOf(savedKey) }
    var modelInput by remember(provider.id, savedModel) { mutableStateOf(savedModel) }
    var endpointInput by remember(provider.id) { mutableStateOf(if (provider.id == "custom") customEndpoint else provider.defaultBaseUrl) }
    var showKey by remember { mutableStateOf(false) }
    var providerMenu by remember { mutableStateOf(false) }
    var modelMenu by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(savedKey) { if (keyInput.isBlank()) keyInput = savedKey }
    val dirty = keyInput.trim() != savedKey || modelInput.trim() != savedModel || (provider.id == "custom" && endpointInput.trim() != customEndpoint)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("AI", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(shapes = IconButtonDefaults.shapes(), onClick = onBack) { SymbolIcon(OpenSplitIcons.ArrowBack, contentDescription = "Back") } },
            actions = { Switch(checked = aiEnabled, onCheckedChange = onToggleAi, modifier = Modifier.padding(end = 12.dp)) },
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Your key stays on this device, encrypted, and is sent only to the provider you pick. Everything works without one.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ExposedDropdownMenuBox(expanded = providerMenu, onExpandedChange = { providerMenu = it }) {
                            OutlinedTextField(
                                value = provider.name, onValueChange = {}, readOnly = true, singleLine = true,
                                label = { Text("Service") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenu) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(expanded = providerMenu, onDismissRequest = { providerMenu = false }) {
                                AI_PROVIDERS_CATALOG.forEach { p ->
                                    val configured = !providerApiKeys[p.id].isNullOrBlank() || p.id == "custom"
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(p.name)
                                                Text(if (configured) "Key saved" else p.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                            }
                                        },
                                        trailingIcon = { if (configured) SymbolIcon(OpenSplitIcons.Check, contentDescription = null) },
                                        onClick = { onSelectProvider(p.id); providerMenu = false; testMessage = null }
                                    )
                                }
                            }
                        }
                        if (provider.id == "custom") {
                            OutlinedTextField(value = endpointInput, onValueChange = { endpointInput = it }, label = { Text("Endpoint (OpenAI-compatible)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = modelInput, onValueChange = { modelInput = it }, label = { Text("Model") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        } else {
                            ExposedDropdownMenuBox(expanded = modelMenu, onExpandedChange = { modelMenu = it }) {
                                OutlinedTextField(
                                    value = modelInput, onValueChange = { modelInput = it }, singleLine = true,
                                    label = { Text("Model") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenu) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryEditable)
                                )
                                ExposedDropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                                    provider.models.forEach { m -> DropdownMenuItem(text = { Text(m) }, onClick = { modelInput = m; modelMenu = false }) }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = keyInput, onValueChange = { keyInput = it; testMessage = null },
                            label = { Text(if (provider.id == "custom") "API key (optional)" else "API key") },
                            singleLine = true,
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(shapes = IconButtonDefaults.shapes(), onClick = { showKey = !showKey }) {
                                    SymbolIcon(if (showKey) OpenSplitIcons.VisibilityOff else OpenSplitIcons.Visibility, contentDescription = if (showKey) "Hide key" else "Show key")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(shapes = ButtonDefaults.shapes(), onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(provider.helpUrl))) }) { Text("Get a key") }
                            Spacer(Modifier.weight(1f))
                            OutlinedButton(shapes = ButtonDefaults.shapes(), 
                                enabled = !testing && (keyInput.isNotBlank() || provider.id == "custom"),
                                onClick = {
                                    testing = true; testMessage = "Testing…"
                                    onTestKey(provider.id, keyInput.trim(), if (provider.id == "custom") endpointInput.trim() else null, modelInput.trim().ifBlank { null }) { r ->
                                        testing = false
                                        testMessage = r.fold({ "Works. Model replied \"${it.take(30)}\"" }, { "Failed: ${it.message ?: "unknown error"}" })
                                    }
                                }
                            ) { Text("Test") }
                            Button(shapes = ButtonDefaults.shapes(), 
                                enabled = dirty,
                                onClick = {
                                    onSaveProviderKey(provider.id, keyInput.trim())
                                    if (provider.id == "custom") onSaveCustomSettings(endpointInput.trim(), modelInput.trim()) else onSetModel(provider.id, modelInput.trim())
                                }
                            ) { Text("Save") }
                        }
                        if (testing) com.opensplit.ui.components.InlineBusy("Testing key…")
                        else testMessage?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium, color = if (it.startsWith("Failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                        AiFeature.entries.forEach { feature ->
                            val on = feature.name !in disabledFeatures
                            Row(modifier = Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(feature.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(feature.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                Spacer(Modifier.width(8.dp))
                                Switch(checked = on && aiEnabled, enabled = aiEnabled, onCheckedChange = { onToggleFeature(feature.name, it) })
                            }
                        }
                        val total = usage.values.sum()
                        if (total > 0) {
                            Text(
                                "Requests from this device: " + usage.entries.joinToString(" · ") { (id, n) -> "${AI_PROVIDERS_CATALOG.find { it.id == id }?.name ?: id} $n" },
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
