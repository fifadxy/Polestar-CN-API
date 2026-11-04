#!/usr/bin/env python3
"""Compare the uploaded business methods with their packaged copies; API adapters have explicit scope."""
import re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
raw=(root/'knowledge/incoming/CARLINK_DUAL_ROUTE_USER_SOURCE_20261104.txt').read_text()
blocks={re.search(r'public (?:final )?class (\w+)',b).group(1):b for b in re.findall(r'```java\n(.*?)```',raw,re.S) if re.search(r'public (?:final )?class (\w+)',b)}
app=root/'android-app/app/src/main/java/io/github/polestarcn/companion/uploadedcarlink'
def body(source,name):
 m=re.search(r'\b'+re.escape(name)+r'\s*\([^)]*\)\s*(?:throws [^{]+)?\{',source)
 if not m:raise AssertionError(name)
 at=m.end();level=1;i=at
 while level:
  level+=(source[i]=='{')-(source[i]=='}');i+=1
 return source[at:i-1]
checks={
 'OppoOfficialVehicleBindState':['hasCarBinding','updateBoundVin'],
 'OppoCarLinkCommandMap':['resolve','resolveFromSdkCommandName'],
 'VirtualVehicleSessionManager':['buildSessionBundle','applyOptimisticUpdate','loadPersistedState','persistState','notifySystemSurfaces'],
 'VehicleProtocolGateway':['executeCarEventAsync','dispatchToRealPolestarStack'],
 'PsAssistantBridgeProvider':['call','query'],
 'OppoOfficialCarLink':['onBind','handleIncomingIntent','onTransact'],
}
count=0
for name,methods in checks.items():
 actual=(app/(name+'.java')).read_text()
 for method in methods:
  current=body(actual,method)
  if name=='OppoOfficialCarLink' and method in ('onBind','onTransact'):
   gate='            if (!io.github.polestarcn.companion.OppoDeviceIntegration.enabled(getApplicationContext())) return '+('null' if method=='onBind' else 'false')+';\n'
   if method=='onBind': gate=gate.replace('            if','        if')
   assert current.count(gate)==1,(name,method,'integration gate')
   current=current.replace(gate,'',1)
  assert body(blocks[name],method)==current,(name,method)
  count+=1
module=(root/'android-app/oppoBridge/src/main/java/io/github/polestarcn/oppobridge/uploaded/OppoCarLinkBridgeModule.java').read_text()
for name in ['hookSystemServerPermissionGrant','hookLocalIotPermissionAndSignatures','hookOfficialDatabaseAndWhitelist','hookHasCarBindingCheck','hookVirtualVehicleSession','hookFourCarControlEvents','extractCarCommandFromArgs','routeCarEventToPsAssistant']:
 current=body(module,name)
 if name=='hookLocalIotPermissionAndSignatures':
  # Beta40 narrows the uploaded permission hook to calls attributable to the PsAssistant UID.
  hardened='if (isOppoIotPermission(perm) && isPsAssistantPermissionRequest(param.args)) {'
  assert current.count(hardened)==1,(name,'PsAssistant UID gate')
  current=current.replace(hardened,'if (isOppoIotPermission(perm)) {',1)
 if name=='hookOfficialDatabaseAndWhitelist':
  # Catalog list injection is allowed only while the local PsAssistant vehicle session is active.
  gate='                            Bundle session = syncSessionFromPsAssistant();\n                            if (session == null || !session.getBoolean("active", false)) return;\n'
  assert current.count(gate)==1,(name,'active local vehicle gate')
  current=current.replace(gate,'',1)
 assert body(blocks['OppoCarLinkBridgeModule'],name)==current,(name,'uploaded business body')
 count+=1
assert 'if (session == null || !session.getBoolean("active", false)) return null;' in module
assert 'uidOwnsPsAssistant(context, android.os.Binder.getCallingUid())' in module
print('PASS:',count,'uploaded method bodies match with Beta40 PsAssistant-only security hardening')

import xml.etree.ElementTree as ET
ns="{http://schemas.android.com/apk/res/android}"
application=ET.parse(root/"android-app/app/src/main/AndroidManifest.xml").getroot().find("application")
service=next(x for x in application.findall("service") if x.get(ns+"name")==".uploadedcarlink.OppoOfficialCarLink")
receiver=next(x for x in application.findall("receiver") if x.get(ns+"name")==".OppoCarLinkCommandReceiver")
assert {a.get(ns+"name") for a in service.findall("intent-filter/action")}=={"com.oppo.carlink.action.CAR_CONTROL_SERVICE","com.oplus.carlink.action.CAR_CONTROL_SERVICE","com.coloros.sceneservice.action.CAR_CONTROL","io.github.polestarcn.companion.action.CARLINK_SERVICE"}
assert {a.get(ns+"name") for a in receiver.findall("intent-filter/action")}=={"io.github.polestarcn.companion.action.CARLINK_COMMAND","com.oppo.carlink.action.CAR_CONTROL_SERVICE","com.oplus.carlink.action.CAR_CONTROL_SERVICE","android.intent.action.BOOT_COMPLETED"}
print("PASS: uploaded service and broadcast action sets preserved without a duplicate command receiver")
