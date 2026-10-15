# OpenSplit — build prompt for Claude Fable 5.1

Paste everything below the line into a Claude Code session opened at the repo root.

---

## Role

You are a senior Android architect and product designer. You are working inside the
existing OpenSplit repository (`/home/shantanu/Documents/OpenSplit`). OpenSplit is a free,
open-source, Splitwise alternative for Android. There will never be a subscription, an ad,
or a locked feature. Every feature Splitwise sells as "Pro" is free here.

Your job is to take the app from "many features exist" to "complete, polished, Material 3
Expressive, 2026 flagship quality". Work in this order: audit what exists, produce a gap
list, agree a plan, then implement in small verified increments. Do not rewrite working
code for style alone. Do not add a backend server; the app is client-driven on Firebase
plus the user's own AI API keys.

## Hard constraints

- Kotlin 2.2, Jetpack Compose, `material3` 1.5.x (Material 3 Expressive APIs:
  `MaterialExpressiveTheme`, `MotionScheme.expressive()`, expressive shapes, loading
  indicators, button groups, FAB menu where they fit). No XML views, no Material 2.
- `compileSdk` 37, `targetSdk` latest stable, `minSdk` 24. Handle Android 15+ edge-to-edge
  and predictive back.
- Authentication: Google Sign-In only (Credential Manager + Firebase Auth). No email or
  password flows; delete any that remain.
- Backend: Firebase Auth, Cloud Firestore (offline persistence on), Firebase Storage for
  receipt images, Firebase Cloud Messaging for push. No custom server. No Cloud Functions
  unless strictly required for push fan-out, and if so keep them minimal and documented.
- AI: bring-your-own-key. Users paste keys for Anthropic Claude, OpenAI GPT, and Google
  Gemini in Settings. Keys are stored encrypted on device (EncryptedSharedPreferences or
  DataStore with Tink) and never leave the device except to the provider's own API. Every
  AI feature must degrade gracefully to a non-AI fallback when no key is set. Every place
  AI is used shows a visible "AI" marker.
- Architecture stays MVVM over domain / data / ui with manual DI in `di/AppContainer`.
  Follow existing patterns before inventing new ones.
- Money: never hardcode a currency symbol. Use the central `CurrencyFormatter`. Balances
  are kept per currency and never summed across currencies. Use the existing
  `SplitCalculator`, `BalanceCalculator`, and `DebtSimplifier` as the single source of
  truth for arithmetic; extend them, do not duplicate them in ViewModels.
- Accessibility: content descriptions on every icon-only control, 48dp minimum touch
  targets, 4.5:1 text contrast, dynamic type up to 200 percent without clipping.
- Tests: domain logic has unit tests (JUnit). Screens get Roborazzi screenshot tests
  for light, dark, and AMOLED, phone and tablet. Keep the build green at every commit.

## What already exists (verify, do not assume)

Read these before planning. Screens live in `app/src/main/java/com/opensplit/ui/screens`,
components in `ui/components`, theme in `ui/theme`, navigation in
`ui/navigation/OpenSplitNavGraph.kt`, shell in `ui/screens/MainDashboard.kt`.

- Shell: five tabs (Home, Groups, Friends, Insights, Account) in a custom bottom bar;
  Activity opens from a bell icon; a custom speed-dial FAB; global search sheet; Haze
  blur headers.
- Screens: Welcome, Splash, PermissionPrimer, ProfileSetup, Home, Groups, GroupDetail,
  AddExpense (also used for edit), ExpenseDetail, SettleUp, SettlementHistory, Friends,
  PersonBalance, Activity, Analytics, Account (2381 lines, must be split),
  NotificationSettings, RecurringExpenses, M3ComponentsCatalog (debug only).
- Theme: `MaterialExpressiveTheme` with dynamic color, custom light and dark schemes,
  `OpenSplitTokens` (28dp brand radius, spacing scale, category colors, motion values),
  `OpenSplitMotion` (fade-through, shared-axis). Dark surface is `#1C1B1F`; there is no
  AMOLED variant yet.
