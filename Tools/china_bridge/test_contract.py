#!/usr/bin/env python3
"""Safe offline tests. No Android, login, token, or vehicle commands required."""
import importlib.util
import json
import pathlib
import time
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
BRIDGE_PATH = ROOT / "Tools/china_bridge/polestar_china_bridge.py"
spec = importlib.util.spec_from_file_location("polestar_cn_bridge", BRIDGE_PATH)
bridge = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bridge)


class BridgeFreshnessTests(unittest.TestCase):
    def setUp(self):
        bridge._update_battery(None)

    def sample(self, delta_ms=0):
        return {
            "reportedAtMs": int(time.time() * 1000) + delta_ms,
            "percentage": 72.0,
            "rangeKm": 300,
            "chargingState": "Unplugged",
        }

    def test_current_provider_time_is_fresh(self):
        now = int(time.time() * 1000)
        self.assertTrue(bridge._battery_is_recent({"reportedAtMs": now}, now))

    def test_old_provider_time_is_cached(self):
        now = int(time.time() * 1000)
        self.assertFalse(bridge._battery_is_recent({"reportedAtMs": now - 700000}, now))
        bridge._update_battery(self.sample(-700000))
        self.assertEqual(bridge._copy_state()["source"], "Cached")

    def test_far_future_time_is_not_fresh(self):
        now = int(time.time() * 1000)
        self.assertFalse(bridge._battery_is_recent({"reportedAtMs": now + 300000}, now))

    def test_absent_timestamp_is_cached(self):
        bridge._update_battery({"percentage": 55.0})
        state = bridge._copy_state()
        self.assertTrue(state["session"]["ready"])
        self.assertEqual(state["source"], "Cached")

    def test_fresh_android_model_is_labeled_live(self):
        bridge._update_battery(self.sample())
        state = bridge._copy_state()
        self.assertTrue(state["session"]["ready"])
        self.assertEqual(state["source"], "Android App Live")
        self.assertEqual(state["battery"]["rangeKm"], 300)

    def test_detachment_is_not_authenticated(self):
        bridge._update_battery(self.sample())
        bridge._update_battery(None)
        state = bridge._copy_state()
        self.assertFalse(state["session"]["ready"])
        self.assertIsNone(state["battery"])

    def test_only_allowlisted_status_fields_are_exposed(self):
        entry = self.sample()
        entry["access_token"] = "DO_NOT_EXPORT"
        entry["location"] = "DO_NOT_EXPORT"
        entry["phone"] = "DO_NOT_EXPORT"
        bridge._update_battery(entry)
        serialized = json.dumps(bridge._copy_state())
        self.assertNotIn("access_token", serialized)
        self.assertNotIn("DO_NOT_EXPORT", serialized)
        self.assertNotIn("phone", serialized)

    def test_default_vehicle_uses_opaque_identifier(self):
        state = bridge._copy_state()
        self.assertEqual(state["vehicle"]["id"], "china-primary")
        self.assertNotIn("vin", json.dumps(state).lower())


class KnowledgeConsistencyTests(unittest.TestCase):
    def setUp(self):
        self.data = json.loads(
            (ROOT / "knowledge/china/capabilities.json").read_text(encoding="utf-8")
        )

    def test_every_cn_capability_is_explicit(self):
        allowed = {"BRIDGE_ONLY", "MAPPING_REQUIRED", "DISABLED", "NOT_APPLICABLE"}
        caps = self.data["capabilities"]
        self.assertGreaterEqual(len(caps), 20)
        self.assertEqual(len({c["name"] for c in caps}), len(caps))
        self.assertTrue(all(c["cn_status"] in allowed for c in caps))

    def test_all_china_controls_stay_disabled(self):
        caps = {c["name"]: c for c in self.data["capabilities"]}
        for name in ["lock_unlock", "remote_climate", "remote_windows_tailgate",
                     "charge_override", "ota"]:
            self.assertEqual(caps[name]["cn_status"], "DISABLED")

    def test_native_china_auth_is_not_claimed_implemented(self):
        caps = {c["name"]: c for c in self.data["capabilities"]}
        self.assertEqual(caps["user_login"]["cn_status"], "MAPPING_REQUIRED")
        self.assertEqual(caps["c3_discovery"]["cn_status"], "MAPPING_REQUIRED")
        self.assertEqual(caps["pccs_chronos"]["cn_status"], "MAPPING_REQUIRED")

    def test_endpoint_entries_are_public_hostnames_only(self):
        endpoints = self.data["china_endpoints"]
        self.assertGreaterEqual(len(endpoints), 5)
        for ep in endpoints:
            self.assertNotIn("@", ep["address"])
            self.assertNotIn("Bearer ", ep["address"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
