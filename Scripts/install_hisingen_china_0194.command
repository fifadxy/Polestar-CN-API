#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_ZIP="$SCRIPT_DIR/Hisingen-China-0.19.4-Native-Mac.app.zip"
TARGET="/Applications/Hisingen China.app"
WORKDIR="$(mktemp -d /tmp/hisingen-cn0194.XXXXXX)"
trap 'rm -rf "$WORKDIR"' EXIT

test -f "$APP_ZIP" || {
  echo "ERROR: Hisingen-China-0.19.4-Native-Mac.app.zip not found."
  exit 1
}

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
test "$PATCH_VERSION" = "0.19.4" || {
  echo "ERROR: unexpected China patch $PATCH_VERSION"
  exit 1
}

pkill -x Hisingen 2>/dev/null || true

if [ -d "$TARGET" ]; then
  BACKUP="$HOME/Desktop/Hisingen China.backup.$(date +%Y%m%d-%H%M%S).app"
  /usr/bin/ditto "$TARGET" "$BACKUP"
  echo "Previous China build backed up to: $BACKUP"
fi

sudo /bin/rm -rf -- "$TARGET"
sudo /usr/bin/ditto "$APP" "$TARGET"
sudo /usr/bin/xattr -dr com.apple.quarantine "$TARGET" 2>/dev/null || true
/usr/bin/codesign --verify --deep --strict --verbose=2 "$TARGET"

open "$TARGET"

echo
echo "Installed: $TARGET"
echo "China Patch: $PATCH_VERSION"
echo
echo "Sign in from Settings -> Accounts with your mainland China phone number and SMS code."
echo "No Android app, localhost service, or companion process is required."
