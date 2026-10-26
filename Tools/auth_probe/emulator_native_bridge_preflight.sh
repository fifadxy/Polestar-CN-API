#!/bin/bash
# Verify root ADB plus ARM64 Native Bridge without reading identifiers or private app data.
set -euo pipefail

command -v adb >/dev/null || { echo "Install Android platform-tools first." >&2; exit 1; }

adb_args=()
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  adb_args=(-s "$ANDROID_SERIAL")
fi
adb_cmd() {
  adb "${adb_args[@]}" "$@"
}

[[ "$(adb_cmd get-state 2>/dev/null || true)" == "device" ]] || {
  echo "ADB emulator is not ready. A non-KVM cold boot can take 8-12 minutes." >&2
  exit 1
}

adb_cmd root >/dev/null
adb_cmd wait-for-device

root_id="$(adb_cmd shell id | tr -d '\r')"
abilist="$(adb_cmd shell getprop ro.product.cpu.abilist | tr -d '\r')"
native_bridge="$(adb_cmd shell getprop ro.dalvik.vm.native.bridge | tr -d '\r')"
bridge_enabled="$(adb_cmd shell getprop ro.enable.native.bridge.exec | tr -d '\r')"
android_release="$(adb_cmd shell getprop ro.build.version.release | tr -d '\r')"

[[ "$root_id" == *"uid=0"* ]] || { echo "ADB root is unavailable: $root_id" >&2; exit 1; }
[[ ",$abilist," == *,arm64-v8a,* ]] || {
  echo "ARM64 is absent from ro.product.cpu.abilist: $abilist" >&2
  exit 1
}
[[ -n "$native_bridge" && "$native_bridge" != "0" ]] || {
  echo "Android Native Bridge is disabled." >&2
  exit 1
}
[[ "$bridge_enabled" == "1" ]] || {
  echo "Native Bridge execution is not enabled: $bridge_enabled" >&2
  exit 1
}
adb_cmd shell test -f "/system/lib64/$native_bridge" || {
  echo "Native Bridge library is missing: /system/lib64/$native_bridge" >&2
  exit 1
}

echo "device=emulator"
echo "root=adb-root"
echo "android_release=$android_release"
echo "cpu_abilist=$abilist"
echo "native_bridge=$native_bridge"
echo "runtime_mode=x86_64-host-with-arm64-translation"

polestar_package="com.polestar.china.android"
if adb_cmd shell pm path "$polestar_package" 2>/dev/null | tr -d '\r' | grep -q '^package:'; then
  version="$(adb_cmd shell dumpsys package "$polestar_package" 2>/dev/null | tr -d '\r' | sed -n 's/^[[:space:]]*versionName=//p' | sed -n '1p')"
  primary_abi="$(adb_cmd shell dumpsys package "$polestar_package" 2>/dev/null | tr -d '\r' | sed -n 's/^[[:space:]]*primaryCpuAbi=//p' | sed -n '1p')"
  echo "polestar_version=${version:-unknown}"
  echo "polestar_primary_abi=${primary_abi:-unknown}"
else
  echo "polestar=not-installed"
fi

echo "preflight=ready"
