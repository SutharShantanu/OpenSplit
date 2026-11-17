# 09 — Prioritized Issue Register & Improvement Catalog

This register catalogs every identified defect, accessibility failure, architectural violation, and UX friction point discovered in the audit, prioritized by severity (P0 to P3).

---

## 1. Issue Register Summary

| Severity | Definition | Count | Focus Areas |
| :--- | :--- | :---: | :--- |
| **P0 (Critical)** | Accessibility failures, severe WCAG AA contrast breaches (< 3:1), sub-48dp touch targets on core buttons, broken state locks. | **6** | Dark theme financial contrast, Shares mode 36dp touch target, TalkBack chart silencing, Volatile motion state. |
| **P1 (High)** | Deprecated M3 APIs, major M3 Expressive guideline breaches, fake search bars, manual connected button math, double padding insets. | **10** | `SplitButtonLayout` deprecation, fake `AppSearchBar`, simulated `HeaderButtonGroup`, 200% font clipping. |
| **P2 (Medium)** | Visual inconsistencies, missing FAB on secondary tabs, unstyled dialog buttons, progressive reveal blockers, un-themed dropdowns. | **12** | Stepwise reveal blocking split selection, FAB spatial jumping, Category chip styling, raw exception display. |
| **P3 (Low)** | Minor polish, micro-interaction haptics, missing illustrations, decorative shapes. | **8** | Keypad typing haptics, swipe row hint animations, trash purge shortcut. |

---

## 2. P0: Critical Issues (Immediate Action Required)

### [M3E-P0-01] Severe WCAG AA Contrast Failure for Owed Balances in Dark Theme
- **Location**: `OpenSplitTokens.kt:10` & `HeroBalanceMotif.kt:28`
- **Severity**: **P0** | **Complexity**: **Low**
- **The Gap**: `val OwedPositive = Color(0xFF1B5E20)` is a static dark forest green. Rendered on dark surfaces (`#12140E`), its contrast ratio is **1.86:1**, failing WCAG AA (minimum 4.5:1).
- **Expected M3E Behavior**: Semantic colors must adapt tonally between light and dark palettes (e.g. `#1B6C3A` on light, `#85DF9B` on dark).
- **User Impact**: Users with low vision or working in dark mode cannot read positive financial balances.
- **Recommended Fix**: Add dynamic semantic extensions on `ColorScheme` in `Theme.kt`:
  ```kotlin
  val ColorScheme.financialPositive: Color
      get() = if (surface.luminance() > 0.5f) Color(0xFF1B6C3A) else Color(0xFF85DF9B)
  ```

### [M3E-P0-02] Sub-48dp Touch Targets on Itemized Shares Calculator Buttons
- **Location**: `AddExpenseScreen.kt:1090, 1100`
- **Severity**: **P0** | **Complexity**: **Low**
- **The Gap**: The `+` and `-` shares modifier buttons have a forced size of `Modifier.size(36.dp)`.
- **Expected M3E Behavior**: All interactive components must meet the minimum 48x48dp touch target standard.
- **User Impact**: High error rate when adjusting bill shares on mobile devices.
- **Recommended Fix**: Update buttons to `Modifier.size(48.dp)` or apply `.minimumInteractiveComponentSize()`.

### [M3E-P0-03] TalkBack Silencing of Donut Chart Categories 5–10
- **Location**: `AnalyticsScreen.kt:802`
- **Severity**: **P0** | **Complexity**: **Low**
- **The Gap**: `DonutChart` semantics reads only `categories.take(4)`, omitting all categories beyond the fourth.
- **Expected M3E Behavior**: Assistive technologies must have access to all data represented visually.
- **User Impact**: Blind and low-vision users cannot discover lower-ranking spending categories.
- **Recommended Fix**: Include all categories in the semantic description or make the legend items individually focusable.

### [M3E-P0-04] Hero Balance Row Tap Targets Below Accessible Minimum (24dp)
- **Location**: `HeroBalanceMotif.kt:176, 213`
- **Severity**: **P0** | **Complexity**: **Low**
- **The Gap**: Clickable balance rows have an interactive height of ~24dp without touch target padding.
- **Expected M3E Behavior**: Clickable rows must provide at least 48dp vertical target bounds.
- **User Impact**: Frustration when tapping "You are owed" or "You owe" to open breakdown sheets.
- **Recommended Fix**: Add `Modifier.defaultMinSize(minHeight = 48.dp)` to the row modifier.

### [M3E-P0-05] Global Volatile State Antipattern for Reduce Motion
- **Location**: `OpenSplitMotion.kt:44`
- **Severity**: **P0** | **Complexity**: **Medium**
- **The Gap**: `@Volatile var reduced = false` on a static singleton is not observed reactively by Compose navigation transitions.
- **Expected M3E Behavior**: System accessibility settings and user toggles must update transition choreography immediately via `CompositionLocal`.
- **User Impact**: Users who toggle Reduce Motion must restart the app for transitions to take effect.
- **Recommended Fix**: Refactor `LocalReduceMotion` to be observed inside `MainNavHost` transition specs.

