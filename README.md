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
- **Material 3 Expressive** — Material Symbols Rounded, brand violet or Material You,
  Light / Dark / AMOLED themes, navigation rail and list-detail panes on tablets.
- **Bring-your-own-key AI** — Claude, OpenAI, Gemini, Groq, Mistral, DeepSeek or any
  OpenAI-compatible endpoint. Keys stay on the device (encrypted). Receipt scanning,
  natural-language expense entry, smart categories, group icons and spending insights,
  each with an offline fallback and a per-feature switch.
- **Invite links with expiry** and QR codes, friend requests, direct 1:1 expenses, archived
  groups, recurring bills (weekly to yearly, pausable), recently deleted (90-day restore),
  edit history on every expense, global search across expenses, payments, groups and people,
  per-category monthly budgets, per-type push notification controls.

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

## 🚀 CI/CD & Android APK Build Flow (GitHub Actions)

OpenSplit uses **GitHub Actions** for automated Continuous Integration and Android APK generation on every push to `main` and on demand via `workflow_dispatch`.

> **Note on Web Hosting Platforms (Vercel / Netlify):**  
> Web platforms such as Vercel and Netlify only support frontend web applications and serverless functions (Next.js, React, Node.js). They do **not** provide the Android SDK, Android Build Tools, or the Gradle toolchain needed to compile Kotlin code into `.apk` binaries. All Android compilation and APK releases are handled through **GitHub Actions**.

### Automated Workflow Pipeline

The workflow defined in [`.github/workflows/android.yml`](.github/workflows/android.yml) executes the following pipeline on GitHub's `ubuntu-latest` runner:

1. **Environment Setup**: Checks out the repository and sets up JDK 21 (Temurin) with Gradle dependency caching.
2. **Permissions**: Ensures Gradle Wrapper has execute permissions (`chmod +x gradlew`).
3. **Firebase Configuration**: Injects `app/google-services.json` from the `GOOGLE_SERVICES_JSON` repository secret (or uses `.github/google-services.debug.json` placeholder for testing).
4. **Environment Variables**: Generates `.env` from the `GEMINI_API_KEY` repository secret (or sets safe fallback values for Secrets Gradle plugin).
5. **Keystore Generation**: Recreates the debug keystore with standard credentials for deterministic debug signing.
6. **Compile & Assemble**: Compiles the project using `./gradlew :app:assembleDebug --stacktrace`.
7. **Artifact Upload**: Uploads `OpenSplit-debug.apk` as a workflow artifact retained for 14 days.
8. **Release Publishing**: Automatically deploys the APK to the rolling [`nightly`](https://github.com/SutharShantanu/OpenSplit/releases/tag/nightly) release on GitHub.

### Optional GitHub Secrets

To customize CI builds, configure the following under **Settings > Secrets and variables > Actions**:

| Secret Name | Description | Default Fallback |
|-------------|-------------|------------------|
| `GOOGLE_SERVICES_JSON` | Base64-encoded `google-services.json` from Firebase console (`base64 -w 0 google-services.json`) | Uses `.github/google-services.debug.json` |
| `GEMINI_API_KEY` | Optional Gemini API key for AI features | Empty string fallback in `.env` |

## Contributing

Contributions are welcome — see **[CONTRIBUTING.md](CONTRIBUTING.md)**.

## License

Open source. (Add your preferred license file, e.g. `LICENSE`, before publishing.)
