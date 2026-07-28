# Setup

OpenSplit needs a Firebase project. The app **will not build** without a
`google-services.json`, because the `com.google.gms.google-services` Gradle plugin
requires it.

## 1. Prerequisites

- Android Studio (latest stable) or the Android SDK command-line tools
- JDK 11+
- A Google account (for Firebase)

## 2. Create the Firebase project

1. Go to the [Firebase console](https://console.firebase.google.com/) and create a
   project.
2. Add an **Android app** with the package name / application ID:

   ```
   com.opensplit
   ```

   (This is the `applicationId` in `app/build.gradle.kts`. If you change that value,
   register the app under the new ID instead.)
3. Download the generated **`google-services.json`** and place it at:

   ```
   app/google-services.json
   ```

   This file is git-ignored on purpose — never commit it.

## 3. Enable Firebase services

In the Firebase console:

- **Authentication** → enable **Email/Password** and **Google** sign-in.
- **Cloud Firestore** → create a database.
- Deploy the security rules from [`firestore.rules`](../firestore.rules):

  ```bash
  # with the Firebase CLI
  firebase deploy --only firestore:rules
  ```

  The app also enables **offline persistence**, so it works without a connection and
  syncs when back online.

## 4. Gemini API key (optional — receipt scanning)

Receipt OCR uses the Gemini API. Copy `.env.example` to `.env` and set your key:

```
GEMINI_API_KEY=your_key_here
```

The Secrets Gradle plugin injects it as `BuildConfig.GEMINI_API_KEY` at build time.
Without a key, the rest of the app works normally; only "Scan receipt (AI)" in the
itemized split is unavailable (it degrades gracefully with a toast).

## 5. Push notifications (optional — Cloud Functions)

The app registers each signed-in user's FCM token on `users/{uid}.fcmToken` and shows
local notifications via `OpenSplitMessagingService`. To actually deliver a push to
*other* group members when someone adds an expense or settlement, deploy the Cloud
Function under [`functions/`](../functions) — client SDKs cannot send to other devices:

```bash
cd functions
npm install
firebase deploy --only functions
```

The `onActivityCreated` function fans a notification out to a group's members (except
the actor) whenever a new activity entry is written. Without it, only self/test
notifications appear.

## 6. Get an APK without a local toolchain (GitHub Actions)

Every push to `main` (or a manual run via the Actions tab → **Build Debug APK** →
*Run workflow*) builds `app-debug.apk` and uploads it as an artifact. Download it from
the run's **Artifacts** section, then `adb install -r app-debug.apk`.

- By default CI uses a placeholder `google-services.json` (`.github/google-services.debug.json`)
  so the build succeeds — the app installs and the UI works, but Firebase (sign-in, sync)
  won't function.
- For a fully working APK, add a repo secret **`GOOGLE_SERVICES_JSON`** =
  `base64 -w0 app/google-services.json` and the workflow will use it instead.

The wrapper is committed, so `./gradlew` works after `git pull` (Gradle 9.3.1).

## 7. Recurring expenses (Firestore index)

Recurring expenses are materialized by a daily WorkManager job that queries each of
your groups for due templates (`recurrence.nextOccurrence <= now`). Firestore
auto-creates the required single-field index on first run; if prompted in the console
or logs, follow the offered link to create it.

## 8. Run on an emulator or device

The Gradle **wrapper is committed**, so you don't need Gradle installed — use `./gradlew`
on macOS/Linux or `.\gradlew.bat` on Windows.

**Prerequisites:** `app/google-services.json` must be present (step 2.3) or the build stops
at the `google-services` plugin. For the friend-invite feature, deploy the Firestore rules
(step 3).

### One-command run (Linux/macOS)

With an emulator running (or a device connected via USB debugging):

```bash
./scripts/run-emulator.sh
```

It pulls the latest `main`, checks your emulator is connected, builds + installs the debug
APK (`:app:installDebug`), and launches OpenSplit — all in one step. Pass `--no-pull` to
build the current working tree without pulling.

> **Google sign-in not completing?** If the Google account chooser appears but sign-in
> doesn't finish, this build's signing **SHA-1** is almost certainly not registered in your
> Firebase project. Print it with:
> ```bash
> ./scripts/print-debug-sha1.sh
> ```
> then add the `SHA1` value in **Firebase console → Project settings → your Android app →
> Add fingerprint**, re-download `google-services.json` into `app/`, and re-run.

```bash
# 1. Get the latest code
git pull origin main

# 2. Start an emulator (or plug in a device with USB debugging enabled)
emulator -list-avds                 # list your AVDs
emulator -avd <your_avd_name>       # e.g. Pixel_7_API_34  (run in a separate terminal)
adb devices                         # must show it as "device" (not unauthorized/offline)

# 3. Build + install to the connected device/emulator, then launch
./gradlew :app:installDebug         # Windows: .\gradlew.bat :app:installDebug
adb shell monkey -p com.opensplit -c android.intent.category.LAUNCHER 1
```

**Android Studio (simplest):** open the project, pick your emulator/device in the toolbar
dropdown, and hit **Run ▶** — it builds, installs, and launches in one click.

### Build / test / lint

```bash
./gradlew :app:assembleDebug        # build the debug APK
./gradlew :app:testDebugUnitTest    # unit tests (split math, balances, currency)
./gradlew :app:lintDebug            # lint
```

Prefer not to build locally? Grab the prebuilt APK from
[Releases](https://github.com/SutharShantanu/OpenSplit/releases) (see the README).

## Troubleshooting

- **`File google-services.json is missing`** — you skipped step 2.3.
- **Sign-in fails with Google** — ensure the SHA-1 of your signing key is added to the
  Firebase Android app settings.
- **Permission-denied reads/writes** — confirm the Firestore rules from
  `firestore.rules` are deployed and you are signed in.
