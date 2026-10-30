@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensplit.ui.components.SettingsGroupCard
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.responsiveContentWidth
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.util.CurrencyData
import com.opensplit.util.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

/* =========================================================================================
 * SUB-SCREEN: CURRENCY
 * Selected currency (flag, country, symbol), popular quick picks, and a searchable dropdown.
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsSubScreen(
    defaultCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val current = remember(defaultCurrency) { CurrencyFormatter.getCurrencyData(defaultCurrency) }
    val popular = remember { listOf("INR", "USD", "EUR", "GBP", "JPY", "AED", "CAD", "SGD") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .responsiveContentWidth(maxWidth = 760.dp)
        ) {
            TopAppBar(
                title = { Text("Currency", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = { com.opensplit.ui.components.RoundBackButton(onClick = onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SettingsGroupCard(title = "Current Currency", icon = OpenSplitIcons.Payments) {
                        SelectedCurrencyRow(current)
                    }
                }

                item {
                    SettingsGroupCard(title = "Choose Currency", icon = OpenSplitIcons.Search) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            CurrencySearchDropdown(selected = current, onSelect = onCurrencySelected)
                        }
                    }
                }

                item {
                    SettingsGroupCard(title = "Popular", icon = OpenSplitIcons.Star) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            popular.chunked(4).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { code ->
                                        val data = CurrencyFormatter.getCurrencyData(code)
                                        val selected = code.equals(defaultCurrency, ignoreCase = true)
                                        Surface(
                                            onClick = { onCurrencySelected(code) },
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${data.flag} ${data.code}", style = MaterialTheme.typography.labelLarge)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The current choice: big flag, code + name, country, symbol and a formatting example. */
@Composable
private fun SelectedCurrencyRow(current: CurrencyData) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(current.flag, fontSize = 34.sp) }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${current.code} · ${current.name}", style = MaterialTheme.typography.titleLarge)
                Text(
                    countryOf(current),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(current.symbol, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(
                "Looks like ${CurrencyFormatter.format(1250.0, current.code)}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Search field with a dropdown of matches. Filtering runs 250 ms after typing stops and off the
 * main thread, so typing stays responsive however long the list grows.
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
private fun CurrencySearchDropdown(selected: CurrencyData, onSelect: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(CurrencyFormatter.ALL_CURRENCIES) }

    LaunchedEffect(Unit) {
        snapshotFlow { query.trim() }
            .debounce(250)
            .distinctUntilChanged()
            .collect { q ->
                results = withContext(Dispatchers.Default) {
                    if (q.isEmpty()) CurrencyFormatter.ALL_CURRENCIES
                    else CurrencyFormatter.ALL_CURRENCIES.filter {
                        it.code.contains(q, true) || it.name.contains(q, true) ||
                            it.symbol.contains(q, true) || countryOf(it).contains(q, true)
                    }
                }
            }
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; expanded = true },
            singleLine = true,
            placeholder = { Text("Search currency or country", maxLines = 1) },
            leadingIcon = { SymbolIcon(OpenSplitIcons.Search, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            if (results.isEmpty()) {
                DropdownMenuItem(text = { Text("No currency matches \"$query\"") }, onClick = {}, enabled = false)
            }
            results.forEach { c ->
                DropdownMenuItem(
                    leadingIcon = { Text(c.flag, fontSize = 22.sp) },
                    text = {
                        Column {
                            Text("${c.code} · ${c.name}", style = MaterialTheme.typography.bodyLarge)
                            Text(countryOf(c), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    trailingIcon = {
                        if (c.code == selected.code) SymbolIcon(OpenSplitIcons.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        else Text(c.symbol, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    onClick = {
                        onSelect(c.code)
                        query = ""
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Country name from the flag emoji's regional-indicator letters ("🇮🇳" -> "India"). */
private fun countryOf(c: CurrencyData): String {
    val letters = c.flag.codePoints().toArray()
        .filter { it in 0x1F1E6..0x1F1FF }
        .map { 'A' + (it - 0x1F1E6) }
        .joinToString("")
    return when {
        letters == "EU" -> "Eurozone"
        letters.length == 2 -> java.util.Locale("", letters).displayCountry.ifBlank { letters }
        else -> c.name
    }
}
