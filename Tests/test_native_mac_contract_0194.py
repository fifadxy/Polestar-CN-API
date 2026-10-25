#!/usr/bin/env python3
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[1]
PATCH = ROOT / "Patches" / "0.19.4"
SESSION = (PATCH / "ChinaPolestarSession.swift").read_text(encoding="utf-8")
API = (PATCH / "ChinaPolestarAPI.swift").read_text(encoding="utf-8")
RUNTIME = (PATCH / "ChinaPolestarRuntime.swift").read_text(encoding="utf-8")
VIEWS = (PATCH / "ChinaPolestarViews.swift").read_text(encoding="utf-8")
APPLY = (ROOT / "Scripts" / "apply_hisingen_china_0194.py").read_text(encoding="utf-8")
WEB = (PATCH / "ChinaPolestarWebSignInWindow.swift").read_text(encoding="utf-8")
CHALLENGE = (PATCH / "ChinaPolestarChallengeWindow.swift").read_text(encoding="utf-8")
PROVISION = (ROOT / "Tools" / "auth_probe" / "provision_cn_protocol_material.py").read_text(encoding="utf-8")


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

    def test_native_phone_login_and_signed_common_parameters(self):
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
            '("appId", clientID)',
            '("timestamp",',
            '("nonce", UUID().uuidString)',
            '("deviceId", stableDeviceID())',
            '("deviceModel", deviceModel)',
            '("brand", "apple")',
            '("clientType", "PHONE")',
            '("systemVer", systemVersion)',
            '("appVer", protocolVersion)',
            'let protocolMaterial = try ChinaPolestarProtocolMaterialStore.loadRequired()',
            'let signSource = protocolMaterial + canonical + protocolMaterial',
            'Insecure.MD5.hash',
            'URLQueryItem(name: "sign", value: signature)',
            'forHTTPHeaderField: "Version"',
            'forHTTPHeaderField: "Device"',
            'forHTTPHeaderField: "X-User-Agent"',
        ]:
            self.assertIn(value, SESSION)
        self.assertNotIn("mobileClientSecret", SESSION)
        self.assertIsNone(re.search(r'(?<![0-9A-F])[0-9A-F]{32}(?![0-9A-F])', SESSION))
        self.assertNotIn('forHTTPHeaderField: "version"', SESSION)
        self.assertIn('service = "io.kheirallah.hisingen.polestar-cn.protocol"', SESSION)
        self.assertIn("requestVerificationCode(", API)
        self.assertIn("completePhoneSignIn(", API)
        self.assertIn("获取验证码", VIEWS)
        self.assertIn("短信验证码", VIEWS)
        self.assertIn("网关协议材料：已就绪", VIEWS)
        self.assertIn("provision_cn_protocol_material.py", VIEWS)

    def test_official_interactive_challenge(self):
        self.assertIn("https://h5.app.polestar-auto.com/account-components/auth-slider/index.html", CHALLENGE)
        self.assertIn('controller.add(self, name: "chromium")', CHALLENGE)
        self.assertIn("window.webkit.messageHandlers.chromium.postMessage", CHALLENGE)
        for key in ["sessionId", "sig", "token"]:
            self.assertIn(f'dictionary["{key}"]', CHALLENGE)
        self.assertIn("websiteDataStore = .nonPersistent()", CHALLENGE)
        self.assertIn("ChinaPolestarChallengeWindow.shared.open", VIEWS)
        self.assertIn("guard let challenge else", VIEWS)
        self.assertIn("请完成极星中国官方滑块验证", VIEWS)
        for forbidden in ["document.cookie", "httpCookieStore", "evaluateJavaScript",
                          "print(", "logger.", "NSLog"]:
            self.assertNotIn(forbidden, CHALLENGE)

    def test_polestar_id_diagnostic_uses_real_cn_host(self):
        self.assertIn("https://polestarid.polestar.cn", WEB)
        self.assertNotIn("https://www.polestar.cn/zh-cn/auth/login/", WEB)
        self.assertIn("ADVANCED_DIAGNOSTIC_ONLY", WEB)

    def test_local_protocol_material_provisioner(self):
        self.assertIn("Lcom/polestar/base/utils/a1;", PROVISION)
        self.assertIn("gateway-client-material-v1", PROVISION)
        self.assertIn("value not displayed", PROVISION)
        self.assertIn("security", PROVISION)
        self.assertNotRegex(PROVISION, r'(?<![0-9A-F])[0-9A-F]{32}(?![0-9A-F])')

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
