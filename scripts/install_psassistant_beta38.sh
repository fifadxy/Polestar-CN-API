#!/bin/sh
set -eu
apk=${1:?usage: install_psassistant_beta38.sh /path/PsAssistant-v0.9.7-beta38-FULL.apk}
command -v adb >/dev/null || { echo '请先安装 Android platform-tools 并连接已授权的手机。' >&2; exit 1; }
[ -f "$apk" ] || { echo 'APK 文件不存在' >&2; exit 1; }
adb get-state
adb install -r "$apk"
adb shell am start -n io.github.polestarcn.companion/.MainActivity
adb shell dumpsys package io.github.polestarcn.companion | sed -n '/versionCode=/p; /versionName=/p'
echo '主APK已内置模块。请在LSPosed选择该模块及原作用域，重启宿主后进入系统车辆接入页自检。此脚本不发送车控。'
