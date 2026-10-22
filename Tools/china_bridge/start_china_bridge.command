#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BRIDGE_SCRIPT="$SCRIPT_DIR/polestar_china_bridge.py"
DOCTOR="$SCRIPT_DIR/china_bridge_doctor.command"
PACKAGE="com.polestar.china.android"
PORT=28765
URL="http://127.0.0.1:${PORT}"

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
  echo "Expected environment: ~/Desktop/PolestarAnalysis/frida-venv"
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

if ! adb shell pm path "$PACKAGE" >/dev/null 2>&1; then
  echo "ERROR: official Polestar China app is not installed: $PACKAGE"
  exit 1
fi

PID="$(adb shell pidof -s "$PACKAGE" 2>/dev/null | tr -d '\r' || true)"
if [ -z "$PID" ]; then
  echo "Official Polestar China app is not running; launching it..."
  adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
  for _ in 1 2 3 4 5 6 7 8 9 10; do
    PID="$(adb shell pidof -s "$PACKAGE" 2>/dev/null | tr -d '\r' || true)"
    [ -n "$PID" ] && break
    sleep 1
  done
fi

if [ -z "$PID" ]; then
  echo "ERROR: could not start official Polestar China app."
  exit 1
fi

if curl -fsS --max-time 1 "$URL/v1/status" >/dev/null 2>&1; then
  echo "China bridge is already running at $URL"
  [ -x "$DOCTOR" ] && "$DOCTOR" || true
  exit 0
fi

echo "Using Python: $PY"
echo "Android app PID: $PID"
echo "Starting Hisingen China read-only bridge..."
echo "This terminal window must remain open while using China vehicle data."
echo

"$PY" "$BRIDGE_SCRIPT" &
BRIDGE_PID=$!
trap 'kill "$BRIDGE_PID" 2>/dev/null || true' EXIT INT TERM

for _ in 1 2 3 4 5 6 7 8 9 10; do
  if curl -fsS --max-time 1 "$URL/v1/status" >/dev/null 2>&1; then
    echo
    echo "Bridge is reachable: $URL"
    break
  fi
  sleep 1
done

wait "$BRIDGE_PID"
