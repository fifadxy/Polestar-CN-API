#!/bin/bash
# Download official Frida server matching the installed CLI for your own rooted phone.
set -euo pipefail
command -v adb >/dev/null || { echo "Install ADB: brew install android-platform-tools"; exit 1; }
command -v frida >/dev/null || { echo "Install CLI: brew install pipx; pipx install frida-tools"; exit 1; }
adb get-state | grep -qx device || { echo "Connect phone by USB and authorize debugging."; exit 1; }
adb shell su -c id | grep -q 'uid=0' || { echo "Root su permission unavailable."; exit 1; }
ver="$(frida --version | tr -d '\r')"
abi="$(adb shell getprop ro.product.cpu.abi | tr -d '\r')"
case "$abi" in
  arm64-v8a) arch=arm64 ;;
  armeabi-v7a|armeabi) arch=arm ;;
  x86_64) arch=x86_64 ;;
  x86) arch=x86 ;;
  *) echo "Unsupported ABI: $abi"; exit 1 ;;
esac
work="$(mktemp -d /tmp/hisingen-frida.XXXXXX)"
trap 'rm -rf "$work"' EXIT
name="frida-server-$ver-android-$arch"
url="https://github.com/frida/frida/releases/download/$ver/$name.xz"
echo "Downloading official Frida release: $name"
curl -fL --retry 3 --progress-bar -o "$work/$name.xz" "$url"
xz -d "$work/$name.xz"
adb push "$work/$name" /data/local/tmp/hisingen-frida-server
adb shell su -c 'chmod 700 /data/local/tmp/hisingen-frida-server'
adb shell su -c '/data/local/tmp/hisingen-frida-server -D'
echo "Verify USB Frida:"
frida-ps -U | head -n 12
