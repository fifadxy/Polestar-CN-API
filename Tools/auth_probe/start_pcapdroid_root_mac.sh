#!/bin/bash
# Start PCAPdroid in root, per-app, no-dump mode. User consent remains mandatory.
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
"$DIR/root_device_preflight_mac.sh"

adb shell am start \
  -e action start \
  --ez root_capture true \
  -e capture_interface @inet \
  -e pcap_dump_mode none \
  -e app_filter com.polestar.china.android \
  --ez tls_decryption false \
  --ez full_payload false \
  -n com.emanuelef.remote_capture/.activities.CaptureCtrl

echo "Approve the PCAPdroid capture request on the phone, then return to the official Polestar app."
echo "No PCAP file, decrypted TLS payload, or full packet payload is requested."
