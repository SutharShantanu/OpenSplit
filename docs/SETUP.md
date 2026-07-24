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

## 6. Recurring expenses (Firestore index)

Recurring expenses are materialized by a daily WorkManager job that queries each of
your groups for due templates (`recurrence.nextOccurrence <= now`). Firestore
auto-creates the required single-field index on first run; if prompted in the console
or logs, follow the offered link to create it.

## 5. Build & run

```bash
./gradlew :app:assembleDebug        # build the debug APK
./gradlew :app:testDebugUnitTest    # run unit tests (split math, balances, currency)
./gradlew :app:lintDebug            # lint
```

Then install on a device/emulator, or open the project in Android Studio and Run.

## Troubleshooting

- **`File google-services.json is missing`** — you skipped step 2.3.
- **Sign-in fails with Google** — ensure the SHA-1 of your signing key is added to the
  Firebase Android app settings.
- **Permission-denied reads/writes** — confirm the Firestore rules from
  `firestore.rules` are deployed and you are signed in.
