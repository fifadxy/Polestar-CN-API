#!/bin/bash
# Only public environment info: no logcat, VIN, phone number, serial or credentials.
set -euo pipefail
command -v adb >/dev/null || { echo "Install: brew install android-platform-tools"; exit 1; }
adb get-state | grep -qx device || { echo "USB debugging not authorized"; exit 1; }
output="android_auth_environment.txt"
{
  echo "package=com.polestar.china.android"
  echo "android_release=$(adb shell getprop ro.build.version.release | tr -d '\r')"
  echo "android_sdk=$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
  echo "cpu_abi=$(adb shell getprop ro.product.cpu.abi | tr -d '\r')"
  echo "root=$(adb shell su -c id 2>/dev/null | grep -o 'uid=0' | head -1 || true)"
  adb shell dumpsys package com.polestar.china.android 2>/dev/null |
    grep -m 1 'versionName=' | sed 's/^ *//'
} > "$output"
echo "Saved non-secret environment: $output"
