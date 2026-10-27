#!/bin/sh
set -eu

# Rebuild Digital Key Native V2 from the exact user-provided Polestar China 4.25.0 APK.
# Packages the observed non-secret PAK/VOCMO native libraries and protocol schemas.
# Never copies account/session data, paired-key caches, private keys, AppSecret/white-box
# libraries, mock keystore passwords, or another app instance's credential stores.
#
# Usage:
#   package_native_v2_from_original.sh ORIGINAL_APK LOADER_APK APKSIGNER OUTPUT_APK

[ "$#" -eq 4 ] || { echo "usage: $0 ORIGINAL_APK LOADER_APK APKSIGNER OUTPUT_APK" >&2; exit 2; }
ORIGINAL=$1
LOADER=$2
APKSIGNER=$3
OUTPUT=$4

EXPECTED_APK_SHA="99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef"
EXPECTED_VOCMO_SHA="9bce3214a74a883fa9839eb00929f55046ad75dd54aaa387a3685d8ffc777c2f"
EXPECTED_GLBLE_SHA="82f141506a913ddb870309e23d42641f8cbff9c1c559129ca96ef25a08556678"

actual=$(sha256sum "$ORIGINAL" | awk '{print $1}')
[ "$actual" = "$EXPECTED_APK_SHA" ] || { echo "ERROR: source APK hash mismatch: $actual" >&2; exit 10; }

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT HUP INT TERM
mkdir -p "$WORK/libs" "$WORK/protocol"
unzip -p "$ORIGINAL" lib/arm64-v8a/libvocmo-lib.so > "$WORK/libs/libvocmo-lib.so"
unzip -p "$ORIGINAL" lib/arm64-v8a/libglble.so > "$WORK/libs/libglble.so"
[ "$(sha256sum "$WORK/libs/libvocmo-lib.so" | awk '{print $1}')" = "$EXPECTED_VOCMO_SHA" ]
[ "$(sha256sum "$WORK/libs/libglble.so" | awk '{print $1}')" = "$EXPECTED_GLBLE_SHA" ]

cat > "$WORK/protocol-manifest.txt" <<'EOF'
messages/passive/passive_key_request.proto|e36a1e0d2f8518e4f22af6153871c95a666268dd09874d5ead392ef20d398a5f
messages/passive/passive_key_response.proto|baf86c0e2ee81d2feafd850a8fa14f36eac50eb5f20adf817f4a782f02f85d54
messages/passive/passive_key_status.proto|43c2013c5d6f95bf772c15a51d498bbfac4fc8def510ab9e320d2ed8ffbc4d38
messages/devicepairing/car_pairing_request.proto|be7c600a9aa98e976df27a454aa6c5873624811a8c4fcf84a5f7c585a8b8b397
messages/devicepairing/car_pairing_response.proto|e74575bbf2157c7e04e36f6dc217b4121468b41dd28594dfa5686045580ae353
messages/devicepairing/car_to_device.proto|d8d35fef1c4c905458bc5826dfe4a7288124c58a3f56b853f86fc1baaaee084f
messages/devicepairing/device_pairing_request.proto|34e68409068010526a52bb45b70f18cc00cd73a2e5f69b93d2f6aad9a57b20cf
messages/devicepairing/device_pairing_response.proto|dbc77c5d5d20ee48aa78d6c465615dfba49c0a38a4f27b639c6959a7dad34d4a
messages/devicepairing/device_to_car.proto|b50318ef54faa19e9e21e85f89ac920c6f4c985ac9a5fd320728508c5eaac0b1
entities/device_pairing_id.proto|7c0d18ea1d5854457cb0bc745c5c2e7357f6cf56a5ac147a1886e9f848f5f5b7
entities/caraccessstate/BasicVehicleData.proto|349fb6115fc2079e81d0add6a202809c3d2cd9081eabb2d5df9125d25294c419
entities/caraccessstate/CarAccessStateStatus.proto|ba1c7ef7cb9c877cf41471395eb4f9ffd16305effd8414a9a6691d1006a951c2
messages/caraccessstate/BasicVehicleDataUpdate.proto|f689059a02deba1361a3ded3b8c0f5dc995fb4e897abc094c54665a48eb85cb0
messages/caraccessstate/CarAccessStateUpdate.proto|e0ec44b376f637dd50c7a48bf05bd2ec5b1d930e563a01910fdc2780d3b50962
messages/caraccessstate/CarStandbyStateUpdate.proto|4e2f53044d667ab55b9f952668e1e69bafa355f1cf142b8069f692a8ec4ee16a
messages/caraccessstate/DataSharingUpdate.proto|1ebb2eb3cfac15d7e156ef286a9fe63efe8796d9f174887011e7006cb4761124
messages/caraccessstate/GetBasicVehicleDataRequest.proto|d0b1454c3366723c39c2e1d22cbb6f9d77c229e353bf9e952bf438f210133499
messages/caraccessstate/GetBasicVehicleDataResponse.proto|09839c4d6cd6f17297777acc3a5693758db64e9f89ea05dacfd09c945f2cb592
messages/caraccessstate/GetCarAccessStateRequest.proto|adcc74fbecf6204e09fe9a78caf620e6e9988e68d10a02cdc0a4af7d71176828
messages/caraccessstate/GetCarAccessStateResponse.proto|94a272af8f622509395ddba9f6e69b2802bead9bcc926f4a8fa8bf9c808015d2
messages/caraccessstate/GetDataSharingRequest.proto|95e440964477667618df129bc7bc33a7ba1d58bd8c1c82ea74e39ab41bcf9797
messages/caraccessstate/GetDataSharingResponse.proto|b897972a8a5f584672c7734737e179ba812f2c569869ea707f458a25ff0d6048
messages/caraccessstate/WorkshopModeUpdate.proto|ed38286d025d8d668987d92d414479c9fb885b3460de7b6163e9323a21a884dd
paired_devices_request.proto|9ab6b737b7f90d39a7bdec5c6def21557c9f9545f36dc89a7be4a3643bf56f40
paired_devices_response.proto|58cfed97bbc51043fbff78e275ae52d77aecd724770381fe76154d14038a6a31
EOF

