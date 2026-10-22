#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_ZIP="$SCRIPT_DIR/Hisingen-China-0.18.1.app.zip"
BRIDGE_PY="$SCRIPT_DIR/polestar_china_bridge.py"
BRIDGE_START="$SCRIPT_DIR/start_china_bridge.command"\nBRIDGE_DOCTOR="$SCRIPT_DIR/china_bridge_doctor.command"\nBRIDGE_README="$SCRIPT_DIR/README-China-Bridge.md"
TARGET="/Applications/Hisingen China.app"
BRIDGE_DIR="$HOME/Library/Application Support/Hisingen China/Bridge"
WORKDIR="$(mktemp -d /tmp/hisingen-cn018.XXXXXX)"
trap 'rm -rf "$WORKDIR"' EXIT

test -f "$APP_ZIP" || { echo "ERROR: Hisingen-China-0.18.1.app.zip not found."; exit 1; }

if [ -f "$SCRIPT_DIR/SHA256SUMS.txt" ]; then
  (
    cd "$SCRIPT_DIR"
    shasum -a 256 -c SHA256SUMS.txt
  )
fi

/usr/bin/ditto -x -k "$APP_ZIP" "$WORKDIR"
APP="$WORKDIR/Hisingen China.app"
test -d "$APP" || { echo "ERROR: Hisingen China.app missing."; exit 1; }

/usr/bin/codesign --verify --deep --strict --verbose=2 "$APP"
PATCH_VERSION="$(/usr/libexec/PlistBuddy -c 'Print :HisingenChinaPatchVersion' "$APP/Contents/Info.plist")"
test "$PATCH_VERSION" = "0.18.1" || { echo "ERROR: unexpected China patch $PATCH_VERSION"; exit 1; }

pkill -x Hisingen 2>/dev/null || true

if [ -d "$TARGET" ]; then
  BACKUP="$HOME/Desktop/Hisingen China.backup.$(date +%Y%m%d-%H%M%S).app"
  /usr/bin/ditto "$TARGET" "$BACKUP"
  echo "Previous China build backed up to: $BACKUP"
fi

sudo rm -rf "$TARGET"
sudo /usr/bin/ditto "$APP" "$TARGET"
sudo /usr/bin/xattr -dr com.apple.quarantine "$TARGET" 2>/dev/null || true
/usr/bin/codesign --verify --deep --strict --verbose=2 "$TARGET"

mkdir -p "$BRIDGE_DIR"
if [ -f "$BRIDGE_PY" ]; then
  cp "$BRIDGE_PY" "$BRIDGE_DIR/polestar_china_bridge.py"
fi
if [ -f "$BRIDGE_START" ]; then
  cp "$BRIDGE_START" "$BRIDGE_DIR/start_china_bridge.command"
  chmod +x "$BRIDGE_DIR/start_china_bridge.command"
fi

open "$TARGET"

echo
echo "Installed: $TARGET"
echo "China Patch: $PATCH_VERSION"
echo
echo "On the first real launch, Hisingen should show:"
echo "  Select Polestar Region"
echo "  China / Global"
echo
echo "Runtime region switch:"
echo "  Hisingen menu -> Polestar Region -> China / Global"
echo
if [ -x "$BRIDGE_DIR/start_china_bridge.command" ]; then
  echo "China read-only vehicle data bridge:"
  echo "  \"$BRIDGE_DIR/start_china_bridge.command\""
fi
