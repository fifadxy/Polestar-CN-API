#!/usr/bin/env bash
set -euo pipefail

EXPECTED_PACKAGE='com.polestar.china.android'
EXPECTED_VERSION_NAME='4.25.0'
EXPECTED_VERSION_CODE='262'
EXPECTED_CERT_SHA256='ae2e24b9dabb4caf85ca8d8b40ecf1bce04b7ebde66ab236dc05ff77b087038f'
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
expected_dex_manifest="$script_dir/polestar-cn-4.25.0-dex.sha256"

usage() {
  cat <<'EOF'
Usage: Tools/apk/verify-polestar-cn-apk.sh <apk>

Checks ZIP integrity, package/version metadata, APK signatures and DEX hashes.
Requires unzip, sha256sum, aapt and apksigner. ANDROID_HOME/ANDROID_SDK_ROOT
may point to an Android SDK when aapt and apksigner are not already in PATH.
EOF
}

if [[ $# -ne 1 ]]; then
  usage >&2
  exit 2
fi

apk=$1
if [[ ! -f $apk ]]; then
  printf 'APK not found: %s\n' "$apk" >&2
  exit 2
fi

find_android_tool() {
  local name=$1
  local sdk_root=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
  local candidate

  if command -v "$name" >/dev/null 2>&1; then
    command -v "$name"
    return 0
  fi

  if [[ -n $sdk_root && -d $sdk_root/build-tools ]]; then
    candidate=$(find "$sdk_root/build-tools" -mindepth 2 -maxdepth 2 -type f -name "$name" -print | sort -V | tail -n 1)
    if [[ -n $candidate ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  fi

  printf 'Required tool not found: %s\n' "$name" >&2
  return 1
}

for command_name in cut diff grep sed sha256sum sort unzip; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    printf 'Required tool not found: %s\n' "$command_name" >&2
    exit 2
  fi
done

aapt=$(find_android_tool aapt)
apksigner=$(find_android_tool apksigner)

printf 'APK SHA-256\n'
sha256sum "$apk"

printf '\nZIP integrity\n'
unzip -tq "$apk"

badging=$("$aapt" dump badging "$apk" | sed -n '1p')
printf '\nPackage metadata\n%s\n' "$badging"

package=$(sed -n "s/^package: name='\([^']*\)'.*/\1/p" <<<"$badging")
version_code=$(sed -n "s/.*versionCode='\([^']*\)'.*/\1/p" <<<"$badging")
version_name=$(sed -n "s/.*versionName='\([^']*\)'.*/\1/p" <<<"$badging")

metadata_ok=true
if [[ $package != "$EXPECTED_PACKAGE" ]]; then
  printf 'Unexpected package: %s (expected %s)\n' "$package" "$EXPECTED_PACKAGE" >&2
  metadata_ok=false
fi
if [[ $version_name != "$EXPECTED_VERSION_NAME" ]]; then
  printf 'Unexpected versionName: %s (expected %s)\n' "$version_name" "$EXPECTED_VERSION_NAME" >&2
  metadata_ok=false
fi
if [[ $version_code != "$EXPECTED_VERSION_CODE" ]]; then
  printf 'Unexpected versionCode: %s (expected %s)\n' "$version_code" "$EXPECTED_VERSION_CODE" >&2
  metadata_ok=false
fi

printf '\nAPK signature and signer certificates\n'
signature_output=$("$apksigner" verify --verbose --print-certs "$apk")
printf '%s\n' "$signature_output"

if ! grep -Eq '^Verified using v(2|3|3\.1) scheme.*: true$' <<<"$signature_output"; then
  printf 'APK is not verified with signature scheme v2 or v3.\n' >&2
  exit 1
fi

signer_count=$(sed -n 's/^Number of signers: //p' <<<"$signature_output")
cert_sha256=$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' <<<"$signature_output")
if [[ $signer_count != 1 || $cert_sha256 != "$EXPECTED_CERT_SHA256" ]]; then
  printf 'Unexpected APK signer certificate.\n' >&2
  printf 'Expected one signer with certificate SHA-256 %s.\n' "$EXPECTED_CERT_SHA256" >&2
  exit 1
fi

printf '\nDEX SHA-256\n'
dex_entries=$(unzip -Z1 "$apk" | sed -n '/^classes\([0-9][0-9]*\)\?\.dex$/p' | sort -V)
if [[ -z $dex_entries ]]; then
  printf 'No top-level classes*.dex entries found\n' >&2
  exit 1
fi
dex_manifest=$(mktemp)
trap 'rm -f "$dex_manifest"' EXIT
while IFS= read -r entry; do
  printf '%s  %s\n' "$(unzip -p "$apk" "$entry" | sha256sum | cut -d' ' -f1)" "$entry" | tee -a "$dex_manifest"
done <<<"$dex_entries"

if [[ $metadata_ok != true ]]; then
  exit 1
fi

printf '\nDEX fingerprint comparison\n'
if ! diff -u "$expected_dex_manifest" "$dex_manifest"; then
  printf 'DEX fingerprints do not match the verified 4.25.0 build.\n' >&2
  exit 1
fi

printf 'Target package metadata and all 16 DEX fingerprints matched.\n'
