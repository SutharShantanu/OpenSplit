# OpenSplit

A **free, open-source, fully-featured expense-splitting Android app** — a Splitwise
alternative with **no paid tiers and no locked features**. Everything is free.

Built with **Kotlin**, **Jetpack Compose** (100% **Material 3**), and **Firebase**
(Auth + Firestore), OpenSplit lets groups of people track shared expenses, split them
fairly, and settle up.

[![Build Debug APK](https://github.com/SutharShantanu/OpenSplit/actions/workflows/android.yml/badge.svg?branch=main)](https://github.com/SutharShantanu/OpenSplit/actions/workflows/android.yml)

## 📥 Download

Grab the latest debug build — no toolchain required:

**[⬇️ Download OpenSplit-debug.apk](https://github.com/SutharShantanu/OpenSplit/releases/download/nightly/OpenSplit-debug.apk)**
&nbsp;·&nbsp; [All releases](https://github.com/SutharShantanu/OpenSplit/releases) &nbsp;·&nbsp; [Build artifacts](https://github.com/SutharShantanu/OpenSplit/actions/workflows/android.yml)

The `nightly` release is refreshed automatically on every push to `main`. Install it on a
connected device with:

```bash
adb install -r OpenSplit-debug.apk
```

> The published APK is built with a **placeholder Firebase config**, so it installs and
> the UI works but sign-in/sync won't function. For a fully working build, compile
> locally with your own `app/google-services.json` (see [docs/SETUP.md](docs/SETUP.md))
> or set the `GOOGLE_SERVICES_JSON` repo secret so CI uses your real config.

## Features

- **Groups & friends** — create groups, add members by email or from contacts, see
  who owes whom.
- **Expenses** — add, edit and delete expenses with five split modes:
  **Equal**, **Exact amounts**, **Percentage**, **Shares**, and **Itemized**
  (per-item assignment).
- **Choose participants** — split an expense among only the people involved, not
  always the whole group.
- **Multiple payers** — record expenses paid by more than one person.
- **Smart settle-up** — debt-simplification minimizes the number of payments needed;
  record settlements (Cash / UPI / Bank transfer / Other) and see settlement history.
- **Balances that are actually correct** — group balances reflect recorded
  settlements, and **multiple currencies are shown per-currency** (never summed into a
  meaningless mixed total).
- **Activity feed**, **analytics** (spend by category / month), and **data export**
  (CSV / JSON / PDF).
- **Offline-first** — Firestore local persistence keeps the app usable without a
  connection.
- **Material 3 everywhere** — dynamic color support, light/dark/system themes.

## Tech stack

| Area | Choice |
|------|--------|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM over Clean Architecture (domain / data / ui) |
| DI | Manual (`di/AppContainer`) — Hilt disabled for AGP 9.1.1 compatibility |
| Backend | Firebase Auth + Cloud Firestore |
| Local prefs | DataStore |
| Async | Kotlin Coroutines + Flow |
| Background | WorkManager (recurring expenses) |
| Images | Coil |
| Testing | JUnit4, Robolectric, Roborazzi |

## Project layout

```
app/src/main/java/com/opensplit/
  domain/     # models, repository interfaces, pure logic (SplitCalculator, DebtSimplifier)
  data/       # Firebase/DataStore repository implementations, export generators
  di/         # AppContainer (manual DI)
  ui/         # Compose screens, viewmodels, navigation, theme, components
  util/       # CurrencyFormatter
```

## Getting started

See **[docs/SETUP.md](docs/SETUP.md)** for full setup, including the Firebase
configuration you must supply (`google-services.json`) before the app will build.

Quick version:

```bash
# 1. Add your own app/google-services.json from the Firebase console
# 2. Add a .env with GEMINI_API_KEY=... (see .env.example)
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Contributing

Contributions are welcome — see **[CONTRIBUTING.md](CONTRIBUTING.md)**.

## License

Open source. (Add your preferred license file, e.g. `LICENSE`, before publishing.)
