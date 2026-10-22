#!/bin/bash
set -u
PACKAGE="com.polestar.china.android"
PORT=28765
BRIDGE_URL="http://127.0.0.1:${PORT}"
PASS=0; WARN=0; FAIL=0
ok(){ echo "[OK] $*"; PASS=$((PASS+1)); }
warn(){ echo "[WARN] $*"; WARN=$((WARN+1)); }
fail(){ echo "[FAIL] $*"; FAIL=$((FAIL+1)); }

echo "Hisingen China Bridge Doctor"
echo "============================"

if command -v adb >/dev/null 2>&1; then ok "adb: $(command -v adb)"; else fail "adb not found in PATH"; fi

PY=""
for p in "$HOME/Desktop/PolestarAnalysis/frida-venv/bin/python" "$HOME/Desktop/PolestarAnalysis/frida-venv/bin/python3" "$(command -v python3 2>/dev/null || true)"; do
  if [ -n "$p" ] && [ -x "$p" ] && "$p" -c 'import frida' >/dev/null 2>&1; then PY="$p"; break; fi
done
if [ -n "$PY" ]; then
  FRIDA_VER="$("$PY" -c 'import frida; print(frida.__version__)' 2>/dev/null || true)"
  ok "Python + frida: $PY ${FRIDA_VER:+(frida $FRIDA_VER)}"
else
  fail "No Python environment with frida module found"
fi

if command -v adb >/dev/null 2>&1 && adb get-state >/dev/null 2>&1; then
  ok "Android device connected: $(adb get-serialno 2>/dev/null || true)"
  if adb shell pm path "$PACKAGE" >/dev/null 2>&1; then ok "Official Polestar China app installed"; else fail "Official Polestar China app not installed"; fi
  PID="$(adb shell pidof -s "$PACKAGE" 2>/dev/null | tr -d '\r' || true)"
  if [ -n "$PID" ]; then ok "Official Polestar China app running (pid $PID)"; else warn "Official Polestar China app is not running"; fi
else
  fail "No adb-connected Android device"
fi

if curl -fsS --max-time 2 "$BRIDGE_URL/v1/status" >/tmp/hisingen-cn-bridge-status.json 2>/dev/null; then
  ok "Local bridge reachable at $BRIDGE_URL"
  python3 - <<'PY'
import json
from pathlib import Path
try:
    d=json.loads(Path("/tmp/hisingen-cn-bridge-status.json").read_text())
    print("      ready:", d.get("ready"))
    print("      battery:", d.get("batteryAvailable"))
    print("      exterior:", d.get("exteriorAvailable"))
    print("      odometer:", d.get("odometerAvailable"))
except Exception:
    pass
PY
else
  warn "Local bridge is not currently running"
fi

echo
echo "Summary: $PASS OK, $WARN warning(s), $FAIL failure(s)"
[ "$FAIL" -eq 0 ]
