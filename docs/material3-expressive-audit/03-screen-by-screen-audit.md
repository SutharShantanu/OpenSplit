# 03 — Comprehensive Screen-by-Screen UI/UX Audit

This document details the exhaustive visual, architectural, interaction, and accessibility audit of every discovered screen, modal bottom sheet, and dialog within OpenSplit.

---

## 1. Authentication & Onboarding

### 1.1 WelcomeScreen & Google Sign-In
- **File**: [WelcomeScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/auth/WelcomeScreen.kt)
- **Primary Function**: First-time user greeting, value proposition showcase, and Google Credential Manager authentication.
- **Findings & Deficiencies**:
  1. *Visual Hierarchy*: The branding icon and tagline ("Split expenses without friction") are well-centered, but the feature highlights list below compresses on screens with display scaling > 1.25x.
  2. *Component Selection*: Uses a custom `GoogleSignInButton.kt` rather than the official Credential Manager Bottom Sheet or M3 Outlined Button with Google logo.
  3. *Typography*: Tagline uses `headlineMedium`, but line-height is not explicitly constrained, leading to overlap with the illustration on compact devices (< 640dp height).
  4. *Accessibility*: The Google Sign-In button has a custom role but lacks an explicit state announcement when the Credential Manager request is executing in the background.

### 1.2 ProfileSetupScreen
- **File**: [ProfileSetupScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/auth/ProfileSetupScreen.kt)
- **Primary Function**: New user profile initialization (display name, default currency, optional profile photo).
- **Findings & Deficiencies**:
  1. *Shape Consistency*: The photo picker avatar uses `CircleShape` with a camera icon overlay. The camera overlay button has a touch target of 32x32dp, failing the 48dp minimum requirement.
  2. *Color Usage*: Save button uses `MaterialTheme.colorScheme.primary`, but text fields use `OutlinedTextField` with hardcoded 12dp rounded corners (`InputShape`) rather than the M3 Expressive medium shape token.

### 1.3 PermissionPrimerScreen
- **File**: [PermissionPrimerScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/PermissionPrimerScreen.kt)
- **Primary Function**: Educational primer explaining why OpenSplit needs Contacts, Camera, and Notification permissions prior to triggering Android system dialogs.
- **Findings & Deficiencies**:
  1. *Layout & Containment*: Centers a single large card (`CardDefaults.cardColors(containerColor = surfaceContainerHigh)`). On tablets and landscape viewports, the card is restricted to 520dp (`responsiveDialogWidth`), which looks clean and properly anchored.
  2. *Component Selection*: The "Skip" action in the `TopAppBar` uses `TextButton`. The primary "Continue" button at the bottom is an unstyled `Button` with no elevation or shape morphing.
  3. *Accessibility*: Permission explanation rows use `SymbolIcon(symbol = ..., contentDescription = null)` without grouping the row semantics. TalkBack reads the icon and text as separate items.

---

## 2. Dashboard & Top-Level Destinations

### 2.1 MainDashboard & Top Navigation
- **File**: [MainDashboard.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/MainDashboard.kt)
- **Primary Function**: Primary application container coordinating five destinations (`GROUPS`, `PEOPLE`, `ACTIVITY`, `INSIGHTS`, `SETTINGS`), pull-to-refresh, wide-screen navigation rail, and global search.
- **Findings & Deficiencies**:
  1. *Top Bar Layout*: Uses a custom 88dp single-row `TopAppBar` (`expandedHeight = 88.dp`) with `headlineLarge` title. While this creates a bold header, the title text is truncated prematurely when 3 action buttons are present in `actions = { ... }`.
  2. *Navigation Architecture*: In phone mode, uses `FloatingNavBar.kt` which applies a background gradient (`Brush.verticalGradient(0f to Transparent, 0.45f to background)`). When scrolling long lists, list items underneath are partially faded before reaching the bottom edge.
  3. *FAB Inconsistency*: In `GROUPS` tab, an expandable `ActionFabMenu` is displayed floating above the nav bar (`bottom = 100.dp`). In `PEOPLE` tab, a plain 72dp circular `FloatingActionButton` is displayed. In `INSIGHTS` and `SETTINGS`, no FAB exists. The spatial jumping of the FAB between tabs disrupts muscle memory.
  4. *Adaptive Support*: Switches to `WideNavigationRail` when `screenWidthDp >= 600`. While functional, the breakpoint check is hardcoded in `useNavigationRail()` rather than using Android's `WindowWidthSizeClass.Compact` / `Medium` / `Expanded`.