### [M3E-P0-06] TalkBack Navigation Traversal Inversion on Main Dashboard
- **Location**: `MainDashboard.kt:458`
- **Severity**: **P0** | **Complexity**: **Medium**
- **The Gap**: `FloatingNavBar` is composed after the content list in a `Box`, forcing TalkBack users to swipe through the entire groups list before reaching navigation tabs.
- **Expected M3E Behavior**: Navigation controls should be reachable first or organized in distinct traversal groups.
- **User Impact**: Disorienting navigation experience for TalkBack users.
- **Recommended Fix**: Apply `Modifier.semantics { isTraversalGroup = true; traversalIndex = -1f }` to the navigation bar.

---

## 3. P1: High Priority Issues

### [M3E-P1-01] Deprecated `SplitButtonLayout` Usage in Group Details
- **Location**: `GroupDetailScreen.kt:237`
- **Severity**: **P1** | **Complexity**: **Medium**
- **The Gap**: Uses deprecated `SplitButtonLayout` with manual `LeadingButton` and `TrailingButton`.
- **Expected M3E Behavior**: Migrate to the unified `androidx.compose.material3.SplitButton` composable.
- **Recommended Fix**: Replace with native `SplitButton` and connect trailing menu directly.

### [M3E-P1-02] Simulated Connected Button Group with Hand-Rolled Corner Math
- **Location**: `HeaderAction.kt:48-67`
- **Severity**: **P1** | **Complexity**: **Medium**
- **The Gap**: Reinvents connected buttons using a custom `Row` and calculating outer/inner corner radii manually.
- **Expected M3E Behavior**: Use native `ButtonGroup` with official `ButtonGroupDefaults.connectedLeadingButtonShapes()`, etc.
- **Recommended Fix**: Refactor `HeaderButtonGroup` to leverage `ButtonGroup`.

### [M3E-P1-03] Fake Search Bar Built on OutlinedTextField
- **Location**: `AppSearchBar.kt:22`
- **Severity**: **P1** | **Complexity**: **Medium**
- **The Gap**: Mimics a search bar with an `OutlinedTextField` rather than using `SearchBar` / `DockedSearchBar`.
- **Expected M3E Behavior**: Official M3 `SearchBar` provides smooth expanding animations and predictive back dismiss.
- **Recommended Fix**: Replace `AppSearchBar.kt` with official `androidx.compose.material3.SearchBar`.

### [M3E-P1-04] Text Truncation and Clipping at 200% Font Scale in Balance Cards
- **Location**: `HeroBalanceMotif.kt:80-106`
- **Severity**: **P1** | **Complexity**: **Medium**
- **The Gap**: `maxLines = 1` and `softWrap = false` with fixed `Row(horizontalArrangement = End)` truncates major currency digits under 200% font scale.
- **Expected M3E Behavior**: Balance figures must remain fully readable at all font scales, wrapping or scaling gracefully.
- **Recommended Fix**: Allow vertical wrapping and adjust layout arrangement when display scale exceeds 1.3x.

### [M3E-P1-05] Synthetic Horizontal Font Distortion (`scaleX = 1.3f`)
- **Location**: `Type.kt:62, 71, 80`
- **Severity**: **P1** | **Complexity**: **Low**
- **The Gap**: `ExpTitleTypography` applies `TextGeometricTransform(scaleX = 1.3f)`, stretching variable fonts artificially.
- **Expected M3E Behavior**: Utilize variable font width axes (`wdth = 112f` or `125f`) instead of geometric matrix scaling.
- **Recommended Fix**: Remove `scaleX` and rely exclusively on `FontVariation.width()`.

### [M3E-P1-06] Double Window Inset Padding on Dashboard Children
- **Location**: `MainDashboard.kt:291` & child screens
- **Severity**: **P1** | **Complexity**: **Medium**
- **The Gap**: Both parent row and child screens consume `contentWindowInsets`, causing an extra 24–48dp gap on gesture navigation devices.
- **Expected M3E Behavior**: System insets must be consumed at exactly one level of the hierarchy.
- **Recommended Fix**: Centralize inset consumption in `MainDashboard` or delegate completely to child `Scaffold`s.

### [M3E-P1-07] Static Ratio Progress Bar Lacking Accessibility Semantics
- **Location**: `HeroBalanceMotif.kt:250-264`
- **Severity**: **P1** | **Complexity**: **Low**
- **The Gap**: Ratio bar is a plain `Box` inside a `Box` with no `progressBarRangeInfo` semantics.
- **Expected M3E Behavior**: Visual progress and ratio indicators must announce values to screen readers.
- **Recommended Fix**: Replace with `LinearWavyProgressIndicator` with `semantics { progressBarRangeInfo = ... }`.

### [M3E-P1-08] Missing Predictive Back Navigation Integration
- **Location**: `OpenSplitNavGraph.kt:130`
- **Severity**: **P1** | **Complexity**: **High**
- **The Gap**: Navigation transitions do not support interactive Predictive Back gestures on Android 14+.
- **Expected M3E Behavior**: Swiping back smoothly previews the underlying screen.
- **Recommended Fix**: Integrate Compose 1.8+ Predictive Back transition specs.

