#!/usr/bin/env bash
#
# print-debug-sha1.sh — print the SHA-1 fingerprint of your debug signing key.
#
# Google Sign-In (Credential Manager) only returns a token when the app's
# signing certificate is registered in the Firebase project. For debug builds
# that's the standard ~/.android/debug.keystore. Paste the SHA1 value printed
# below into:
#
#   Firebase console -> Project settings -> Your apps (Android) -> Add fingerprint
#
# ...then RE-DOWNLOAD google-services.json into app/ and rebuild.
#
set -euo pipefail

KEYSTORE="${HOME}/.android/debug.keystore"

command -v keytool >/dev/null 2>&1 || { echo "keytool not found — install a JDK." >&2; exit 1; }

if [[ ! -f "${KEYSTORE}" ]]; then
  echo "No debug keystore at ${KEYSTORE}."
  echo "It's created automatically the first time you build/run an Android app."
  echo "Run a build once (e.g. ./scripts/run-emulator.sh) and try again."
  exit 1
fi

echo "Debug keystore: ${KEYSTORE}"
echo
keytool -list -v \
  -keystore "${KEYSTORE}" \
  -alias androiddebugkey \
  -storepass android \
  -keypass android \
  | grep -E "SHA1:|SHA256:"

echo
echo "Add the SHA1 above to Firebase -> Project settings -> Android app -> Add fingerprint,"
echo "then re-download google-services.json into app/ and re-run ./scripts/run-emulator.sh"
