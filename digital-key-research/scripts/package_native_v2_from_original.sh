#!/bin/sh
set -eu

# Usage:
# package_native_v2_from_original.sh ORIGINAL_APK LOADER_APK APKSIGNER OUTPUT_APK
#
# Optional:
#   NATIVE_V2_KEYSTORE=/path/key.p12
#   NATIVE_V2_KEYSTORE_PASS=...
#   NATIVE_V2_KEY_ALIAS=nativev2
#
# This script packages ONLY libvocmo-lib.so + libglble.so and three PassiveEntry
# protobuf schemas from the exact user-provided Polestar China 4.25.0 sample.
# It never copies account/key data, certificates, mock keystores, AppSecret or white-box libs.

if [ "$#" -ne 4 ]; then
  echo "usage: $0 ORIGINAL_APK LOADER_APK APKSIGNER OUTPUT_APK" >&2
  exit 2
fi

ORIGINAL=$1
LOADER=$2
APKSIGNER=$3
OUTPUT=$4

EXPECTED_APK_SHA="99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef"
EXPECTED_VOCMO_SHA="9bce3214a74a883fa9839eb00929f55046ad75dd54aaa387a3685d8ffc777c2f"
EXPECTED_GLBLE_SHA="82f141506a913ddb870309e23d42641f8cbff9c1c559129ca96ef25a08556678"
EXPECTED_REQ_SHA="e36a1e0d2f8518e4f22af6153871c95a666268dd09874d5ead392ef20d398a5f"
EXPECTED_RESP_SHA="baf86c0e2ee81d2feafd850a8fa14f36eac50eb5f20adf817f4a782f02f85d54"
EXPECTED_STATUS_SHA="43c2013c5d6f95bf772c15a51d498bbfac4fc8def510ab9e320d2ed8ffbc4d38"

actual=$(sha256sum "$ORIGINAL" | awk '{print $1}')
[ "$actual" = "$EXPECTED_APK_SHA" ] || {
  echo "ERROR: source APK hash mismatch: $actual" >&2
  exit 10
}

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT HUP INT TERM
mkdir -p "$WORK/libs" "$WORK/passive"
unzip -p "$ORIGINAL" lib/arm64-v8a/libvocmo-lib.so > "$WORK/libs/libvocmo-lib.so"
unzip -p "$ORIGINAL" lib/arm64-v8a/libglble.so > "$WORK/libs/libglble.so"
unzip -p "$ORIGINAL" messages/passive/passive_key_request.proto > "$WORK/passive/passive_key_request.proto"
unzip -p "$ORIGINAL" messages/passive/passive_key_response.proto > "$WORK/passive/passive_key_response.proto"
unzip -p "$ORIGINAL" messages/passive/passive_key_status.proto > "$WORK/passive/passive_key_status.proto"

[ "$(sha256sum "$WORK/libs/libvocmo-lib.so" | awk '{print $1}')" = "$EXPECTED_VOCMO_SHA" ]
[ "$(sha256sum "$WORK/libs/libglble.so" | awk '{print $1}')" = "$EXPECTED_GLBLE_SHA" ]
[ "$(sha256sum "$WORK/passive/passive_key_request.proto" | awk '{print $1}')" = "$EXPECTED_REQ_SHA" ]
[ "$(sha256sum "$WORK/passive/passive_key_response.proto" | awk '{print $1}')" = "$EXPECTED_RESP_SHA" ]
[ "$(sha256sum "$WORK/passive/passive_key_status.proto" | awk '{print $1}')" = "$EXPECTED_STATUS_SHA" ]

UNSIGNED="$WORK/native-v2-unsigned.apk"
python3 - "$LOADER" "$UNSIGNED" "$WORK" <<'PY'
import sys,zipfile,os,json
base,out,work=sys.argv[1:]
def sig(name):
    u=name.upper()
    if not u.startswith("META-INF/"): return False
    b=u.rsplit("/",1)[-1]
    return b=="MANIFEST.MF" or b.endswith((".SF",".RSA",".DSA",".EC"))
