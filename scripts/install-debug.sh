#!/usr/bin/env bash
# Install the debug APK to a connected Android device via adb.
#
# Prerequisites:
#   - adb in PATH
#   - Device connected and USB debugging enabled

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "ERROR: Debug APK not found at $APK_PATH"
    echo "Run './gradlew assembleDebug' first."
    exit 1
fi

echo "Installing debug APK..."
adb install -r "$APK_PATH"
echo "Done. Launch with:"
echo "  adb shell am start -n com.plausiden.app/.MainActivity"
