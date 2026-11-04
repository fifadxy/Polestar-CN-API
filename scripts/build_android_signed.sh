#!/bin/sh
set -eu
: "${PSA_SIGNING_KEYSTORE:?Set PSA_SIGNING_KEYSTORE to the existing private signing file outside the repository}"
: "${PSA_SIGNING_STORE_PASSWORD:?Set PSA_SIGNING_STORE_PASSWORD in the build environment}"
repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$repo_dir/android-app"
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
printf '\nSigned APK: %s\n' "$repo_dir/android-app/app/build/outputs/apk/release/app-release.apk"
