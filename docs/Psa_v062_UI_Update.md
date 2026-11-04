# Psa v0.6.2 UI update

Android branch: `android-polestar-assistant-main`. Application version: `0.6.2-beta1`, versionCode `62`.

## Home and controls

- Portrait control cards are 76 dp high, roughly half the previous height on a typical phone. All four remain on one page. Home supports up to 12 selected fields; existing saved field choices are retained. The default selection now contains nine fields.
- Door/window and camping shortcuts have separate spacing, thin outlines and explicit labels. An open/ajar door, window or tailgate turns the body shortcut orange and adds `!`. Camping active/stopping turns its shortcut orange.
- Four main controls select the opposite of the current known state and open strong biometric verification directly. The subtitle names the exact action, including start/stop or lock/unlock. Cancel, failed verification, backgrounding or changing the selected vehicle never authorizes a command. Unknown/processing states ask for a refresh rather than guessing. Widget lock/climate entries use the same direct path.
- Closed/off control states remain white; active/unlocked states are orange. Pending feedback does not alter confirmed vehicle state. The black vehicle icon from v0.6.1 remains the shared Android icon; see [icon convention](../branding/android/README.md).

## Command confirmation

After a successful send, the app displays a waiting state and schedules its first vehicle read after 15 seconds. If the requested state is absent or reading fails, at most two further read attempts follow, five seconds apart after each read completes. The timer uses the main Handler and network work uses the existing worker queue; the UI is not blocked by a sleep. Only a live matching vehicle state produces confirmation. Timeouts are labelled unconfirmed and no control command is replayed.

Readback uses the normal session logic: access tokens refresh when due. Refreshing an unexpired token is not treated as vehicle confirmation. Transport-uncertain sends also wait and check state without retrying the write. Camping confirmation likewise waits 15 seconds before its first state read; its existing cancellation and battery checks remain in place.

## Door and window diagram

The diagram reuses the original template top-view vehicle artwork, rendered with the front upward. Four callouts mark front-left, front-right, rear-left and rear-right door/window pairs. Orange identifies open/ajar items, `--` is unknown, and tailgate status is shown separately. Accessibility text includes all eight states and the tailgate. The drawing uses the existing resource rather than adding another large car asset.

## Haptics

Settings → Button haptics provides an on/off switch, 1–100% strength and a test button. App buttons, native app dialog choices/buttons, switches and slider release share the same short pulse. The system haptics switch and hardware availability are respected. Phones with amplitude control receive a strength-scaled amplitude; other phones use a short duration approximation. The OS biometric UI keeps its system behavior.

## Verification and previews

Local validation: 50 tests, including small-screen/large-text and landscape layouts, all four direct biometric cancellation paths, delayed/bounded readback, unknown/cached-state rejection and haptics off/strength fallback. Lint has zero errors; existing warnings remain. APK signature verification passes. Synthetic fixture data in the screenshots demonstrates layout and states; it is not evidence of a real vehicle command.

Native view renders at 1080 × 2340:

- [Home](previews/v062/home.png)
- [Door/window positions](previews/v062/exterior.png)
- [Camping settings](previews/v062/camping.png)

The previews are exported by `AppRegressionTest.exportThreeNativePagePreviewsAndOpeningBadges` into `android-app/app/build/reports/previews/v062-*.png`; the copies here match the delivered build. Account/vehicle and OPPO Watch X2 device acceptance remain pending. This is still the phone APK, not a separate Wear OS application.
