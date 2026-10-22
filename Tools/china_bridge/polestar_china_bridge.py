#!/usr/bin/env python3
"""
Polestar China localhost read-only bridge for Hisingen China 0.18.0.

Data source:
  already signed-in official Android app com.polestar.china.android

Security boundary:
  - binds only to 127.0.0.1
  - reads vehicle status only
  - does not read/export access tokens, refresh tokens, cookies, SMS/2FA codes,
    phone numbers, full VIN, precise location, or Digital Key secrets
  - sends no remote vehicle command

Verified with China Android app 4.25.0-262 / Frida 17.24.3 battery model mapping.
"""
import argparse
import json
import re
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOST = "127.0.0.1"
PORT = 28765
PACKAGE = "com.polestar.china.android"

_state_lock = threading.Lock()
_state = {
    "version": 1,
    "session": {"ready": False, "source": "official-android-app"},
    "vehicle": {"id": "china-primary", "title": "Polestar China", "modelName": "Polestar 2", "modelYear": None},
    "battery": None,
    "exterior": None,
    "odometerKm": None,
    "source": "Android App Live",
}

FRIDA_JS = r"""
'use strict';

function getField(o, name) {
    if (!o) return null;
    var c = o.getClass();
    while (c) {
        try {
            var f = c.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(o);
        } catch (_) {
            try { c = c.getSuperclass(); } catch (_) { c = null; }
        }
    }
    return null;
}

function boxedNumber(v) {
    if (v === null || v === undefined) return null;
    try { return Number(v.toString()); } catch (_) { return null; }
}

function parseToken(text, name) {
    var m = new RegExp(name + '=([^,\\)]+)').exec(text || '');
    return m ? m[1] : null;
}

rpc.exports = {
    snapshot: function () {
        return new Promise(function (resolve) {
            Java.perform(function () {
                var found = null;
                try {
                    Java.choose('com.polestar.explore.domain.carcontrol.battery.CarBatteryInteractorImpl', {
                        onMatch: function (instance) {
                            if (found !== null) return;
                            try {
                                var model = getField(instance, 'clicase');
                                if (!model) return;
                                var text = model.toString();
                                found = {
                                    reportedAtMs: boxedNumber(getField(model, 'clido')),
                                    percentage: boxedNumber(getField(model, 'cliif')),
                                    rangeKm: boxedNumber(getField(model, 'clicatch')),
                                    rangeMi: boxedNumber(getField(model, 'cliclass')),
                                    estimatedChargingTimeMinutes: boxedNumber(getField(model, 'clifor')),
                                    averageConsumption: boxedNumber(getField(model, 'cliint')),
                                    chargingState: parseToken(text, 'batteryChargingState'),
                                    chargingPowerWatts: null,
                                    chargingCurrentAmps: null,
                                    chargingVoltageVolts: null,
                                    chargingType: parseToken(text, 'batteryChargingType'),
                                    chargerPowerStatus: parseToken(text, 'chargerPowerStatus')
                                };
                            } catch (_) {}
                        },
                        onComplete: function () { resolve(found); }
                    });
                } catch (_) {
                    resolve(null);
                }
            });
        });
    }
};
"""

def _copy_state():
    with _state_lock:
        return json.loads(json.dumps(_state))

def _update_battery(battery):
    with _state_lock:
        _state["battery"] = battery
        _state["session"]["ready"] = battery is not None

def _frida_worker():
    try:
        import frida
    except Exception as exc:
        print(f"[bridge] frida Python module unavailable: {exc}", file=sys.stderr)
        return

    while True:
        session = None
        try:
            device = frida.get_usb_device(timeout=5)
            pid = None
            try:
                for app in device.enumerate_applications():
                    if getattr(app, "identifier", None) == PACKAGE and getattr(app, "pid", 0):
                        pid = app.pid
                        break
            except Exception:
                pass
            if pid is None:
                for proc in device.enumerate_processes():
                    if proc.name == PACKAGE:
                        pid = proc.pid
                        break
            if pid is None:
                print("[bridge] official Polestar China app is not running; retrying...")
                _update_battery(None)
                time.sleep(3)
                continue

            session = device.attach(pid)
            script = session.create_script(FRIDA_JS)
            script.load()
            print(f"[bridge] attached read-only to {PACKAGE} pid={pid}")

            while True:
                battery = script.exports_sync.snapshot()
                _update_battery(battery)
                time.sleep(2)
        except KeyboardInterrupt:
            return
        except Exception as exc:
            print(f"[bridge] attach/read failed: {exc}", file=sys.stderr)
            _update_battery(None)
            time.sleep(3)
        finally:
            if session is not None:
                try:
                    session.detach()
                except Exception:
                    pass

class Handler(BaseHTTPRequestHandler):
    server_version = "HisingenChinaBridge/0.18.0"

    def log_message(self, fmt, *args):
        return

    def _send(self, payload, code=200):
        raw = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self):
        if self.path == "/v1/status":
            current = _copy_state()
            self._send({
                "version": 1,
                "ready": current["session"]["ready"],
                "source": current["session"]["source"],
                "batteryAvailable": current["battery"] is not None,
                "exteriorAvailable": current["exterior"] is not None,
                "odometerAvailable": current["odometerKm"] is not None,
            })
            return
        if self.path == "/v1/state":
            self._send(_copy_state())
            return
        self._send({"error": "not_found"}, 404)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mock", action="store_true", help="serve a safe fixture without Android/Frida")
    args = parser.parse_args()

    if args.mock:
        _update_battery({
            "reportedAtMs": int(time.time() * 1000),
            "percentage": 72.0,
            "rangeKm": 300,
            "rangeMi": 180,
            "estimatedChargingTimeMinutes": 0,
            "averageConsumption": 19.1,
            "chargingState": "Unplugged",
            "chargingPowerWatts": None,
            "chargingCurrentAmps": None,
            "chargingVoltageVolts": None,
            "chargingType": "Unknown",
            "chargerPowerStatus": "NoPower",
        })
    else:
        threading.Thread(target=_frida_worker, name="frida-reader", daemon=True).start()

    server = ThreadingHTTPServer((HOST, PORT), Handler)
    print(f"[bridge] read-only localhost API: http://{HOST}:{PORT}")
    print("[bridge] no tokens/VIN/phone/location/commands are exposed")
    server.serve_forever()

if __name__ == "__main__":
    main()
