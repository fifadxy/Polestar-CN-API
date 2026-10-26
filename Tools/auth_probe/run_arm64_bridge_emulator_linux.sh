#!/bin/bash
# Start the reproducible Native Bridge AVD. It is x86_64 Android, not an ARM64 CPU emulator.
set -euo pipefail

sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
[[ -n "$sdk_root" ]] || { echo "Set ANDROID_SDK_ROOT first." >&2; exit 1; }
emulator="$sdk_root/emulator/emulator"
[[ -x "$emulator" ]] || { echo "Android Emulator not found: $emulator" >&2; exit 1; }

avd_name="${ANDROID_BRIDGE_AVD:-polestar_armbridge_api30}"
memory="${ANDROID_BRIDGE_MEMORY_MB:-3072}"
cores="${ANDROID_BRIDGE_CORES:-4}"
port="${ANDROID_BRIDGE_PORT:-5558}"
accel="${ANDROID_BRIDGE_ACCEL:-off}"

args=(
  -avd "$avd_name"
  -no-window
  -no-audio
  -no-boot-anim
  -no-metrics
  -gpu swiftshader_indirect
  -accel "$accel"
  -memory "$memory"
  -cores "$cores"
  -port "$port"
)

if [[ "${ANDROID_BRIDGE_WRITABLE_SYSTEM:-0}" == "1" ]]; then
  args+=(-writable-system)
fi
if [[ "${ANDROID_BRIDGE_SNAPSHOT:-0}" != "1" ]]; then
  args+=(-no-snapshot)
fi

echo "Starting $avd_name on emulator-$port (accel=$accel)."
if [[ "$accel" == "off" ]]; then
  echo "KVM is disabled; a cold boot can take 8-12 minutes."
fi
exec "$emulator" "${args[@]}"
