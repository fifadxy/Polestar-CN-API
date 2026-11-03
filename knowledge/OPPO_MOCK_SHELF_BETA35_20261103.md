# beta35 Mock / Shelf integration

Base: beta34 b9491a235f1154d2add44e569739f604406fd834. App 0.9.7-beta35-mock-shelf /149, Bridge 1.20.0 /32.

## Confirmed defects and changes

* beta34 Mock OCS used an in-process fixture but the IPC snapshot, virtual session and command gateway still used the real coordinator. These now select the same Mock fixture when OPPO_SDK_MOCK is enabled. Mock commands cannot fall through into real vehicle writes.
* Supplied MyDevices APK declares `DeviceQuickAppCardWidgetProvider.handleClickEvent(String, Bundle): Bundle`. Previous dispatch scanning excluded Bundle return methods. The new hook uses this exact provider endpoint and requires `deviceId` to equal the current projected vehicle plus one of four known CAR shortcut IDs.
* Supplied 17.4.15 RPK CAR module calls free `clickEvent(...)` without importing public/api.js. The exact SHA-gated patch now inserts the module-local API binding as well as the CAR event whitelist. The patch still depends on observing the actual runtime ZipFile entry loader; static support is not proof that runtime patching ran.
* Card data publishes top-level connectState and current lock/climate shortcut states. Own-card failures return a reason instead of falling through into a second-level OEM page. Duplicate clicks are consumed.
* Mock stages wait for observed init/auth/bind/subscription and task-correlated command receipts, with bounded local-only timeouts. Reset stops the runtime and invalidates delayed writes. A successful onShowCard callback no longer claims that an OEM card was actually rendered.

## Test on phone

1. Install the beta35 Mock APK. If Android reports a signature mismatch, retain your current installation/data and use the signed upgrade artifact once available; do not uninstall merely to bypass this check.
2. Enable its integrated LSPosed module for the existing MyDevices, AssistantScreen and QuickApp runtime scopes. Do not enable a second standalone bridge simultaneously. Restart the affected host processes or reboot once.
3. Open PsAssistant > OPPO/CarLink setup; complete the existing first-use confirmation. Tap **一键完整 Mock OCS 测试**. Expect init, binding, subscription and eight simulated action receipts, with explicit PASS/FAIL.
4. Add/reselect **Polestar 2 TestLab** in the device-space 4x2 card on the negative screen. Tap the actual lock or climate button, not the whole card. Tap again after at least five seconds. The fixture should change and the provider notifies observers. No real car write occurs.
5. Return to setup and tap **复制本次测试及负一屏诊断**. Check the event/command timestamps against this tap, rather than treating an old true flag as a new pass. Text export is small; the existing bounded root export remains available for loader/host failures.

## Interpreting results

* Mock suite pass only: protocol callbacks and local fixture work.
* No new carEventReceived timestamp: card did not emit/reach the exact hooked provider; inspect RPK patch applied, live process/module version, and hook installation failure.
* Event received, pending intent not sent: inspect session/gateway rejection reason.
* Pending intent sent, no command acceptance: inspect Activity delivery and current session/vehicle validation.
* Mock receipt/state update: the local end-to-end card command path works. It does not grant safe.IOT, produce OPPO credentials, bind OPPO cloud or establish watch support.
* Official SDK builds retain their real coordinator path. Moving to real-car verification requires a separate observed command/readback; do not infer it from Mock pass.

No timer polls the car. The test's 50ms waits inspect local Mock state only and stop on completion or timeout. No new vehicle images or heavy runtime dependency are added.
