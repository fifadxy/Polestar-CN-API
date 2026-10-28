#!/usr/bin/env python3
"""Lightweight project integrity checks (does not replace Gradle compilation)."""
from pathlib import Path
import re, sys
root=Path(__file__).resolve().parents[1]
main=root/'android-app/app/src/main'
java=list(main.rglob('*.java'))
assert len(java)>=9, f'Expected Java files, got {len(java)}'
all_java='\n'.join(p.read_text() for p in java)
gradle=(root/'android-app/app/build.gradle').read_text()
manifest=(main/'AndroidManifest.xml').read_text()
assert 'minifyEnabled false' in gradle and 'shrinkResources false' in gradle
assert 'ENABLE_REMOTE_COMMANDS = true' in all_java
assert 'ENABLE_NETWORK_VEHICLE_API = true' in all_java
assert 'DEMO / OFFLINE' in all_java
assert 'classify(boolean isDemo' in all_java
assert 'VEHICLE_CONFIRMED' in all_java
assert '0L,' in (main/'java/io/github/polestarcn/companion/DemoRepository.java').read_text()
assert 'Select Polestar Region' not in all_java
assert 'showInitialRegionChooser' not in all_java
assert 'new DemoRepository' not in (main/'java/io/github/polestarcn/companion/MainActivity.java').read_text()
assert 'China' in all_java and 'Global' in all_java
assert 'android.permission.INTERNET' in manifest
assert 'class MainActivity' in all_java
assert 'WidgetRenderer.update' in all_java
assert 'class VehicleWidgetProvider' in all_java
assert 'class CompactWidgetProvider' in all_java
client=(main/'java/io/github/polestarcn/companion/auth/ClientSignatureProvider.java').read_text()
assert 'client-signing.bin' in client and 'getInstalledPackages' not in client
assert 'standalone/account_compat.bin' in client
assert 'PSA_PRIOR_PSASSISTANT_APK' in gradle
assert 'PSACFG2' in gradle
assert 'app.c3garage.companion' not in all_java
assert not re.search(r'["\\\'][0-9A-F]{32}["\\\']', all_java), 'private client material must not be committed'
assert 'ACTION_OPEN_DOCUMENT' not in (main/'java/io/github/polestarcn/companion/MainActivity.java').read_text()
assert 'ChinaLiveActivity' in manifest
assert 'grpc-okhttp:1.68.2' in gradle
version = re.search(r"versionName\s+'([^']+)'", gradle)
assert version, 'missing APK versionName'
workflow = (root/'.github/workflows/android-apk.yml').read_text()
assert f'PsAssistant-v{version.group(1)}-debug.apk' in workflow, 'CI artifact version must match APK metadata'
assert 'VehicleSnapshotStore.saveChina' in all_java
assert 'secret + canonical + secret' in all_java
assert 'ProtoWire.stringField(2, vin)' in all_java
assert '<appwidget-provider' in (main/'res/xml/widget_standard.xml').read_text()
assert '<appwidget-provider' in (main/'res/xml/widget_compact.xml').read_text()
assert '<appwidget-provider' in (main/'res/xml/widget_wide.xml').read_text()
assert '<appwidget-provider' in (main/'res/xml/widget_mini.xml').read_text()
assert 'widget_action' in (main/'java/io/github/polestarcn/companion/WidgetRenderer.java').read_text()
assert not (root/'android-app/local.properties').exists()
assert 'BIOMETRIC_STRONG' in all_java
assert 'setOngoing(true)' in all_java
assert 'TailgateControl' in all_java
print('PASS: source structure, real UI gating, biometric controls, private sessions and widgets; Java count',len(java))


assert 'ACCESS_BACKGROUND_LOCATION' not in manifest
assert 'ACCESS_FINE_LOCATION' in manifest and 'ACCESS_COARSE_LOCATION' in manifest
assert 'restapi.amap.com/v3/staticmap' not in all_java
assert 'addJavascriptInterface' not in all_java
assert 'https://uri.amap.com/marker' in all_java
assert not any('com.amap.api' in line for line in gradle.splitlines())
print('PASS: key-free official map URI, optional foreground location, no map SDK or JavaScript bridge')
