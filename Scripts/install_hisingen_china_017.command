#!/bin/bash
set -euo pipefail

ZIP_NAME="Hisingen-China-0.17.0.app.zip"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ZIP="$SCRIPT_DIR/$ZIP_NAME"
SUMS="$SCRIPT_DIR/SHA256SUMS.txt"
TARGET="/Applications/Hisingen China.app"
WORKDIR="$(mktemp -d /tmp/hisingen-cn017.XXXXXX)"
trap 'rm -rf "$WORKDIR"' EXIT

if [ ! -f "$ZIP" ]; then
  echo "ERROR: $ZIP_NAME not found next to this installer."
  exit 1
fi

if [ -f "$SUMS" ]; then
  EXPECTED="$(awk '/Hisingen-China-0.17.0.app.zip/{print $1; exit}' "$SUMS")"
  if [ -n "$EXPECTED" ]; then
    ACTUAL="$(shasum -a 256 "$ZIP" | awk '{print $1}')"
    echo "SHA256: $ACTUAL"
    if [ "$ACTUAL" != "$EXPECTED" ]; then
      echo "ERROR: SHA256 verification failed."
      exit 1
    fi
  fi
fi

/usr/bin/ditto -x -k "$ZIP" "$WORKDIR"
APP="$WORKDIR/Hisingen China.app"
test -d "$APP" || { echo "ERROR: app bundle missing."; exit 1; }

/usr/bin/codesign --verify --deep --strict --verbose=2 "$APP"

PATCH_VERSION="$(/usr/libexec/PlistBuddy -c 'Print :HisingenChinaPatchVersion' "$APP/Contents/Info.plist")"
test "$PATCH_VERSION" = "0.17.0" || { echo "ERROR: unexpected patch version $PATCH_VERSION"; exit 1; }

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
open "$TARGET"

echo
echo "Installed: $TARGET"
echo "China Patch: $PATCH_VERSION"
