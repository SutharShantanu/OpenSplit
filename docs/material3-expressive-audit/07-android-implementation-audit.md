# 07 — Android & Jetpack Compose Architecture Implementation Audit

This document audits OpenSplit's technical Android implementation, edge-to-edge window insets handling, Jetpack Compose recomposition hygiene, architecture patterns, and OS-level integrations.

---

## 1. Edge-to-Edge System Bar Implementation

### 1.1 Inset Architecture
OpenSplit declares edge-to-edge compliance using a custom helper [OpenSplitStatusBarStyle](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/theme/Theme.kt#L94-L125):
```kotlin
window.statusBarColor = android.graphics.Color.TRANSPARENT
window.navigationBarColor = android.graphics.Color.TRANSPARENT
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    window.isStatusBarContrastEnforced = false
    window.isNavigationBarContrastEnforced = false
}
```
The controller dynamically computes luminance using `ColorUtils.calculateLuminance(color.toArgb()) > 0.55` and sets `isAppearanceLightStatusBars` and `isAppearanceLightNavigationBars`.

### 1.2 Inset Deficiencies & Visual Collisions:
1. **Double Padding on Scaffold Content**:
   In `MainDashboard.kt:291`:
   ```kotlin
   Row(
       modifier = Modifier
           .fillMaxSize()
           .background(MaterialTheme.colorScheme.background)
           .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
   )
   ```
   Nested children screens (e.g. `GroupsScreen` and `FriendsScreen`) also apply their own padding or use `Scaffold(contentWindowInsets = ...)`. On devices with gesture navigation, this causes a double-bottom inset gap between the floating nav bar and screen content.
2. **Keyboard IME Padding Jitter in Add Expense**:
   In `AddExpenseScreen.kt`, when the user focuses the amount text field or notes field, the soft keyboard pushes the bottom sheet up. Because `navigationBarsPadding()` and `imePadding()` are both applied at different levels of the hierarchy, the modal sheet experiences vertical stutter during keyboard show/hide animations.
3. **Hardcoded Bottom Clearance**:
   In `FloatingNavBar.kt:148`:
   ```kotlin
   val FloatingNavBarClearance = 120.dp
   ```
   Scrollable lists apply this hardcoded 120dp bottom clearance to prevent content from hiding behind the floating pill. On devices with high display scaling (200%), 120dp is insufficient, causing the last list item to be partially covered by the floating nav bar.

---

## 2. Compose Recomposition Performance & Slot Table Stability

### 2.1 Recomposition Bottlenecks Identified:
1. **Dynamic Font Resizing in Amount Text**:
   In [HeroBalanceMotif.kt:73, 102](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/HeroBalanceMotif.kt#L73):
   ```kotlin
   var fontSizeMultiplier by remember(textToDisplay) { mutableStateOf(1.0f) }
   ...
   onTextLayout = { result ->
       if ((result.hasVisualOverflow || result.didOverflowWidth) && fontSizeMultiplier > 0.5f) {
           fontSizeMultiplier *= 0.88f
       }
   }
   ```
   When a long amount is drawn, `onTextLayout` detects overflow and mutates `fontSizeMultiplier`. This triggers an immediate recomposition of the text composable. If it still overflows, it mutates again. This causes 2–3 recomposition frames per text render, creating visible micro-stutter during rapid balance updates or scrolling.
   - **Fix**: Use static auto-sizing or pre-calculate layout constraints with `rememberTextMeasurer()`.
2. **Lambda Allocation in Dashboard Loop**:
   In `MainDashboard.kt:213-240`, FAB composables (`groupsFabMenu`, `peopleFab`) are defined as local composable lambdas inside the function body. The comment notes:
   > *"Both FAB lambdas exist unconditionally; the `when` only picks one. Creating composable lambdas inside `when` branches shifted the slot table when switching tabs, which made Compose rebuild everything below..."*
   - While this prevents slot table restructuring, defining composable lambdas inside an active composable function without `@Stable` or `remember` still invalidates the lambda instance on every dashboard state change (e.g. unread count update).
   - **Fix**: Hoist FAB definitions to standalone `@Composable` functions.

---

## 3. Architecture & Data Layer Integrity

### 3.1 Dependency Injection & ViewModel Architecture
- **DI Engine**: Lightweight manual dependency injection via `AppContainer` (`com.opensplit.di.AppContainer`).
- **ViewModel Instantiation**: Uses `ViewModelFactory` implementing `ViewModelProvider.Factory`.
- **Quality**: **CLEAN**. Repositories are interface-driven (`ExpenseRepository`, `GroupRepository`, `UserRepository`), enabling unit testing with in-memory test doubles.

### 3.2 Offline-First Strategy
- Uses Firebase Firestore offline persistence (`isPersistenceEnabled = true`).
- In `MainDashboard.kt:333-337`, pull-to-refresh invokes `waitForPendingWrites()`:
  ```kotlin
  FirebaseFirestore.getInstance().waitForPendingWrites()
  ```
  This ensures that user pull-down gestures actively flush pending local writes to the cloud rather than simply reading stale local cache.
- Local user preferences and security settings are safely backed by `DataStore` and encrypted with AndroidX Security Crypto (`MasterKey` with AES256-GCM).

---

## 4. System-Level Android Integrations

### 4.1 Glance AppWidgets
- **Files**: [OpenSplitBalanceWidgetProvider.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/widget/OpenSplitBalanceWidgetProvider.kt), [OpenSplitQuickActionsWidgetProvider.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/widget/OpenSplitQuickActionsWidgetProvider.kt).
- **Implementation**: Written with `androidx.glance:glance-appwidget:1.2.0` and `glance-material3`.
- **Features**:
  1. Quick actions widget: 4 shortcut buttons (Add Expense, Settle Up, QR Code, Activity).
  2. Balance widget: Displays net owed/owe amount with one-tap deep links.
- **Audit Finding**: Widgets use `GlanceTheme`, mirroring the app's dynamic color on Android 12+.

### 4.2 Quick Settings Tile
- **File**: `AddExpenseTileService.kt`
- Allows launching the "Add Expense" flow directly from the Android Quick Settings pull-down shade.

### 4.3 Background WorkManager
- **Workers**: `RecurringExpenseWorker` (generates recurring bills on schedule) and `DebtReminderWorker` (notifies users of pending debts).
- Both workers execute reliably in the background with network and battery constraints.
