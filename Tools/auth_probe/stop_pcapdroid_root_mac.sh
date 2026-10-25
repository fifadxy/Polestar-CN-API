#!/bin/bash
# Stop a PCAPdroid capture started by this kit.
set -euo pipefail

command -v adb >/dev/null || {
  echo "ADB is unavailable." >&2
  exit 1
}
adb get-state 2>/dev/null | grep -qx device || {
  echo "ADB device unavailable." >&2
  exit 1
}

adb shell am start \
  -e action stop \
  -n com.emanuelef.remote_capture/.activities.CaptureCtrl >/dev/null
echo "PCAPdroid capture stop requested."
