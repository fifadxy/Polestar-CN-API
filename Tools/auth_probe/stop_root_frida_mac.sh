#!/bin/bash
# Stop only the Frida server binary provisioned by setup_root_frida_mac.sh.
set -euo pipefail

command -v adb >/dev/null || {
  echo "ADB is unavailable." >&2
  exit 1
}
adb get-state 2>/dev/null | grep -qx device || {
  echo "ADB device unavailable." >&2
  exit 1
}
adb shell su -c id 2>/dev/null | grep -q 'uid=0' || {
  echo "Root shell unavailable." >&2
  exit 1
}

# Inspect only executable symlinks and stop an exact path match. This avoids
# reading other process command lines or terminating another Frida installation.
adb shell su -c '
  for proc in /proc/[0-9]*; do
    if [ "$(readlink "$proc/exe" 2>/dev/null)" = "/data/local/tmp/hisingen-frida-server" ]; then
      kill "${proc##*/}"
    fi
  done
'
echo "Hisingen Frida server stop requested."
