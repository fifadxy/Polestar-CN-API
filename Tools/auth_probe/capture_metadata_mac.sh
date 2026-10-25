#!/bin/bash
# Only CN_AUTH_META lines emitted by the metadata-only instrumentation are saved.
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
command -v frida >/dev/null || { echo "Install frida-tools with pipx"; exit 1; }
command -v adb >/dev/null || { echo "Install Android platform-tools"; exit 1; }
adb get-state | grep -qx device || { echo "ADB device unavailable"; exit 1; }
OUT="china_auth_metadata_$(date +%Y%m%d-%H%M%S).txt"
echo "Open the official com.polestar.china.android app before running this probe."
echo "Perform one normal login if needed, open vehicle list and refresh SOC; Ctrl+C to stop."
echo "Saving only sanitized field NAMES, host, endpoint templates and status codes to $OUT"
frida -U -n com.polestar.china.android -l "$DIR/okhttp_metadata_only.js" 2>&1 |
  awk '/CN_AUTH_META / { sub(/^.*CN_AUTH_META /, "CN_AUTH_META "); print; fflush(); }' |
  tee "$OUT"
