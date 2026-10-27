#!/bin/sh
set -eu

# Build one self-contained PsAssistant APK.
#
# Usage:
#   sh scripts/build_android_standalone.sh REFERENCE_APK [POLESTAR_CHINA_4250_APK]
#
# REFERENCE_APK is the exact user-verified app.c3garage.companion sample and is
# used only at build time to generate an opaque account-compatibility asset.
#
# Optional POLESTAR_CHINA_4250_APK adds the two already-researched Native V2
# libraries and non-secret protocol schema resources. No user/session/key cache
# data is copied. Neither input APK is needed on the phone after installation.

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "usage: $0 REFERENCE_APK [POLESTAR_CHINA_4250_APK]" >&2
  exit 2
fi

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
REFERENCE=$1
POLESTAR=${2:-}
EXPECTED_REFERENCE_SHA="8cae07a3b5c6050487443914ebb55b1d2f2f26e258f4b369837f2b88de052735"
EXPECTED_POLESTAR_SHA="99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef"
ASSET_DIR="$ROOT/android-app/app/src/main/assets/standalone"
ASSET="$ASSET_DIR/account_compat.bin"
OEM_ASSET_DIR="$ROOT/android-app/app/src/main/assets/oem-native-v2"
JNI_DIR="$ROOT/android-app/app/src/main/jniLibs/arm64-v8a"
OUT_DIR="$ROOT/dist"
OUT="$OUT_DIR/PsAssistant-v0.8.5-beta1-standalone.apk"

hash_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

actual=$(hash_file "$REFERENCE")
if [ "$actual" != "$EXPECTED_REFERENCE_SHA" ]; then
  echo "ERROR: reference APK SHA-256 mismatch" >&2
  exit 10
fi

mkdir -p "$ASSET_DIR" "$OUT_DIR"
cleanup() {
  rm -f "$ASSET"
  rm -rf "$OEM_ASSET_DIR"
  rm -f "$JNI_DIR/libvocmo-lib.so" "$JNI_DIR/libglble.so"
  rmdir "$ASSET_DIR" 2>/dev/null || true
  rmdir "$JNI_DIR" 2>/dev/null || true
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
print("Standalone account compatibility asset generated from verified build input.")
PY

if [ -n "$POLESTAR" ]; then
  polestar_sha=$(hash_file "$POLESTAR")
  if [ "$polestar_sha" != "$EXPECTED_POLESTAR_SHA" ]; then
    echo "ERROR: Polestar China 4.25.0 APK SHA-256 mismatch" >&2
    exit 11
  fi
  mkdir -p "$JNI_DIR" "$OEM_ASSET_DIR"
  python3 - "$POLESTAR" "$JNI_DIR" "$OEM_ASSET_DIR" <<'PY'
import os,sys,zipfile
apk,jni,out=sys.argv[1:]
entries=[
"messages/passive/passive_key_request.proto",
"messages/passive/passive_key_response.proto",
"messages/passive/passive_key_status.proto",
"messages/devicepairing/car_pairing_request.proto",
"messages/devicepairing/car_pairing_response.proto",
"messages/devicepairing/car_to_device.proto",
"messages/devicepairing/device_pairing_request.proto",
"messages/devicepairing/device_pairing_response.proto",
"messages/devicepairing/device_to_car.proto",
"entities/device_pairing_id.proto",
"entities/caraccessstate/BasicVehicleData.proto",
"entities/caraccessstate/CarAccessStateStatus.proto",
"messages/caraccessstate/BasicVehicleDataUpdate.proto",
"messages/caraccessstate/CarAccessStateUpdate.proto",
"messages/caraccessstate/CarStandbyStateUpdate.proto",
"messages/caraccessstate/DataSharingUpdate.proto",
"messages/caraccessstate/GetBasicVehicleDataRequest.proto",
"messages/caraccessstate/GetBasicVehicleDataResponse.proto",
"messages/caraccessstate/GetCarAccessStateRequest.proto",
"messages/caraccessstate/GetCarAccessStateResponse.proto",
"messages/caraccessstate/GetDataSharingRequest.proto",
"messages/caraccessstate/GetDataSharingResponse.proto",
"messages/caraccessstate/WorkshopModeUpdate.proto",
"paired_devices_request.proto",
"paired_devices_response.proto",
]
with zipfile.ZipFile(apk) as z:
    for src,name in [
        ("lib/arm64-v8a/libvocmo-lib.so","libvocmo-lib.so"),
        ("lib/arm64-v8a/libglble.so","libglble.so"),
    ]:
        with open(os.path.join(jni,name),"wb") as f:
            f.write(z.read(src))
    for src in entries:
        dst=os.path.join(out,src)
        os.makedirs(os.path.dirname(dst),exist_ok=True)
        with open(dst,"wb") as f:
            f.write(z.read(src))
print("Digital-key Native V2 resources: 2 libraries + %d schemas." % len(entries))
PY
fi

cd "$ROOT"
gradle -p android-app --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
cp "$ROOT/android-app/app/build/outputs/apk/debug/app-debug.apk" "$OUT"
echo "Built: $OUT"
hash_file "$OUT"
