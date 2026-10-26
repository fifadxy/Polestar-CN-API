#!/bin/sh
set -eu
cd "$(dirname "$0")/../android-app"
if ! command -v java >/dev/null 2>&1; then
  echo 'JDK 17 required; Java command was not found.' >&2
  exit 1
fi
if ! command -v gradle >/dev/null 2>&1; then
  echo 'Gradle 8.7 not found. Install Gradle 8.7 / use Android Studio Gradle sync.' >&2
  exit 2
fi
if [ -z "${ANDROID_HOME:-}${ANDROID_SDK_ROOT:-}" ] && [ ! -f local.properties ]; then
  echo 'Android SDK not configured. Set ANDROID_HOME or create local.properties (sdk.dir=...).' >&2
  exit 3
fi
gradle --no-daemon :app:assembleDebug
printf '\nOutput: %s\n' "$(pwd)/app/build/outputs/apk/debug/app-debug.apk"
