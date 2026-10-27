#!/bin/sh
set -eu

OUT="digital-key-research/runtime/native-v2-avd"
mkdir -p "$OUT"
PACKAGE="io.github.polestarcn.companion.digitalkeyv2"
COMPONENT="$PACKAGE/io.github.polestarcn.companion.digitalkey.DigitalKeyResearchActivity"
APK="android-app/app/build/outputs/apk/debug/app-debug.apk"

adb wait-for-device
adb root || true
adb wait-for-device
adb install -r "$APK" | tee "$OUT/install.txt"

for p in android.permission.BLUETOOTH_SCAN android.permission.BLUETOOTH_CONNECT android.permission.BLUETOOTH_ADVERTISE; do
  adb shell pm grant "$PACKAGE" "$p" >/dev/null 2>&1 || true
done

adb logcat -c || true
adb shell am start -n "$COMPONENT" | tee "$OUT/start.txt"
sleep 4
adb shell uiautomator dump /sdcard/nativev2-before.xml >/dev/null
adb pull /sdcard/nativev2-before.xml "$OUT/before.xml" >/dev/null
adb exec-out screencap -p > "$OUT/before.png"

python3 - "$OUT/before.xml" "$OUT/tap.txt" <<'PY'
import re,sys
xml=open(sys.argv[1],encoding='utf-8').read()
m=re.search(r'text="[^"]*Load bundled native SDK"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',xml)
if not m:
    m=re.search(r'text="[^"]*加载原厂 Native SDK[^"]*"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',xml)
if not m:
    raise SystemExit("native SDK load button not found")
x1,y1,x2,y2=map(int,m.groups())
x=(x1+x2)//2;y=(y1+y2)//2
open(sys.argv[2],'w').write(f"{x} {y}\n")
print(x,y)
PY
set -- $(cat "$OUT/tap.txt")
adb shell input tap "$1" "$2"
sleep 5

adb shell uiautomator dump /sdcard/nativev2-after.xml >/dev/null
adb pull /sdcard/nativev2-after.xml "$OUT/after.xml" >/dev/null
adb exec-out screencap -p > "$OUT/after.png"
adb logcat -d > "$OUT/logcat.txt"

grep -q 'Native SDK V2' "$OUT/after.xml"
grep -E 'vocmo-lib|glble|LOADED|LOAD FAILED|NOT PACKAGED|VOCMO Java bridge' "$OUT/after.xml" > "$OUT/native-result.txt" || true
grep -E 'UnsatisfiedLinkError|dlopen failed|FATAL EXCEPTION|vocmo|glble' "$OUT/logcat.txt" > "$OUT/native-log-errors.txt" || true

echo "=== UI native result ==="
cat "$OUT/native-result.txt" || true
echo "=== filtered native log ==="
tail -n 100 "$OUT/native-log-errors.txt" || true
echo "PASS: Native V2 Activity remained alive after library load probe"