### [M3E-P1-09] Floating Navigation Bar Gradient Obscuring Content
- **Location**: `FloatingNavBar.kt:65`
- **Severity**: **P1** | **Complexity**: **Low**
- **The Gap**: Background vertical gradient on the floating nav bar container washes out list content underneath before reaching the bottom edge.
- **Expected M3E Behavior**: Use blur (`haze`) or transparent scrim that only tints when scrolled.
- **Recommended Fix**: Replace heavy background gradient with a localized subtle scrim or clean floating elevation.

### [M3E-P1-10] Recomposition Loop in Dynamic Font Size Calculation
- **Location**: `HeroBalanceMotif.kt:101-105`
- **Severity**: **P1** | **Complexity**: **Medium**
- **The Gap**: `onTextLayout` mutates state, triggering 2–3 additional recompositions during number rendering.
- **Expected M3E Behavior**: Font sizing should be computed in layout measurement without mutating composed state.
- **Recommended Fix**: Use `rememberTextMeasurer()` to pre-calculate sizing during composition.

---

## 4. P2 & P3: Medium and Low Priority Improvements

| ID | Title | Location | Severity | Gap & Solution |
| :--- | :--- | :--- | :---: | :--- |
| **M3E-P2-01** | Stepwise Reveal Blocks Split Mode Selection | `AddExpenseScreen.kt:389` | **P2** | Users cannot pick a split mode until entering an amount. Remove artificial sequential gating. |
| **M3E-P2-02** | FAB Spatial Jumping Across Dashboard Tabs | `MainDashboard.kt:236` | **P2** | Menu FAB in Groups vs circular FAB in People vs none in Insights. Standardize `ActionFabMenu`. |
| **M3E-P2-03** | Missing Loading State on Downloadable Fonts | `AppearanceSettings.kt:95` | **P2** | Tapping a Google Font shows no spinner while downloading. Add inline indicator on font row. |
| **M3E-P2-04** | Raw Exception Display on Save Errors | `AddExpenseScreen.kt:1264` | **P2** | Displays raw `FirebaseFirestoreException`. Map error codes to user-friendly messages. |
| **M3E-P2-05** | Category Chips Lack Dark Tonal Variants | `CategoryChipRow.kt:27` | **P2** | 17 hardcoded hex values glare in dark mode. Define tonal surface pairs for categories. |
| **M3E-P2-06** | Sub-Screen Bottom Nav Collision | `AccountScreen.kt:167` | **P2** | Floating nav bar stays visible on sub-screens, colliding with soft keyboard. Hide nav bar on sub-routes. |
| **M3E-P2-07** | Non-Interactive Activity Rows Look Clickable | `ActivityScreen.kt:80` | **P2** | Member join and settlement rows look identical to clickable expense rows. Add chevrons to actionable rows. |
| **M3E-P2-08** | Inconsistent Card Corner Radii (8 to 32dp) | Multiple screens | **P2** | Fragmented corner radius usage. Standardize on `ShapeBrandCard` (28dp) and medium (16dp). |
| **M3E-P2-09** | Missing Currency Disabled State in Settle Up | `SettleUpScreen.kt:434` | **P2** | UPI remains selectable in non-INR groups. Visually disable UPI button when currency != "INR". |
| **M3E-P2-10** | Unstyled Confirm Buttons in Alert Dialogs | `AccountScreen.kt:343` | **P2** | Uses sharp `shapes.extraSmall` on logout button. Standardize on pill-shaped dialog actions. |
| **M3E-P2-11** | Floating Pill Tabs Lack Position Announcement | `FloatingNavBar.kt:118` | **P2** | Screen reader announces "Tab, selected" without "1 of 5". Add `collectionInfo` semantics. |
| **M3E-P2-12** | Sensitive Swipe Row Activation Threshold | `SwipeActionRow.kt` | **P2** | Horizontal swipe triggers accidentally during vertical scrolling. Add 24dp directional deadzone. |
| **M3E-P3-01** | Missing Haptic Feedback on Keypad Taps | `AddExpenseScreen.kt:654` | **P3** | Typing in custom amount fields provides no tactile click. Add `TextHandleMove` haptic feedback. |
| **M3E-P3-02** | Missing Visual Affordance for Swipe Actions | `GroupsScreen.kt:115` | **P3** | Users cannot tell that group rows are swipeable. Add initial hint animation or contextual icon. |
| **M3E-P3-03** | Lack of "Purge All" Action in Trash Bin | `RecentlyDeletedScreen.kt` | **P3** | Deleting multiple items requires tedious single purges. Add "Empty Trash" in `TopAppBar`. |
| **M3E-P3-04** | Missing Tabletop Posture on Foldables | `MainDashboard.kt:549` | **P3** | Dual-pane layout does not adapt to foldable hinge. Support `FoldingFeature` occlusion bounds. |
