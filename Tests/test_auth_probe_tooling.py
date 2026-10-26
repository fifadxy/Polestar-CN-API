#!/usr/bin/env python3
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]
PROBE = ROOT / "Tools" / "auth_probe"
PREFLIGHT = (PROBE / "root_device_preflight_mac.sh").read_text(encoding="utf-8")
START = (PROBE / "start_pcapdroid_root_mac.sh").read_text(encoding="utf-8")
STOP_FRIDA = (PROBE / "stop_root_frida_mac.sh").read_text(encoding="utf-8")
SESSION = (PROBE / "capture_root_metadata_mac.sh").read_text(encoding="utf-8")
README = (PROBE / "README.md").read_text(encoding="utf-8")
EMULATOR_SETUP = (PROBE / "setup_arm64_bridge_emulator_linux.sh").read_text(
    encoding="utf-8"
)
EMULATOR_RUN = (PROBE / "run_arm64_bridge_emulator_linux.sh").read_text(
    encoding="utf-8"
)
EMULATOR_PREFLIGHT = (PROBE / "emulator_native_bridge_preflight.sh").read_text(
    encoding="utf-8"
)
EMULATOR_TCG_PREPARE = (PROBE / "prepare_tcg_emulator_install.sh").read_text(
    encoding="utf-8"
)


class AuthProbeToolingTests(unittest.TestCase):
    def test_preflight_requires_owned_arm64_root_target(self):
        for value in [
            "adb shell su -c id",
            'POLESTAR_PACKAGE="com.polestar.china.android"',
            'PCAPDROID_PACKAGE="com.emanuelef.remote_capture"',
            '[[ "$abi" != "arm64-v8a" ]]',
        ]:
            self.assertIn(value, PREFLIGHT)
        self.assertNotIn("ro.serialno", PREFLIGHT)

    def test_pcapdroid_is_metadata_only_and_per_app(self):
        for value in [
            "--ez root_capture true",
            "-e capture_interface @inet",
            "-e pcap_dump_mode none",
            "-e app_filter com.polestar.china.android",
            "--ez tls_decryption false",
            "--ez full_payload false",
        ]:
            self.assertIn(value, START)
        for forbidden in [
            "pcap_file",
            "udp_exporter",
            "tcp_exporter",
            "--ez tls_decryption true",
            "--ez full_payload true",
            "api_key",
        ]:
            self.assertNotIn(forbidden, START)

    def test_coordinated_session_always_stops_pcapdroid(self):
        self.assertIn("trap cleanup EXIT", SESSION)
        self.assertIn("trap on_signal HUP INT TERM", SESSION)
        self.assertIn('"$DIR/capture_metadata_mac.sh"', SESSION)
        self.assertIn('"$DIR/stop_pcapdroid_root_mac.sh"', SESSION)

    def test_frida_stop_is_limited_to_provisioned_binary(self):
        self.assertIn("/data/local/tmp/hisingen-frida-server", STOP_FRIDA)
        self.assertIn('readlink "$proc/exe"', STOP_FRIDA)
        self.assertNotIn("pkill", STOP_FRIDA)
        self.assertNotIn("killall", STOP_FRIDA)

    def test_readme_keeps_sensitive_outputs_local(self):
        for value in [
            "device-side consent",
            "No packet file",
            "Do not share the screen recording",
            "Do not use SSL-unpinning",
        ]:
            self.assertIn(value, README)

    def test_emulator_setup_pins_proven_native_bridge_image(self):
        for value in [
            "system-images;android-${api};google_apis;x86_64",
            'ANDROID_BRIDGE_API:-30',
            'disk.dataPartition.size',
            'hw.ramSize',
        ]:
            self.assertIn(value, EMULATOR_SETUP)

    def test_emulator_runner_is_headless_and_can_disable_acceleration(self):
        for value in [
            "-no-window",
            "-writable-system",
            'ANDROID_BRIDGE_ACCEL:-off',
            'ANDROID_BRIDGE_PORT:-5558',
            'ANDROID_BRIDGE_WRITABLE_SYSTEM:-0',
        ]:
            self.assertIn(value, EMULATOR_RUN)

    def test_emulator_preflight_proves_root_and_arm64_translation(self):
        for value in [
            "adb_cmd root",
            "ro.product.cpu.abilist",
            "arm64-v8a",
            "ro.dalvik.vm.native.bridge",
            "ro.enable.native.bridge.exec",
            "/system/lib64/$native_bridge",
            "runtime_mode=x86_64-host-with-arm64-translation",
        ]:
            self.assertIn(value, EMULATOR_PREFLIGHT)
        self.assertNotIn("ro.serialno", EMULATOR_PREFLIGHT)

    def test_tcg_install_preparation_waits_for_framework_and_skips_dex_verification(self):
        for value in [
            "Service package: found",
            "Service activity: found",
            "private mounted",
            "init.svc.bootanim",
            "pm.dexopt.install extract",
        ]:
            self.assertIn(value, EMULATOR_TCG_PREPARE)
        for forbidden in ["curl ", "wget ", "resetprop", "ro.serialno"]:
            self.assertNotIn(forbidden, EMULATOR_TCG_PREPARE)


if __name__ == "__main__":
    unittest.main()
