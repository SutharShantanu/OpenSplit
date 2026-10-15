@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import com.opensplit.ui.theme.Symbol
import com.opensplit.data.ai.AI_PROVIDERS_CATALOG
import com.opensplit.data.ai.AiProviderInfo
import com.opensplit.data.ai.AiFeature
import com.opensplit.ui.components.SymbolIcon
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.opensplit.di.AppContainer
import com.opensplit.ui.components.ExportBottomSheet
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.components.StateLayout
import com.opensplit.ui.components.appHazeHeader
import com.opensplit.ui.components.appHazeSource
import com.opensplit.ui.theme.OpenSplitIcons
import com.opensplit.ui.viewmodel.AccountUiState
import com.opensplit.ui.viewmodel.AccountViewModel
import com.opensplit.util.CurrencyFormatter
import dev.chrisbanes.haze.HazeState

/* =========================================================================================
 * 3. SUB-SCREEN: CURRENCIES & LIVE CONVERTER (36+ CURRENCIES + REAL-TIME CALCULATOR)
 * ========================================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsSubScreen(
    defaultCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onBack: () -> Unit
) {
    var showCurrencyPickerDialog by remember { mutableStateOf(false) }

    var converterAmount by remember { mutableStateOf("100") }
    var sourceCurrency by remember { mutableStateOf("USD") }
    var targetCurrency by remember { mutableStateOf(defaultCurrency) }

    val currentCurrencyData = remember(defaultCurrency) {
        CurrencyFormatter.getCurrencyData(defaultCurrency)
    }

    val parsedAmount = converterAmount.toDoubleOrNull() ?: 0.0
    val convertedAmount = remember(parsedAmount, sourceCurrency, targetCurrency) {
        CurrencyFormatter.convert(parsedAmount, sourceCurrency, targetCurrency)
    }
    val singleUnitRate = remember(sourceCurrency, targetCurrency) {
        CurrencyFormatter.convert(1.0, sourceCurrency, targetCurrency)
    }
    val reverseUnitRate = remember(sourceCurrency, targetCurrency) {
        CurrencyFormatter.convert(1.0, targetCurrency, sourceCurrency)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Currencies & Exchange Rates", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(shapes = IconButtonDefaults.shapes(), onClick = onBack) {
                    SymbolIcon(OpenSplitIcons.ArrowBack, contentDescription = "Back")
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currentCurrencyData.symbol,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Default Expense Currency",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentCurrencyData.flag} ${currentCurrencyData.code} - ${currentCurrencyData.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showCurrencyPickerDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SymbolIcon(OpenSplitIcons.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Default Currency (36+ Available)")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SymbolIcon(
                                symbol = OpenSplitIcons.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Live Currency Converter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = converterAmount,
                            onValueChange = { converterAmount = it },
                            label = { Text("Amount to convert") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            var showSourcePicker by remember { mutableStateOf(false) }
                            Surface(
                                onClick = { showSourcePicker = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val sData = CurrencyFormatter.getCurrencyData(sourceCurrency)
                                    Text("${sData.flag} ${sData.code}", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (showSourcePicker) {
                                CurrencySelectionModal(
                                    selectedCode = sourceCurrency,
                                    onSelect = {
                                        sourceCurrency = it
                                        showSourcePicker = false
                                    },
                                    onDismiss = { showSourcePicker = false }
                                )
                            }

                            IconButton(shapes = IconButtonDefaults.shapes(), 
                                onClick = {
                                    val temp = sourceCurrency
                                    sourceCurrency = targetCurrency
                                    targetCurrency = temp
                                }
                            ) {
                                SymbolIcon(OpenSplitIcons.SwapHoriz, contentDescription = "Swap", tint = MaterialTheme.colorScheme.primary)
                            }

                            var showTargetPicker by remember { mutableStateOf(false) }
                            Surface(
                                onClick = { showTargetPicker = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val tData = CurrencyFormatter.getCurrencyData(targetCurrency)
                                    Text("${tData.flag} ${tData.code}", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (showTargetPicker) {
                                CurrencySelectionModal(
                                    selectedCode = targetCurrency,
                                    onSelect = {
                                        targetCurrency = it
                                        showTargetPicker = false
                                    },
                                    onDismiss = { showTargetPicker = false }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = CurrencyFormatter.format(convertedAmount, targetCurrency),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1 $sourceCurrency = ${String.format(java.util.Locale.US, "%.4f", singleUnitRate)} $targetCurrency  •  1 $targetCurrency = ${String.format(java.util.Locale.US, "%.4f", reverseUnitRate)} $sourceCurrency",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Exchange Rates Benchmark",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Standard benchmark rates against 1 $defaultCurrency",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        listOf("USD", "EUR", "GBP", "INR", "JPY", "CAD", "AUD", "SGD", "AED").filter { it != defaultCurrency }.take(6).forEach { code ->
                            val cData = CurrencyFormatter.getCurrencyData(code)
                            val rate = CurrencyFormatter.convert(1.0, defaultCurrency, code)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cData.flag, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(cData.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                                Text(
                                    text = "${cData.symbol}${String.format(java.util.Locale.US, "%.2f", rate)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCurrencyPickerDialog) {
        CurrencySelectionModal(
            selectedCode = defaultCurrency,
            onSelect = {
                onCurrencySelected(it)
                showCurrencyPickerDialog = false
            },
            onDismiss = { showCurrencyPickerDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionModal(
    selectedCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) CurrencyFormatter.ALL_CURRENCIES
        else CurrencyFormatter.ALL_CURRENCIES.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Currency", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search currency or code...") },
                    leadingIcon = { SymbolIcon(OpenSplitIcons.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered) { item ->
                    val isSelected = item.code.equals(selectedCode, ignoreCase = true)
                    Surface(
                        onClick = { onSelect(item.code) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item.flag, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.code} (${item.symbol})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                SymbolIcon(
                                    symbol = OpenSplitIcons.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(shapes = ButtonDefaults.shapes(), onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