with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out,"w",allowZip64=True) as zout:
    for info in zin.infolist():
        if sig(info.filename): continue
        data=zin.read(info.filename)
        ni=zipfile.ZipInfo(info.filename,date_time=info.date_time)
        ni.external_attr=info.external_attr
        ni.internal_attr=info.internal_attr
        ni.create_system=info.create_system
        ni.flag_bits=info.flag_bits & ~0x08
        ni.compress_type=info.compress_type
        ni.comment=info.comment
        ni.extra=info.extra
        zout.writestr(ni,data)
    for src,name in [
        (os.path.join(work,"libs/libvocmo-lib.so"),"lib/arm64-v8a/libvocmo-lib.so"),
        (os.path.join(work,"libs/libglble.so"),"lib/arm64-v8a/libglble.so"),
        (os.path.join(work,"passive/passive_key_request.proto"),"assets/oem-native-v2/passive/passive_key_request.proto"),
        (os.path.join(work,"passive/passive_key_response.proto"),"assets/oem-native-v2/passive/passive_key_response.proto"),
        (os.path.join(work,"passive/passive_key_status.proto"),"assets/oem-native-v2/passive/passive_key_status.proto"),
    ]:
        zi=zipfile.ZipInfo(name)
        zi.compress_type=zipfile.ZIP_DEFLATED
        zi.external_attr=(0o100644 << 16)
        with open(src,"rb") as f:
            zout.writestr(zi,f.read(),compress_type=zipfile.ZIP_DEFLATED,compresslevel=6)
    meta={
      "source_apk_sha256":"99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef",
      "included":["libvocmo-lib.so","libglble.so","passive_key_request.proto","passive_key_response.proto","passive_key_status.proto"],
      "purpose":"research native-loader / schema packaging only; no credentials or paired key material"
    }
    zi=zipfile.ZipInfo("assets/oem-native-v2/source_manifest.json")
    zi.compress_type=zipfile.ZIP_DEFLATED
    zi.external_attr=(0o100644 << 16)
    zout.writestr(zi,json.dumps(meta,ensure_ascii=False,indent=2).encode(),compress_type=zipfile.ZIP_DEFLATED,compresslevel=6)
PY

KEYSTORE=${NATIVE_V2_KEYSTORE:-"$WORK/native-v2-research.p12"}
PASS=${NATIVE_V2_KEYSTORE_PASS:-android}
ALIAS=${NATIVE_V2_KEY_ALIAS:-nativev2}
if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair -noprompt -storetype PKCS12 -keystore "$KEYSTORE" \
    -storepass "$PASS" -keypass "$PASS" -alias "$ALIAS" -keyalg RSA -keysize 3072 \
    -validity 3650 -dname "CN=PsAssistant Digital Key Native V2 Research,O=Local Research" \
    >/dev/null 2>&1
fi

"$APKSIGNER" sign --ks "$KEYSTORE" --ks-key-alias "$ALIAS" \
  --ks-pass "pass:$PASS" --key-pass "pass:$PASS" --out "$OUTPUT" "$UNSIGNED"
"$APKSIGNER" verify --verbose "$OUTPUT"

# Safety inventory checks.
if unzip -Z1 "$OUTPUT" | grep -Eiq 'libAppSecret|libwhite-box|\.pfx$|keystore_password|ccc_ca_certs'; then
  echo "ERROR: excluded sensitive/unrelated resource found in final package" >&2
  exit 20
fi

echo "Built: $OUTPUT"
sha256sum "$OUTPUT"
echo "OEM included hashes:"
for entry in   lib/arm64-v8a/libvocmo-lib.so   lib/arm64-v8a/libglble.so   assets/oem-native-v2/passive/passive_key_request.proto   assets/oem-native-v2/passive/passive_key_response.proto   assets/oem-native-v2/passive/passive_key_status.proto; do
  printf "%s  " "$entry"
  unzip -p "$OUTPUT" "$entry" | sha256sum | awk '{print $1}'
done
