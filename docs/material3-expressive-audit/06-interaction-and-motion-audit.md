# 06 — Interaction, Motion Choreography & Feedback Audit

This document examines the physics, timing curves, micro-interactions, haptic feedback, and gesture handling in OpenSplit compared to Google's official Material 3 Expressive motion specifications.

---

## 1. Motion Choreography & Easing Curves

In [OpenSplitMotion.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/theme/OpenSplitMotion.kt), the motion physics are modeled after Google Pixel native system choreography:

```kotlin
val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val EmphasizedDecelerateEasing = CubicBezierEasing(0.2f, 0.85f, 0.7f, 1f)
val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
const val DurationMs = 400
```

### 1.1 Transition Patterns Evaluated
1. **Shared Axis (X)** — Hierarchical Forward / Backward Transitions:
   - Configured globally on `MainNavHost` (`OpenSplitNavGraph.kt:133-136`).
   - Pushing a screen uses `slideInHorizontally` (40% enter offset) + `scaleIn` (0.94 to 1.0) + `fadeIn`.
   - Popping reverses this cleanly (`slideOutHorizontally` + `scaleOut` + `fadeOut`).
   - **Quality**: **EXCELLENT**. The subtle scale zoom mimics the Pixel System UI experience.
2. **Fade Through** — Peer Destination Switching:
   - Used inside `GroupDetailScreen.kt:180` between `Expenses`, `Balances`, and `Members` tabs.
   - Combines a rapid fade-out of the outgoing page (`DurationMs / 3`) with a delayed fade-in and scale-in of the incoming page (`DurationMs / 2`, delay `DurationMs / 4`).
   - **Quality**: **COMPLIANT**. Content does not visually collide or slide laterally.

---

## 2. Predictive Back Gesture Handling (Android 14+ / 15)

Predictive Back allows users to preview the destination behind the current screen during a swipe-from-edge gesture, canceling or completing the back action fluidly.

### Current Gap in OpenSplit:
- OpenSplit targets SDK 36 and compiles against SDK 37.
- In `OpenSplitNavGraph.kt`, navigation uses standard `NavHost` transitions.
- **The Defect**: While the static transitions use `sharedAxisX()`, they do **not** bind to `SeekableTransitionState` or `PredictiveBackHandler`. When a user drags their thumb from the left or right screen edge on Android 14+, the current screen stays rigid until the gesture completes, at which point it abruptly triggers the exit animation.
- **The Required Fix**:
  Migrate navigation transitions to Compose 1.8+ Predictive Back animation specs:
  ```kotlin
  NavHost(
      navController = navController,
      startDestination = startDest,
      // Enables interactive swiping back with destination preview
      enterTransition = { OpenSplitMotion.sharedAxisX(forward = true).targetContentEnter },
      exitTransition = { OpenSplitMotion.sharedAxisX(forward = true).initialContentExit }
  )
  ```
  Ensure `android:enableOnBackInvokedCallback="true"` is declared in `AndroidManifest.xml` (already present) and hook the Compose predictive back modifier to sheets and dialogs.

---

## 3. Pull-to-Refresh & Finger-Following Physics

In [MainDashboard.kt:321-363](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/MainDashboard.kt#L321), pull-to-refresh is implemented via Compose M3's `pullToRefresh` modifier:

### Inspection Findings:
1. **Gmail-Style Content Offset**:
   - Rather than letting a spinner float arbitrarily on top of the list, OpenSplit applies:
     ```kotlin
     modifier = Modifier.graphicsLayer {
         translationY = pullState.distanceFraction.coerceIn(0f, 1f) * pullOffsetPx
     }
     ```
   - As the user pulls down, the entire tab content slides downward, opening a gap where `PullToRefreshDefaults.LoadingIndicator` emerges.
   - **Rating**: **EXCELLENT**. Matches Google's flagship app interaction pattern.
2. **Gesture Collision with Horizontal Swiping**:
   - `MainDashboard.kt` houses `SlidingTabHost`. When a user pulls downward at a slight diagonal angle, the gesture detector intermittently triggers tab switching instead of refreshing.
   - **Fix**: Apply a vertical drag orientation lock on `pullToRefresh` until the pull threshold passes 16dp.

---

## 4. Haptic Feedback Integration

Haptic feedback provides tactile confirmation for financial transactions, mode changes, and invalid inputs.

### Current Implementation:
OpenSplit centralizes haptics through a custom `CompositionLocalProvider`:
```kotlin
// Theme.kt:188
androidx.compose.ui.platform.LocalHapticFeedback provides if (haptics) systemHaptics else NoHaptics
```
When user disables haptics in Settings, every call across the app is gracefully silenced without requiring null checks.

### Audit of Haptic Triggers:
1. **Expense Saved / Updated**:
   - Uses `HapticFeedbackType.Confirm` on success and `HapticFeedbackType.Reject` on validation error in `AddExpenseScreen.kt:1232, 1241`.
   - **Quality**: **COMPLIANT**.
2. **Navigation Pill Selection**:
   - In `FloatingNavBar.kt:108`:
     ```kotlin
     haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
     ```
   - **Quality**: **GOOD**. Provides a subtle tactile click as destinations switch.
3. **Missing Haptic Opportunities**:
   - Tapping the numeric keypad or typing in `BasicTextField` amount inputs provides no haptic click.
   - Swiping an item in `SwipeActionRow.kt` does not vibrate when passing the swipe activation threshold.
