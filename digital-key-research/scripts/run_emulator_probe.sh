#!/bin/sh
set -eu

OUT=digital-key-research/runtime/emulator.txt
mkdir -p digital-key-research/runtime
: > "$OUT"

log() {
  printf '%s\n' "$*" | tee -a "$OUT"
}

run() {
  "$@" 2>&1 | tee -a "$OUT"
}

adb wait-for-device
log "=== BEFORE ADB ROOT ==="
run adb shell id
run adb shell getprop ro.build.fingerprint
run adb shell getprop ro.build.type
run adb shell getprop ro.product.cpu.abilist
run adb shell getprop ro.product.cpu.abilist64
run adb shell getprop ro.boot.verifiedbootstate

log "=== FEATURES ==="
adb shell pm list features 2>/dev/null | grep -Ei 'bluetooth|location|keystore|hardware' | sort | tee -a "$OUT" || true

log "=== ADB ROOT REQUEST ==="
adb root 2>&1 | tee -a "$OUT" || true
sleep 2
adb wait-for-device

log "=== AFTER ADB ROOT ==="
run adb shell id
run adb shell whoami
log "=== SELINUX ==="
run adb shell getenforce
log "=== BLUETOOTH SERVICE ==="
adb shell dumpsys bluetooth_manager 2>/dev/null | sed -n '1,120p' | tee -a "$OUT" || true
log "=== KEYSTORE SERVICE ==="
adb shell service list 2>/dev/null | grep -Ei 'keystore|keymint|strongbox' | tee -a "$OUT" || true
log "=== PACKAGE ABI SUPPORT ==="
run adb shell getprop ro.product.cpu.abilist
run adb shell getprop ro.dalvik.vm.native.bridge

log "=== REFERENCE APK INSTALL ==="
APK="/tmp/polestar-china-4.25.0.apk"
if [ -f "$APK" ]; then
  run md5sum "$APK"
  run sha256sum "$APK"
  run adb install -r -g "$APK"
  log "=== PACKAGE INFO ==="
  run adb shell pm path com.polestar.china.android
  adb shell dumpsys package com.polestar.china.android 2>/dev/null | grep -E 'versionName=|versionCode=|primaryCpuAbi=|secondaryCpuAbi=|nativeLibraryDir=|BLUETOOTH|ACCESS_BACKGROUND_LOCATION|MSDKService|PhoneAsKeyBootCompleteReceiver|PakCarScanningActivity|PakSettingsActivity' | sed -n '1,220p' | tee -a "$OUT" || true
  log "=== APP LAUNCH ==="
  adb logcat -c || true
  RESOLVED=$(adb shell cmd package resolve-activity --brief com.polestar.china.android 2>/dev/null | tr -d '\r' | tail -n 1)
  log "resolved_activity=$RESOLVED"
  adb shell monkey -p com.polestar.china.android -c android.intent.category.LAUNCHER 1 2>&1 | tee -a "$OUT" || true
  sleep 8
  log "pid=$(adb shell pidof com.polestar.china.android 2>/dev/null | tr -d '\r')"
  adb shell dumpsys activity activities 2>/dev/null | grep -E 'mResumedActivity|topResumedActivity|com[.]polestar[.]china[.]android' | head -n 80 | tee -a "$OUT" || true
  log "=== SAFE RUNTIME ERRORS ==="
  adb logcat -d 2>/dev/null | grep -E 'com[.]polestar[.]china[.]android|PhoneAsKey|PAK|p319|UnsatisfiedLinkError|ClassNotFoundException|FATAL EXCEPTION' | grep -v -Ei 'token|authorization|cookie|vin|phone|password' | tail -n 180 | tee -a "$OUT" || true
else
  log "reference_apk=unavailable"
fi

log "=== PROBE COMPLETE ==="
