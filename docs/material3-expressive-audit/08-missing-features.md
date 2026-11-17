# 08 — Missing Features & Product Design Opportunities

Comparing OpenSplit against best-in-class financial and bill-splitting applications (Splitwise, Google Wallet, Revolut, Toshl) and Google's latest Material 3 Expressive benchmarks reveals high-impact design and feature opportunities.

---

## 1. Top 10 Missing Features & Design Opportunities

### 1. Interactive Receipt Bounding-Box OCR Visualizer
- **The Gap**: Currently, when an AI model or ML Kit parses a receipt image, the items and prices are extracted and appended into a flat text list in `itemizedList` (`AddExpenseScreen.kt:248`). The user cannot see where each item came from on the receipt image.
- **Opportunity**: Implement an interactive receipt canvas where bounding boxes are drawn over detected items on the receipt image. Tapping an item on the receipt automatically checks it for the active participant, creating an intuitive restaurant bill-splitting experience.

### 2. Proportional Tax & Tip Calculator in Itemized Mode
- **The Gap**: In `AddExpenseScreen.kt:1107`, itemized splits sum individual item prices. However, restaurant bills include subtotal, tax, service charge, and tip. Currently, users must manually calculate and add tax as an extra item or adjust item prices.
- **Opportunity**: Add a "Tax & Tip" footer card in itemized mode with percentage or fixed amount shortcuts (+5%, +10%, +15%, +18%) that proportionally distributes the overhead across participants based on their subtotal share.

### 3. Biometric App Lock & Privacy Shield
- **The Gap**: Financial balance sheets, debts, and settlement histories contain sensitive personal data. OpenSplit currently has no in-app biometric lock.
- **Opportunity**: Integrate AndroidX Biometric (`androidx.biometric:biometric:1.4.0`) in `SecuritySettingsSubScreen.kt`. Allow users to require Fingerprint/Face Unlock upon opening the app, with automatic canvas obfuscation in Android Recents overview (`FLAG_SECURE` toggle).

### 4. Live Multi-Currency Conversion & Offline Forex Rates
- **The Gap**: Groups and friends support multi-currency balances, but the application does not convert or aggregate balances across different currencies into a single net total (e.g. converting a $20 debt and ₹1,500 owed into a unified INR net balance).
- **Opportunity**: Implement an offline-cached daily exchange rate repository (fetching rates from Open Exchange Rates or European Central Bank). In `HeroBalanceCard`, display a unified converted balance toggle: *"Total Net: ~₹150 in INR (based on today's rates)"*.

### 5. Native Material 3 `SearchBar` & `DockedSearchBar` Experience
- **The Gap**: OpenSplit builds a custom search bar using `OutlinedTextField` inside [AppSearchBar.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/AppSearchBar.kt).
- **Opportunity**: Adopt the official Compose `SearchBar` and `DockedSearchBar`. On phones, the search bar expands smoothly to fill the screen with predictive back navigation. On tablets/wide screens, it docks as an anchored search dropdown without shifting background content.

### 6. Wavy Progress Indicators for Budget Consumption
- **The Gap**: Budgets in `AnalyticsScreen.kt` and debt ratios in `HeroBalanceCard` use plain horizontal boxes or standard linear progress bars.
- **Opportunity**: Adopt `LinearWavyProgressIndicator` (stable in Compose Material 3 `1.5.0-rc01`). As the user approaches their monthly budget limit (80%+), the wave frequency and amplitude dynamically increase to convey caution organically without alarming the user.

### 7. Unified M3 Expressive SplitButton Architecture
- **The Gap**: Action variants are fragmented. The primary "Settle Up" action in `GroupDetailScreen` uses a deprecated `SplitButtonLayout`, while "Export" in `AnalyticsScreen` and "Add Expense" in `MainDashboard` use disconnected buttons or plain overflow menus.
- **Opportunity**: Create a reusable `SplitButtonActions.kt` component utilizing native `SplitButton` across all screens:
  - Add Expense: Primary "Add", Trailing "Add recurring", "Add from payment", "Add from template".
  - Settle Up: Primary "Settle", Trailing "Settle via UPI", "Settle via cash", "Settle via bank".
  - Export: Primary "Export PDF", Trailing "CSV", "JSON", "Share summary".

### 8. Granular Notification Channel Preferences
- **The Gap**: `NotificationSettingsScreen.kt` currently provides a binary switch for all notifications.
- **Opportunity**: Expose Android Notification Channels for fine-grained control:
  1. New Expenses & Edits (High Priority)
  2. Settlement Confirmations (High Priority)
  3. Debt Reminders & Bill Due Dates (Gentle / Silent)
  4. AI Spending Insights & Weekly Summaries (Low Priority)

### 9. In-App PDF Statement Previewer
- **The Gap**: Exporting to PDF generates a document on disk and triggers an Android Share sheet (`ExportBottomSheet.kt`). The user cannot review the generated PDF inside the app before sharing it with group members.
- **Opportunity**: Implement an in-app PDF preview sheet leveraging Android's `PdfRenderer` to display pages, total pages, and a direct "Share via WhatsApp" or "Save to Files" button.

### 10. True Adaptive Two-Pane Layout on Tablets & Foldables
- **The Gap**: In `MainDashboard.kt:549`, `ListDetail()` provides a basic side-by-side view on wide screens (`weight(0.42f)` and `weight(0.58f)`). However, it does not support fold state posture (`FoldingFeature.OcclusionType`, tabletop mode), and detail screens like `ExpenseDetailScreen` do not adapt into pane slots.
- **Opportunity**: Migrate to `androidx.compose.material3.adaptive:adaptive-navigation:1.1.0` with `ListDetailPaneScaffold`.