### 2.2 GroupsScreen (Tab 1)
- **File**: [GroupsScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/GroupsScreen.kt)
- **Primary Function**: Display all expense-sharing groups, net balances per group, pinned/archived groups, and group creation trigger.
- **Findings & Deficiencies**:
  1. *Visual Hierarchy*: Features a `HeroBalanceCard` at the top summarising total owed/owe balances, followed by search bar and filter chips, then group cards. The visual weight of the hero card dominates the viewport, pushing actual groups below the fold on standard 1080p phones.
  2. *Group Cards*: Group cards display group avatar, name, last activity timestamp, and net balance badge. The net balance badge uses `OwedPositive` (`#1B5E20`) or `OwedNegative` (`#BA1A1A`) as text color with insufficient contrast on dark theme surfaces.
  3. *Swipe Actions*: Supports swipe-to-pin and swipe-to-archive (`SwipeActionRow.kt`). However, there is no visual affordance or tutorial indicating that swipe gestures exist on the group items.

### 2.3 FriendsScreen (Tab 2)
- **File**: [FriendsScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/FriendsScreen.kt)
- **Primary Function**: Directory of 1-on-1 friends, incoming/outgoing friend invites, friend QR code sharing, and individual net balances.
- **Findings & Deficiencies**:
  1. *Incoming Invites Banner*: Displays a card with "Accept" and "Ignore" buttons for incoming requests. The buttons use standard `TextButton` and `Button` with default shapes, visually clashing with the adjacent rounded list items.
  2. *Empty State*: When the user has no friends, `ExpressiveEmptyState` renders a soft burst icon with "Add your first friend". However, the call-to-action button is missing inside the empty state component itself, forcing users to discover the floating action button at the screen bottom.

### 2.4 ActivityScreen (Tab 3)
- **File**: [ActivityScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/ActivityScreen.kt)
- **Primary Function**: Chronological feed of all actions (expenses created, edited, deleted, settlements recorded, members joined).
- **Findings & Deficiencies**:
  1. *Content Organization*: Groups items into date sections ("Today", "Yesterday", "MMM d, yyyy"). The section header typography uses small plain text, blending into the transaction list items.
  2. *Interactive Items*: Tapping an expense activity row navigates to `ExpenseDetailScreen`, but settlement activities and member join activities are non-interactive. There is no chevron or visual cue distinguishing clickable rows from non-clickable ones.
  3. *Touch Target Defect*: The filter chip row (`FilterChip` for Expenses, Settlements, Members) uses horizontal scroll with 4dp vertical spacing. The touch padding around chips is less than 48dp vertically.

### 2.5 AnalyticsScreen / Insights (Tab 4)
- **File**: [AnalyticsScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AnalyticsScreen.kt)
- **Primary Function**: Financial insights, spending breakdown charts, monthly cash flow bar charts, top categories donut chart, and AI spending commentary.
- **Findings & Deficiencies**:
  1. *CashFlowBarChart Semantics*: In `AnalyticsScreen.kt:749`, the entire bar chart row is merged into a single accessibility semantics node (`clearAndSetSemantics`). TalkBack users cannot touch or focus individual bars (weeks/months) to inspect specific numbers.
  2. *DonutChart Truncation*: In `AnalyticsScreen.kt:802`, the semantic content description only reads the top 4 categories (`categories.take(4)`). Categories 5 through 10 are completely hidden from assistive technologies.
  3. *Chart Bar Styling*: Bars in `CashFlowBarChart` use `clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp))`. In M3 Expressive, chart elements should use organic pill shapes or connected bars with smooth corner morphing.
  4. *Period Picker Dialog*: `PeriodPickerDialog` in lines 981-1050 uses a raw `AlertDialog` containing a custom 4x3 month grid. The month chips have touch targets of ~36dp, causing accidental selections on touchscreens.

