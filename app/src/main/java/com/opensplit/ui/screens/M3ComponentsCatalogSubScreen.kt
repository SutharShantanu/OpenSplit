package com.opensplit.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensplit.ui.components.AppLoadingIndicator
import java.text.SimpleDateFormat
import java.util.*

/**
 * Interactive Material 3 Component Catalog based on https://m3.material.io/components
 * Showcases every official Material 3 component with live interaction and dynamic theming.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun M3ComponentsCatalogSubScreen(
    onBack: () -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf(
        "Buttons & FABs",
        "Date & Time",
        "Loading & Progress",
        "Navigation & Bars",
        "Sheets & Menus",
        "Inputs & Controls",
        "Containment & Chips"
    )

    // Interactive states hoisted to screen level
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(initialHour = 17, initialMinute = 45)

    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Material 3 Catalog",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Official components from m3.material.io",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            // Primary Scrollable Tab Row for Categories
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (selectedCategoryIndex) {
                    0 -> {
                        // 1. BUTTONS & FABS
                        item {
                            M3SectionCard(title = "Common Buttons", description = "Filled, Elevated, Tonal, Outlined, and Text buttons") {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(onClick = { snackbarMessage = "Clicked Filled Button" }) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Filled")
                                    }
                                    ElevatedButton(onClick = { snackbarMessage = "Clicked Elevated Button" }) {
                                        Text("Elevated")
                                    }
                                    FilledTonalButton(onClick = { snackbarMessage = "Clicked Tonal Button" }) {
                                        Text("Filled Tonal")
                                    }
                                    OutlinedButton(onClick = { snackbarMessage = "Clicked Outlined Button" }) {
                                        Text("Outlined")
                                    }
                                    TextButton(onClick = { snackbarMessage = "Clicked Text Button" }) {
                                        Text("Text")
                                    }
                                }
                            }
                        }

                        item {
                            M3SectionCard(title = "Segmented Buttons (Button Groups)", description = "Single-choice and multi-choice segmented buttons") {
                                var selectedSegment by remember { mutableIntStateOf(0) }
                                val options = listOf("Day", "Week", "Month", "Year")

                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    options.forEachIndexed { index, label ->
                                        SegmentedButton(
                                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                            onClick = { selectedSegment = index },
                                            selected = index == selectedSegment
                                        ) {
                                            Text(label)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            M3SectionCard(title = "Floating Action Buttons (FABs)", description = "Extended FAB, Large FAB, Standard FAB, and Small FAB") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ExtendedFloatingActionButton(
                                        onClick = { snackbarMessage = "Extended FAB clicked" },
                                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                        text = { Text("Extended FAB") }
                                    )

                                    FloatingActionButton(
                                        onClick = { snackbarMessage = "Standard FAB clicked" }
                                    ) {
                                        Icon(Icons.Rounded.Edit, contentDescription = null)
                                    }

                                    SmallFloatingActionButton(
                                        onClick = { snackbarMessage = "Small FAB clicked" }
                                    ) {
                                        Icon(Icons.Rounded.Share, contentDescription = null)
                                    }
                                }
                            }
                        }

                        item {
                            M3SectionCard(title = "Icon Buttons", description = "Standard, Filled, Filled Tonal, and Outlined icon buttons") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { snackbarMessage = "Standard IconButton" }) {
                                        Icon(Icons.Rounded.FavoriteBorder, contentDescription = null)
                                    }
                                    FilledIconButton(onClick = { snackbarMessage = "Filled IconButton" }) {
                                        Icon(Icons.Rounded.Favorite, contentDescription = null)
                                    }
                                    FilledTonalIconButton(onClick = { snackbarMessage = "Filled Tonal IconButton" }) {
                                        Icon(Icons.Rounded.Bookmark, contentDescription = null)
                                    }
                                    OutlinedIconButton(onClick = { snackbarMessage = "Outlined IconButton" }) {
                                        Icon(Icons.Rounded.BookmarkBorder, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // 2. DATE & TIME PICKERS
                        item {
                            M3SectionCard(title = "Date Picker Dialog", description = "Material 3 DatePicker with calendar modal") {
                                val selectedDateText = remember(datePickerState.selectedDateMillis) {
                                    datePickerState.selectedDateMillis?.let {
                                        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it))
                                    } ?: "No date selected"
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Selected Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(selectedDateText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    }
                                    Button(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Pick Date")
                                    }
                                }
                            }
                        }

                        item {
                            M3SectionCard(title = "Time Picker Dialog", description = "Material 3 clock dial & time input") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Selected Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        val formattedHour = if (timePickerState.hour % 12 == 0) 12 else timePickerState.hour % 12
                                        val amPm = if (timePickerState.hour < 12) "AM" else "PM"
                                        Text(String.format("%02d:%02d %s", formattedHour, timePickerState.minute, amPm), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    }
                                    Button(onClick = { showTimePicker = true }) {
                                        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Pick Time")
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // 3. LOADING & PROGRESS
                        item {
                            M3SectionCard(title = "Expressive Loading Indicator", description = "Material 3 shape-morphing flower loader from m3.material.io") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AppLoadingIndicator(size = 48.dp)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Primary", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AppLoadingIndicator(size = 48.dp, color = MaterialTheme.colorScheme.tertiary)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Tertiary", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        item {
                            M3SectionCard(title = "Progress Indicators", description = "Circular & Linear progress indicators (indeterminate & determinate)") {
                                var progress by remember { mutableFloatStateOf(0.65f) }

                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("Progress: ${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.size(44.dp),
                                                strokeWidth = 5.dp
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text("Determinate", style = MaterialTheme.typography.labelSmall)
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(44.dp),
                                                strokeWidth = 5.dp
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text("Indeterminate", style = MaterialTheme.typography.labelSmall)
                                        }

                                        Slider(
                                            value = progress,
                                            onValueChange = { progress = it },
                                            modifier = Modifier.width(140.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // 4. NAVIGATION & BARS
                        item {
                            M3SectionCard(title = "App Bars", description = "CenterAlignedTopAppBar, MediumTopAppBar, and BottomAppBar") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CenterAlignedTopAppBar(
                                            title = { Text("Center Aligned", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                                            navigationIcon = {
                                                IconButton(onClick = {}) { Icon(Icons.Rounded.Menu, contentDescription = null) }
                                            },
                                            actions = {
                                                IconButton(onClick = {}) { Icon(Icons.Rounded.Search, contentDescription = null) }
                                            },
                                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        BottomAppBar(
                                            actions = {
                                                IconButton(onClick = {}) { Icon(Icons.Rounded.Check, contentDescription = null) }
                                                IconButton(onClick = {}) { Icon(Icons.Rounded.Edit, contentDescription = null) }
                                                IconButton(onClick = {}) { Icon(Icons.Rounded.Share, contentDescription = null) }
                                            },
                                            floatingActionButton = {
                                                FloatingActionButton(onClick = {}) {
                                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                                }
                                            },
                                            containerColor = Color.Transparent
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            M3SectionCard(title = "Badges & BadgedBox", description = "Navigation item badges with counts and alert dots") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BadgedBox(
                                        badge = { Badge { Text("8") } }
                                    ) {
                                        Icon(Icons.Rounded.Mail, contentDescription = null, modifier = Modifier.size(32.dp))
                                    }

                                    BadgedBox(
                                        badge = { Badge { Text("99+") } }
                                    ) {
                                        Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(32.dp))
                                    }

                                    BadgedBox(
                                        badge = { Badge() }
                                    ) {
                                        Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                        }

                        item {
                            M3SectionCard(title = "Navigation Drawer Items", description = "Material 3 Drawer Items with active/inactive indicators") {
                                var selectedDrawerItem by remember { mutableIntStateOf(0) }
                                val drawerItems = listOf(
                                    Triple("Inbox", Icons.Rounded.Inbox, "12"),
                                    Triple("Outbox", Icons.AutoMirrored.Rounded.Send, null),
                                    Triple("Favorites", Icons.Rounded.Favorite, "3")
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    drawerItems.forEachIndexed { index, (label, icon, badge) ->
                                        NavigationDrawerItem(
                                            label = { Text(label) },
                                            selected = selectedDrawerItem == index,
                                            onClick = { selectedDrawerItem = index },
                                            icon = { Icon(icon, contentDescription = null) },
                                            badge = badge?.let { { Text(it) } },
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // 5. SHEETS & MENUS
                        item {
                            M3SectionCard(title = "Modal Bottom Sheet", description = "Bottom sheet anchored to bottom with authentic drag handle") {
                                Button(
                                    onClick = { showBottomSheet = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Layers, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Open Modal Bottom Sheet")
                                }
                            }
                        }

                        item {
                            M3SectionCard(title = "Exposed Dropdown Menu", description = "Selectable options menu embedded in text field") {
                                var expanded by remember { mutableStateOf(false) }
                                val menuOptions = listOf("OpenAI GPT-4o", "Google Gemini 1.5", "Anthropic Claude 3.5", "Groq Llama 3.3")
                                var selectedOptionText by remember { mutableStateOf(menuOptions[0]) }

                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedOptionText,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        menuOptions.forEach { selectionOption ->
                                            DropdownMenuItem(
                                                text = { Text(selectionOption) },
                                                onClick = {
                                                    selectedOptionText = selectionOption
                                                    expanded = false
                                                },
                                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    5 -> {
                        // 6. INPUTS & CONTROLS
                        item {
                            M3SectionCard(title = "Text Fields", description = "Filled and Outlined text fields with icons and supporting text") {
                                var filledText by remember { mutableStateOf("") }
                                var outlinedText by remember { mutableStateOf("") }

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TextField(
                                        value = filledText,
                                        onValueChange = { filledText = it },
                                        label = { Text("Filled Text Field") },
                                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = outlinedText,
                                        onValueChange = { outlinedText = it },
                                        label = { Text("Outlined Text Field") },
                                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                                        supportingText = { Text("Helper text goes here") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        item {
                            M3SectionCard(title = "Switches, Checkboxes & Radio Buttons", description = "Selection controls with animated states") {
                                var switchChecked by remember { mutableStateOf(true) }
                                var triState by remember { mutableStateOf(ToggleableState.Indeterminate) }
                                var radioSelected by remember { mutableIntStateOf(0) }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Switch with Icon")
                                        Switch(
                                            checked = switchChecked,
                                            onCheckedChange = { switchChecked = it },
                                            thumbContent = {
                                                Icon(
                                                    if (switchChecked) Icons.Rounded.Check else Icons.Rounded.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                                )
                                            }
                                        )
                                    }

                                    HorizontalDivider()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Tri-State Checkbox (${triState.name})")
                                        TriStateCheckbox(
                                            state = triState,
                                            onClick = {
                                                triState = when (triState) {
                                                    ToggleableState.On -> ToggleableState.Indeterminate
                                                    ToggleableState.Indeterminate -> ToggleableState.Off
                                                    ToggleableState.Off -> ToggleableState.On
                                                }
                                            }
                                        )
                                    }

                                    HorizontalDivider()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Radio Button Group")
                                        Row {
                                            listOf("A", "B").forEachIndexed { i, label ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    RadioButton(
                                                        selected = radioSelected == i,
                                                        onClick = { radioSelected = i }
                                                    )
                                                    Text(label)
                                                    Spacer(Modifier.width(8.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            M3SectionCard(title = "Sliders & Range Sliders", description = "Continuous and discrete range selectors") {
                                var sliderValue by remember { mutableFloatStateOf(40f) }
                                var rangeValues by remember { mutableStateOf(20f..80f) }

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Slider Value: ${sliderValue.toInt()}", style = MaterialTheme.typography.labelSmall)
                                    Slider(
                                        value = sliderValue,
                                        onValueChange = { sliderValue = it },
                                        valueRange = 0f..100f,
                                        steps = 4,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text("Range: ${rangeValues.start.toInt()} - ${rangeValues.endInclusive.toInt()}", style = MaterialTheme.typography.labelSmall)
                                    RangeSlider(
                                        value = rangeValues,
                                        onValueChange = { rangeValues = it },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    6 -> {
                        // 7. CONTAINMENT & CHIPS
                        item {
                            M3SectionCard(title = "Cards (Filled, Elevated, Outlined)", description = "Material 3 Card variants") {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                                    ) {
                                        Text("Filled Card", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                                    }

                                    ElevatedCard(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Elevated Card (Elevated shadow)", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedCard(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Outlined Card (Border stroke)", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item {
                            M3SectionCard(title = "Horizontal Card Carousel", description = "Scrollable carousel of multi-item preview cards") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(listOf("Trip to Italy \uD83C\uDDEE\uD83C\uDDF9", "Apartment Rent \uD83C\uDFE0", "Dinner Party \uD83C\uDF55", "Ski Resort \u26F7\uFE0F")) { title ->
                                        Card(
                                            modifier = Modifier.width(160.dp).height(100.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.BottomStart) {
                                                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            M3SectionCard(title = "Chips", description = "Assist, Filter, Input, and Suggestion chips") {
                                var filterSelected by remember { mutableStateOf(true) }
                                var chipDeleted by remember { mutableStateOf(false) }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AssistChip(
                                        onClick = { snackbarMessage = "Assist chip clicked" },
                                        label = { Text("Assist Chip") },
                                        leadingIcon = { Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    )

                                    FilterChip(
                                        selected = filterSelected,
                                        onClick = { filterSelected = !filterSelected },
                                        label = { Text("Filter Chip") },
                                        leadingIcon = if (filterSelected) {
                                            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                        } else null
                                    )

                                    if (!chipDeleted) {
                                        InputChip(
                                            selected = false,
                                            onClick = { snackbarMessage = "Input chip clicked" },
                                            label = { Text("Input Chip") },
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = { chipDeleted = true },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Close, contentDescription = "Delete")
                                                }
                                            }
                                        )
                                    }

                                    SuggestionChip(
                                        onClick = { snackbarMessage = "Suggestion chip clicked" },
                                        label = { Text("Suggestion Chip") }
                                    )
                                }
                            }
                        }

                        item {
                            M3SectionCard(title = "List Items & Dividers", description = "Three-line list item with leading and trailing content") {
                                ListItem(
                                    headlineContent = { Text("Material 3 ListItem", fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text("Lists are continuous, vertical indexes of text and images.") },
                                    leadingContent = {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        Text("Trailing", style = MaterialTheme.typography.labelSmall)
                                    }
                                )
                            }
                        }

                        item {
                            M3SectionCard(title = "Plain & Rich Tooltips", description = "Tooltips display brief labels or rich informative messages") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                                        tooltip = { PlainTooltip { Text("This is a plain tooltip") } },
                                        state = rememberTooltipState()
                                    ) {
                                        IconButton(onClick = {}) {
                                            Icon(Icons.Rounded.Info, contentDescription = "Plain tooltip info")
                                        }
                                    }

                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                                        tooltip = {
                                            RichTooltip(
                                                title = { Text("Rich Tooltip Title") },
                                                action = { TextButton(onClick = {}) { Text("Action") } }
                                            ) {
                                                Text("Rich tooltips contain longer descriptive text and action buttons.")
                                            }
                                        },
                                        state = rememberTooltipState()
                                    ) {
                                        Button(onClick = {}) {
                                            Text("Long-press for Rich Tooltip")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modals rendered outside LazyColumn
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Select Time", fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TimePicker(state = timePickerState)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                }
            )
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Modal Bottom Sheet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Bottom sheets show secondary content anchored to the bottom of the screen. Supports swipe gestures and scrim dismissal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showBottomSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss Sheet")
                    }
                }
            }
        }
    }
}

@Composable
private fun M3SectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
