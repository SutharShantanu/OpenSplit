# 02 — Material 3 Expressive Foundations & Component Catalog Audit

This document provides a deep audit of OpenSplit's design foundations and evaluates the codebase against the complete **Material 3 Expressive Component Catalog** (`androidx.compose.material3:1.5.0-rc01`).

---

## 1. Color System & Tonal Palettes

### 1.1 Theming Architecture
OpenSplit defines a custom dual-palette dynamic color engine in [Theme.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/theme/Theme.kt#L45-L81):
- **Sage Light Scheme**: Primary `#4C662B`, Container `#CDEDA3`, Surface `#F9FAEF`.
- **Sage Dark Scheme**: Primary `#B1D18A`, Container `#354E16`, Surface `#12140E`.
- **Dynamic Color**: Leverages `dynamicLightColorScheme` / `dynamicDarkColorScheme` on Android 12+ (API 31+). For devices with low saturation (e.g. Nothing OS monochrome themes), it extracts wallpaper colors via `WallpaperManager` and generates CIELAB tonal palettes via a custom `TonalPalette` generator (`Theme.kt:240`).

### 1.2 Critical Color Deficiencies
1. **Hardcoded Static Financial Colors**:
   In [OpenSplitTokens.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/theme/OpenSplitTokens.kt#L10-L15):
   ```kotlin
   val OwedPositive = Color(0xFF1B5E20) // Deep forest green
   val OwedNegative = Color(0xFFBA1A1A) // Deep red
   val OwedNeutral  = Color(0xFF494551) // Grey
   ```
   **Defect**: In Dark Theme, `Color(0xFF1B5E20)` is drawn directly over `surfaceContainer` (`#1E201A`) and `surface` (`#12140E`).
   - Contrast ratio of `#1B5E20` against `#12140E` is **1.86:1**.
   - This causes an immediate **WCAG 2.2 AA failure** (minimum required: 4.5:1 for body text, 3.0:1 for large display text).
   - In [HeroBalanceMotif.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/HeroBalanceMotif.kt#L28-L38), `getBalanceColor()` returns these static colors regardless of light/dark mode.
2. **Hardcoded Static Category Tokens**:
   All 17 category colors in `OpenSplitTokens.kt:17-34` (`CategoryFood = Color(0xFFFF6B35)`, `CategoryTransport = Color(0xFF4A90E2)`, etc.) are static sRGB constants with fixed luminance. When rendered in dark mode inside category chips or chart indicators, they create visual glare and uneven contrast.
3. **Missing Expressive Surface Roles**:
   M3 Expressive relies heavily on `surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`, and `surfaceContainerHighest` to convey depth instead of drop shadows. OpenSplit implements these roles in `Theme.kt`, but several screens (such as `ActivityScreen.kt` and `ExpenseDetailScreen.kt`) fall back to `Surface(tonalElevation = 2.dp)` or raw `MaterialTheme.colorScheme.surface`, producing a flat appearance.

---

## 2. Typography & Font Mechanics

OpenSplit configures a variable font stack in [Type.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/theme/Type.kt):
- **Base Family**: Google Sans Flex variable font (`res/font/google_sans_flex.ttf`) with axis settings for `wght`, `wdth`, and custom roundness `ROND = 100f`.
- **Numeric Font**: JetBrains Mono (`res/font/jetbrains_mono.ttf`) with variable weights for all currency values and tabular numbers.
- **Display Typography**: `ExpTitleTypography` configured with geometric width transformations (`scaleX = 1.3f`) and negative letter spacing (`letterSpacing = -0.02.em`).

### Typography Deficiencies
1. **Artificial Geometric Distortion (`scaleX = 1.3f`)**:
   In `Type.kt:62, 71, 80`, display styles use `textGeometricTransform = TextGeometricTransform(scaleX = 1.3f)`. Applying synthetic horizontal stretching to a font that already contains variable width axes (`wdth = 112f`) creates uneven stroke weights, letter clipping in small viewports, and baseline misalignment.
2. **Missing `includeFontPadding = false` Standard**:
   While `ExpTitleTypography` sets `includeFontPadding = false`, the main `Typography` definition in `Type.kt:101-117` does not define `platformStyle = PlatformTextStyle(includeFontPadding = false)`. This causes vertical centering jitter on older Android versions.
3. **Hardcoded Text Spanning**:
   In `HeroBalanceMotif.kt:73`, a dynamic font resizing workaround (`fontSizeMultiplier *= 0.88f`) calculates size in `onTextLayout`. In Compose, this triggers an additional recomposition loop when overflowing, producing frame drops during rapid balance updates.

---

## 3. Shape System & Expressive Polygons

### 3.1 Shape Cache Implementation
In [ShapeCache.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/theme/ShapeCache.kt), polygon shapes from `androidx.graphics.shapes` are cached:
- `MaterialShapes.Cookie9Sided`: Intended for user avatars.
- `MaterialShapes.Clover8Leaf`: Intended for group collection motifs.
- `MaterialShapes.SoftBurst`: Intended for empty-state badges.
- `MaterialShapes.Sunny`: Intended for insights cards.

### 3.2 Shape Inconsistencies
1. **Disconnected Shape Usage**:
   Despite having `ShapeCache.kt`, actual screens inconsistently bypass it. For example, [UserAvatar.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/UserAvatar.kt) defaults to plain `CircleShape`, and [GroupAvatar.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/GroupAvatar.kt) uses `RoundedCornerShape(12.dp)` instead of `MaterialShapes.Clover8Leaf`.
2. **Conflicting Corner Radius Tokens**:
   `OpenSplitTokens.kt:36-49` defines `ShapeBrandCard = 28.dp`, but screens use hardcoded shapes (`RoundedCornerShape(16.dp)`, `RoundedCornerShape(20.dp)`, `RoundedCornerShape(32.dp)`), fragmenting the visual rhythm.
3. **Button Press Morphing**:
   While `appButtonShapes()` in `Theme.kt:129` defines a rest shape of `CircleShape` and pressed shape of `RoundedCornerShape(16.dp)`, only a fraction of buttons across the app actually pass `shapes = appButtonShapes()`. Most buttons use standard Compose defaults.

---

## 4. Motion & Reduced Motion

### 4.1 Motion Scheme
OpenSplit configures M3 Expressive motion in [Theme.kt:180](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/theme/Theme.kt#L180):
- Uses `MotionScheme.expressive()`.
- Implements `ReducedMotionScheme` which overrides all spatial and effects specs with `snap()`.
- System accessibility listener reads `Settings.Global.ANIMATOR_DURATION_SCALE` to automatically enable reduced motion when system animations are toggled off.

### 4.2 Motion Deficiencies
1. **Global Volatile State Antipattern**:
   In [OpenSplitMotion.kt:44](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/theme/OpenSplitMotion.kt#L44), `var reduced = false` is stored in a volatile variable on an `object` rather than inside a `CompositionLocal`. When a user toggles Reduce Motion in `AppearanceSettingsSubScreen`, navigation transitions do not cleanly react without an app restart or activity recreation.
2. **No Predictive Back Progress Handling**:
   The transitions in `MainNavHost` (`OpenSplitNavGraph.kt:133-136`) use standard `EnterTransition` / `ExitTransition` without integrating `SeekableTransitionState` or Android 14+ Predictive Back gestures. Users swiping back see a sudden pop rather than a smooth interactive scale-down.

---

## 5. M3 Expressive Component Catalog Audit

Below is the exhaustive audit of every component in the M3 Expressive catalog (`androidx.compose.material3:1.5.0-rc01`), comparing the target usage against the actual code.

### Group A: New M3 Expressive Components (Not in Baseline M3)

| Component | Target Location in OpenSplit | Current Code State | The Gap | The Fix |
| :--- | :--- | :--- | :--- | :--- |
| **A1. SplitButton** | 1. GroupDetail: "Settle Up" primary action<br>2. AddExpense: "Add" primary with template/recurring<br>3. Analytics: "Export" with CSV/PDF/JSON | `GroupDetailScreen.kt:237` uses deprecated `SplitButtonLayout` with manual `DropdownMenu`. `AddExpenseScreen.kt:1201` uses a plain `Button`. `AnalyticsScreen.kt` has no direct split button. | Deprecated API used; action variants are disconnected from the primary button. | Replace with native `SplitButton(leadingButton = { SplitButtonDefaults.LeadingButton(onClick) }, trailingButton = { SplitButtonDefaults.TrailingButton(checked) })` and anchor an expressive dropdown. |
| **A2. ButtonGroup (Connected & Standard)** | 1. AddExpense: Split Mode selector (Equal/Exact/%/Shares/Itemized)<br>2. Expense list filters: All/This Month/Pending<br>3. Settings: Theme mode (System/Light/Dark)<br>4. Header action clusters | `HeaderAction.kt:48` simulates connected buttons with a custom `Row` and manual corner math (`corners(24.dp, 8.dp)`). `AddExpenseScreen.kt:1009` uses individual `ToggleButton`s in a horizontal scroll. | Re-inventing connected shapes manually; missing press compression physics and focus management. | Migrate all segmented clusters to `ButtonGroup` with `ButtonGroupDefaults.connectedLeadingButtonShapes()`, `connectedMiddleButtonShapes()`, and `connectedTrailingButtonShapes()`. |
| **A3. FloatingActionButtonMenu (FAB Menu)** | 1. Main Dashboard: Create menu ("Add expense", "Settle up", "New group", "Add friend")<br>2. Analytics: Export menu ("Export PDF", "Export CSV", "Share image") | Implemented in `ExpressiveControls.kt:119` and wired in `MainDashboard.kt:214` for Groups. Missing on Analytics and GroupDetail screens. | Fragmented FAB behavior: Groups has a menu FAB, People has a plain circular FAB, Analytics has no FAB. | Standardize `ActionFabMenu` across all creation hubs. |
| **A4. LoadingIndicator (Shape-Morphing)** | Global loading indicators, submit buttons, pull-to-refresh headers | Implemented in `AppLoadingIndicator.kt:26` via M3E `LoadingIndicator`. Used across `MainDashboard.kt`, `AddExpenseScreen.kt`, and `AccountScreen.kt`. | Well-adopted, but `InlineBusy` in `Busy.kt` still references standard size without minimum accessible layout constraints. | Ensure all inline indicators maintain 48dp touch bounds if wrapped inside interactive surfaces. |
| **A5. FloatingToolbar / DockedToolbar** | Bottom action bar in `GroupDetailScreen` and `AddExpenseScreen` bottom bar | `FloatingNavBar.kt:77` creates a custom `Surface` with `CircleShape` and 72dp height. | Custom floating pill instead of the official M3 `FloatingToolbar` or `DockedToolbar`. Cannot dock or morph dynamically. | Refactor `FloatingNavBar` to wrap `FloatingToolbar` or migrate to `ShortNavigationBar`. |
| **A6. WavyProgressIndicator** | 1. Ratio progress bar in `HeroBalanceCard`<br>2. Budget consumption bar in `AnalyticsScreen`<br>3. Storage usage indicator in `AccountScreen` | Custom nested `Box` with static background in `HeroBalanceMotif.kt:250`. `LinearProgressIndicator` in budget cards. | Static box has no accessibility semantics (`progressBarRangeInfo`) and lacks M3 Expressive organic wavy animation. | Replace custom progress boxes with `LinearWavyProgressIndicator` or `CircularWavyProgressIndicator`. |
| **A7. NavigationSuiteScaffold** | Adaptive top-level navigation switching between Bar, Rail, and Drawer | `MainDashboard.kt:293` has manual `if (wide) WideNavigationRail else FloatingNavBar`. | Hand-rolled breakpoint check (`screenWidthDp >= 600`) without window size classes or foldable posture support. | Adopt `NavigationSuiteScaffold` with `NavigationSuiteType.NavigationBar` (compact) and `NavigationSuiteType.NavigationRail` (medium/expanded). |

---

### Group B: Refined Material 3 Components

| Component | Target Location in OpenSplit | Current Code State | The Gap | The Fix |
| :--- | :--- | :--- | :--- | :--- |
| **B1. Button (Expressive Sizes & Shapes)** | All forms, dialogs, and primary actions | Buttons use `ButtonDefaults.shapesFor(MediumContainerHeight)` in `AddExpenseScreen:574`, but others use un-styled `Button`. | Inconsistent button heights (36dp, 40dp, 48dp, 56dp) across screens. | Unify using M3E `ButtonDefaults.XSmallContainerHeight` through `XLargeContainerHeight` with pill-to-rounded-rect press morphs. |
| **B2. IconButton (Expressive Sizes)** | TopAppBars, header action clusters, list item actions | `HeaderAction.kt:31` uses `FilledTonalIconButton` with 48dp size. But list icons in `AddExpenseScreen:1090` use 36dp. | Sub-48dp icon buttons violate accessibility standards. | Enforce `Modifier.minimumInteractiveComponentSize()` and use `appIconButtonShapes()` everywhere. |
| **B3. FloatingActionButton** | Dashboard primary action buttons | `MainDashboard.kt:230` uses a 72dp custom FAB. | Large FAB (`72dp`) is manually sized rather than using `LargeFloatingActionButton`. | Use official `LargeFloatingActionButton` and `FloatingActionButton` tokens. |
| **B4. Cards** | Balance cards, expense rows, settings cards | Mixed usage: `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)` in some, hardcoded `surfaceVariant` borders in others. | Inconsistent tonal levels; some cards lack elevation while others use drop shadows. | Standardize on 28dp brand corners (`ShapeBrandCard`) and map containers to `surfaceContainerLow` / `surfaceContainerHigh`. |
| **B5. NavigationBar & Rail** | Top-level dashboard navigation | `MainDashboard.kt:295` uses `WideNavigationRail` (good) on wide screens, but phone mode uses custom `FloatingNavBar` (custom Surface). | Custom `FloatingNavBar` gradient obscures scrollable content underneath; non-standard tab accessibility. | Align `FloatingNavBar` accessibility semantics with `NavigationBarItem` or adopt `ShortNavigationBar`. |
| **B6. TopAppBar (Expressive)** | Screen headers across all 21 screens | Screens mix `TopAppBar` with pinned scroll behavior (`TopAppBarDefaults.pinnedScrollBehavior()`), custom `RoundBackButton`, and custom blur headers. | Large titles (e.g. `MainDashboard.kt:251`) use `TopAppBar(expandedHeight = 88.dp)` instead of native `LargeTopAppBar` or `MediumTopAppBar`. | Replace 88dp custom single-row hacks with `LargeTopAppBar` or `MediumTopAppBar` with collapsing title typography. |
| **B7. Menu / DropdownMenu** | Context menus, export selectors, currency dropdowns | Plain `DropdownMenu` and `DropdownMenuItem` used in `GroupDetailScreen.kt:258` and `CreateGroupDialog.kt:82`. | Plain rectangular dropdowns lack the Expressive rounded containers and icon alignment. | Apply M3 Expressive menu styling with 16dp rounded corners and tonal container backgrounds. |
| **B8. AlertDialog** | Destructive actions (delete expense, purge trash, logout) | `AlertDialog` used in `AccountScreen.kt:331` and `RecentlyDeletedScreen.kt`. | Confirm buttons use sharp or inconsistent corner shapes (`shapes.extraSmall` vs `shapes.small`). | Standardize on `AlertDialog` with `MaterialTheme.shapes.extraLarge` and pill-shaped action buttons. |
| **B9. ModalBottomSheet** | Add expense form, filter sheet, global search, exports | `ModalBottomSheet` used extensively (`AddExpenseScreen.kt:89`, `FilterAndSortBottomSheet.kt:40`). | In `AddExpenseScreen`, `skipPartiallyExpanded = true` causes abrupt full-screen snapping. Keyboard IME handling causes content jumping. | Implement edge-to-edge window insets padding inside `ModalBottomSheet` and smooth IME padding animation. |
| **B10. SearchBar** | Global search sheet, dashboard search, settings search | `AppSearchBar.kt` builds a fake search bar using `OutlinedTextField`. | Lacks search expansion animation, predictive back dismiss, and integrated suggestions container. | Replace `AppSearchBar.kt` with official `androidx.compose.material3.SearchBar` and `DockedSearchBar`. |

---

### Group C: Deprecated / Replaced Components

| Deprecated Component | Where Found in OpenSplit | Modern M3 Expressive Replacement | Replacement Action |
| :--- | :--- | :--- | :--- |
| **C1. SegmentedButton** | `AddExpenseScreen.kt:423` uses custom `PillTabs`; filter rows use `Row` of `FilterChip`s. | `ButtonGroup` with `toggleableItem` or `ConnectedChoiceGroup`. | Migrate all segmented button rows to `ConnectedChoiceGroup` / `ButtonGroup`. |
| **C2. SplitButtonLayout** | [GroupDetailScreen.kt:237](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/GroupDetailScreen.kt#L237) | `SplitButton` (`androidx.compose.material3.SplitButton`) | Replace `SplitButtonLayout` with the new unified `SplitButton` composable. |
