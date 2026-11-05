#!/usr/bin/env bash
set -euo pipefail
# Private FULL build on authorized workstation/CI; never include credentials in GitHub or stdout.
: "${ANDROID_HOME:?Android SDK location required}"
: "${PSA_PRIOR_PSASSISTANT_APK:?Path to previously approved PsAssistant FULL APK required}"
: "${PSA_SIGNING_KEYSTORE:?Original signing keystore path required}"
: "${PSA_SIGNING_STORE_PASSWORD:?Original keystore passphrase must be exported privately}"
[ -f "$PSA_PRIOR_PSASSISTANT_APK" ] || { echo 'Previous FULL APK missing'; exit 1; }
[ -f "$PSA_SIGNING_KEYSTORE" ] || { echo 'Original keystore missing'; exit 1; }
command -v gradle >/dev/null || { echo 'Gradle 8.7 required'; exit 1; }
cd "$(dirname "$0")/../android-app"
gradle --no-daemon --stacktrace :app:testDebugUnitTest :oppoBridge:testDebugUnitTest :app:lintDebug :app:assembleRelease :oppoBridge:assembleDebug
APK=app/build/outputs/apk/release/app-release.apk
[ -s "$APK" ] || { echo 'Release APK missing'; exit 1; }
python3 - "$APK" <<'PY'
import sys,zipfile
f=sys.argv[1]
with zipfile.ZipFile(f) as z:
 assert z.testzip() is None
 assert len(z.read('assets/client-signing.bin'))==48,'Standalone login client asset missing'
 assert 'classes.dex' in z.namelist()
print('Standalone client resource and ZIP integrity: PASS')
PY
if command -v apksigner >/dev/null; then apksigner verify --verbose --print-certs "$APK"; fi
sha256sum "$APK"
echo 'Built FULL APK from current Beta46 source; real China login and car control still require physical testing.'