- Domain: `Expense` model supports EQUAL, EXACT, PERCENTAGE, SHARES, ITEMIZED splits,
  multiple payers, items, recurrence (NONE/DAILY/WEEKLY/MONTHLY), category, notes, receipt
  URL, comments, soft delete (`isDeleted`). Logic: `SplitCalculator`, `BalanceCalculator`,
  `DebtSimplifier`, `CurrencyFormatter`, each with unit tests.
- Data: Firebase Auth, Firestore, Storage, Messaging; Room; DataStore preferences;
  WorkManager; Retrofit + Moshi + OkHttp; Coil; Haze.
- AI (Gemini only today): `data/ai/GeminiReceiptParser`, `GeminiSmartSplitSuggester`,
  `GeminiInsightsGenerator`, `GeminiGroupIconSuggester`. Account screen already stores an
  `aiProvider` and a `providerApiKeys` map in preferences.
- Export: CSV, JSON, PDF in `data/export`.
- Tests: unit tests for the three calculators and currency formatting. Roborazzi is wired
  in Gradle but has no tests.

Start by running the build and the unit tests, then produce a table: feature, status
(done / partial / missing / broken), file. Only then plan.

## Target feature set

Everything below is free. "Reference" lines name where the idea comes from so you can
match or beat it; do not copy anyone's layout or branding.

### Groups

- Name, optional description, avatar as emoji, letter, or uploaded image. AI can suggest
  an emoji from the name (exists for Gemini; make it provider-agnostic).
- Default currency per group. Lock the currency once the group has expenses (reference:
  Splitup). Expenses may still be in other currencies; balances settle per currency.
- Invite link and QR code with a creator-chosen expiry (1 hour, 1 day, 7 days, 30 days,
  never) and an optional max-use count. Expired or used links show a clear error page
  with "ask for a fresh link". Default expiry 7 days.
- Guests: a member can add a named guest who does not use the app. The guest's share is
  tracked; a claim link lets the real person take over the guest's balance later
  (reference: Splitup `/claim/:token`).
- Roles: creator and admin can edit group info, currency, and remove members. Members
  can add and edit expenses. Show a "can't be changed" explanation where locked.
- Leave group: allowed only when the member is settled in that group; otherwise point to
  Settle Up. On leaving, all past expenses and settlements keep the member's name and
  avatar; the member list shows a "left" badge; the member stops receiving activity.
  Undo via snackbar for a few seconds.
- Archive group: hides it from the main list, balances still count, new activity
  unarchives it. Archived list is reachable from Groups overflow.
- Delete group: creator only, double confirmation, soft delete with 90-day restore.
- Group settings: info, currency, members and invites, default split method, simplify
  debts on or off (reference: Splitwise), notifications for this group, export.
- Pin groups; sort by recent activity, name, member count; filter by currency.

### Expenses

- Add and edit use the same screen. Fields: description, amount with currency, date,
  category, paid by (one or many payers), participants (any subset of the group), split
  method, receipt photo, notes, recurrence. Description gets AI category suggestion as
  the user types; no key means keyword heuristic.
- Split methods: equal, exact, percentage, shares, itemized (assign receipt items to
  people, tax and tip spread proportionally). Show each person's resulting amount live;
  validate that exact and percentage totals match and explain the difference inline.
