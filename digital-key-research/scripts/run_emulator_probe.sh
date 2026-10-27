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
log "=== PROBE COMPLETE ==="
