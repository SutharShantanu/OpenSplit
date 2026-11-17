# 01 — Project Overview & Material 3 Expressive Reference Checklist

**Project**: OpenSplit (Native Android Expense-Sharing Application)  
**Package**: `com.opensplit`  
**Target SDK**: 36 | **Compile SDK**: 37 (Android 16/17 Preview compatible) | **Min SDK**: 24  
**Audit Conducted**: November 2026  
**Auditor**: Senior Android UX Engineer, Material Design 3 Expressive Specialist, Accessibility Auditor & Jetpack Compose Architect  

---

## 1. Executive Overview

OpenSplit is an open-source, native Android peer-to-peer and group expense-sharing application (Splitwise alternative). The application targets modern Android devices with an offline-first architecture, Firebase backend, local on-device heuristics coupled with BYO-AI provider models (OpenAI, Anthropic, Gemini, Groq, Ollama), Android 16/17 system integration (Glance AppWidgets, Quick Settings Tiles, Edge-to-Edge display, and notification listener payment detection).

The design ambition is to adopt **Material Design 3 Expressive (M3E)** as codified in `androidx.compose.material3:1.5.0-rc01` and Google's 2024–2026 design research. 

This audit evaluates the current implementation against Google's official design system specifications, accessibility standards (WCAG 2.2 AA and Android Accessibility Guidelines), modern Jetpack Compose architecture patterns, and product usability requirements.

---

## 2. Technical Stack & Component Versions

| Dimension | Implementation | File / Location | Status / Notes |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin 2.4.0 | `gradle/libs.versions.toml` | Modern Kotlin compiler with Compose compiler plugin |
| **Compose BOM** | `2024.09.00` | `gradle/libs.versions.toml` | Pinned BOM baseline |
| **Material 3 Library** | `1.5.0-rc01` | `gradle/libs.versions.toml:17` | Required for Expressive components (`LoadingIndicator`, `ButtonGroup`, `SplitButton`) |
| **Navigation** | `androidx.navigation:navigation-compose:2.8.9` | `gradle/libs.versions.toml:18` | Type-safe route strings with query parameter formatting |
| **UI Blur / Glass** | `dev.chrisbanes.haze:haze:1.3.1` | `gradle/libs.versions.toml:39` | Used on `TopAppBar` and floating containers |
| **Typography Engine** | `androidx.compose.ui:ui-text-google-fonts:1.12.0` | `gradle/libs.versions.toml:38` | Google Sans Flex variable font bundled + Play Services downloadable fonts |
| **Widgets** | `androidx.glance:glance-appwidget:1.2.0` | `gradle/libs.versions.toml:36` | Glance with `glance-material3` integration |
| **Async Image** | `io.coil-kt:coil-compose:2.7.0` | `gradle/libs.versions.toml:22` | Used for avatars and receipt thumbnails |
| **Barcode / Scanner** | Google ML Kit Barcode Scanning `18.3.1` | `gradle/libs.versions.toml:50` | CameraX-driven friend invite and payment QR scanning |

---

## 3. Architecture & Navigation Map

The application follows an **Offline-First Single-Activity Clean Architecture** with the following navigation flow:

