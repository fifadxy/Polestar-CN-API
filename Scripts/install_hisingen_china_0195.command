#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_ZIP="$SCRIPT_DIR/Hisingen-China-0.19.5-Native-Mac.app.zip"
TARGET="/Applications/Hisingen China.app"
WORKDIR="$(mktemp -d /tmp/hisingen-cn0195.XXXXXX)"
trap 'rm -rf "$WORKDIR"' EXIT

test -f "$APP_ZIP" || {
  echo "ERROR: Hisingen-China-0.19.5-Native-Mac.app.zip not found."
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
test "$PATCH_VERSION" = "0.19.5" || {
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
if /usr/bin/security find-generic-password \
  -s "io.kheirallah.hisingen.polestar-cn.oauth" \
  -a "local-refresh-client-secret-v1" >/dev/null 2>&1; then
  echo "China OAuth refresh material: configured in Keychain."
else
  echo "China OAuth refresh material: NOT configured."
  echo "Initial SMS login can proceed. To support automatic local-token refresh, run:"
  echo "  python3 \"$SCRIPT_DIR/provision_cn_oauth_secret.py\" /path/to/Polestar-China-Dex.zip"
fi
echo
echo "Then sign in from Settings -> Accounts with your mainland China phone number and SMS code."
echo "The DEX is used only for optional one-time OAuth refresh provisioning; no Android bridge or companion process is required at runtime."
