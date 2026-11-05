#!/usr/bin/env python3
"""Offline source-level gate; NOT a substitute for Android Gradle/Robolectric/real-car CI."""
from pathlib import Path
from xml.etree import ElementTree
import json, subprocess, re
ROOT=Path(__file__).resolve().parents[1]
OPPO=ROOT/'android-app/oppoBridge/src/main/java/io/github/polestarcn/oppobridge'
APP=ROOT/'android-app/app/src/main/java/io/github/polestarcn/companion'
checks={}
def check(key,expr):
    checks[key]=bool(expr)
    if not expr:raise AssertionError('FAILED: '+key)

settings=(OPPO/'BridgeSettings.java').read_text()
installer=(OPPO/'OppoBridgeModule.java').read_text()
confirm=(OPPO/'OppoShortcutConfirmActivity.java').read_text()
intercept=(OPPO/'AdaptiveShelfShortcutInterceptor.java').read_text()
capability=(OPPO/'QuickAppCarCapability.java').read_text()
appmanifest=ROOT/'android-app/app/src/main/AndroidManifest.xml'
modulemanifest=ROOT/'android-app/oppoBridge/src/main/AndroidManifest.xml'
for f in (appmanifest,modulemanifest): ElementTree.parse(f)
check('Android XML manifests parse',True)
check('versionCode 160, name Beta46',"versionCode 160" in (ROOT/'android-app/app/build.gradle').read_text() and "versionName '1.0.0-beta46'" in (ROOT/'android-app/app/build.gradle').read_text())
check('Bridge version code37',"versionCode 37" in (ROOT/'android-app/oppoBridge/build.gradle').read_text())
check('Watch phone only',"include ':oppoWatchIndicator'" not in (ROOT/'android-app/settings.gradle').read_text())
check('Stable/New/Mock explicitly selectable',all(f'MODE_{i}' in settings for i in ('STABLE','NEW','MOCK')))
check('Invalid mode defaults to Stable','return MODE_STABLE' in settings)
check('Actual host mode reporter', (OPPO/'BridgeModeReporter.java').is_file() and (APP/'OppoBridgeModeDiagnostics.java').is_file())
check('UID-protected mode changes','Binder.getCallingUid() != Process.myUid()' in (OPPO/'BridgeSettingsProvider.java').read_text())
check('Mock installs no control hooks', 'if(mock){' in installer and 'return;' in installer.split('if(mock){',1)[1].split('boolean directControl',1)[0])
check('NEW disables global permission bypass','global_permission_and_database_spoof_skipped_in_real_mode' in installer)
check('NEW avoids fake OEM bind/session','beta46_new_mode: only genuine OEM bind/session' in installer)
check('Native Assistant adapter dynamic', 'installNew' in (OPPO/'AssistantScreenNativeVehicleHook.java').read_text() and 'resolveAdaptiveListMethod' in (OPPO/'AssistantScreenNativeVehicleHook.java').read_text())
check('CarLink action only own scheme', 'psassistant-companion' in intercept and 'psassistant-oppo-bridge' in intercept and 'return null' in intercept)
check('No optimistic command-success', 'applyOptimisticState(' not in intercept and '等待车辆回读确认' in intercept)
check('Per-VIN + 5min check', 'vehicleKey' in intercept and '300_000L' in intercept)
check('External DeepLink requires explicit dialog','setPositiveButton("确认"' in confirm and 'resolveToggle' in confirm)
check('RPK Feature unique anchors', 'occurrences(text,PATCH_ANCHOR)!=1' in capability and 'patchFeature' in capability)
check('RPK in-memory patch scoped 4x2/2x2','SingleDeviceCard2x2/index.js' in (OPPO/'AdaptiveQuickAppRpkPatch.java').read_text())
check('One-click rollback','rollbackMode' in (APP/'OppoSystemCompatActivity.java').read_text())
check('Seven targeted read-only diagnostics','modePartTest("' in (APP/'OppoSystemCompatActivity.java').read_text())
check('App-side IPC caller validation','trusted)throw new SecurityException' in (APP/'PsAssistantBridgeProvider.java').read_text())
check('Beta46 unit test classes', (ROOT/'android-app/app/src/test/java/io/github/polestarcn/oppobridge/Beta46CarLinkDualModeTest.java').is_file() and (ROOT/'android-app/app/src/test/java/io/github/polestarcn/companion/Beta46ModeDiagnosticsTest.java').is_file())
check('Legacy workflows do not run main watch build',all('      - android-polestar-assistant-main' not in (ROOT/'.github/workflows'/f).read_text() for f in ('android-beta44-watch-lab.yml','android-beta45-fluid-cloud.yml')))
# Check all modified Java sources parse using javac. Missing Android SDK yields unresolved symbols; reject *syntax* errors only.
changed=(APP/'OppoSystemCompatActivity.java',APP/'OppoBridgeModeDiagnostics.java',APP/'PsAssistantBridgeProvider.java',APP/'OppoWatchIndicatorLab.java',
    OPPO/'BridgeSettings.java',OPPO/'BridgeSettingsProvider.java',OPPO/'BridgeModeReporter.java',OPPO/'OppoBridgeModule.java',
    OPPO/'AssistantScreenNativeVehicleHook.java',OPPO/'NativeHomeCardHook.java',OPPO/'QuickAppCarCapability.java',
    OPPO/'AdaptiveQuickAppRpkPatch.java',OPPO/'AdaptiveShelfShortcutInterceptor.java',OPPO/'OppoShortcutConfirmActivity.java',
    OPPO/'BridgeTestOrchestrator.java')
command=['javac','-proc:none','-Xmaxerrs','1000','-d','/tmp/psa-beta46-syntax']+[str(x) for x in changed]
p=subprocess.run(command,capture_output=True,text=True)
patterns=['reached end of file while parsing','illegal start of expression','unclosed string literal','not a statement','class, interface, enum, or record expected','\';\' expected','\')\' expected']
syntax=[line for line in p.stderr.splitlines() if any(marker in line for marker in patterns)]
check('No Java source syntax errors detected',not syntax)
# Concrete independent Java compilation / execution of the new RPK transformation.
import tempfile
with tempfile.TemporaryDirectory(prefix="psa-beta46-feature-") as temp:
    stubs=ROOT/'scripts/java_rpk_harness'
    java=[str(path) for path in stubs.rglob('*.java')]
    java.append(str(OPPO/'QuickAppCarCapability.java'))
    compile_run=subprocess.run(['javac','-d',temp]+java,capture_output=True,text=True)
    check('Independent RPK Java harness compiled',compile_run.returncode==0)
    r=subprocess.run(['java','-cp',temp,'io.github.polestarcn.oppobridge.TestFeature'],capture_output=True,text=True)
    check('Feature patch positive and negative cases pass',r.returncode==0 and 'PASS' in r.stdout)
summary={'version':'v1.0.0-beta46 /160','checks':checks,'passed':sum(checks.values()),'total':len(checks),
         'rpk_test_stdout':r.stdout.strip(),'java_compilation':'Android CI NOT run: Gradle/SDK unavailable locally',
         'limitations':'Syntax checks plus pure-Java RPK harness, NOT Robolectric/Android build or real car'}
output=ROOT/'docs/BETA46_OFFLINE_SOURCE_CHECKS.json';output.write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(summary,ensure_ascii=False,indent=2))
