#!/bin/bash
set -euo pipefail

BRIDGE_SCRIPT="$(cd "$(dirname "$0")" && pwd)/polestar_china_bridge.py"
CANDIDATES=(
  "$HOME/Desktop/PolestarAnalysis/frida-venv/bin/python"
  "$HOME/Desktop/PolestarAnalysis/frida-venv/bin/python3"
  "$(command -v python3 || true)"
)

PY=""
for candidate in "${CANDIDATES[@]}"; do
  if [ -n "$candidate" ] && [ -x "$candidate" ]; then
    if "$candidate" -c 'import frida' >/dev/null 2>&1; then
      PY="$candidate"
      break
    fi
  fi
done

if [ -z "$PY" ]; then
  echo "ERROR: Python with the frida module was not found."
  echo "Expected existing environment: ~/Desktop/PolestarAnalysis/frida-venv"
  exit 1
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: adb is not available in PATH."
  exit 1
fi

if ! adb get-state >/dev/null 2>&1; then
  echo "ERROR: no Android device is connected through adb."
  exit 1
fi

echo "Using: $PY"
echo "Starting Hisingen China read-only bridge..."
exec "$PY" "$BRIDGE_SCRIPT"
