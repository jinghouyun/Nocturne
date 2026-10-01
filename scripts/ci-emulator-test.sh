#!/usr/bin/env bash
#
# Runs the instrumented test on the already-booted emulator and always
# collects screenshots + logcat, propagating the real instrumentation exit
# code. Kept as a file (rather than inline `script:` lines) because the
# emulator-runner action executes each script line in a separate `sh -c`,
# which would drop shell variables and the failure flag between lines.
set -u

PKG=com.nocturne.player.debug
mkdir -p artifacts/screenshots

set +e
./gradlew connectedCoreDebugAndroidTest --no-daemon
TEST_RC=$?
set -e

adb root || true
adb wait-for-device || true
sleep 2
adb pull "/sdcard/Android/data/${PKG}/files/test_screenshots" artifacts/screenshots || true
# Fallback: screencap is a basic system tool (works on ATD where the in-test
# UiAutomation screenshot may not), so always capture the final screen.
adb exec-out screencap -p > artifacts/screenshots/final_screencap.png 2>/dev/null || true
adb logcat -d > artifacts/logcat.txt || true

echo "connectedCoreDebugAndroidTest exit code: ${TEST_RC}"
exit "${TEST_RC}"