while IFS='|' read -r entry expected; do
  dest="$WORK/protocol/$entry"
  mkdir -p "$(dirname "$dest")"
  unzip -p "$ORIGINAL" "$entry" > "$dest"
  got=$(sha256sum "$dest" | awk '{print $1}')
  [ "$got" = "$expected" ] || { echo "ERROR: protocol asset mismatch: $entry $got" >&2; exit 11; }
done < "$WORK/protocol-manifest.txt"

UNSIGNED="$WORK/native-v21-unsigned.apk"
python3 - "$LOADER" "$UNSIGNED" "$WORK" <<'PY'
import sys,zipfile,os,json
base,out,work=sys.argv[1:]
def old_sig(name):
    u=name.upper()
    if not u.startswith("META-INF/"): return False
    b=u.rsplit("/",1)[-1]
    return b=="MANIFEST.MF" or b.endswith((".SF",".RSA",".DSA",".EC"))

with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out,"w",allowZip64=True) as zout:
    for info in zin.infolist():
        if old_sig(info.filename): continue
        data=zin.read(info.filename)
        ni=zipfile.ZipInfo(info.filename,date_time=info.date_time)
        ni.external_attr=info.external_attr; ni.internal_attr=info.internal_attr
        ni.create_system=info.create_system; ni.flag_bits=info.flag_bits & ~0x08
        ni.compress_type=info.compress_type; ni.comment=info.comment; ni.extra=info.extra
        zout.writestr(ni,data)

    additions=[
      (os.path.join(work,"libs/libvocmo-lib.so"),"lib/arm64-v8a/libvocmo-lib.so"),
      (os.path.join(work,"libs/libglble.so"),"lib/arm64-v8a/libglble.so"),
    ]
    with open(os.path.join(work,"protocol-manifest.txt"),encoding="utf-8") as f:
        for line in f:
            entry=line.split("|",1)[0].strip()
            if entry:
                additions.append((os.path.join(work,"protocol",entry),
                                  "assets/oem-native-v21/protocol/"+entry))
    for src,name in additions:
        zi=zipfile.ZipInfo(name); zi.compress_type=zipfile.ZIP_DEFLATED
        zi.external_attr=(0o100644 << 16)
        with open(src,"rb") as f:
            zout.writestr(zi,f.read(),compress_type=zipfile.ZIP_DEFLATED,compresslevel=6)

    meta={
      "source_apk_sha256":"99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef",
      "scope":"VOCMO/GLBLE + PassiveEntry + DevicePairing + CarAccessState + PairedDevices protocol resources",
      "credential_material_included":False,
      "purpose":"authorized-flow compatibility research; no cloned paired key"
    }
    zi=zipfile.ZipInfo("assets/oem-native-v21/source_manifest.json")
    zi.compress_type=zipfile.ZIP_DEFLATED; zi.external_attr=(0o100644 << 16)
    zout.writestr(zi,json.dumps(meta,ensure_ascii=False,indent=2).encode("utf-8"),
                  compress_type=zipfile.ZIP_DEFLATED,compresslevel=6)
PY

KEYSTORE=${NATIVE_V2_KEYSTORE:-"$WORK/native-v21-research.p12"}
PASS=${NATIVE_V2_KEYSTORE_PASS:-android}
ALIAS=${NATIVE_V2_KEY_ALIAS:-nativev21}
if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair -noprompt -storetype PKCS12 -keystore "$KEYSTORE"     -storepass "$PASS" -keypass "$PASS" -alias "$ALIAS" -keyalg RSA -keysize 3072     -validity 3650 -dname "CN=PsAssistant Digital Key V2.1 Research,O=Local Research"     >/dev/null 2>&1
fi

"$APKSIGNER" sign --ks "$KEYSTORE" --ks-key-alias "$ALIAS"   --ks-pass "pass:$PASS" --key-pass "pass:$PASS" --out "$OUTPUT" "$UNSIGNED"
"$APKSIGNER" verify --verbose "$OUTPUT"

# Hard stop if secret or paired-key material was accidentally imported.
if unzip -Z1 "$OUTPUT" | grep -Eiq 'libAppSecret|libwhite-box|\.pfx$|keystore_password|key_token|refresh_token|paired.?key.?cache'; then
  echo "ERROR: excluded credential/security material found in final package" >&2
  exit 20
fi

COUNT=$(unzip -Z1 "$OUTPUT" | grep '^assets/oem-native-v21/protocol/' | wc -l | tr -d ' ')
[ "$COUNT" = "25" ] || { echo "ERROR: expected 25 protocol assets, got $COUNT" >&2; exit 21; }

echo "Built V2.1: $OUTPUT"
sha256sum "$OUTPUT"
echo "protocol_assets=$COUNT"