- Multiple payers with per-payer amounts that must sum to the total.
- Receipt scanning: photo or gallery, send to the user's chosen AI provider, stream
  partial fields into the form as they arrive, show a confidence note ("low confidence,
  check every field"), and allow "scan again". Fallback with no key: attach the photo
  only.
- Natural-language entry: a text box ("John paid 1500 for dinner yesterday, split with
  Rahul and me") that the AI turns into a prefilled expense form for review, never
  auto-saved. Fallback with no key: open the normal form with the text as description.
- Recurring bills: weekly, fortnightly, monthly, yearly, with next-occurrence preview,
  pause and resume, and a "set up by you / set up by others" split in the list.
  Occurrences are created by WorkManager on device and reconciled on sync.
- Drafts: when offline, an expense is saved as a local draft and the user adds it when
  back online; a draft review list lets them add several at once (reference: Splitup).
- Soft delete with undo snackbar; "Recently deleted" screen holds 90 days and restoring
  puts the expense back into balances. Permanent delete needs explicit confirmation.
- Edit history on each expense (who changed what, when) and comments under the expense,
  stored under `groups/{groupId}/expenses/{expenseId}/comments`.
- Currency: symbol from the formatter; in a multi-currency group show the ISO code next
  to any amount not in the group default. Optional conversion display using a cached
  daily rate from a free API, clearly labelled "approx" and never used for balances.

### Balances and settling

- Dashboard hero shows net balance per currency with "you owe" and "you are owed"
  breakdowns; tapping either opens a sheet that lists who and from which group.
- Every group row shows your position in that group as a chip ("You owe ₹1,250",
  "Rahul owes you ₹1,200", "Settled").
- Person balance screen: net across all groups plus direct expenses, per group lines,
  shared expenses list, settle up, remind.
- Simplify debts via `DebtSimplifier`, with a "why am I paying this person" explanation
  sheet that traces the minimal-transaction result (reference: Splitup "Why?").
- Settle up: pick person, amount defaults to the suggested amount, method (cash, UPI,
  bank, other), optional reference (UPI transaction id), date, note. Partial payments
  allowed. Warn when the direction contradicts the balance. Undo a settlement from
  history.
- Reminders: "Remind" sends a push to the debtor if they allow it, with rate limiting.

### People

- Friends list with mutual balance, search by name or email, filter owe / owed /
  settled, sort by balance.
- Add friend by email (friend request with accept, decline, ignore), by invite link
  (opening it and signing in makes you friends), or by sharing a group.
- Direct one-to-one expenses outside any group (reference: Splitup "Direct").
- Empty states for no friends and no shared expenses.

### Search

- Global search from the top bar on every main tab. Debounced input (300 ms), queries
  groups, expenses (description, notes, amount), payments, people. Works offline on the
  local cache.
- Results grouped by type with counts; filters for group, person, category, date range,
  expenses only / payments only, paid by me / anyone. Loading, no results, and empty
  "nothing to search yet" states.

### Activity

- Chronological feed: expense added, edited, deleted, restored; payment recorded or
  undone; member joined, left, removed; group created, renamed, archived; currency
  changed; comment added; recurring bill created or paused.
- Tabs or chips: All, per group, mentions me. Unread badge on the tab. Pull to refresh.
  Tapping an item deep-links to the expense, payment, or group.

### Insights

- Spending by category (donut), spending over time (line, month by month), top
  expenses, group versus direct, your share versus total paid. Range picker with month
  navigation and custom range.
- Budgets per category per month with progress; optional.
- Text insights: rule-based always ("Food is 40 percent of this month"), AI-written
  summary and suggestions when a key is set ("Settle with Rahul to clear three debts").
  Charts use the category colors from `OpenSplitTokens`, are legible in light, dark,
  and AMOLED, and have text alternatives for screen readers.

### AI, bring your own key

- Settings screen "AI features": provider selector (Claude, OpenAI, Gemini), key field
  per provider with show and hide, "Test key" button that makes one cheap request, model
  picker with a sensible default per provider, master toggle, and per-feature toggles
  (categorize, receipt scan, natural-language entry, insights, split suggestions, icon
  suggestions). Explain plainly that keys stay on device and calls go directly to the
  provider.
- One `AiProvider` interface in `data/ai` with three implementations; the existing
  Gemini classes become callers of that interface. Use structured output (JSON schema or
  tool call) for parsing tasks. Time out at 20 seconds, show errors as snackbars, never
  block the form.
- Usage counter per provider shown in settings so the user understands cost.

### Notifications

- Push for: new expense in your group, expense edited that changes your share, payment
  recorded with you, friend request, group invite accepted, reminder received, recurring
  bill due. In-app notification center with read state and a badge on the Activity tab.
- Settings: per-type toggles, quiet hours, per-group mute. Android 13+ runtime
  permission requested from a primer screen, never cold.

### Onboarding, settings, misc

- First launch: three short pages (split anything, settle smart, AI with your keys), then
  Google Sign-In, then profile setup (name, avatar, default currency), then permission
  primer. Skippable.
- Settings: profile (Google photo, name, email), appearance (System, Light, Dark,
  AMOLED, dynamic color toggle), default currency, language, notifications, AI features,
  recurring bills, archived groups, recently deleted, export (CSV, JSON, PDF per group
  or all), import from Splitwise CSV, sign out, delete account with data wipe, about and
  licenses.
- Offline banner ("You're offline, viewing only" or "saved as draft") on every screen
  that needs it.

## Design system

- Palette: brand primary is a soft violet (seed `#7C5CFF`, tonal palette generated via
  Material color utilities). Dynamic color is opt-in with brand as default. Three dark
  modes: Dark (surface `#141218` family) and AMOLED (surface `#000000`, containers built
  from `#0A0A0F` upward, no pure-grey tints). Money semantics: owed green, owing warm
  red-orange, settled neutral; both must pass contrast on all three backgrounds.
- Shape: 28dp for cards, sheets, dialogs; 40dp for the hero balance card; 16dp for list
  rows inside containers; full pill for buttons, chips, nav indicator, avatars.
- Spacing: 4dp grid; 16dp screen gutter on phones, 24dp on tablets; 24dp to 32dp inside
  hero containers.
- Typography: Material 3 scale. Display for the hero amount, headline for screen titles,
  title for card headers, body for content, label for chips. Tabular figures for all
  money; keep the existing mono convention for amounts.
- Surfaces: tonal containers (`surfaceContainerLow` to `surfaceContainerHighest`) rather
  than shadows. Group related rows into one rounded container with dividers instead of
  stacking many small cards.
- Motion: `MotionScheme.expressive()` springs; fade-through between tabs; shared-axis for
  drill-down; container transform from a group row to its detail; animated number for
  balances; undo snackbars with 5 second timeout.
- Navigation: M3 `NavigationBar` with five destinations (Groups, People, Activity,
  Insights, Settings), filled icon and pill indicator for the active tab. On medium
  and expanded widths (`WindowSizeClass`) switch to `NavigationRail`, and use list-detail
  panes for Groups and People. A persistent top app bar carries the section title,
  search, and overflow. One contextual FAB per tab (Groups: add expense; People: add
  friend; none elsewhere); "new group" lives in the Groups top bar.
- Bottom sheets for forms and filters, sized to content with a drag handle; full-screen
  only for Add Expense.
- Empty and error states with small illustrations and one clear action.
- Haptics on success (settle recorded, expense saved) and on destructive confirm.

## Working method

1. Build, run tests, read the shell, nav graph, theme, and the Expense and Group models.
2. Write the audit table to `docs/audit/YYYY-MM-DD-feature-audit.md`.
3. Propose a phased plan, each phase shippable and under a week:
   shell and theme foundation; AI provider abstraction and settings; expenses (drafts,
   deleted, history, scan streaming); groups (invites with expiry, guests, archive,
   leave); people and direct expenses; search; activity and notifications; insights
   and budgets; tablet layouts; onboarding and polish.
4. For each phase: write the spec, write failing tests where logic is involved, implement,
   add screenshot tests, run everything, commit with a conventional message.
5. Never leave a phase with a red build, a TODO without an issue, or a feature that works
   only with an AI key.

## Non-goals

- Payment rails (Venmo, PayPal, Paytm). Record payments only.
- Bank or card transaction import.
- Web or iOS clients.
- Any server-side AI proxy.

## Output expected from you now

The audit table, the phased plan with file-level touch points, and a list of questions
whose answers would change the plan. Then wait for approval before implementation.
