# PsAssistant · v0.5.0-beta1
Android app original ID remains io.github.polestarcn.companion for update compatibility.

## Changes
- Polestar-inspired darker neutral and high-contrast metric dashboard; configurable vehicle image, charging, timestamp, odometer, door, window and air information
- Independent Widget language (中文 / English / system), sizes 4x2, 2x2, 4x1, 2x1 with lock choice and climate choice shortcuts
- Shortcuts open the real app and require confirmation and strong biometrics before any command is sent. No background PendingIntent directly sends remote writes
- Opaque or translucent Widget, opacity and simulated frosting controls
- Notification now shows vehicle data timestamp, not token refresh timestamp
- Climate Running / verifying states and locally estimated duration only after vehicle state confirmation, with explicit estimated/unknown labels

## Limitations
- Android RemoteViews cannot apply genuine live wallpaper blur. Widget frost is simulated.
- China readback does not expose proven remaining climate duration; the local default 30-min interval is an **estimate** not server truth. If no confirmed local start exists, show unknown.
- Cloud and static tests do not establish user-vehicle remote command success. App still needs original compatible reference APK for first-time account setup.
- The UI design study is independent of Polestar; third-party brand assets require appropriate permission.
