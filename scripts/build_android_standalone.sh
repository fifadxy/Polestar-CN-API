#!/bin/sh
set -eu

# Build a single self-contained PsAssistant APK from the exact user-verified
# compatibility reference APK. The reference APK is used only at build time.
#
# Usage:
#   sh scripts/build_android_standalone.sh /path/to/app-release.apk.1.1\(1\).1
#
# The raw compatibility material is never printed or committed.

if [ "$#" -ne 1 ]; then
  echo "usage: $0 REFERENCE_APK" >&2
  exit 2
fi

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
REFERENCE=$1
EXPECTED_SHA="8cae07a3b5c6050487443914ebb55b1d2f2f26e258f4b369837f2b88de052735"
ASSET_DIR="$ROOT/android-app/app/src/main/assets/standalone"
ASSET="$ASSET_DIR/account_compat.bin"
OUT_DIR="$ROOT/dist"
OUT="$OUT_DIR/PsAssistant-v0.8.5-beta1-standalone.apk"

actual=$(sha256sum "$REFERENCE" | awk '{print $1}')
if [ "$actual" != "$EXPECTED_SHA" ]; then
  echo "ERROR: reference APK SHA-256 mismatch" >&2
  exit 10
fi

mkdir -p "$ASSET_DIR" "$OUT_DIR"
cleanup() {
  rm -f "$ASSET"
  rmdir "$ASSET_DIR" 2>/dev/null || true
}
trap cleanup EXIT HUP INT TERM

python3 - "$REFERENCE" "$ASSET" <<'PY'
import hashlib,sys,zipfile
apk,out=sys.argv[1:]
with zipfile.ZipFile(apk) as z:
    dex=z.read("classes.dex")

def upperhex(b):
    return 48 <= b <= 57 or 65 <= b <= 70

values=[]
i=0
while i+32 <= len(dex):
    if upperhex(dex[i]) and all(upperhex(x) for x in dex[i:i+32]):
        left=i>0 and upperhex(dex[i-1])
        right=i+32<len(dex) and upperhex(dex[i+32])
        if not left and not right:
            values.append(bytes(dex[i:i+32]))
            i += 32
            continue
    i += 1

values=list(dict.fromkeys(values))
if len(values) != 1:
    raise SystemExit("ERROR: compatibility material candidate count=%d" % len(values))

mask=hashlib.sha256(b"PsAssistant-Standalone-Account-Compatibility-v1").digest()
encoded=bytes(a ^ b for a,b in zip(values[0],mask))
with open(out,"wb") as f:
    f.write(encoded)
print("Standalone compatibility asset generated from verified reference APK.")
PY

cd "$ROOT"
gradle -p android-app --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
cp "$ROOT/android-app/app/build/outputs/apk/debug/app-debug.apk" "$OUT"
echo "Built: $OUT"
sha256sum "$OUT"
