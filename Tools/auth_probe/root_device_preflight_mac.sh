#!/bin/bash
# Check only non-secret compatibility facts for the owner's rooted Android phone.
set -euo pipefail

POLESTAR_PACKAGE="com.polestar.china.android"
PCAPDROID_PACKAGE="com.emanuelef.remote_capture"

command -v adb >/dev/null || {
  echo "Install ADB: brew install android-platform-tools" >&2
  exit 1
}

state="$(adb get-state 2>/dev/null || true)"
if [[ "$state" != "device" ]]; then
  echo "Connect one authorized Android device. Set ANDROID_SERIAL when multiple devices are attached." >&2
  exit 1
fi

root_id="$(adb shell su -c id 2>/dev/null | tr -d '\r' || true)"
if [[ "$root_id" != *"uid=0"* ]]; then
  echo "Root shell unavailable. Approve the su request and retry." >&2
  exit 1
fi

package_installed() {
  adb shell pm path "$1" 2>/dev/null | tr -d '\r' | grep -q '^package:'
}

package_version() {
  adb shell dumpsys package "$1" 2>/dev/null |
    tr -d '\r' |
    sed -n 's/^[[:space:]]*versionName=//p' |
    sed -n '1p'
}

if ! package_installed "$POLESTAR_PACKAGE"; then
  echo "Official Polestar China app is not installed: $POLESTAR_PACKAGE" >&2
  exit 1
fi
if ! package_installed "$PCAPDROID_PACKAGE"; then
  echo "Install the official PCAPdroid app first: $PCAPDROID_PACKAGE" >&2
  exit 1
fi

abi="$(adb shell getprop ro.product.cpu.abi | tr -d '\r')"
if [[ "$abi" != "arm64-v8a" ]]; then
  echo "Unsupported validation target ABI: $abi (the official app requires a real ARM64 device)." >&2
  exit 1
fi

android_release="$(adb shell getprop ro.build.version.release | tr -d '\r')"
polestar_version="$(package_version "$POLESTAR_PACKAGE")"
pcapdroid_version="$(package_version "$PCAPDROID_PACKAGE")"

echo "device=authorized"
echo "root=uid=0"
echo "cpu_abi=$abi"
echo "android_release=${android_release:-unknown}"
echo "polestar_version=${polestar_version:-unknown}"
echo "pcapdroid_version=${pcapdroid_version:-unknown}"

if command -v frida >/dev/null; then
  echo "frida_cli=$(frida --version | tr -d '\r')"
else
  echo "frida_cli=missing (install with pipx install frida-tools)"
fi

if command -v scrcpy >/dev/null; then
  echo "scrcpy=available"
else
  echo "scrcpy=optional-missing (install with brew install scrcpy)"
fi

echo "preflight=ready"
