# 05 — Content Design, Microcopy & Usability Audit

Content design in a financial application requires exceptional clarity, unambiguous action labels, intuitive terminology, and full transparency around AI-generated inferences.

---

## 1. Action Labels & Button Terminology Consistency

An audit of action verbs and labels across OpenSplit reveals multiple inconsistencies where identical actions use conflicting terms across different screens:

| Action Intent | Screen / Location 1 | Screen / Location 2 | Screen / Location 3 | Audit Finding & Recommendation |
| :--- | :--- | :--- | :--- | :--- |
| **Recording Debt Settlement** | `GroupDetailScreen.kt:242`<br>**"Settle Up"** | `SettleUpScreen.kt:190`<br>**"Record Payment"** | `MainDashboard.kt:220`<br>**"Settle up"** | **INCONSISTENCY**: The screen header says "Record Payment" while every entry point button says "Settle Up". Standardize on **"Record Settlement"** or **"Settle Up"** uniformly. |
| **Expense Creation** | `MainDashboard.kt:219`<br>**"Add expense"** | `AddExpenseScreen.kt:413`<br>**"Add Expense"** | `GroupSelectionDialog.kt:494`<br>**"Select Group for New Expense"** | **MINOR CASING INCONSISTENCY**: Title casing vs Sentence casing mixed (`"Add expense"` vs `"Add Expense"`). Standardize on Sentence case across all buttons per Google M3 Content Design guidance (`"Add expense"`). |
| **Form Submission** | `AddExpenseScreen.kt:1259`<br>**"Save Expense"** | `AddExpenseScreen.kt:618`<br>**"Process with AI"** | `CreateGroupDialog.kt:174`<br>**"Create"** | **MISMATCH**: Save button in Add Expense switches dynamically to "Update Expense" when editing, which is good. However, "Process with AI" reads like an internal machine operation. Better: **"Review with AI"** or **"Parse Expense"**. |
| **Navigation Back** | `HeaderAction.kt:73`<br>**"Back"** | `AddExpenseScreen.kt:418`<br>**"Close"** | `SettleUpScreen.kt:196`<br>**"Close"** | Sub-screens pushed onto the stack use a left arrow with contentDescription "Back", but modal sheets use a close icon with contentDescription "Close". This distinction is correct, but modal sheets pushed via `dialog()` routes inconsistently use arrow back in `PermissionPrimerScreen`. |

---

## 2. Financial Terminology & Mental Models

### 2.1 "You Are Owed" vs "You Owe"
- In [HeroBalanceMotif.kt:125-127](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/ui/components/HeroBalanceMotif.kt#L125), the application uses all-caps labels:
  - `YOU ARE OWED IN TOTAL`
  - `YOU OWE IN TOTAL`
  - `ALL SETTLED UP`
- **Critique**: All-caps text (`uppercase()`) with wide letter spacing (`letterSpacing = 1.sp`) slows down readability for financial figures. Research indicates mixed-case text is recognized up to 14% faster than all-caps text.
- **Recommendation**: Switch to Title Case or Sentence Case: `"You are owed in total"` / `"You owe in total"`.

### 2.2 Debt Simplification Explanation
- OpenSplit implements a graph simplification algorithm in [DebtSimplifier.kt](file:///home/shantanu/Documents/OpenSplit/app/src/main/java/com/opensplit/domain/logic/DebtSimplifier.kt) to minimize transaction counts between members.
- **The Gap**: On `GroupDetailScreen.kt:288`, the UI displays a simplified settlement suggestion:
  > *"Rahul owes you ₹850 — Record it once they pay you"*
- **User Confusion**: Users who know Rahul did not directly participate in a specific lunch expense are confused why Rahul owes them money. The app fails to communicate that **Debt Simplification** reorganized the multi-person debt chain.
- **Recommendation**: Add a subtle contextual badge or info sheet: `"Simplified debt (2 transactions saved) · Learn how this was calculated"`.

---

## 3. Empty State Messaging & Tone

Empty states in financial applications must be constructive, reassuring, and guide the user toward the single most logical next step.

### Audit of `ExpressiveEmptyState` Implementations:

| Screen | Empty State Heading | Subtitle Message | Next Step Affordance | Audit Rating |
| :--- | :--- | :--- | :--- | :--- |
| **GroupsScreen** | "No groups yet" | "Create a group to start splitting bills with friends or roommates." | Shows button: "Create Group" | **EXCELLENT** |
| **FriendsScreen** | "No friends yet" | "Add friends to split bills 1-on-1 without creating a group." | **MISSING**: No button inside the illustration; only an arrow pointing to the FAB | **NEEDS IMPROVEMENT** |
| **ActivityScreen** | "No activity yet" | "Transactions and group updates will appear here." | Purely static; no action provided | **ACCEPTABLE** |
| **AnalyticsScreen** | "No Analytics Yet" | "Analytics will show up once you've added expenses to your groups." | Static illustration; no shortcut to add an expense | **NEEDS IMPROVEMENT** |
| **RecentlyDeleted** | "Trash is empty" | "Expenses you delete stay here for 90 days before permanent removal." | Informative and reassuring | **EXCELLENT** |

---

## 4. Error Messages & Feedback Recovery

Error handling in OpenSplit was inspected across network timeouts, Firebase authorization failures, camera permission denials, and AI parsing errors:

1. **AI Parsing Failures**:
   - In `AddExpenseScreen.kt:604`:
     > *"Couldn't understand that. Try: 'Rahul paid 1200 for dinner'."*
   - **Rating**: **EXCELLENT**. Instead of a generic error, it provides a concrete natural language example.
2. **Receipt Scan Failures**:
   - In `AddExpenseScreen.kt:259`:
     > *"Couldn't read the receipt: Model timeout. Try a clearer photo."*
   - **Rating**: **GOOD**. Explains both what happened and how to recover.
3. **Save Failures**:
   - In `AddExpenseScreen.kt:1264`:
     > *"Couldn't save: $it"*
   - **Critique**: Emits raw exception strings (e.g. `com.google.firebase.firestore.FirebaseFirestoreException: PERMISSION_DENIED`) directly onto the UI.
   - **Fix**: Map Firestore error codes to friendly human copy: *"We couldn't save this expense because you don't have permission in this group. Check your connection or contact the group admin."*

---

## 5. AI Transparency & Ethical Disclosure

OpenSplit offers BYO-AI features where expenses can be categorized, parsed from receipts, or extracted from natural language text.

### Compliance with Google's AI UX Guidelines:
1. **AI Inferences Explicitly Marked**:
   - In `AddExpenseScreen.kt:748`, when a category is predicted by AI, an `AiBadge` is displayed: `"AI suggested · Gemini 2.5 Flash"`.
   - **Rating**: **COMPLIANT**. Gives clear credit to the AI model and allows the user to tap another category to override it.
2. **Low-Confidence Warning**:
   - In `AddExpenseScreen.kt:253`, when receipt OCR has confidence below 0.65:
     > *"Low confidence: the photo was hard to read. Check every field."*
   - **Rating**: **COMPLIANT**. Alerts the user to verify amounts before committing.
3. **Offline / Local Fallback Disclosure**:
   - When no AI API key is configured, the app uses `AiHeuristics.kt` (regex/keyword matcher).
   - In `AddExpenseScreen.kt:601`, it displays: *"Filled in what could be read offline. Add an AI key in Settings for smarter parsing."*
   - **Rating**: **EXCELLENT**. Transparently communicates capabilities.
