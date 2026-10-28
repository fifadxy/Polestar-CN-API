#!/bin/sh
set -eu
umask 077

# Build a complete standalone PsAssistant release from the exact user-provided
# official Polestar China 4.25.0 APK without requiring any APK at runtime.
#
# The official APK is used only on the build machine to derive the fixed
# non-user account client configuration. No password, token, VIN, paired key,
# private key, certificate cache or user data is copied.
#
# Usage:
#   PSA_SIGNING_KEYSTORE=/secure/app.keystore \
#   PSA_SIGNING_STORE_PASSWORD=... \
#   PSA_SIGNING_KEY_ALIAS=... \
#   PSA_SIGNING_KEY_PASSWORD=... \
#   sh scripts/build_android_standalone_official.sh /path/to/polestar-4.25.0.apk

if [ "$#" -ne 1 ]; then
  echo "usage: $0 POLESTAR_CHINA_4250_APK" >&2
  exit 2
fi

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APK=$1
EXPECTED_APK_SHA="99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef"
EXPECTED_CONFIG_SHA="59e1ca066e23a43e3976ded60c6691800dd19c4b173bd94b241bee42be5a7274"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
CONFIG="$TMP/client-config.txt"

hash_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

[ "$(hash_file "$APK")" = "$EXPECTED_APK_SHA" ] || {
  echo "ERROR: official Polestar China APK SHA-256 mismatch" >&2
  exit 10
}

python3 - "$APK" "$CONFIG" "$EXPECTED_CONFIG_SHA" <<'PY'
import hashlib, re, sys, zipfile
apk, out, expected = sys.argv[1:]
with zipfile.ZipFile(apk) as z:
    dex = z.read("classes3.dex")
values = []
for match in re.finditer(rb"(?<![A-F0-9])[A-F0-9]{32}(?![A-F0-9])", dex):
    value = match.group(0)
    if hashlib.sha256(value).hexdigest() == expected and value not in values:
        values.append(value)
if len(values) != 1:
    raise SystemExit("ERROR: verified account client configuration candidate count=%d" % len(values))
with open(out, "wb") as f:
    f.write(values[0] + b"\n")
print("Verified standalone client configuration derived from exact official build input.")
PY
chmod 600 "$CONFIG"

export PSA_CLIENT_CONFIG_FILE="$CONFIG"

if [ -z "${PSA_SIGNING_KEYSTORE:-}" ] || [ -z "${PSA_SIGNING_STORE_PASSWORD:-}" ]; then
  echo "ERROR: persistent signing variables are required for a distributable standalone release" >&2
  exit 20
fi

cd "$ROOT"
gradle -p android-app --no-daemon \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleRelease

OUT="$ROOT/android-app/app/build/outputs/apk/release/app-release.apk"
test -s "$OUT"
test "$(unzip -p "$OUT" assets/client-signing.bin | wc -c | tr -d ' ')" = "48"

echo "Built complete standalone APK:"
echo "$OUT"
hash_file "$OUT"
