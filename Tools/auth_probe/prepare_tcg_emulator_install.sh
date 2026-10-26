#!/bin/bash
# Avoid emulator-only DEX verification costs on a TCG host before loading a large APK.
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
  echo "ADB emulator is not ready." >&2
  exit 1
}

adb_cmd root >/dev/null
adb_cmd wait-for-device

for _ in $(seq 1 120); do
  package_service="$(adb_cmd shell service check package 2>/dev/null | tr -d '\r' || true)"
  activity_service="$(adb_cmd shell service check activity 2>/dev/null | tr -d '\r' || true)"
  volumes="$(adb_cmd shell sm list-volumes all 2>/dev/null | tr -d '\r' || true)"
  bootanim="$(adb_cmd shell getprop init.svc.bootanim 2>/dev/null | tr -d '\r' || true)"
  if [[ "$package_service" == "Service package: found" &&
        "$activity_service" == "Service activity: found" &&
        "$volumes" == *"private mounted"* &&
        "$bootanim" == "stopped" ]]; then
    # Android 11 ART defines extract as a valid no-AOT, no-verification compiler filter.
    adb_cmd shell setprop pm.dexopt.install extract
    echo "install_compiler_filter=$(adb_cmd shell getprop pm.dexopt.install | tr -d '\r')"
    echo "package_manager=ready"
    exit 0
  fi
  sleep 5
done

echo "Android framework or private storage did not become ready within 10 minutes." >&2
exit 1