```mermaid
flowchart TD
    AppStart[App Launch / SplashScreen] --> AuthCheck{Auth State Flow}
    AuthCheck -->|Logged Out| WelcomeScreen[WelcomeScreen - Google Sign In]
    AuthCheck -->|Loading| LoadingScreen[AppLoadingIndicator]
    AuthCheck -->|Logged In| PrimerCheck{Completed Permission Primer?}
    PrimerCheck -->|No| PermissionPrimer[PermissionPrimerScreen]
    PrimerCheck -->|Yes| MainDashboard[MainDashboard / Navigation Host]

    subgraph DashboardTabs ["MainDashboard (SlidingTabHost / FloatingNavBar)"]
        TabGroups["GroupsTab (GroupsScreen)"]
        TabPeople["PeopleTab (FriendsScreen)"]
        TabActivity["ActivityTab (ActivityScreen)"]
        TabInsights["InsightsTab (AnalyticsScreen)"]
        TabSettings["SettingsTab (AccountScreen)"]
    end

    MainDashboard --> DashboardTabs

    TabGroups --> GroupDetail[GroupDetailScreen]
    TabGroups --> CreateGroup[CreateGroupDialog]
    GroupDetail --> AddExpense[AddExpenseScreen (ModalBottomSheet / Dialog)]
    GroupDetail --> ExpenseDetail[ExpenseDetailScreen]
    GroupDetail --> SettleUp[SettleUpScreen]
    GroupDetail --> FilterSort[FilterAndSortBottomSheet]
    GroupDetail --> ExportSheet[ExportBottomSheet]

    TabPeople --> PersonBalance[PersonBalanceScreen]
    TabPeople --> FriendQr[FriendQrSheet]
    TabPeople --> JoinGroup[JoinGroupScreen]
    TabPeople --> AcceptFriend[AcceptFriendScreen]

    TabSettings --> AppearanceSub[AppearanceSettingsSubScreen]
    TabSettings --> CurrencySub[CurrencySettingsSubScreen]
    TabSettings --> AiProvidersSub[AiProvidersSubScreen]
    TabSettings --> PaymentDetection[PaymentDetectionScreen]
    TabSettings --> SecuritySub[AccountSecuritySubScreen]
    TabSettings --> DataBackupSub[DataBackupSubScreen]
    TabSettings --> NotificationSettings[NotificationSettingsScreen]
    TabSettings --> SettlementHistory[SettlementHistoryScreen]
    TabSettings --> RecurringExpenses[RecurringExpensesScreen]
    TabSettings --> RecentlyDeleted[RecentlyDeletedScreen]
    TabSettings --> BugReport[BugReportSettings]
    TabSettings --> AboutSupport[AboutSettings]
```

### Screen Inventory & Route Registry

1. **`welcome`** — [WelcomeScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/auth/WelcomeScreen.kt): Google Sign-In with Credential Manager.
2. **`permission_primer`** — [PermissionPrimerScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/PermissionPrimerScreen.kt): Contacts, Camera, and Notification permissions primer.
3. **`home`** — [MainDashboard.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/MainDashboard.kt): Central dashboard hosting five destinations (`GROUPS`, `PEOPLE`, `ACTIVITY`, `INSIGHTS`, `SETTINGS`).
4. **`group_detail/{groupId}`** — [GroupDetailScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/GroupDetailScreen.kt): Group expenses list, spending total card, simplified balances, and member list.
5. **`add_expense/{groupId}`** — [AddExpenseScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AddExpenseScreen.kt): Natural language AI entry, camera receipt OCR, manual entry, split modes (Equal, Exact, %, Shares, Itemized), multi-payer selection.
6. **`expense_detail/{groupId}/{expenseId}`** — [ExpenseDetailScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/ExpenseDetailScreen.kt): Split breakdown, comments, receipt viewer, edit/delete actions.
7. **`edit_expense/{groupId}/{expenseId}`** — [AddExpenseScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AddExpenseScreen.kt): Edit mode for existing expenses.
8. **`settle_up/{groupId}`** — [SettleUpScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/SettleUpScreen.kt): Debt settlement with UPI deep-linking, Cash, Bank Transfer, and Other methods.
9. **`settlement_history`** — [SettlementHistoryScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/SettlementHistoryScreen.kt): Timeline of settlements, PDF/CSV/JSON export hub.
10. **`person_balance/{friendId}`** — [PersonBalanceScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/PersonBalanceScreen.kt): Multi-currency net balances with a specific friend across all shared groups.
11. **`activity`** — [ActivityScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/ActivityScreen.kt): Chronological transaction, member change, and settlement feed.
12. **`analytics`** — [AnalyticsScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AnalyticsScreen.kt): Spending by week/month, donut category charts, MoM comparison, AI insights.
13. **`account`** — [AccountScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AccountScreen.kt): Settings hub with categorized preference cards.
14. **`recurring_expenses`** — [RecurringExpensesScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/RecurringExpensesScreen.kt): Active and paused recurring schedules.
15. **`recently_deleted`** — [RecentlyDeletedScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/RecentlyDeletedScreen.kt): 90-day trash bin with restore and purge actions.
16. **`payment_detection`** — [PaymentDetectionScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/PaymentDetectionScreen.kt): UPI/Bank notification interception review inbox.
17. **`notification_settings`** — [NotificationSettingsScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/NotificationSettingsScreen.kt): Channel-level preference controls.
18. **`join/{token}`** — [JoinGroupScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/JoinGroupScreen.kt): Deep-link group join sheet.
19. **`friend/{token}`** — [AcceptFriendScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AcceptFriendScreen.kt): Deep-link friend connection sheet.
20. **`profile_setup`** — [ProfileSetupScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/auth/ProfileSetupScreen.kt): First-time user profile onboarding.
21. **`set_new_password`** — [SetNewPasswordScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/SetNewPasswordScreen.kt): Firebase out-of-band link password updater.

