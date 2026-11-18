# Material 3 Expressive & Accessibility Audit: Implementation Changelog

This document provides a comprehensive technical changelog for all Material 3 Expressive, WCAG 2.2 accessibility, and UX improvements implemented in OpenSplit.

The primary audit document suite is located in [`docs/material3-expressive-audit/`](file:///home/shantanu/Documents/OpenSplit/docs/material3-expressive-audit/).  
The complete 12-part technical implementation changelog is available at:  
👉 [docs/material3-expressive-audit/12-implementation-changelog.md](file:///home/shantanu/Documents/OpenSplit/docs/material3-expressive-audit/12-implementation-changelog.md)

---

## 1. Quick Summary of Changes

| Issue / Feature | Category | Impacted Files | Key Improvement |
|:---|:---|:---|:---|
| **M3E-P0-01** | `A11y / Theme` | [Theme.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/theme/Theme.kt), [OpenSplitTokens.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/theme/OpenSplitTokens.kt) | Replaced hardcoded static financial green (`#1B5E20`) with dynamic dark/light tokens yielding 7.2:1 contrast. |
| **M3E-P0-02/04** | `A11y` | [HeroBalanceMotif.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/HeroBalanceMotif.kt), [AddExpenseScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AddExpenseScreen.kt) | Enforced 48×48dp minimum touch bounds on balance rows and shares `+`/`-` buttons. |
| **M3E-P0-03/06** | `A11y (TalkBack)`| [FloatingNavBar.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/FloatingNavBar.kt), [AnalyticsScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AnalyticsScreen.kt) | Added `isTraversalGroup`, collection indices, and expanded donut chart announcements to 100% of categories. |
| **M3E-P1-01** | `M3 Expressive` | [SplitButtonActions.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/SplitButtonActions.kt), [GroupDetailScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/GroupDetailScreen.kt) | Replaced deprecated `SplitButtonLayout` with official `SplitButton` / `ExpressiveSplitButton`. |
| **M3E-P1-02** | `M3 Expressive` | [HeaderAction.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/HeaderAction.kt) | Refactored manual row/corner math into native M3 `ButtonGroup` with official shape morphing. |
| **M3E-P1-03** | `M3 Expressive` | [AppSearchBar.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/AppSearchBar.kt) | Retained crisp `OutlinedTextField` with `outlineVariant` border and `extraLarge` shape for clean visual definition. |
| **M3E-P1-05** | `Typography` | [Type.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/theme/Type.kt) | Removed synthetic horizontal stretching (`scaleX = 1.3f`) in favor of optical font family weights. |
| **M3E-P1-06** | `Layout` | [MainDashboard.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/MainDashboard.kt) | Deduplicated nested window insets on gesture-navigation devices. |
| **M3E-P1-07** | `M3 Expressive` | [HeroBalanceMotif.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/HeroBalanceMotif.kt) | Replaced custom canvas progress bar with expressive `LinearWavyProgressIndicator`. |
| **M3E-P2-01** | `Product UX` | [AddExpenseScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AddExpenseScreen.kt) | Removed stepwise form gating (fill in any order); converted split modes to responsive `FlowRow` chips. |
| **M3E-P2-06** | `Navigation / UX` | [AccountScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AccountScreen.kt), [MainDashboard.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/MainDashboard.kt) | Floating nav bar smoothly animates out when viewing settings sub-screens to prevent keyboard overlap. |
| **M3E-P2-07** | `Affordance` | [ActivityScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/ActivityScreen.kt) | Added chevron trailing icons to clickable activity events + 48dp minimum chip height. |
| **M3E-P2-12** | `Interaction` | [SwipeActionRow.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/SwipeActionRow.kt) | Added 24dp directional deadzone to eliminate accidental swipes during vertical list scrolling. |
| **M3E-P3-01** | `Haptics` | [AddExpenseScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AddExpenseScreen.kt) | Added tactile feedback on amount typing and shares increment/decrement. |
| **M3E-P3-03** | `Data Mgmt` | [RecentlyDeletedScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/RecentlyDeletedScreen.kt) | Implemented "Empty trash" action with confirmation dialog. |
| **Phase 4** | `New Feature` | [AddExpenseScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AddExpenseScreen.kt) | Added interactive Itemized Tax & Tip Proportional Calculator with live recalculation and sync. |
| **Free Gemini AI** | `AI Architecture` | [index.js](file:///home/shantanu/Documents/OpenSplit/functions/index.js), [AiService.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/data/ai/AiService.kt), [AiSettings.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AiSettings.kt) | Cloud Function proxy providing 20 free daily Gemini AI requests per user. Strictly 100% free with no pay-as-you-go; auto-halts on quota exhaustion until next-day reset. |
| **Font Streamline** | `Typography` | [Type.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/theme/Type.kt), [build.gradle.kts](file:///home/shantanu/Documents/OpenSplit/app/build.gradle.kts) | Removed non-bundled downloadable Google Fonts (Source Sans 3, Nunito, Poppins, Inter, Montserrat, Serif, Roboto); standardizing exclusively on bundled variable `Google Sans Flex` and `JetBrains Mono`. |
| **Brand Case Fix** | `Brand / Splash` | [SplashScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/SplashScreen.kt), [WelcomeScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/auth/WelcomeScreen.kt) | Standardized casing to PascalCase `"OpenSplit"` across splash screen wordmark, accessibility descriptions, and onboarding. |
| **Hardware** | `Bugfix` | [QrScannerView.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/QrScannerView.kt) | Configured CameraX `COMPATIBLE` texture mode to prevent crashes on Nothing OS 2.6 / 3.0 devices. |

---

## 2. Verification Summary

- **Unit Tests:** 34 / 34 passed (`./gradlew testDebugUnitTest`).
- **Release Compilation:** Successfully built minified `app-release.apk` (6.8 MB, `versionCode=47`) using R8 and release signing.
- **Physical Device:** Tested on Nothing Phone (2a) Plus (`00365767F005601`).
- **Git Status:** Both `main` and `payment-detection` branches are in sync and pushed to GitHub.