### 2.6 AccountScreen & Settings Hub (Tab 5)
- **File**: [AccountScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AccountScreen.kt)
- **Primary Function**: Comprehensive user settings hub with categorized preferences, sync status, cache management, security, and appearance.
- **Findings & Deficiencies**:
  1. *Section Categorization*: Previously an unorganized flat list, now grouped into CategoryGroups ("Sync & Storage", "Preferences", "Intelligence", "Data & Backup", "Security & About") with segmented container tiles (`segmentShape(idx, group.categories.size)`).
  2. *Search in Settings*: Features `AppSearchBar` that filters categories in real-time. If no categories match, `ExpressiveEmptyState` is shown.
  3. *Sub-Screen Navigation*: Switches between the master list and sub-screens via `AnimatedContent` with `sharedAxisX()`. However, the top-level bottom nav bar remains visible on sub-screens, causing layout collisions with keyboard inputs in sub-screens like `AiProvidersSubScreen`.

---

## 3. Expense Management & Core Workflows

### 3.1 GroupDetailScreen
- **File**: [GroupDetailScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/GroupDetailScreen.kt)
- **Primary Function**: Core group screen displaying group expense list, monthly total spending banner, simplified settlements, and member roster across 3 tabs (`Expenses`, `Balances`, `Members`).
- **Findings & Deficiencies**:
  1. *Deprecated SplitButtonLayout*: In lines 237-257, uses deprecated `SplitButtonLayout` with manual `SplitButtonDefaults.LeadingButton` and `TrailingButton`.
  2. *Trailing Dropdown Menu*: The trailing menu uses a standard Compose `DropdownMenu` with sharp corners, rather than the M3 Expressive rounded tonal menu.
  3. *Spending Banner*: The "Group Total Spending" card uses `surfaceContainerLow` with an inner `SplitButton`. When the font scale is enlarged to 1.3x+, the split button wraps awkwardly, pushing the total spending text out of alignment.
  4. *Swipe Actions on Expense Rows*: Swiping an expense row allows quick editing or deleting (`SwipeActionRow.kt`), but the swipe threshold is overly sensitive, frequently triggering when users attempt a vertical scroll.

### 3.2 AddExpenseScreen
- **File**: [AddExpenseScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AddExpenseScreen.kt)
- **Primary Function**: Comprehensive expense entry supporting natural language AI description, Camera receipt OCR, manual entry, 5 split modes, multiple payers, repeat recurrence, and document attachment.
- **Findings & Deficiencies**:
  1. *ModalBottomSheet Behavior*: Wrapped in a `ModalBottomSheet` with `skipPartiallyExpanded = true`. When launched, it snaps immediately to full height. On tablets, this creates a massive full-height sheet that obscures context.
  2. *Progressive Stepwise Reveal*: Uses `AnimatedVisibility` to reveal Description after Amount is typed, Category after Description is typed, and Split Mode after Category. While designed to simplify the form, users who want to change the split mode first are artificially blocked until they enter a dummy amount.
  3. *Split Mode Selector*: In lines 1008-1019, split modes ("Equally", "Exact", "Percent", "Shares", "Itemized") are rendered as `ToggleButton`s inside a `Row` with `horizontalScroll`. In M3 Expressive, horizontal scrolling within a segmented button group is an antipattern; a wrapping `FlowRow` or `ButtonGroup` with icons should be used.
  4. *Touch Target Violations in Itemized Mode*: In lines 1087-1101, the shares increment/decrement buttons (`+` and `-`) have hardcoded sizes of `Modifier.size(36.dp)`, violating the 48dp minimum touch target.
  5. *Currency Amount Visual Transformation*: In lines 1333-1376, `CurrencyAmountVisualTransformation` formats digits with Indian lakh/crore commas or international thousand commas. When typing rapidly or backspacing near commas, the cursor offset mapping occasionally jumps to the end of the string.