---

## 4. Material 3 & M3 Expressive Reference Checklist

This checklist maps Google's official Material 3 Expressive guidelines directly to the OpenSplit codebase:

| Principle / Specification | Official Guideline | Current OpenSplit State | Compliance Status |
| :--- | :--- | :--- | :--- |
| **Primary Theme Wrapper** | Use `MaterialExpressiveTheme` with `MotionScheme.expressive()` | Implemented in `Theme.kt:178` | **COMPLIANT** |
| **Dynamic Color Adaptation** | Extract seeds from user wallpaper via `dynamicLightColorScheme` & `WallpaperColors` | Implemented in `Theme.kt:315` with custom Lab tonal palette fallback | **COMPLIANT** |
| **Color Semantic Roles** | Positive/Negative semantic financial tones mapped to accessible tonal containers | Static hex codes `OwedPositive = Color(0xFF1B5E20)` in `OpenSplitTokens.kt` break dark mode contrast | **NON-COMPLIANT** |
| **Edge-to-Edge** | Full transparent status and nav bars with dynamic icon contrast | Window insets configured via `OpenSplitStatusBarStyle`, but multiple screens use hardcoded clearances | **PARTIAL** |
| **Typography System** | Expressive scale with variable optical sizing and clear hierarchical roles | Google Sans Flex + JetBrains Mono implemented, but negative tracking & manual scaleX distort body legibility | **PARTIAL** |
| **Shape Expressive Range** | 9-sided cookies, clovers, and soft bursts for organic brand moments | `ShapeCache.kt` generates shapes, but cards and items use inconsistent corner radii (8dp, 12dp, 16dp, 28dp, 50dp) | **PARTIAL** |
| **SplitButton** | Stable M3E component replacing separated action buttons | Deprecated `SplitButtonLayout` used in `GroupDetailScreen.kt:237` | **NON-COMPLIANT** |
| **ButtonGroup (Connected)** | Continuous multi-segment controls with morphing corner radii on select | Manually simulated with `Row` and `corners()` math in `HeaderAction.kt:56` | **NON-COMPLIANT** |
| **FAB Menu** | `FloatingActionButtonMenu` with `ToggleFloatingActionButton` | Implemented in `ExpressiveControls.kt:119`, but missing in `AnalyticsScreen` and `GroupDetailScreen` | **PARTIAL** |
| **Loading Affordance** | Shape-morphing wavy indicator replacing circular indeterminate spinners | Centralized via `AppLoadingIndicator.kt` using M3E `LoadingIndicator` | **COMPLIANT** |
| **Touch Targets** | Minimum 48x48dp interactive bounding box | Multiple icon buttons in headers and ratio bars measure 24dp to 36dp | **NON-COMPLIANT** |
| **Search Experience** | Native M3 `SearchBar` / `DockedSearchBar` | Custom `OutlinedTextField` wrapper inside `AppSearchBar.kt` | **NON-COMPLIANT** |
| **Navigation Component** | `NavigationSuiteScaffold` or `ShortNavigationBar` | Custom floating pill `FloatingNavBar.kt` with fixed 72dp height | **PARTIAL** |
| **Predictive Back** | Predictive back gesture animations on Android 14+ | Transitions use `sharedAxisX()`, but lack Compose 1.8+ predictive back seekable transitions | **PARTIAL** |
