#!/bin/sh
set -eu
for cmd in adb emulator; do
 if command -v "$cmd" >/dev/null 2>&1; then echo "AVAILABLE $cmd: $(command -v "$cmd")"
 else echo "MISSING $cmd"; fi
done
if command -v adb >/dev/null 2>&1; then
 adb devices -l
 COUNT=$(adb devices | awk 'NR>1 && $2=="device" {n++} END {print n+0}')
 if [ "$COUNT" -gt 0 ]; then
  ABI=$(adb shell getprop ro.product.cpu.abi | tr -d '\r')
  echo "ABI=$ABI"
  echo "BUILD_TYPE=$(adb shell getprop ro.build.type | tr -d '\r')"
  echo "SHELL_UID=$(adb shell id -u | tr -d '\r')"
  [ "$ABI" = "arm64-v8a" ] || echo "WARNING: APK native libraries are arm64-v8a only"
  adb shell pm list packages com.polestar.china.android
 fi
fi
