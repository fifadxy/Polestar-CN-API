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
assert 'Select Polestar Region' in all_java
assert 'China' in all_java and 'Global' in all_java
assert 'android.permission.INTERNET' in manifest
assert 'class MainActivity' in all_java
assert 'WidgetRenderer.update' in all_java
assert 'class VehicleWidgetProvider' in all_java
assert 'class CompactWidgetProvider' in all_java
reference=(main/'java/io/github/polestarcn/companion/auth/ReferenceSignatureProvider.java').read_text()
assert 'app.c3garage.companion' in reference  # only used to locate the user-installed reference APK
other_java='\n'.join(p.read_text() for p in java if p.name != 'ReferenceSignatureProvider.java')
assert 'app.c3garage.companion' not in other_java
assert not re.search(r'["\\\'][0-9A-F]{32}["\\\']', all_java), 'fixed compatibility signing material must not be committed'
assert 'ChinaLiveActivity' in manifest
assert 'grpc-okhttp:1.68.2' in gradle
assert "versionName '0.3.0-alpha2'" in gradle
assert 'VehicleSnapshotStore.saveChina' in all_java
assert 'secret + canonical + secret' in all_java
assert 'ProtoWire.stringField(2, vin)' in all_java
assert '<appwidget-provider' in (main/'res/xml/widget_standard.xml').read_text()
assert '<appwidget-provider' in (main/'res/xml/widget_compact.xml').read_text()
assert not (root/'android-app/local.properties').exists()
print('PASS: source structure, demo gating, regions, widgets, deferred R8; Java count',len(java))
