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
  assert body(blocks[name],method)==body(actual,method),(name,method)
  count+=1
module=(root/'android-app/oppoBridge/src/main/java/io/github/polestarcn/oppobridge/uploaded/OppoCarLinkBridgeModule.java').read_text()
for name in ['hookSystemServerPermissionGrant','hookLocalIotPermissionAndSignatures','hookOfficialDatabaseAndWhitelist','hookHasCarBindingCheck','hookVirtualVehicleSession','hookFourCarControlEvents','extractCarCommandFromArgs','routeCarEventToPsAssistant']:
 assert body(blocks['OppoCarLinkBridgeModule'],name)==body(module,name),name
 count+=1
print('PASS:',count,'uploaded business methods unchanged; namespace, typed access and hook API transport are the explicit adapters')
