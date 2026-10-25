#!/bin/bash
# Coordinate PCAPdroid root flow metadata with the existing Frida metadata probe.
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
cleaned=0

cleanup() {
  if [[ "$cleaned" -eq 0 ]]; then
    cleaned=1
    "$DIR/stop_pcapdroid_root_mac.sh" >/dev/null 2>&1 || true
  fi
}

on_signal() {
  cleanup
  exit 130
}

trap cleanup EXIT
trap on_signal HUP INT TERM

if [[ ! -t 0 ]]; then
  echo "Run this helper in an interactive Terminal so capture consent can be confirmed on the phone." >&2
  exit 1
fi

"$DIR/start_pcapdroid_root_mac.sh"
echo
echo "On the phone: approve capture, reopen the official Polestar China app, and leave it on screen."
read -r -p "Press Return when the app is ready; Ctrl+C will stop PCAPdroid: " _

"$DIR/capture_metadata_mac.sh"
