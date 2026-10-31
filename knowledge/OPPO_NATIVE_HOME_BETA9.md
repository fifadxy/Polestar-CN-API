# beta9 / 120: native MyDevices home content

Exact target: MyDevices 17.4.15. Decompiled HomeCardView.u(QuickCardDeviceData)
sets the bound ID and invokes D(data). D loads the RPK view through RS.f and
installs it through inherited DD.i(View). DD.i removes previous engine/error
content, adds the new view, applies the existing outline and tracks engine content.

The adapter intercepts D only for the PsAssistant package + CAR + psassistant_ ID.
It supplies our native view via the same DD.i(View) lifecycle and records the
same host fields j/i. All other rows and unsupported versions use the original
method. Main provider hook installation is independent of this optional UI hook.

Native content shows cached SOC, range, source time, lock/climate/air/trunk buttons,
and an explicit refresh button. It observes the local bridge only while attached,
uses a background executor, ignores completions from old attachment generations,
and never starts cloud polling on render. Button requests use existing bridge
integration/session/busy/readback checks. Unlock/trunk retain explicit confirmation.
A queued request is never labelled vehicle-confirmed. Demo/missing vehicle data
cannot enable the four command buttons.

This is an Android native content adapter inside the device-space home card;
it does not patch OEM RPK files or register an official car brand. It does not
change the separately hosted Shelf renderer. Real rendering, host measurements,
click routing and actual vehicle response still require target-device testing.

Acceptance: install integrated beta9, restart MyDevices after enabling module scope,
open device-space home, check four controls and cache source time, manually refresh,
then deliberately test lock and read back actual vehicle state. Check another
non-PsAssistant device card still renders normally. Shelf is a separate pending
acceptance surface, as is Watch X2 endurance mode.
