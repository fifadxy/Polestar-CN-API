#!/bin/sh
set -eu

# Build one self-contained PsAssistant APK from the user-provided official
# Polestar China 4.25.0 APK. The official APK is a BUILD-TIME input only.
# The installed PsAssistant never imports, scans or requires another APK.
#
# Usage:
#   sh scripts/build_android_standalone.sh POLESTAR_CHINA_4250_APK
#
# The exact official sample supplies:
# - one account-compatibility constant from classes3.dex, transformed into an
#   opaque asset without printing the original value;
# - libvocmo-lib.so + libglble.so for Digital Key V2 diagnostics;
# - non-secret PAK/VOCMO protocol schema resources.
#
# No token, password, VIN, paired digital key, private key, certificate cache
# or challenge/response data is copied into the build.

if [ "$#" -ne 1 ]; then
  echo "usage: $0 POLESTAR_CHINA_4250_APK" >&2
  exit 2
fi

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
POLESTAR=$1
EXPECTED_POLESTAR_SHA="99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef"
# Hash of the expected 32-character compatibility constant. The raw value is
# deliberately not stored in the repository or printed by this script.
EXPECTED_COMPAT_SHA="59e1ca066e23a43e3976ded60c6691800dd19c4b173bd94b241bee42be5a7274"

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

actual=$(hash_file "$POLESTAR")
if [ "$actual" != "$EXPECTED_POLESTAR_SHA" ]; then
  echo "ERROR: Polestar China 4.25.0 APK SHA-256 mismatch" >&2
  exit 10
fi

mkdir -p "$ASSET_DIR" "$OEM_ASSET_DIR" "$JNI_DIR" "$OUT_DIR"
cleanup() {
  rm -f "$ASSET"
  rm -rf "$OEM_ASSET_DIR"
  rm -f "$JNI_DIR/libvocmo-lib.so" "$JNI_DIR/libglble.so"
  rmdir "$ASSET_DIR" 2>/dev/null || true
  rmdir "$JNI_DIR" 2>/dev/null || true
}
trap cleanup EXIT HUP INT TERM

python3 - "$POLESTAR" "$ASSET" "$EXPECTED_COMPAT_SHA" "$JNI_DIR" "$OEM_ASSET_DIR" <<'PY'
import hashlib, os, re, sys, zipfile
apk, compat_out, expected_compat_sha, jni_dir, schema_out = sys.argv[1:]

schema_entries = [
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
    dex = z.read("classes3.dex")
    values = []
    for m in re.finditer(rb"(?<![A-F0-9])[A-F0-9]{32}(?![A-F0-9])", dex):
        value = m.group(0)
        if value not in values:
            values.append(value)
    if len(values) != 1:
        raise SystemExit("ERROR: official compatibility candidate count=%d" % len(values))
    material = values[0]
    if hashlib.sha256(material).hexdigest() != expected_compat_sha:
        raise SystemExit("ERROR: official compatibility material fingerprint mismatch")

    mask = hashlib.sha256(b"PsAssistant-Standalone-Account-Compatibility-v1").digest()
    encoded = bytes(a ^ b for a, b in zip(material, mask))
    with open(compat_out, "wb") as f:
        f.write(encoded)

    native = [
        ("lib/arm64-v8a/libvocmo-lib.so", "libvocmo-lib.so"),
        ("lib/arm64-v8a/libglble.so", "libglble.so"),
    ]
    for src, name in native:
        with open(os.path.join(jni_dir, name), "wb") as f:
            f.write(z.read(src))

    for src in schema_entries:
        dst = os.path.join(schema_out, src)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with open(dst, "wb") as f:
            f.write(z.read(src))

print("Standalone account asset generated from exact official Polestar China 4.25.0 sample.")
print("Digital Key V2 resources packaged: 2 native libraries + %d protocol schemas." % len(schema_entries))
PY

cd "$ROOT"
gradle -p android-app --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
cp "$ROOT/android-app/app/build/outputs/apk/debug/app-debug.apk" "$OUT"

# Final package checks: one APK, no runtime reference-app dependency.
unzip -Z1 "$OUT" | grep -q '^assets/standalone/account_compat.bin$'
unzip -Z1 "$OUT" | grep -q '^lib/arm64-v8a/libvocmo-lib.so$'
unzip -Z1 "$OUT" | grep -q '^lib/arm64-v8a/libglble.so$'
count=$(unzip -Z1 "$OUT" | grep -c '^assets/oem-native-v2/.*\.proto$')
[ "$count" -eq 25 ]

echo "Built: $OUT"
echo "Protocol schemas: $count/25"
hash_file "$OUT"
