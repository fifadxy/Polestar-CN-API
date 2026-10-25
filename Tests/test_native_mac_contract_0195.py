#!/usr/bin/env python3
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[1]
PATCH = ROOT / "Patches" / "0.19.5"
SESSION = (PATCH / "ChinaPolestarSession.swift").read_text(encoding="utf-8")
API = (PATCH / "ChinaPolestarAPI.swift").read_text(encoding="utf-8")
RUNTIME = (PATCH / "ChinaPolestarRuntime.swift").read_text(encoding="utf-8")
VIEWS = (PATCH / "ChinaPolestarViews.swift").read_text(encoding="utf-8")
CHALLENGE = (PATCH / "ChinaPolestarChallengeWindow.swift").read_text(encoding="utf-8")
APPLY = (ROOT / "Scripts" / "apply_hisingen_china_0195.py").read_text(encoding="utf-8")
PROVISION = (ROOT / "Tools" / "auth_probe" / "provision_cn_oauth_secret.py").read_text(encoding="utf-8")


class NativeMacContractTests(unittest.TestCase):
    def test_verified_phone_login_contract(self):
        for value in [
            "user/api/v1/credcode/send",
            "user/api/v2/oauth/login/phone",
            '"principal": normalized',
            '"aliScene": "ic_login_h5"',
            '"aliSessionId": challenge.sessionID',
            '"aliSign": challenge.signature',
            '"aliToken": challenge.token',
            '"clientId": Self.clientID',
            '"type": "SIGNIN_BY_PHONE"',
            '"phone": normalized',
            '"credential": cleanCode',
            '"autocrt": true',
            'forHTTPHeaderField: "Version"',
            'forHTTPHeaderField: "Device"',
            'forHTTPHeaderField: "X-User-Agent"',
        ]:
            self.assertIn(value, SESSION)
        for forbidden in [
            "signedGatewayURL", "Insecure.MD5", "stableDeviceID",
            'URLQueryItem(name: "sign"', "X-SIGNATURE", "X-TIMESTAMP",
        ]:
            self.assertNotIn(forbidden, SESSION)

    def test_official_interactive_challenge(self):
        self.assertIn(
            "https://h5.app.polestar-auto.com/account-components/auth-slider/index.html",
            CHALLENGE,
        )
        self.assertIn('controller.add(self, name: "chromium")', CHALLENGE)
        for key in ["sessionId", "sig", "token"]:
            self.assertIn(f'dictionary["{key}"]', CHALLENGE)
        self.assertIn("websiteDataStore = .nonPersistent()", CHALLENGE)
        self.assertIn("ChinaPolestarChallengeWindow.shared.open", VIEWS)

    def test_oauth_material_is_refresh_only(self):
        self.assertIn("ChinaPolestarOAuthMaterialStore.loadRequired()", SESSION)
        self.assertIn('service = "io.kheirallah.hisingen.polestar-cn.oauth"', SESSION)
        self.assertIn('account = "local-refresh-client-secret-v1"', SESSION)
        self.assertIn('URLQueryItem(name: "client_secret", value: oauthClientSecret)', SESSION)
        before_refresh = SESSION.split("func refreshLocalSession", 1)[0]
        self.assertNotIn("ChinaPolestarOAuthMaterialStore.loadRequired()", before_refresh)
        self.assertNotIn("!hasOAuthMaterial ||", VIEWS)
        self.assertIn("首次短信登录不需要额外材料", VIEWS)
        self.assertIn("Lcom/polestar/base/di/clishort;", PROVISION)
        self.assertIn('TARGET_METHOD = "clitry"', PROVISION)
        self.assertNotRegex(SESSION + PROVISION, r"(?<![0-9A-F])[0-9A-F]{32}(?![0-9A-F])")

    def test_repository_tree_has_no_embedded_32_hex_material(self):
        pattern = re.compile(r"(?<![0-9A-F])[0-9A-F]{32}(?![0-9A-F])")
        findings = []
        for path in ROOT.rglob("*"):
            if not path.is_file() or ".git" in path.parts:
                continue
            try:
                text = path.read_text(encoding="utf-8")
            except (UnicodeDecodeError, OSError):
                continue
            if pattern.search(text):
                findings.append(str(path.relative_to(ROOT)))
        self.assertEqual([], findings)

    def test_token_families_and_rotating_refresh_fallbacks(self):
        for value in [
            'root["globleToken"]',
            "user/api/v2/oauth/refresh-global-token",
            'appendingPathComponent("oauth/token")',
            '"grant_type", value: "refresh_token"',
            "fallbackRefreshToken: current.localRefreshToken",
            "fallbackRefreshToken: current.globalRefreshToken",
            "fallbackIDToken: current.globalIDToken",
            "refreshedLocalToken",
            "let refreshMargin: TimeInterval = 60",
        ]:
            self.assertIn(value, SESSION + API)
        self.assertNotIn("status == 400 || status == 401", SESSION)

    def test_public_dynamic_c3_discovery(self):
        self.assertIn('private static let c3Discovery = URL(string: "https://cnepmob.volvocars.com.cn")!', SESSION)
        self.assertIn('let value = c3["url"] as? String', SESSION)
        self.assertIn('url.scheme?.lowercased() == "https"', SESSION)
        self.assertIn('c3REST.appendingPathComponent("car-information/car")', SESSION)
        discovery = SESSION.split("private func discoverC3RESTBaseURL", 1)[1].split("static func normalizedPhone", 1)[0]
        self.assertNotIn("Authorization", discovery)
        cars = SESSION.split("func fetchCars", 1)[1].split("static func normalizedPhone", 1)[0]
        self.assertNotIn("psoneid", cars)
        self.assertNotIn("https://cepmobtoken.prod.c3.volvocars.com.cn", SESSION)
        self.assertIn("sendsAuthorizationForDiscovery: false", API)
        self.assertIn("sendsAuthorizationForDiscovery: Bool = true", APPLY)
        self.assertIn("if sendsAuthorizationForDiscovery", APPLY)

    def test_vehicle_discovery_and_capability_routing(self):
        for value in [
            '"vinCode"', '"vehicleIdentificationNumber"', '"vehicles"', '"licensePlate"',
            "supportsGlobalChargeAmperageLimitSetting",
            "supportsTargetChargeLevelSetting",
            "supportsGlobalChargeTimer",
            "supportsChargeLocations",
            "supportsParkingClimatizationTimerSettings",
        ]:
            self.assertIn(value, SESSION + API)
        self.assertIn("needsCharging && supportsTargetSoc", API)
        self.assertIn("needsCharging && supportsAmpLimit", API)
        self.assertNotIn("Dictionary(uniqueKeysWithValues:", API)

    def test_native_session_verification_is_exposed(self):
        self.assertIn("func verifyStoredSession()", API)
        self.assertIn("try await loadCarsWithTokenRecovery()", API)
        self.assertIn("ChinaPolestarAPI.shared.verifyStoredSession()", VIEWS)
        self.assertIn("会话有效，未发现已绑定车辆", VIEWS)

    def test_session_and_region_isolation(self):
        self.assertIn('service = "io.kheirallah.hisingen.polestar-cn"', SESSION)
        self.assertIn('account = "native-session-v1"', SESSION)
        self.assertIn("kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly", SESSION)
        for value in [
            "restoreCachedState: false", "mayRestoreCachedSnapshot",
            "markCachedSnapshotRegion", "clearRegionScopedSessionCache",
        ]:
            self.assertIn(value, APPLY + RUNTIME)

    def test_no_runtime_bridge_or_cookie_session(self):
        corpus = "\n".join([SESSION, API, RUNTIME, VIEWS])
        for value in ["127.0.0.1", "28765", "ChinaBridge", "httpCookieStore", "document.cookie"]:
            self.assertNotIn(value, corpus)
        self.assertFalse((ROOT / "Tools" / "china_bridge").exists())

    def test_remote_commands_fail_closed(self):
        self.assertRegex(API, re.compile(r"func executeRemoteCommand[\s\S]+?throw RemoteCommandError\.unsupported"))
        self.assertIn("ChinaPolestarRuntime.currentRegion == .mainlandChina", APPLY)

    def test_global_defaults_and_version_are_preserved(self):
        self.assertIn('discoveryURL: URL = URL(string: "https://cnepmob.volvocars.com")!', APPLY)
        self.assertIn('pccsURL: URL = URL(string: "https://api.pccs-prod.plstr.io:443")!', APPLY)
        self.assertIn('patchVersion = "0.19.5"', RUNTIME)
        self.assertIn('info["HisingenChinaPatchVersion"] = "0.19.5"', APPLY)


if __name__ == "__main__":
    unittest.main()
