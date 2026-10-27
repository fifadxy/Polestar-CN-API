#!/bin/sh
set -eu
OUT="digital-key-research/runtime/ui-smoke"
mkdir -p "$OUT"

adb wait-for-device
adb root || true
adb wait-for-device
adb shell id | tee "$OUT/adb-identity.txt"

APK="digital-key-research/runtime/research-apk/app-debug.apk"
test -s "$APK"
adb install -r "$APK" | tee "$OUT/install.txt"

PACKAGE="io.github.polestarcn.companion.digitalkeyv2"
COMPONENT="$PACKAGE/io.github.polestarcn.companion.digitalkey.DigitalKeyResearchActivity"

adb shell am start -n "$COMPONENT" | tee "$OUT/activity-start.txt"
sleep 5

adb shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity|DigitalKeyResearchActivity' | head -n 40 > "$OUT/activity-state.txt" || true
adb shell pidof "$PACKAGE" > "$OUT/process-id.txt" || true
test -s "$OUT/process-id.txt"

adb shell uiautomator dump /sdcard/digital-key-ui.xml >/dev/null
adb pull /sdcard/digital-key-ui.xml "$OUT/window.xml" >/dev/null
if ! grep -q 'RESEARCH ONLY' "$OUT/window.xml"; then
  echo "ERROR: diagnostic title was not visible on screen" >&2
  exit 1
fi
if ! grep -q 'Working digital key' "$OUT/window.xml"; then
  echo "WARNING: the lower diagnostic content is below the viewport"
fi

adb exec-out screencap -p > "$OUT/diagnostic.png"
test -s "$OUT/diagnostic.png"
echo "PASS: read-only digital-key research Activity started on rooted Android emulator"
