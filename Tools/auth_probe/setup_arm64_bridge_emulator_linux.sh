#!/bin/bash
# Create an API 30 Google APIs x86_64 AVD with Android Native Bridge support.
set -euo pipefail

sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$sdk_root" ]]; then
  echo "Set ANDROID_SDK_ROOT to an Android SDK containing cmdline-tools and emulator." >&2
  exit 1
fi

sdkmanager="$sdk_root/cmdline-tools/latest/bin/sdkmanager"
avdmanager="$sdk_root/cmdline-tools/latest/bin/avdmanager"
emulator="$sdk_root/emulator/emulator"
for tool in "$sdkmanager" "$avdmanager" "$emulator"; do
  [[ -x "$tool" ]] || { echo "Missing Android SDK tool: $tool" >&2; exit 1; }
done

api="${ANDROID_BRIDGE_API:-30}"
avd_name="${ANDROID_BRIDGE_AVD:-polestar_armbridge_api30}"
avd_home="${ANDROID_AVD_HOME:-${HOME}/.android/avd}"
image="system-images;android-${api};google_apis;x86_64"

mkdir -p "$avd_home"
export ANDROID_AVD_HOME="$avd_home"

"$sdkmanager" "platforms;android-${api}" "$image"

if "$emulator" -list-avds | grep -Fxq "$avd_name"; then
  echo "Reusing existing AVD: $avd_name"
else
  printf 'no\n' | "$avdmanager" create avd \
    --name "$avd_name" \
    --package "$image" \
    --device "pixel_3a"
fi

config="$avd_home/$avd_name.avd/config.ini"
[[ -f "$config" ]] || { echo "AVD config not found: $config" >&2; exit 1; }

set_config() {
  local key="$1"
  local value="$2"
  if grep -q "^${key}=" "$config"; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$config"
  else
    printf '%s=%s\n' "$key" "$value" >> "$config"
  fi
}

set_config "disk.dataPartition.size" "6G"
set_config "hw.cpu.ncore" "4"
set_config "hw.lcd.density" "320"
set_config "hw.lcd.height" "1280"
set_config "hw.lcd.width" "720"
set_config "hw.ramSize" "3072"

echo "avd=$avd_name"
echo "avd_home=$avd_home"
echo "system_image=$image"
echo "setup=ready"
echo "Run with: Tools/auth_probe/run_arm64_bridge_emulator_linux.sh"