### 3.3 ExpenseDetailScreen
- **File**: [ExpenseDetailScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/ExpenseDetailScreen.kt)
- **Primary Function**: Inspect an individual expense's details: payer, exact amount per participant, itemized breakdown, attached receipt image/PDF, and comments thread.
- **Findings & Deficiencies**:
  1. *Receipt Image Handling*: Displays receipt thumbnail. Tapping the receipt thumbnail opens an external viewer or expands inline without a full pan/zoom modal, making small receipt items unreadable.
  2. *Top App Bar Actions*: Top bar actions (Edit and Delete) are plain `IconButton`s with 24dp glyphs inside standard 48dp boxes. They lack container fills and do not use the expressive button morph shape.
  3. *Delete Confirmation*: In lines 65-71, the delete button directly launches an `AlertDialog`. The dialog confirm button is styled with `ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)`, but the cancel button uses plain `TextButton` without consistent padding.

### 3.4 SettleUpScreen
- **File**: [SettleUpScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/SettleUpScreen.kt)
- **Primary Function**: Record debt settlement payments between two group members with UPI intent dispatch, Cash, Bank Transfer, or Other methods.
- **Findings & Deficiencies**:
  1. *Swap Direction Affordance*: In lines 264-299, a pill button ("Paying" with sync icon) swaps payer and payee. While useful, the touch area is constrained to the small pill between two large avatar columns, leading to frequent mis-taps on the adjacent avatars.
  2. *Amount Input Alignment*: Uses `BasicTextField` with custom `dynamicFontSize` (lines 123-129) scaling down from 34sp to 20sp. The text field decoration box lacks an explicit cursor tint for dark mode, causing the blinking cursor to blend into the container background.
  3. *Payment Method Selector*: In lines 434-439, uses `ConnectedChoiceGroup` for methods (UPI, Cash, Bank, Other). If the selected currency is not INR, UPI is disabled or displays a snackbar warning, but the UPI button remains visually active in the selector without a disabled state.
  4. *SettlementSuccessOverlay*: In lines 175-183, a full-screen animated overlay celebrates the completed payment. The checkmark animation is smooth, but the dismiss button has no keyboard focus target, trapping TalkBack virtual cursor.

### 3.5 SettlementHistoryScreen
- **File**: [SettlementHistoryScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/SettlementHistoryScreen.kt)
- **Primary Function**: Filterable historical ledger of all past settlements across all groups, with export options (PDF, CSV, JSON).
- **Findings & Deficiencies**:
  1. *Visual Density*: The settlement rows are dense with payer avatar, arrow indicator, payee avatar, amount, date, and method badge. On small screens, text overlaps the method badge.
  2. *Export Hub Integration*: The export bottom sheet (`ExportBottomSheet.kt`) triggers background PDF generation via `PdfExportGenerator.kt`. While processing, there is no progress indicator on the export button, leading users to tap repeatedly.

### 3.6 PersonBalanceScreen
- **File**: [PersonBalanceScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/PersonBalanceScreen.kt)
- **Primary Function**: View net balances with an individual person across all shared groups, broken down by currency.
- **Findings & Deficiencies**:
  1. *Multi-Currency Display*: If balances exist in both INR and USD, renders separate balance rows. The currency balance cards use `surfaceContainer` with identical styling regardless of whether the user is owed or owes, removing quick color-coded visual distinction.
  2. *Action Row*: Provides "Settle up" and "Add expense" buttons at the bottom. However, if multiple shared groups exist, tapping "Settle up" does not prompt the user which group to settle in, defaulting to the first group in the list.

---

## 4. Settings & Secondary Sub-Screens

