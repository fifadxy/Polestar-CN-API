#!/usr/bin/env python3
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[1]
PATCH = ROOT / "Patches" / "0.19.3"
SESSION = (PATCH / "ChinaPolestarSession.swift").read_text(encoding="utf-8")
API = (PATCH / "ChinaPolestarAPI.swift").read_text(encoding="utf-8")
RUNTIME = (PATCH / "ChinaPolestarRuntime.swift").read_text(encoding="utf-8")
VIEWS = (PATCH / "ChinaPolestarViews.swift").read_text(encoding="utf-8")
APPLY = (ROOT / "Scripts" / "apply_hisingen_china_0193.py").read_text(encoding="utf-8")
WEB = (PATCH / "ChinaPolestarWebSignInWindow.swift").read_text(encoding="utf-8")


class NativeMacContractTests(unittest.TestCase):
    def test_deferred_visible_onboarding(self):
        self.assertIn("static var needsInitialRegionSelection: Bool", RUNTIME)
        self.assertIn("setRegion(selected)", RUNTIME)
        self.assertIn("ChinaPolestarRuntime.needsInitialRegionSelection", APPLY)
        self.assertIn("ChinaPolestarRuntime.ensureInitialRegionSelection()", APPLY)
        self.assertIn("DispatchQueue.main.async", APPLY)
        self.assertIn("self.showSettingsWindow()", APPLY)
        self.assertIn("self?.showSettingsWindow()", APPLY)
        self.assertNotIn("setRegion(selected, notify: false)", RUNTIME)

    def test_official_china_website_flow(self):
        for term in ["https://www.polestar.cn/zh-cn/auth/login/",
                     "WKWebView", "WKWebViewConfiguration",
                     "websiteDataStore = .default()", "NSWindow"]:
            self.assertIn(term, WEB)
        self.assertIn("ChinaPolestarWebSignInWindow.shared.open()", VIEWS)
        self.assertIn("NSWorkspace.shared.open(ChinaPolestarWebSignInWindow.officialLoginURL)", VIEWS)
        self.assertIn("车联 API：尚未获得独立授权", VIEWS)
        self.assertIn("网页登录成功并不代表获得 C3/PCCS 车辆访问权限", VIEWS)
        self.assertNotIn("requestVerificationCode(phone:", SESSION)
        self.assertNotIn("user/api/v2/oauth/login/phone", SESSION)
        self.assertNotIn("completePhoneSignIn(", API)
        self.assertIn("throw PolestarError.authenticationRequired(.noStoredSession)", API)
        self.assertIn('copy_patch("ChinaPolestarWebSignInWindow.swift"', APPLY)

    def test_redacted_web_diagnostics(self):
        self.assertIn("web_to_vehicle_grant=NOT_IMPLEMENTED", WEB)
        self.assertIn("vehicle_api_auth=NOT_VERIFIED", WEB)
        self.assertIn("last_http_status=", WEB)
        self.assertIn("lastSafeHost", WEB)
        self.assertIn("lastHTTPStatus", WEB)
        self.assertIn("复制脱敏登录诊断", VIEWS)
        self.assertIn("NSPasteboard.general", VIEWS)
        for dangerous in ["url.absoluteString", "navigationResponse.response.allHeaderFields",
                          "httpCookieStore", "evaluateJavaScript", "document.cookie"]:
            self.assertNotIn(dangerous, WEB)

    def test_website_cannot_impersonate_vehicle_session(self):
        for term in ["httpCookieStore", "evaluateJavaScript", "document.cookie",
                     "kSecValueData", "ChinaPolestarSessionStore.save",
                     "127.0.0.1", "ChinaBridge"]:
            self.assertNotIn(term, WEB)

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
        forbidden = [
            "127.0.0.1",
            "28765",
            "ChinaBridge",
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

    def test_remote_commands_fail_closed(self):
        self.assertRegex(
            API,
            re.compile(r"func executeRemoteCommand[\s\S]+?throw RemoteCommandError\.unsupported"),
        )
        self.assertIn("ChinaPolestarRuntime.currentRegion == .mainlandChina", APPLY)
        self.assertIn("return false", APPLY)

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
