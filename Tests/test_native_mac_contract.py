#!/usr/bin/env python3
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[1]
PATCH = ROOT / "Patches" / "0.19.0"
SESSION = (PATCH / "ChinaPolestarSession.swift").read_text(encoding="utf-8")
API = (PATCH / "ChinaPolestarAPI.swift").read_text(encoding="utf-8")
RUNTIME = (PATCH / "ChinaPolestarRuntime.swift").read_text(encoding="utf-8")
VIEWS = (PATCH / "ChinaPolestarViews.swift").read_text(encoding="utf-8")
APPLY = (ROOT / "Scripts" / "apply_hisingen_china_019.py").read_text(encoding="utf-8")


class NativeMacContractTests(unittest.TestCase):
    def test_phone_login_protocol(self):
        for value in [
            "user/api/v1/credcode/send",
            "user/api/v2/oauth/login/phone",
            '"principal": normalized',
            '"aliScene": "ic_login_h5"',
            '"clientId": Self.clientID',
            '"type": "SIGNIN_BY_PHONE"',
            '"phone": normalized',
            '"credential": cleanCode',
            '"autocrt": true',
        ]:
            self.assertIn(value, SESSION)

    def test_token_families_and_refresh(self):
        for value in [
            'root["globleToken"]',
            "user/api/v2/oauth/refresh-global-token",
            'appendingPathComponent("oauth/token")',
            '"grant_type", value: "refresh_token"',
            '"client_id", value: Self.clientID',
            '"refresh_token", value: current.localRefreshToken',
        ]:
            self.assertIn(value, SESSION)

    def test_china_hosts(self):
        corpus = SESSION + API
        for host in [
            "https://gw.app.polestar-auto.com",
            "https://uc.app.polestar-auto.com",
            "https://cnepmob.volvocars.com.cn",
            "https://cepmobtoken.prod.c3.volvocars.com.cn",
            "https://api.pccs-prod.origin.polestar.cn",
        ]:
            self.assertIn(host, corpus)

    def test_separate_keychain(self):
        self.assertIn('service = "io.kheirallah.hisingen.polestar-cn"', SESSION)
        self.assertIn('account = "native-session-v1"', SESSION)
        self.assertIn("kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly", SESSION)
        self.assertNotIn("errSecEncode", SESSION)
        self.assertNotIn("polestar-refresh-token", SESSION)
        self.assertIn("signOutChinaSession", APPLY)
        self.assertIn("ChinaPolestarSessionStore.delete()", APPLY)
        self.assertIn("ChinaPolestarRuntime.forgetChinaVehicle()", APPLY)

    def test_region_cache_isolation(self):
        self.assertIn("restoreCachedState: false", APPLY)
        self.assertIn("let isChinaRegion", APPLY)
        self.assertIn("mayRestoreCachedSnapshot", APPLY)
        self.assertIn("markCachedSnapshotRegion", APPLY)
        self.assertIn("fleetStore.dropCachedSnapshots(.vehicle", APPLY)
        self.assertIn("clearRegionScopedSessionCache", APPLY)
        self.assertIn("snapshotRegionMarkerPrefix", RUNTIME)

    def test_no_local_bridge_or_web_cookie_path(self):
        corpus = "\n".join([SESSION, API, RUNTIME, VIEWS])
        self.assertFalse((ROOT / "Tools" / "china_bridge").exists())
        forbidden = [
            "127.0.0.1",
            "28765",
            "ChinaBridge",
            "WKWebView",
            "httpCookieStore",
            "evaluateJavaScript",
            "document.cookie",
        ]
        for value in forbidden:
            self.assertNotIn(value, corpus)

    def test_native_vehicle_and_live_paths(self):
        self.assertIn('appendingPathComponent("car-information/car")', SESSION)
        self.assertIn("grpc.fetchBattery", API)
        self.assertIn("grpc.fetchExterior", API)
        self.assertIn("grpc.fetchOdometer", API)
        self.assertIn("grpc.liveUpdates", API)

    def test_china_discovery_is_public_but_global_default_is_unchanged(self):
        self.assertIn("sendsAuthorizationForDiscovery: false", API)
        self.assertIn("sendsAuthorizationForDiscovery: Bool = true", APPLY)
        self.assertIn("if sendsAuthorizationForDiscovery", APPLY)

    def test_vehicle_discovery_accepts_real_world_shapes(self):
        for value in [
            '"vinCode"',
            '"vehicleIdentificationNumber"',
            '"vehicles"',
            '"licensePlate"',
            "return rows.compactMap",
        ]:
            self.assertIn(value, SESSION)
        self.assertNotIn("guard !cars.isEmpty", SESSION)
        self.assertNotIn("Dictionary(uniqueKeysWithValues:", API)

    def test_pccs_reads_follow_vehicle_capabilities(self):
        for value in [
            "supportsGlobalChargeAmperageLimitSetting",
            "supportsTargetChargeLevelSetting",
            "supportsGlobalChargeTimer",
            "supportsChargeLocations",
            "supportsParkingClimatizationTimerSettings",
        ]:
            self.assertIn(value, SESSION)
            self.assertIn(value, API)
        self.assertIn("needsCharging && supportsTargetSoc", API)
        self.assertIn("needsCharging && supportsAmpLimit", API)

    def test_remote_commands_fail_closed(self):
        self.assertRegex(
            API,
            re.compile(r"func executeRemoteCommand[\s\S]+?throw RemoteCommandError\.unsupported"),
        )
        self.assertIn("ChinaPolestarRuntime.currentRegion == .mainlandChina", APPLY)
        self.assertIn("return false", APPLY)

    def test_refresh_preserves_rotating_token_fallbacks(self):
        self.assertIn("fallbackRefreshToken: current.localRefreshToken", SESSION)
        self.assertIn("fallbackRefreshToken: current.globalRefreshToken", SESSION)
        self.assertIn("fallbackIDToken: current.globalIDToken", SESSION)
        self.assertNotIn("status == 400 || status == 401", SESSION)
        self.assertIn("refreshedLocalToken", API)

    def test_global_defaults_are_preserved(self):
        self.assertIn(
            'discoveryURL: URL = URL(string: "https://cnepmob.volvocars.com")!',
            APPLY,
        )
        self.assertIn(
            'pccsURL: URL = URL(string: "https://api.pccs-prod.plstr.io:443")!',
            APPLY,
        )


if __name__ == "__main__":
    unittest.main()
