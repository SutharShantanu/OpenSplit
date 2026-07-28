#!/usr/bin/env bash
#
# run-emulator.sh — one command to get the latest OpenSplit debug build onto a
# running Android emulator (or a connected device).
#
#   1. pulls the latest `main`
#   2. builds the debug APK and installs it (./gradlew :app:installDebug)
#   3. launches the app
#
# Run it on YOUR machine (Linux/macOS) where the Android SDK and the emulator
# live — it cannot run inside the cloud CI/agent sandbox.
#
# Usage:
#   ./scripts/run-emulator.sh            # pull + build + install + launch
#   ./scripts/run-emulator.sh --no-pull  # skip the git pull (use current tree)
#
set -euo pipefail

APP_ID="com.opensplit"

# --- locate the repo root (this script lives in <root>/scripts) ---------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT_DIR}"

info()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
warn()  { printf '\033[1;33m[!]\033[0m %s\n' "$*"; }
die()   { printf '\033[1;31m[x]\033[0m %s\n' "$*" >&2; exit 1; }

# --- preflight ----------------------------------------------------------------
command -v adb >/dev/null 2>&1 || die "adb not found on PATH. Install the Android SDK platform-tools and add them to PATH (or set ANDROID_HOME)."

if [[ ! -f "app/google-services.json" ]]; then
  die "app/google-services.json is missing. The build stops at the google-services plugin without it.
     Add your own from the Firebase console — see docs/SETUP.md steps 2 & 3."
fi

# --- 1. pull ------------------------------------------------------------------
if [[ "${1:-}" != "--no-pull" ]]; then
  info "Pulling latest main..."
  git pull --ff-only origin main
else
  info "Skipping git pull (--no-pull)."
fi

# --- 2. wait for a usable device ---------------------------------------------
info "Waiting for an emulator/device (start one if none is running)..."
adb start-server >/dev/null 2>&1 || true
adb wait-for-device

# Confirm at least one device is actually 'device' (not offline/unauthorized).
if ! adb devices | awk 'NR>1 && $2=="device"{found=1} END{exit !found}'; then
  adb devices
  die "No device in state 'device'. If it shows 'unauthorized', accept the debugging prompt on the device;
     if 'offline', restart the emulator."
fi

# --- 3. build + install -------------------------------------------------------
info "Building and installing the debug APK (./gradlew :app:installDebug)..."
./gradlew :app:installDebug --stacktrace

# --- 4. launch ----------------------------------------------------------------
info "Launching ${APP_ID}..."
adb shell monkey -p "${APP_ID}" -c android.intent.category.LAUNCHER 1 >/dev/null

info "Done. OpenSplit is installed and launched on the emulator."
echo
warn "If Google sign-in stops at the account chooser, this build's signing SHA-1"
warn "is probably not registered in Firebase. Run:  ./scripts/print-debug-sha1.sh"
