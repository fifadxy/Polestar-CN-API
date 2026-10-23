# Hisingen China – Rooted Android auth metadata kit

Purpose: identify only **non-secret request structure** from the official China Android app on a device/account you own. The Mac 0.19.2 website flow does not provide a supported China C3 API grant; earlier direct SMS requests returned "缺少公共参数". We must NOT invent header signatures, extract credentials, bypass CAPTCHA, or replay app sessions.

## Requirements

- Your own rooted Android device with USB debugging and official Polestar China app \`com.polestar.china.android\`.
- Mac with ADB and Frida CLI. Install using:
  \`brew install android-platform-tools pipx\`
  \`pipx install frida-tools\` (if needed \`pipx ensurepath\`, then open a new Terminal).
- Check \`adb devices -l\` and \`adb shell su -c id\` (root should report \`uid=0\`).
- The official app itself should work: sign in and display the car/SOC. Otherwise investigate official-app connectivity first.
- A strong account is vulnerable to repeated login attempts. Do not continuously request SMS codes or repeat failed logins.

## One-time setup

Download this directory from the project GitHub branch, then:

\`\`\`bash
cd Tools/auth_probe
chmod +x *.sh
./collect_device_info.sh
./setup_root_frida_mac.sh
\`\`\`

The setup downloads the matching server from official Frida releases for your device ABI. For general references see https://frida.re/docs/android/ . It runs a debugging service as root, so close it after your session; do not leave it exposed to untrusted networks.

## Capture steps

1. Open the official China Polestar app on the Android device.
2. Run \`./capture_metadata_mac.sh\` in Mac Terminal.
3. In the official app, complete normal login if needed (one attempt). Open the associated-car list and SOC/range screen; perform one manual vehicle-status refresh. Only use your own account/car.
4. Ctrl+C to finish. Review \`china_auth_metadata_*.txt\`, then share that file and \`android_auth_environment.txt\` privately for diagnosis.

### What is recorded?

Only fields: request stage (\`initial\` or \`interceptor\`), HTTP method, official/allowed hostname, sanitized route template, **query parameter names** (not values), **HTTP header names** (not values), **JSON/form field names** (not values), and numeric HTTP status. No response body is read. No full URL with query is logged. No raw credentials/cookies/tokens/VIN/phone/device IDs.

If the app uses a transport other than regular OkHttp, the probe prints "hook unavailable" or shows no calls. That is a *coverage gap*, not evidence there are no calls. Do not bypass application integrity or pinning to force output.

### What we want to determine

- The actual sequence of sign-in / challenges / vehicle-list / battery calls.
- Which **header and JSON parameter names** are added after the request's initial construction.
- Whether the official website and the vehicle client share a *permitted* OAuth code/PKCE authorization flow.
- Whether the backend permits an independent macOS client grant. No app token is copied or imported.

**DO NOT SEND OR PUBLISH**: raw logcat or full HTTP captures, callback URLs containing codes, auth headers, OTP, password, phone/VIN, any JWT/access/refresh tokens, cookies, signing material, location coordinates, Digital Key artifacts, or private application storage. Do not upload private data to public GitHub issues.

## Current status

0.19.3 is a **diagnostic preview**, not an end-to-end China vehicle login fix. A proper implementation needs a supported/authorized China app grant and real-account end-to-end tests: callback → token issue/refresh → linked vehicle → C3 telemetry → restart/reconnect. Remote commands remain disabled.