### 4.1 AppearanceSettingsSubScreen
- **File**: [AppearanceSettings.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AppearanceSettings.kt)
- **Primary Function**: Configure Theme (System, Light, Dark), Dynamic Color (Monet), Font Choice (Google Sans Flex, Inter, Poppins, Mono, etc.), Reduce Motion, and Haptics.
- **Findings & Deficiencies**:
  1. *Theme Mode Selection Card*: Uses 3 preview cards for System, Light, and Dark. In Dark mode, the "Light" card preview has an un-themed white border that glows harshly.
  2. *Font Selection*: Lists 9 downloadable fonts. When tapping a font that is downloading from Google Play Services, there is no loading indicator on the item, causing users to think the selection failed until the font suddenly switches.

### 4.2 AiProvidersSubScreen
- **File**: [AiSettings.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/AiSettings.kt)
- **Primary Function**: Configure BYO-AI API keys (Gemini, OpenAI, Anthropic, Groq, DeepSeek, Ollama), test connections, enable/disable features (categorization, receipt scan, natural language entry), and view token usage.
- **Findings & Deficiencies**:
  1. *Security Input*: API key input field has a show/hide password toggle. However, once saved, the API key is displayed as a masked string `sk-...1234` without a clear "Remove Key" or "Replace Key" workflow.
  2. *Test Connection Button*: In lines 84-85, "Test API Key" executes an asynchronous verification call. During testing, the button displays a spinner, but on error, the error message is displayed as red caption text without an announcement to screen readers.

### 4.3 PaymentDetectionScreen
- **File**: [PaymentDetectionScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/PaymentDetectionScreen.kt)
- **Primary Function**: Review intercepted payments from notification listener service, convert payments to expenses or settlements, and manage detection preferences.
- **Findings & Deficiencies**:
  1. *Permission State*: If Notification Listener permission is not granted, displays a card directing user to system settings. On Android 13+ (API 33+), restricted settings can block sideloaded apps from enabling notification access. The screen mentions "Allow restricted settings", but lacks an illustration or deep link to App Info.
  2. *Pending Transaction Cards*: Displays merchant, amount, date, and "Add Expense" / "Settle Up" actions. The action buttons use `FilledTonalButton` with 36dp height, which is difficult to tap accurately in quick succession.

### 4.4 RecurringExpensesScreen & RecentlyDeletedScreen
- **Files**: [RecurringExpensesScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/RecurringExpensesScreen.kt), [RecentlyDeletedScreen.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/screens/RecentlyDeletedScreen.kt)
- **Findings & Deficiencies**:
  1. *Recurring Expenses*: Lists scheduled expenses with pause/resume and stop actions. The next occurrence date is displayed in small text without indicating how many days remain until the next trigger.
  2. *Recently Deleted*: Trash bin retaining items for 90 days. Lacks an "Empty Trash" / "Purge All" action in the `TopAppBar`, forcing users to purge items one-by-one.

---

## 5. Dialogs, Sheets & Modals

### 5.1 GlobalSearchSheet
- **File**: [GlobalSearchSheet.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/GlobalSearchSheet.kt)
- **Primary Function**: Universal search across groups, expenses, settlements, and friends.
- **Findings & Deficiencies**:
  1. *Search Input*: Uses an `OutlinedTextField` with a manual 300ms debounce. In M3 Expressive, the official `SearchBar` handles expanding from rest state to full-screen results seamlessly.
  2. *Result Categorization*: Results are displayed in a single `LazyColumn` with small section headers. Searching for a common term (e.g. "Dinner") produces mixed rows of groups and expenses that look too similar.

### 5.2 CreateGroupDialog
- **File**: [CreateGroupDialog.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/CreateGroupDialog.kt)
- **Primary Function**: New group creation with automatic AI avatar suggestion, group type chips, and currency dropdown.
- **Findings & Deficiencies**:
  1. *Modal Containment*: Rendered inside a `ModalBottomSheet`. The "Create" action is placed in the bottom bar rather than the top-end header, causing the button to be obscured when the soft keyboard opens on compact screens.
  2. *Currency Selector*: Uses `ExposedDropdownMenuBox`. On small screens with the keyboard open, the exposed dropdown expands upwards, covering the group name field.
